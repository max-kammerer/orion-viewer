/* Globals */

/* For compilation only */
#include "common_header.h"

#ifdef ORION_PDF
JNIEXPORT void
JNICALL JNI_FN(MuPDFCore_setContrast)(JNIEnv * env, jobject thiz, jint contrast1);

/* Setting the watermark removal threshold */
JNIEXPORT void
JNICALL JNI_FN(MuPDFCore_setThreshold)(JNIEnv * env, jobject thiz, jint threshold1);

#else

JNIEXPORT void
JNICALL JNI_FN(DjvuDocument_setContrast)(JNIEnv *env, jobject thiz, jint contrast1);

JNIEXPORT void
JNICALL JNI_FN(DjvuDocument_setThreshold)(JNIEnv *env, jobject thiz, jint threshold1);

#endif


void orion_updateContrast(unsigned char *data, int startRow, int startCol, int  endRow, int endCol, int width);