
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
typedef void*       JNIEnv;


#define JNI_FN(A) native_ ## A
#else
#include <jni.h>

#include <android/log.h>
#include <android/bitmap.h>

#define JNI_FN(A) Java_universe_constellation_orion_viewer_djvu_DjvuDocument_ ## A
#endif

extern JNIEXPORT jlong JNICALL JNI_FN(initContext)(JNIEnv *env, jclass type);

extern JNIEXPORT jlong JNICALL
JNI_FN(openFile)(JNIEnv *env, jclass type, jstring jfileName, jobject docInfo, jlong contextl);