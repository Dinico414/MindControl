/*
 * Minimal uinput probe for Android.
 *
 * Creates a virtual D-pad device ("MindControl Probe") via /dev/uinput, then:
 *   1. prints whether creation succeeded,
 *   2. sleeps, and
 *   3. emits one DPAD_DOWN press.
 *
 * While it sleeps you can run `getevent -i` on the device to see whether the
 * kernel/Android treated the device as a D-pad/gamepad source.
 *
 * Build (NDK):
 *   clang --target=aarch64-linux-android24 uinput_probe.c -o uinput_probe
 */
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <linux/uinput.h>
#include <linux/input.h>

static void emit(int fd, int type, int code, int val) {
    struct input_event ie;
    memset(&ie, 0, sizeof(ie));
    ie.type = type;
    ie.code = code;
    ie.value = val;
    if (write(fd, &ie, sizeof(ie)) < 0) {
        fprintf(stderr, "write event failed: %s\n", strerror(errno));
    }
}

int main(void) {
    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        fprintf(stderr, "open /dev/uinput failed: %s (errno=%d)\n", strerror(errno), errno);
        return 1;
    }
    printf("opened /dev/uinput ok (fd=%d)\n", fd);

    /* Enable the buttons a D-pad/gamepad exposes. */
    ioctl(fd, UI_SET_EVBIT, EV_KEY);
    ioctl(fd, UI_SET_EVBIT, EV_SYN);
    ioctl(fd, UI_SET_KEYBIT, BTN_DPAD_UP);
    ioctl(fd, UI_SET_KEYBIT, BTN_DPAD_DOWN);
    ioctl(fd, UI_SET_KEYBIT, BTN_DPAD_LEFT);
    ioctl(fd, UI_SET_KEYBIT, BTN_DPAD_RIGHT);
    ioctl(fd, UI_SET_KEYBIT, BTN_A);   /* helps Android classify it as SOURCE_GAMEPAD */

    struct uinput_setup usetup;
    memset(&usetup, 0, sizeof(usetup));
    usetup.id.bustype = BUS_VIRTUAL;
    usetup.id.vendor = 0x045e;   /* Microsoft/Xbox vendor, so Android uses the Xbox keylayout */
    usetup.id.product = 0x02fd;
    usetup.id.version = 0x0903;
    strncpy(usetup.name, "MindControl Probe", UINPUT_MAX_NAME_SIZE - 1);

    if (ioctl(fd, UI_DEV_SETUP, &usetup) < 0) {
        fprintf(stderr, "UI_DEV_SETUP failed: %s\n", strerror(errno));
        close(fd);
        return 1;
    }
    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        fprintf(stderr, "UI_DEV_CREATE failed: %s\n", strerror(errno));
        close(fd);
        return 1;
    }
    printf("UI_DEV_CREATE ok - device should now exist\n");

    sleep(3);

    printf("emitting DPAD_DOWN press\n");
    emit(fd, EV_KEY, BTN_DPAD_DOWN, 1);
    emit(fd, EV_SYN, SYN_REPORT, 0);
    usleep(50000);
    emit(fd, EV_KEY, BTN_DPAD_DOWN, 0);
    emit(fd, EV_SYN, SYN_REPORT, 0);

    sleep(2);
    ioctl(fd, UI_DEV_DESTROY);
    close(fd);
    printf("destroyed, done\n");
    return 0;
}
