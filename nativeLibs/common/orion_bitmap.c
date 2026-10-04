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
#include <stdint.h>
#include "mupdf/fitz.h"

/* Images whose decoded pixmap (after l2factor downsampling) takes up to this
 * many bytes are decoded and cached whole, so tiled rendering reuses one cached
 * pixmap instead of re-decoding the page image per tile. Settable from Java. */
static volatile int64_t orion_full_decode_max_bytes = (int64_t)40 << 20;

JNIEXPORT void
JNICALL JNI_FN(PdfDocument_setFullImageDecodeBytes)(JNIEnv *env, jclass clazz, jlong bytes)
{
    orion_full_decode_max_bytes = bytes;
}

/* Sized image decode tuning callback (fz_tune_image_decode_sized), registered
 * from mupdf's init_base_context; n is bytes per decoded pixel.
 * Tiled rendering asks for a different subarea of the same image for every
 * tile, so each tile gets its own store key and scanned pages (one huge
 * JBIG2/JPEG per page) are re-decoded from scratch per tile. Expanding the
 * subarea to the whole image makes the first tile decode and cache the full
 * pixmap once; every other tile and later scrolling reuse it from the store.
 * The limit is in bytes: a 1 byte/pixel scan may be large, a color image of
 * the same size takes 3-4 times more. Images too large to cache keep the
 * stock fz_default_image_decode logic. */
void orion_image_decode(void *arg, int w, int h, int n, int l2factor, fz_irect *subarea)
{
	(void)arg;

	if (((((int64_t)w * h) >> (2 * l2factor)) * n) <= orion_full_decode_max_bytes ||
		(int64_t)(subarea->x1 - subarea->x0) * (subarea->y1 - subarea->y0) >= ((int64_t)w * h / 10) * 9)
	{
		subarea->x0 = 0;
		subarea->y0 = 0;
		subarea->x1 = w;
		subarea->y1 = h;
	}
	else
	{
		/* Clip to the edges if they are within 1% */
		if (subarea->x0 <= w/100)
			subarea->x0 = 0;
		if (subarea->y0 <= h/100)
			subarea->y0 = 0;
		if (subarea->x1 >= w*99/100)
			subarea->x1 = w;
		if (subarea->y1 >= h*99/100)
			subarea->y1 = h;
	}
}

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