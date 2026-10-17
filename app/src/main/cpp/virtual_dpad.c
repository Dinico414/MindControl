/*
 * virtual_dpad.c - virtual gamepad D-pad for controller-style navigation.
 *
 * Creates a synthetic input device through /dev/uinput that Android classifies as a gamepad.
 * Because the device is tagged SOURCE_GAMEPAD, the event is routed through the system's Focus
 * Navigation Engine instead of being handed to the focused app, which is what makes the arrows
 * behave the same way a physical Xbox controller does.
 *
 * The Microsoft vendor/product id is advertised on purpose so Android binds the stock Xbox
 * keylayout (Vendor_045e_Product_02fd.kl), which maps the HID D-pad usages below to
 * KEYCODE_DPAD_*.
 *
 * Protocol (one command per line on stdin, replies on stdout):
 *   CREATE        -> "CREATED" | "ERROR <detail>"   (D-pad device, no axes)
 *   DESTROY       -> "DESTROYED"
 *   UP|DOWN|LEFT|RIGHT
 *                 -> "OK"      (press + release; a discrete D-pad tap)
 *   PAD_CREATE    -> "CREATED" | "ERROR <detail>"   (resident gamepad device, with axes)
 *   PAD_DESTROY   -> "DESTROYED"
 *   PAD_DOWN <code>   -> "OK"   (press a controller button, HID usage code)
 *   PAD_UP <code>     -> "OK"   (release it)
 *   PAD_STICK <lx> <ly> <rx> <ry>
 *                     -> "OK"   (absolute stick positions, -32767..32767)
 *   PAD_DPAD <x> <y>  -> "OK"   (hat axes: -1, 0 or 1 each; 0,0 is neutral)
 *   PAD_TRIGGER <l> <r>
 *                     -> "OK"   (trigger axes: 0 = released, 255 = fully pressed)
 *   QUIT          -> exits
 *
 * Asynchronous events (not replies to any command, and may arrive at any time):
 *   FFEVT <magnitude> <duration_ms>
 *                 The active app asked the pad to rumble. `<magnitude>` is 0..255 (already
 *                 scaled by any FF_GAIN the app set) and `<duration_ms>` is how long to buzz.
 *                 Magnitude 0 means "stop now".
 *
 * IMPORTANT: FFEVT lines are unsolicited and are interleaved with command replies on the same
 * stdout. A caller that blindly reads one line per command would take an FFEVT as the reply and
 * desync. Callers must skip/route lines starting with "FFEVT" and keep waiting for the reply.
 *
 * Two independent devices are used on purpose and never shared:
 *
 *   * The D-pad device (g_fd) has no axes. Registering axes makes Android classify the device as
 *     a joystick, and in that shape the D-pad keycodes stop reaching the Focus Navigation Engine,
 *     which is what makes arrow-key navigation work at all. That shape must not be changed.
 *   * The gamepad device (g_pad_fd) does have axes, because controller testers detect a
 *     "connected" pad by enumerating AXIS_X / AXIS_Y. It also advertises force-feedback, so games
 *     treat it as a rumble-capable pad; its FF events are read back here and reported as FFEVT for
 *     the app to play on the real phone vibrator. It is created only while gamepad mode is on and
 *     stays resident, so it is never involved in the arrow path.
 *
 * This is a plain executable, not a JNI library: the uinput setup ioctls have no JVM binding.
 */

#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/* Keylayout is selected by vendor/product; 0x045e/0x02fd is the wired Xbox 360 pad. */
#define GAMEPAD_VENDOR 0x045e
#define GAMEPAD_PRODUCT 0x02fd
#define GAMEPAD_VERSION 0x0903

/* How long a discrete tap holds the button down. Long enough for a fling, short for a step. */
#define TAP_HOLD_US 12000

static int g_fd = -1;
static int g_held_code = -1;

/*
 * The resident gamepad device, used only while gamepad mode is on. Deliberately a separate file
 * descriptor from g_fd so the two devices can never affect each other.
 */
