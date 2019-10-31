# The ARMv7 is significanly faster due to the use of the hardware FPU
#armeabi mips
APP_ABI := armeabi-v7a x86
APP_PLATFORM=android-16
APP_OPTIM := release
APP_STL := system
APP_CPPFLAGS += -fexceptions
APP_STL := c++_static
