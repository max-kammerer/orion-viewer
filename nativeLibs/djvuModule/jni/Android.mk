LOCAL_PATH := $(call my-dir)
TOP_LOCAL_PATH := $(LOCAL_PATH)

MY_ROOT := ..

include $(TOP_LOCAL_PATH)/Core.mk

include $(CLEAR_VARS)
LOCAL_CFLAGS += -DORION_FOR_ANDROID

LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := \
	.. \
	../djvu/libdjvu

LOCAL_MODULE    := djvu
LOCAL_SRC_FILES := djvu.c

LOCAL_STATIC_LIBRARIES := djvucore

LOCAL_LDLIBS    := -lm -llog -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