static int g_pad_fd = -1;

/*
 * Force-feedback state for the resident pad.
 *
 * A rumble effect is uploaded once and then played many times, so the upload is remembered here
 * and one-shot effects are treated as "play for N ms". A gain (FF_GAIN) scales the requested
 * magnitude; Android's Vibrator takes a 1..255 amplitude, which is what FFEVT carries.
 */
static int g_ff_effect_id = -1;
static int g_ff_magnitude = 0;
static int g_ff_duration_ms = 0;
static int g_ff_gain = 255; /* 0..255, defaults to "full" until the app says otherwise */
static int g_ff_playing = 0;

/** Reports a rumble request to the caller as an FFEVT line (see the protocol note above). */
static void send_ff_event(int magnitude, int duration_ms) {
    printf("FFEVT %d %d\n", magnitude, duration_ms);
    fflush(stdout);
}

/* Maps our direction names to the HID usage codes Android expects from a gamepad D-pad. */
static int code_for_direction(const char *dir) {
    if (strcmp(dir, "UP") == 0) return BTN_DPAD_UP;
    if (strcmp(dir, "DOWN") == 0) return BTN_DPAD_DOWN;
    if (strcmp(dir, "LEFT") == 0) return BTN_DPAD_LEFT;
    if (strcmp(dir, "RIGHT") == 0) return BTN_DPAD_RIGHT;
    return -1;
}

static void emit(int fd, int type, int code, int value) {
    struct input_event ie;
    memset(&ie, 0, sizeof(ie));
    ie.type = type;
    ie.code = code;
    ie.value = value;
    if (write(fd, &ie, sizeof(ie)) < 0) {
        fprintf(stderr, "write event failed: %s\n", strerror(errno));
    }
}

static void press(int code) {
    if (g_fd < 0) return;
    emit(g_fd, EV_KEY, code, 1);
    emit(g_fd, EV_SYN, SYN_REPORT, 0);
    g_held_code = code;
}

static void release_held(void) {
    if (g_fd < 0 || g_held_code < 0) return;
    emit(g_fd, EV_KEY, g_held_code, 0);
    emit(g_fd, EV_SYN, SYN_REPORT, 0);
    g_held_code = -1;
}

static void destroy_device(void) {
    if (g_fd < 0) return;
    release_held();
    ioctl(g_fd, UI_DEV_DESTROY);
    close(g_fd);
    g_fd = -1;
}

