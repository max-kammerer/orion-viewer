#include "../../common/common_header.h"

#ifdef ORION_FOR_ANDROID
#define DocInfo jobject
#define PageInfo(A) jobject A
#else
#define DocInfo long *
#define PageInfo(A) void * (*  A)(int, int, int)
#endif


extern JNIEXPORT jlong JNICALL JNI_FN(DjvuDocument_initContext)(JNIEnv *env, jclass type);

extern JNIEXPORT jlong JNICALL
JNI_FN(DjvuDocument_openFile)(JNIEnv *env, jclass type, jstring jfileName, DocInfo docInfo, jlong contextl);

extern JNIEXPORT void JNICALL JNI_FN(DjvuDocument_destroying)(JNIEnv *env, jclass type, jlong doc, jlong context);

extern JNIEXPORT jobject JNICALL
JNI_FN(DjvuDocument_getPageInfo)(JNIEnv *env, jclass type, jlong docl, jint pageNum, PageInfo(info));