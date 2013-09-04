/* Globals */

/* For compilation only */
#include <math.h>
#include <jni.h>

#include <android/log.h>

#define LOG_TAG "orion_bitmap"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


unsigned char orion_gamma [256];
unsigned const int DEFAULT_CONTRAST = 100;
unsigned int contrast = 100;
unsigned int threshold = 255;

void orion_setContrast(JNIEnv * env, jobject thiz, jint contrast1);

#ifdef ORION_PDF
JNIEXPORT void
JNICALL Java_com_artifex_mupdf_MuPDFCore_setContrast(JNIEnv * env, jobject thiz, jint contrast1)
{
    orion_setContrast(env, thiz, contrast1);
}

/* Setting the watermark removal threshold */
JNIEXPORT void
JNICALL Java_com_artifex_mupdf_MuPDFCore_setThreshold(JNIEnv * env, jobject thiz, jint threshold1)
{
	threshold = threshold1;
}

#else

JNIEXPORT void
JNICALL Java_universe_constellation_orion_viewer_djvu_DjvuDocument_setContrast(JNIEnv * env, jobject thiz, jint contrast1)
{
    orion_setContrast(env, thiz, contrast1);
}

JNIEXPORT void
JNICALL Java_universe_constellation_orion_viewer_djvu_DjvuDocument_setThreshold(JNIEnv * env, jobject thiz, jint threshold1)
{
    threshold = threshold1;
}

#endif

void orion_setContrast(JNIEnv * env, jobject thiz, jint contrast1)
{
    LOGI("Set contrast : %i",contrast1);
    contrast=contrast1;
    float kgamma=100.0f/contrast1;
    int i;
    for(i=0;i<256;i++) {
        orion_gamma[i]=pow(i/255.0f,kgamma)*255;
    }
}


void orion_updateContrast(unsigned char * data , int size) {
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

