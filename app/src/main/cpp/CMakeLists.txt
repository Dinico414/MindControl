cmake_minimum_required(VERSION 3.22.1)
project(mindcontrol_native C)

# Packaged as a native *executable* (not a JNI .so) so it can be launched as a separate process
# through Shizuku/root. The uinput setup ioctls have no JVM binding, which is why this exists.
#
# The target is deliberately named lib*.so: AGP only packages native files matching that pattern
# (as JNI libs), and silently drops a plain executable. Android will exec a lib*.so happily, so
# this is purely a packaging convention.
add_executable(virtual_dpad virtual_dpad.c)

# Small and dependency-free: no libraries beyond libc are needed, so nothing is linked here.
set_target_properties(virtual_dpad PROPERTIES
    OUTPUT_NAME "virtual_dpad"
    C_STANDARD 11
    C_STANDARD_REQUIRED ON
    POSITION_INDEPENDENT_CODE ON
    SUFFIX ".so")
