#ifndef ORION_COMMON_HEADER
#define ORION_COMMON_HEADER

#ifndef ORION_FOR_ANDROID
#include <stdint.h>

#define JNIIMPORT
#define JNIEXPORT  __attribute__ ((visibility ("default")))
#define JNICALL


/* Primitive types that match up with Java equivalents. */
typedef uint8_t  jboolean; /* unsigned 8 bits */
typedef int8_t   jbyte;    /* signed 8 bits */
typedef uint16_t jchar;    /* unsigned 16 bits */
typedef int16_t  jshort;   /* signed 16 bits */
typedef int32_t  jint;     /* signed 32 bits */
typedef int64_t  jlong;    /* signed 64 bits */
typedef float    jfloat;   /* 32-bit IEEE 754 */
typedef double   jdouble;  /* 64-bit IEEE 754 */

typedef void*       jobject;
typedef void*        jclass;
typedef char*       jstring;
typedef char*       jmethodID;
typedef void*       JNIEnv;


#define JNI_FN(A) djvu_ ## A
#define ANDROID_LOG_INFO "info"
#define ANDROID_LOG_ERROR "error"

//int __android_log_print(int prio, const char *tag,  const char *fmt, ...) {
//    //TODO
//}

#define LOGI(...) printf(__VA_ARGS__); printf("\n")
#define LOGT(...) printf(__VA_ARGS__); printf("\n")
#define LOGE(...) printf(__VA_ARGS__); printf("\n")

#else
#include <jni.h>

#include <android/log.h>
#include <android/bitmap.h>

#ifdef ORION_PDF
#define JNI_FN(A) Java_universe_constellation_orion_viewer_pdf_ ## A
#define LOG_TAG "libmupdf"
#else
#define JNI_FN(A) Java_universe_constellation_orion_viewer_djvu_ ## A
#define LOG_TAG "djvulib"
#endif

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGT(...) __android_log_print(ANDROID_LOG_INFO,"alert",__VA_ARGS__)
#endif

#endif

