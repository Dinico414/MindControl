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
 *   CREATE    -> "CREATED" | "ERROR <detail>"
 *   DESTROY   -> "DESTROYED"
 *   UP|DOWN|LEFT|RIGHT
 *             -> "OK"      (press + release; a discrete D-pad tap)
 *   QUIT      -> exits
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
    snprintf(setup.name, UINPUT_MAX_NAME_SIZE, "MindControl D-pad");

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
}

int main(void) {
    serve();
    return 0;
}
