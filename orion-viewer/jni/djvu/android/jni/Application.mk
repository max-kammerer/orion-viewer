# The ARMv7 is significanly faster due to the use of the hardware FPU
APP_ABI := armeabi armeabi-v7a x86 mips
APP_PLATFORM=android-14
#armeabi
#armeabi-v7a mips x86
APP_OPTIM := release
LOCAL_ARM_MODE := arm
APP_STL := system
APP_CPPFLAGS += -fexceptions
APP_STL := c++_static