static int create_device(void) {
    if (g_fd >= 0) return 0;

    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        printf("ERROR cannot open /dev/uinput: %s\n", strerror(errno));
        return -1;
    }

    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_SYN) < 0) {
        printf("ERROR evbit: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    /* BTN_A is not used for navigation; it is what convinces Android to report SOURCE_GAMEPAD
     * rather than treating this as a plain D-pad only device. */
    const int keys[] = {BTN_DPAD_UP, BTN_DPAD_DOWN, BTN_DPAD_LEFT, BTN_DPAD_RIGHT, BTN_A};
    for (size_t i = 0; i < sizeof(keys) / sizeof(keys[0]); i++) {
        if (ioctl(fd, UI_SET_KEYBIT, keys[i]) < 0) {
            printf("ERROR keybit %d: %s\n", keys[i], strerror(errno));
            close(fd);
            return -1;
        }
    }

    struct uinput_setup setup;
    memset(&setup, 0, sizeof(setup));
    setup.id.bustype = BUS_VIRTUAL;
    setup.id.vendor = GAMEPAD_VENDOR;
    setup.id.product = GAMEPAD_PRODUCT;
    setup.id.version = GAMEPAD_VERSION;
    snprintf(setup.name, UINPUT_MAX_NAME_SIZE, "iKKO Mind Controller");

    if (ioctl(fd, UI_DEV_SETUP, &setup) < 0) {
        printf("ERROR UI_DEV_SETUP: %s\n", strerror(errno));
        close(fd);
        return -1;
    }
    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        printf("ERROR UI_DEV_CREATE: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    g_fd = fd;
    return 0;
}

/**
 * Creates the resident gamepad device (D-pad + analog axes).
 *
 * This is what makes Android, and therefore any controller tester, treat an Xbox pad as
 * connected. It exists only while gamepad mode is on; the arrow path uses g_fd and is never
 * affected by this device.
 */
static int create_pad_device(void) {
    if (g_pad_fd >= 0) return 0;

    /*
     * O_RDWR (not O_WRONLY) so the FF upload/play events the active app sends can be read back.
     * Force feedback is the one thing that flows *into* us from the same fd we write to, so a
     * write-only descriptor would make the pad advertise rumble it could never actually hear.
     */
    int fd = open("/dev/uinput", O_RDWR | O_NONBLOCK);
    if (fd < 0) {
        printf("ERROR cannot open /dev/uinput: %s\n", strerror(errno));
        return -1;
    }

    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_SYN) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_ABS) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_FF) < 0) {
        printf("ERROR pad evbit: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    /*
     * NOTE: the FF effect bits are deliberately set *after* UI_DEV_SETUP below.
     *
     * uinput only records an effect capability when the device already has a non-zero effect
     * limit; setting UI_SET_FFBIT while ff_effects_max is still 0 is silently discarded, which is
     * exactly why the pad advertised EV_FF yet reported no FF bits (EVIOCGBIT(EV_FF) empty).
     * ff_effects_max is populated by UI_DEV_SETUP, so the bits must follow it -- but still precede
     * UI_DEV_CREATE, which is when the kernel freezes the capability bitmap.
     */

    /*
     * A full Xbox-style button set, so a tester sees a complete controller rather than a device
     * with only a D-pad.
     *
     * 158 (0x09e) is registered as well as BTN_SELECT. The stock Xbox keylayout maps its Select
     * button to key 158, while BTN_SELECT is 0x13a -- registering both means whichever code is
     * sent is actually a valid key on this device instead of being silently dropped.
     */
    const int keys[] = {
        BTN_DPAD_UP, BTN_DPAD_DOWN, BTN_DPAD_LEFT, BTN_DPAD_RIGHT,
        BTN_A, BTN_B, BTN_X, BTN_Y,
        BTN_TL, BTN_TR, BTN_TL2, BTN_TR2,
        BTN_SELECT, BTN_START, BTN_MODE,
        BTN_THUMBL, BTN_THUMBR,
        158 /* Select on the stock Xbox keylayout */
    };
    for (size_t i = 0; i < sizeof(keys) / sizeof(keys[0]); i++) {
        if (ioctl(fd, UI_SET_KEYBIT, keys[i]) < 0) {
            printf("ERROR pad keybit %d: %s\n", keys[i], strerror(errno));
            close(fd);
            return -1;
        }
    }

    /*
     * Left and right stick, the hat axes, and the triggers.
     *
     * The hat axes also carry the D-pad on real pads. ABS_BRAKE / ABS_GAS are the trigger axes:
     * the Xbox keylayout maps them to LTRIGGER / RTRIGGER, and they are 0..255 rather than centred.
     * Registering them is what makes the triggers readable at all; without them a trigger press has
     * nowhere to go.
     */
    const int abs_codes[] = {
        ABS_X, ABS_Y, ABS_Z, ABS_RZ, ABS_HAT0X, ABS_HAT0Y, ABS_BRAKE, ABS_GAS
    };
    const int ranges[][2] = {
        {-32767, 32767}, {-32767, 32767},
        {-32767, 32767}, {-32767, 32767},
        {-1, 1}, {-1, 1},
        {0, 255}, {0, 255},
    };
    for (size_t i = 0; i < sizeof(abs_codes) / sizeof(abs_codes[0]); i++) {
        struct uinput_abs_setup abs;
        memset(&abs, 0, sizeof(abs));
        abs.code = abs_codes[i];
        abs.absinfo.minimum = ranges[i][0];
        abs.absinfo.maximum = ranges[i][1];
        abs.absinfo.value = 0; /* centred */
        if (ioctl(fd, UI_ABS_SETUP, &abs) < 0) {
            printf("ERROR pad abs %d: %s\n", abs_codes[i], strerror(errno));
            close(fd);
            return -1;
        }
    }

    struct uinput_setup setup;
    memset(&setup, 0, sizeof(setup));
    /*
     * BUS_USB on purpose, not BUS_VIRTUAL. A virtual device reports as neither wired nor
     * wireless, and controller testers / games commonly ignore anything they cannot classify as
     * a USB or Bluetooth pad. Advertising USB is what makes the phone treat this as a wired
     * Xbox controller.
     */
    setup.id.bustype = BUS_USB;
    setup.id.vendor = GAMEPAD_VENDOR;
    setup.id.product = GAMEPAD_PRODUCT;
    setup.id.version = GAMEPAD_VERSION;
    /*
     * Reserve slots for uploaded effects. Without this the device advertises FF but rejects every
     * upload, so a game's rumble request silently fails instead of being played.
     */
    setup.ff_effects_max = 16;
    snprintf(setup.name, UINPUT_MAX_NAME_SIZE, "iKKO Mind Controller");

    if (ioctl(fd, UI_DEV_SETUP, &setup) < 0) {
        printf("ERROR pad UI_DEV_SETUP: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    /*
     * Advertise rumble, now that ff_effects_max is set (see the note above the evbit block).
     *
     * Without these bits the device simply has no force-feedback capability, which is exactly why
     * Xbox Game Pass reported "this controller does not support vibration". A real Xbox pad exposes
     * FF_RUMBLE plus the periodic waveform family, so the same set is advertised here.
     */
    const int ff_bits[] = {
        FF_RUMBLE, FF_PERIODIC,
        FF_SQUARE, FF_TRIANGLE, FF_SINE, FF_SAW_UP, FF_SAW_DOWN,
        FF_GAIN,
    };
    for (size_t i = 0; i < sizeof(ff_bits) / sizeof(ff_bits[0]); i++) {
        if (ioctl(fd, UI_SET_FFBIT, ff_bits[i]) < 0) {
            /* Logged, not fatal: a missing effect type should cost that one effect, not the pad. */
            fprintf(stderr, "pad ffbit %d failed: %s\n", ff_bits[i], strerror(errno));
        }
    }

    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        printf("ERROR pad UI_DEV_CREATE: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    g_pad_fd = fd;
    return 0;
}

static void destroy_pad_device(void) {
    if (g_pad_fd < 0) return;
    ioctl(g_pad_fd, UI_DEV_DESTROY);
    close(g_pad_fd);
    g_pad_fd = -1;
}

/**
 * Applies a gain change and reports a rumble request.
 *
 * Clamps the magnitude into 1..255 for the phone vibrator (0 is reserved for "stop", which the
 * caller handles) and emits an FFEVT line. Kept in one place so the play, stop and gain paths
 * cannot drift apart.
 */
static void report_rumble(int magnitude, int duration_ms) {
    int scaled = g_ff_gain > 0 ? magnitude * g_ff_gain / 255 : magnitude;
    if (scaled < 1) scaled = 1;
    if (scaled > 255) scaled = 255;
    send_ff_event(scaled, duration_ms);
}

/**
 * Fetches an uploaded effect's parameters and stores the rumble strength/duration.
 *
 * The parameters are *not* delivered as an event payload: the kernel asks the driver to describe
 * the effect it just received via UI_BEGIN_FF_UPLOAD / UI_END_FF_UPLOAD, and only through that
 * handshake can the magnitude and length be read. Without it a "play" has nothing to buzz with.
 */
static void fetch_ff_upload(int effect_id) {
    struct uinput_ff_upload upload;
    memset(&upload, 0, sizeof(upload));
    upload.request_id = (__u32)effect_id;

    /*
     * UI_BEGIN_FF_UPLOAD is _IOWR: the kernel *writes* the uploaded effect back into `effect` as
     * part of the call (the members are request_id / retval / effect / old on this ABI). Only
     * after it succeeds are the magnitude and length known.
     */
    if (ioctl(g_pad_fd, UI_BEGIN_FF_UPLOAD, &upload) < 0) {
        return;
    }

    /*
     * On this ABI a one-shot rumble carries its strength in the effect's "strong" magnitude and
     * its length in replay.length (milliseconds). Both are translated to the 0..255 / ms pair the
     * phone vibrator expects.
     */
    if (upload.effect.type == FF_RUMBLE) {
        g_ff_magnitude = upload.effect.u.rumble.strong_magnitude >> 8;
        if (g_ff_magnitude == 0) {
            g_ff_magnitude = upload.effect.u.rumble.weak_magnitude >> 8;
        }
    }

    g_ff_duration_ms = (int)upload.effect.replay.length;
    if (g_ff_duration_ms <= 0) g_ff_duration_ms = 200;

    upload.retval = 0;
    ioctl(g_pad_fd, UI_END_FF_UPLOAD, &upload);
}

/**
 * Handles force-feedback events read back from the pad device.
 *
 * Two different event streams matter here:
 *   * EV_UINPUT / UI_FF_UPLOAD -- the kernel telling us an effect was uploaded and asking us to
 *     accept it. The parameters are pulled with UI_BEGIN_FF_UPLOAD, which is the only way to learn
 *     the magnitude and length a game requested.
 *   * EV_FF -- the actual play (value 1), stop (value 0 / -1) and the FF_GAIN control.
 */
static void handle_ff_event(const struct input_event *ie) {
    /*
     * An effect was uploaded by userspace. This *must* be acknowledged, or the kernel will keep
     * waiting for the driver and the game's request never completes.
     */
    if (ie->type == EV_UINPUT && ie->code == UI_FF_UPLOAD) {
        g_ff_effect_id = (int)ie->value;
        fetch_ff_upload((int)ie->value);
        return;
    }

    if (ie->type != EV_FF) return;

    /* FF_GAIN has its own code rather than an effect id: it scales every later magnitude. */
    if (ie->code == FF_GAIN) {
        int gain = ie->value;
        if (gain < 0) gain = 0;
        if (gain > 255) gain = 255;
        g_ff_gain = gain;
        return;
    }

    int effect_id = ie->code;
    int value = ie->value;

    if (value == 0 || value == -1) {
        /*
         * Stop: tell the phone to cut the buzz immediately (magnitude 0). Only reported when
         * something is actually playing, so a repeated stop does not restart a zero-length buzz.
         */
        if (g_ff_playing) {
            g_ff_playing = 0;
            send_ff_event(0, 0);
        }
        return;
    }

    if (effect_id == g_ff_effect_id) {
        g_ff_playing = 1;
        report_rumble(g_ff_magnitude, g_ff_duration_ms);
    }
}

/**
 * Drains any pending FF events from the pad device without blocking.
 *
 * Called from the main poll loop whenever the pad fd is readable. Reads are non-blocking, so it
 * returns as soon as the kernel has handed over everything it has for now.
 */
static void drain_ff_events(void) {
    if (g_pad_fd < 0) return;

    struct input_event ie;
    while (read(g_pad_fd, &ie, sizeof(ie)) == (ssize_t)sizeof(ie)) {
        handle_ff_event(&ie);
    }
}

/* Keeps stdout line-buffered so the caller sees each reply immediately. */
/**
 * Handles one command line from the caller. Returns 0 to keep serving, -1 to exit.
 */
static int handle_command(char *line) {
    if (strcmp(line, "CREATE") == 0) {
        printf(create_device() == 0 ? "CREATED\n" : "ERROR create failed\n");
    } else if (strcmp(line, "DESTROY") == 0) {
        destroy_device();
        printf("DESTROYED\n");
    } else if (strcmp(line, "PAD_CREATE") == 0) {
        printf(create_pad_device() == 0 ? "CREATED\n" : "ERROR pad create failed\n");
    } else if (strcmp(line, "PAD_DESTROY") == 0) {
        destroy_pad_device();
        printf("DESTROYED\n");
    } else if (strncmp(line, "PAD_DOWN ", 9) == 0) {
        int code = 0;
        if (g_pad_fd < 0) {
            printf("ERROR no pad\n");
        } else if (sscanf(line + 9, "%d", &code) != 1) {
            printf("ERROR bad code\n");
        } else {
            emit(g_pad_fd, EV_KEY, code, 1);
            emit(g_pad_fd, EV_SYN, SYN_REPORT, 0);
            printf("OK\n");
        }
    } else if (strncmp(line, "PAD_UP ", 7) == 0) {
        int code = 0;
        if (g_pad_fd < 0) {
            printf("ERROR no pad\n");
        } else if (sscanf(line + 7, "%d", &code) != 1) {
            printf("ERROR bad code\n");
        } else {
            emit(g_pad_fd, EV_KEY, code, 0);
            emit(g_pad_fd, EV_SYN, SYN_REPORT, 0);
            printf("OK\n");
        }
    } else if (strcmp(line, "PAD_FFINFO") == 0) {
        /*
         * Reports what the *created* pad device actually advertises for force feedback, read back
         * with EVIOCGBIT. This is the same source Android's VibratorInputMapper consults, so it is
         * the ground truth for "does the phone think this controller can rumble".
         */
        if (g_pad_fd < 0) {
            printf("ERROR no pad\n");
        } else {
            unsigned long ff_bits[(FF_CNT + 8 * sizeof(unsigned long) - 1) /
                                  (8 * sizeof(unsigned long))];
            memset(ff_bits, 0, sizeof(ff_bits));
            int rumble = 0;
            int periodic = 0;
            int ev_ff = 0;
            int effects = -1;
            /* Does the device carry EV_FF at all in its event-type bitmap? */
            unsigned long ev_bits[(EV_MAX + 8 * sizeof(unsigned long)) /
                                  (8 * sizeof(unsigned long))];
            memset(ev_bits, 0, sizeof(ev_bits));
            if (ioctl(g_pad_fd, EVIOCGBIT(0, sizeof(ev_bits)), ev_bits) >= 0) {
                ev_ff = (ev_bits[EV_FF / (8 * sizeof(unsigned long))] >>
                         (EV_FF % (8 * sizeof(unsigned long)))) & 1UL;
            }
            /* How many effect slots the kernel granted (0 or negative == FF unusable). */
            ioctl(g_pad_fd, EVIOCGEFFECTS, &effects);
            if (ioctl(g_pad_fd, EVIOCGBIT(EV_FF, sizeof(ff_bits)), ff_bits) >= 0) {
                rumble = (ff_bits[FF_RUMBLE / (8 * sizeof(unsigned long))] >>
                          (FF_RUMBLE % (8 * sizeof(unsigned long)))) & 1UL;
                periodic = (ff_bits[FF_PERIODIC / (8 * sizeof(unsigned long))] >>
                            (FF_PERIODIC % (8 * sizeof(unsigned long)))) & 1UL;
            }
            printf("FFINFO ev_ff=%d effects=%d rumble=%d periodic=%d\n",
                   ev_ff, effects, rumble, periodic);
        }
    } else if (strncmp(line, "PAD_DPAD ", 9) == 0) {
        int x = 0;
        int y = 0;
        if (g_pad_fd < 0) {
            printf("ERROR no pad\n");
        } else if (sscanf(line + 9, "%d %d", &x, &y) != 2) {
            printf("ERROR bad dpad values\n");
        } else {
            /* The D-pad lives on the hat axes, which is how the Xbox keylayout reads it. */
            emit(g_pad_fd, EV_ABS, ABS_HAT0X, x);
            emit(g_pad_fd, EV_ABS, ABS_HAT0Y, y);
            emit(g_pad_fd, EV_SYN, SYN_REPORT, 0);
            printf("OK\n");
        }
    } else if (strncmp(line, "PAD_TRIGGER ", 12) == 0) {
        int left = 0;
        int right = 0;
        if (g_pad_fd < 0) {
            printf("ERROR no pad\n");
        } else if (sscanf(line + 12, "%d %d", &left, &right) != 2) {
            printf("ERROR bad trigger values\n");
        } else {
            /* Triggers are 0..255 axes rather than buttons, so they are set to an absolute
             * position: 255 while held, back to 0 on release. */
            emit(g_pad_fd, EV_ABS, ABS_BRAKE, left);
            emit(g_pad_fd, EV_ABS, ABS_GAS, right);
            emit(g_pad_fd, EV_SYN, SYN_REPORT, 0);
            printf("OK\n");
        }
    } else if (strncmp(line, "PAD_STICK ", 10) == 0) {
        int lx = 0, ly = 0, rx = 0, ry = 0;
        if (g_pad_fd < 0) {
            printf("ERROR no pad\n");
        } else if (sscanf(line + 10, "%d %d %d %d", &lx, &ly, &rx, &ry) != 4) {
            printf("ERROR bad stick values\n");
        } else {
            /* Left stick on X/Y, right stick on Z/RZ, matching a real pad. */
            emit(g_pad_fd, EV_ABS, ABS_X, lx);
            emit(g_pad_fd, EV_ABS, ABS_Y, ly);
            emit(g_pad_fd, EV_ABS, ABS_Z, rx);
            emit(g_pad_fd, EV_ABS, ABS_RZ, ry);
            emit(g_pad_fd, EV_SYN, SYN_REPORT, 0);
            printf("OK\n");
        }
    } else if (strcmp(line, "QUIT") == 0) {
        return -1;
    } else {
        int code = code_for_direction(line);
        if (code < 0) {
            printf("ERROR unknown command\n");
        } else if (g_fd < 0) {
            printf("ERROR no device\n");
        } else {
            release_held();
            press(code);
            usleep(TAP_HOLD_US);
            release_held();
            printf("OK\n");
        }
    }
    fflush(stdout);
    return 0;
}

/**
 * Drains one whole line from the command pipe and applies it.
 *
 * Reads byte-by-byte so stdin is never blocked on more input than one command: a blocking fgets
 * would park the process for the entire time no command was pending, during which FF events (which
 * need prompt reporting) would go unserviced.
 */
static int service_command_input(void) {
    static char line[256];
    static size_t length = 0;
    char ch;

    while (read(STDIN_FILENO, &ch, 1) == 1) {
        if (ch == '\n') {
            line[length] = '\0';
            length = 0;
            if (line[0] == '\0') return 0;
            return handle_command(line);
        }
        if (length < sizeof(line) - 1) {
            line[length++] = ch;
        }
    }
    return 0;
}

/**
 * Main loop: serves commands and FF events together.
 *
 * poll() over both descriptors is what lets a rumble request be noticed and reported while the
 * process is otherwise idle waiting for the next command.
 */
static void serve(void) {
    while (1) {
        struct pollfd fds[2];
        nfds_t count = 0;

        fds[count].fd = STDIN_FILENO;
        fds[count].events = POLLIN;
        fds[count].revents = 0;
        count++;

        /* The pad fd only exists between PAD_CREATE and PAD_DESTROY, so it is polled conditionally. */
        int pad_index = -1;
        if (g_pad_fd >= 0) {
            pad_index = (int)count;
            fds[count].fd = g_pad_fd;
            fds[count].events = POLLIN;
            fds[count].revents = 0;
            count++;
        }

        int ready = poll(fds, count, -1);
        if (ready < 0) {
            if (errno == EINTR) continue;
            break;
        }

        if (fds[0].revents & POLLIN) {
            if (service_command_input() != 0) break;
        }
        if (pad_index >= 0 && (fds[pad_index].revents & POLLIN)) {
            drain_ff_events();
        }
    }

    destroy_device();
    destroy_pad_device();
}

int main(void) {
    serve();
    return 0;
}
