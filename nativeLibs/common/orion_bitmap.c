/* Globals */

/* For compilation only */
#include "common_header.h"
#include <math.h>

unsigned char orion_gamma[256];
unsigned const int DEFAULT_CONTRAST = 100;
unsigned int contrast = 100;
unsigned int threshold = 255;

void orion_setContrast(JNIEnv *env, jobject thiz, jint contrast1);
void orion_updateContrast(unsigned char *data, int startRow, int startCol, int  endRow, int endCol, int width);

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
JNICALL JNI_FN(PdfDocument_updateContrast)(JNIEnv *env, jobject thiz, jobject jbitmap, jint startRow, jint startCol, jint  endRow, jint endCol, jint width) {
    void *pixels;
    int ret;

    ret = AndroidBitmap_lockPixels(env, jbitmap, (void **)&pixels);
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        //TODO: log
        return;
    }
    orion_updateContrast((unsigned char *) pixels, startRow, startCol, endRow, endCol, width);

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
        orion_gamma[i] = (uint8_t) (pow(i / 255.0f, kgamma) * 255);
    }
}


void orion_updateContrast(uint8_t *data, int startRow, int startCol, int  endRow, int endCol, int width) {
    if (contrast != DEFAULT_CONTRAST) {
        LOGI("Update gamma : %i-%i %i-%i %i", startRow, endRow, startCol, endCol, width);
        int i, j;
        for (i = startRow; i < endRow; i++) {
            for (j = 4 * startCol; j < 4 * endCol; j++) {
                int index = j + i * width * 4;
                data[index] = orion_gamma[data[index]];
            }
        }
    }

    if (threshold > 0 && threshold < 255) {
        int i, j;
        for (i = startRow; i < endRow; i++) {
            for (j = 4 * startCol; j < 4 * endCol; j++) {
                int index = j + i * width * 4;
                if (data[index] > threshold) {
                    data[index] = 255;
                }
            }
        }
    }
}