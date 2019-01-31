#include "../../common/common_header.h"

#ifdef ORION_FOR_ANDROID
#define DocInfo jobject
#else
#define DocInfo long *
#endif


extern JNIEXPORT jlong JNICALL JNI_FN(DjvuDocument_initContext)(JNIEnv *env, jclass type);

extern JNIEXPORT jlong JNICALL
JNI_FN(DjvuDocument_openFile)(JNIEnv *env, jclass type, jstring jfileName, DocInfo docInfo, jlong contextl);