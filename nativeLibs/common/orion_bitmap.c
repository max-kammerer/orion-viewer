/* Globals */

/* For compilation only */
#include "common_header.h"
#include <math.h>

unsigned char orion_gamma[256];
unsigned const int DEFAULT_CONTRAST = 100;
unsigned int contrast = 100;
unsigned int threshold = 255;

void orion_setContrast(JNIEnv *env, jobject thiz, jint contrast1);
void orion_updateContrast(unsigned char *data, int size);

#ifdef ORION_PDF
JNIEXPORT void
JNICALL JNI_FN(PdfDocument_setContrast)(JNIEnv * env, jobject thiz, jint contrast1)
{
    orion_setContrast(env, thiz, contrast1);
}

/* Setting the watermark removal threshold */
JNIEXPORT void
JNICALL JNI_FN(PdfDocument_setThreshold)(JNIEnv * env, jobject thiz, jint threshold1)
{
    threshold = threshold1;
}

JNIEXPORT void
JNICALL JNI_FN(PdfDocument_updateContrast)(JNIEnv *env, jobject thiz, jobject jbitmap, jint size) {
    void *pixels;
    int ret;

    ret = AndroidBitmap_lockPixels(env, jbitmap, (void **)&pixels);
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        //TODO: log
        return;
    }
    orion_updateContrast((unsigned char *) pixels, (unsigned int)size);

    if (AndroidBitmap_unlockPixels(env, jbitmap) != ANDROID_BITMAP_RESULT_SUCCESS) {
        //log
    }
}


#else

JNIEXPORT void
JNICALL JNI_FN(DjvuDocument_setContrast)(JNIEnv *env, jobject thiz, jint contrast1) {
    orion_setContrast(env, thiz, contrast1);
}

JNIEXPORT void
JNICALL JNI_FN(DjvuDocument_setThreshold)(JNIEnv *env, jobject thiz, jint threshold1) {
    threshold = (unsigned int) threshold1;
}

#endif

void orion_setContrast(JNIEnv *env, jobject thiz, jint contrast1) {
    LOGI("Set contrast : %i", contrast1);
    contrast = (unsigned int) contrast1;
    float kgamma = contrast1 / 100.0f;
    int i;
    for (i = 0; i < 256; i++) {
        orion_gamma[i] = (unsigned char) (pow(i / 255.0f, kgamma) * 255);
    }
}


void orion_updateContrast(unsigned char *data, int size) {
    if (contrast != DEFAULT_CONTRAST) {
        LOGI("Update gamma : %i", size);
        int i;
        for (i = 0; i < size; i++) {
            data[i] = orion_gamma[data[i]];
        }
    }

    if (threshold > 0 && threshold < 255) {
        int i;
        for (i = 0; i < size; i++) {
            if (data[i] > threshold) {
                data[i] = 255;
            }
        }
    }
}