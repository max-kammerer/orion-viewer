#include "../../common/common_header.h"

#ifdef ORION_FOR_ANDROID
#define DocInfo jobject
#define PageSize jobject
#else
#define DocInfo long *
#define PageSize(A) void * (*  A)(int, int, int)
#endif


extern JNIEXPORT jlong JNICALL JNI_FN(DjvuDocument_initContext)(JNIEnv *env, jclass type);

extern JNIEXPORT jlong JNICALL
JNI_FN(DjvuDocument_openFile)(JNIEnv *env, jclass type, jstring jfileName, DocInfo docInfo, jlong contextl);

extern JNIEXPORT void JNICALL JNI_FN(DjvuDocument_destroy)(JNIEnv *env, jclass type, jlong doc, jlong context);

extern JNIEXPORT PageSize JNICALL
JNI_FN(DjvuDocument_getPageInfo)(JNIEnv *env, jclass type, jlong docl, jint pageNum, PageSize info);