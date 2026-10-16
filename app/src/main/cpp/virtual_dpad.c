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
 * Two independent devices are used on purpose and never shared:
 *
 *   * The D-pad device (g_fd) has no axes. Registering axes makes Android classify the device as
 *     a joystick, and in that shape the D-pad keycodes stop reaching the Focus Navigation Engine,
 *     which is what makes arrow-key navigation work at all. That shape must not be changed.
 *   * The gamepad device (g_pad_fd) does have axes, because controller testers detect a
 *     "connected" pad by enumerating AXIS_X / AXIS_Y. It is created only while gamepad mode is
 *     on and stays resident, so it is never involved in the arrow path.
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

    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        printf("ERROR cannot open /dev/uinput: %s\n", strerror(errno));
        return -1;
    }

    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_SYN) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_ABS) < 0) {
        printf("ERROR pad evbit: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

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
    snprintf(setup.name, UINPUT_MAX_NAME_SIZE, "iKKO Mind Controller");

    if (ioctl(fd, UI_DEV_SETUP, &setup) < 0) {
        printf("ERROR pad UI_DEV_SETUP: %s\n", strerror(errno));
        close(fd);
        return -1;
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

/* Keeps stdout line-buffered so the caller sees each reply immediately. */
static void serve(void) {
    char line[256];

    while (fgets(line, sizeof(line), stdin) != NULL) {
        line[strcspn(line, "\r\n")] = '\0';
        if (line[0] == '\0') continue;

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
            break;
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
    }

    destroy_device();
    destroy_pad_device();
}

int main(void) {
    serve();
    return 0;
}
