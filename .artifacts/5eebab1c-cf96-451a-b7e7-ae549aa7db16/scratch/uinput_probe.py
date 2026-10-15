#!/system/bin/sh
# Minimal uinput probe: create a virtual D-pad device and see if Android registers it.
# Run via: adb shell sh uinput_probe.py   (needs the file pushed first)

# We drive uinput through a tiny python helper if python exists, else fall back to a C-less
# approach using `input` is NOT possible. So we just verify the sysfs side.

echo "=== uinput sysfs ==="
ls -l /sys/class/input/ 2>/dev/null | head -20
echo "=== /sys/class/misc/uinput ==="
ls -l /sys/class/misc/uinput/ 2>/dev/null
echo "=== can we open for write (again, via cat) ==="
if [ -w /dev/uinput ]; then echo "WRITABLE"; else echo "NOT-WRITABLE-DIRECT"; fi
