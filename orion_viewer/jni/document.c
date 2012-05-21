/* Globals */
#include <math.h>
#include <jni.h>

#include <android/log.h>

#define LOG_TAG "djvulib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


unsigned char orion_gamma [256];
unsigned int DEFAULT_CONTRAST = 100;
unsigned int contrast = 100;

JNIEXPORT void
JNICALL Java_universe_constellation_orion_viewer_AbstractDocumentWrapper_setContrast(JNIEnv * env, jobject thiz, jint contrast1)
{
   LOGI("Set contrast : %i", contrast1);		
    contrast = contrast1;
    float kgamma = 100.0f / contrast1;
int i;
for (i = 0; i < 256; i++) 
    {
        orion_gamma[i] = pow(i / 255.0f, kgamma) * 255;
 	}
}


void orion_updateContrast(unsigned char * data , int size) {
if (contrast != DEFAULT_CONTRAST) {
   LOGI("Update gamma : %i", size);		
	int i;
    for (i = 0; i < size; i++) 
    {
        data[i] = orion_gamma[data[i]];
    }
}

}
