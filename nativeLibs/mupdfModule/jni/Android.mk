LOCAL_PATH := $(call my-dir)
TOP_LOCAL_PATH := $(LOCAL_PATH)

MUPDF_ROOT := ../mupdf

ifdef NDK_PROFILER
include android-ndk-profiler.mk
endif

include $(TOP_LOCAL_PATH)/Core.mk
include $(TOP_LOCAL_PATH)/ThirdParty.mk

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := \
	jni/andprof \
	$(MUPDF_ROOT)/include \
	$(MUPDF_ROOT)/source \
	$(MUPDF_ROOT)/source/fitz \
	$(MUPDF_ROOT)/source/pdf \
    $(MUPDF_ROOT)/platform/java \
    ..

LOCAL_CFLAGS := -DHAVE_ANDROID -DORION_PDF -DORION_FOR_ANDROID
LOCAL_MODULE    := mupdf
LOCAL_SRC_FILES := mupdf.c  \
                   ../../common/orion_bitmap.c \
                   ../../common/list.c

LOCAL_STATIC_LIBRARIES := mupdfcore mupdfthirdparty
ifdef NDK_PROFILER
LOCAL_CFLAGS += -pg -DNDK_PROFILER
LOCAL_STATIC_LIBRARIES += andprof
endif
ifdef FZ_ENABLE_GPRF
LOCAL_CFLAGS += -DFZ_ENABLE_GPRF
endif

LOCAL_LDLIBS := -lm -llog -ljnigraphics
ifdef CRYPTO_BUILD
LOCAL_LDLIBS += -L$(MUPDF_ROOT)/thirdparty/openssl-$(APP_ABI)/lib -lcrypto
endif

include $(BUILD_SHARED_LIBRARY)
