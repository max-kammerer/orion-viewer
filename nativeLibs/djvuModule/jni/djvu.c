#include <time.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include <miniexp.h>
#include <ddjvuapi.h>
#include "debug.h"

#include "../../common/list.h"
#include "../../common/orion_bitmap.h"
#include "base_geometry.c"
#include "djvu.h"
#include "jni.h"
#include <unistd.h>

static jclass textBuilderClass;
static jmethodID addWordId;
static jmethodID newLineId;
static jmethodID addSpaceId;

//extern void orion_updateContrast(unsigned char *data, unsigned int startRow, unsigned int startCol, unsigned int  endRow, unsigned int endCol);

static inline jlong jlong_cast(void *p) {
    return (jlong) (intptr_t) p;
}

void record_jni_error(JNIEnv* env, const char* msg)
{
    jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (!exceptionClass)
        return;
    if (!msg)
        (*env)->ThrowNew(env, exceptionClass, "Djvu decoding error!");
    else (*env)->ThrowNew(env, exceptionClass, msg);
}

void record_error_from_message(JNIEnv* env, const ddjvu_message_t* msg)
{
    if (!msg || !msg->m_error.message)
        record_jni_error(env, "Djvu decoding error!");
    else record_jni_error(env, msg->m_error.message);
}

int handle_ddjvu_messages_or_record_error(JNIEnv *env, ddjvu_context_t *ctx, int wait, int throw)
{
    const ddjvu_message_t *msg;
    if (wait) {
        ddjvu_message_wait(ctx);
    }
    int failed = 0;
    while ((msg = ddjvu_message_peek(ctx)))
    {
        switch(msg->m_any.tag)
        {
            case DDJVU_ERROR:
                LOGE("error: %s", msg->m_error.message);
                if (throw && !(*env)->ExceptionCheck(env)) {
                    record_error_from_message(env, msg);
                }
                failed = 1;
                break;
            default:
                break;
        }
        ddjvu_message_pop(ctx);
    }
    return failed;
}

int handle_error_if_present(JNIEnv *env, ddjvu_context_t *ctx) {
    return handle_ddjvu_messages_or_record_error(env, ctx, FALSE, TRUE);
}

int exceptionStatus(JNIEnv *env) {
    return (*env)->ExceptionCheck(env);
}

void resolveErrorIfPossibleStub(JNIEnv *env, ddjvu_context_t *ctx, ddjvu_status_t status) {

}

void resolveErrorIfPossible(JNIEnv *env, ddjvu_context_t *ctx, ddjvu_status_t status) {
    int ex = exceptionStatus(env);
    if (status == DDJVU_JOB_OK || status == DDJVU_JOB_STOPPED) {
        if (ex) {
            (*env)->ExceptionClear(env);
        }
    } else if (status == DDJVU_JOB_FAILED || status == DDJVU_JOB_NOTSTARTED) {
        if (!ex) {
            for (int i = 0; i < 5; i++) {
                //Don't call blocking function as error can be absent
                if (handle_error_if_present(env, ctx)) break;
                usleep(1);
            }
        }
    }
}

void handle_ddjvu_messages_and_wait_stub(JNIEnv *env, ddjvu_context_t *ctx) {
    handle_ddjvu_messages_or_record_error(env, ctx, TRUE, FALSE);
}

void handle_ddjvu_messages_and_wait(JNIEnv *env, ddjvu_context_t *ctx) {
    handle_ddjvu_messages_or_record_error(env, ctx, TRUE, TRUE);
}

JNIEXPORT JNICALL void JNI_FN(DjvuDocument_initNative)(JNIEnv *env, jclass type) {
    if (textBuilderClass == NULL) {
        textBuilderClass = (*env)->FindClass(env, "universe/constellation/orion/viewer/document/TextInfoBuilder");
    }
    if (addWordId == NULL) {
        addWordId = (*env)->GetMethodID(env, textBuilderClass, "addWord", "(Ljava/lang/String;IIII)V");
    }
    if (newLineId == NULL) {
        newLineId = (*env)->GetMethodID(env, textBuilderClass, "newLine", "()V");
    }
    if (addSpaceId == NULL) {
        addSpaceId = (*env)->GetMethodID(env, textBuilderClass, "addSpace", "()V");
    }
}

JNIEXPORT jlong JNICALL JNI_FN(DjvuDocument_initContext)(JNIEnv *env, jclass type) {
    LOGI("Creating context");
    return jlong_cast(ddjvu_context_create("orion"));
}

JNIEXPORT jlong JNICALL
JNI_FN(DjvuDocument_openFile)(JNIEnv *env, jclass type, jstring jfileName, jlong contextl, DocInfo docInfo) {
    ddjvu_context_t *context = (ddjvu_context_t *) contextl;

#ifdef ORION_FOR_ANDROID
    const char *fileName = (*env)->GetStringUTFChars(env, jfileName, 0);
#else
    const char *fileName = jfileName;
#endif

    LOGI("Opening document: %s", fileName);
    ddjvu_document_t *doc = ddjvu_document_create_by_filename_utf8(context, fileName, 0);
    LOGI("Start decoding document: %p", doc);

    ddjvu_status_t status = DDJVU_JOB_NOTSTARTED;
    if (doc) {
        while ((status = ddjvu_document_decoding_status(doc)) < DDJVU_JOB_OK) {
            handle_ddjvu_messages_and_wait(env, context);
        }
    }

    resolveErrorIfPossible(env, context, status);
    if (status == DDJVU_JOB_OK) {
        int pageNum = ddjvu_document_get_pagenum(doc);
        LOGI("Doc opened successfully: %p, pagecount = %i", doc, pageNum);

#ifdef ORION_FOR_ANDROID
        jclass cls = (*env)->GetObjectClass(env, docInfo);
        jfieldID pageCountF = (*env)->GetFieldID(env, cls, "pageCount", "I");
        (*env)->SetIntField(env, docInfo, pageCountF, pageNum);
#else
        *docInfo = pageNum;
#endif
    } else {
        LOGE("Error during document opening: %p, status=%d, failed=%d", doc, status, exceptionStatus(env));
        ddjvu_document_release(doc);
        doc = NULL;
    }

    return jlong_cast(doc);
}


JNIEXPORT jlong JNICALL
JNI_FN(DjvuDocument_getPageInternal)(JNIEnv *env, jclass type, jlong contextl, jlong docl, jint pageNum) {
    ddjvu_context_t *context = (ddjvu_context_t *) contextl;
    ddjvu_document_t *doc = (ddjvu_document_t *) docl;
    LOGI("Opening page: %d", pageNum);

    ddjvu_page_t *page = ddjvu_page_create_by_pageno(doc, pageNum);
    ddjvu_status_t status = DDJVU_JOB_NOTSTARTED;
    if (page) {
        LOGI("Start decoding page: %p", page);
        while ((status = ddjvu_page_decoding_status(page)) < DDJVU_JOB_OK) {
            handle_ddjvu_messages_and_wait_stub(env, context);
        }
        LOGI("End decoding page: %p", page);
    }

    resolveErrorIfPossibleStub(env, context, status);
    if (status != DDJVU_JOB_OK) {
        LOGE("Error during page extraction: %p, status=%d, failed=%d", page, status, exceptionStatus(env));
        ddjvu_page_release(page);
        page = NULL;
    }

    return jlong_cast(page);
}

JNIEXPORT PageSize JNICALL
JNI_FN(DjvuDocument_getPageDimension)(JNIEnv *env, jclass type, jlong contextl, jlong docl, jint pageNum, PageSize info) {
    ddjvu_context_t *context = (ddjvu_context_t *) contextl;
    ddjvu_document_t *doc = (ddjvu_document_t *) docl;
    LOGI("Obtaining page %i info in %p", pageNum, doc);
    /*
    ddjvu_page_t * mypage = ddjvu_page_create_by_pageno(doc, pageNum);
    int pageWidth =  ddjvu_page_get_width(mypage);
    int pageHeight = ddjvu_page_get_height(mypage);
    //LOGI("mypage: %p", mypage);

    ddjvu_page_release(mypage);
    */
    ddjvu_status_t status;
    ddjvu_pageinfo_t dinfo;
    while ((status = ddjvu_document_get_pageinfo(doc, pageNum, &dinfo)) < DDJVU_JOB_OK) {
        handle_ddjvu_messages_and_wait_stub(env, context);
    }

    resolveErrorIfPossibleStub(env, context, status);
    if (status != DDJVU_JOB_OK) {
        LOGE("Error during document PageDimension calculation: %d, status=%d, failed=%d", pageNum, status, exceptionStatus(env));
        return NULL;
    }

#ifdef ORION_FOR_ANDROID
    jclass cls = (*env)->GetObjectClass(env, info);
    jfieldID width = (*env)->GetFieldID(env, cls, "width", "I");
    jfieldID height = (*env)->GetFieldID(env, cls, "height", "I");
    (*env)->SetIntField(env, info, width, dinfo.width);
    (*env)->SetIntField(env, info, height, dinfo.height);
    return info;
#else
    return info(pageNum, dinfo.width, dinfo.height);
#endif
}

JNIEXPORT jboolean JNICALL
JNI_FN(DjvuDocument_drawPage)(JNIEnv *env, jclass type, jlong context, jlong docl, jlong pagel, jobject bitmap,
                              jfloat zoom, jint bitmapWidth, jint bitmapHeight, jint patchX, jint patchY, jint patchW,
                              jint patchH, jint originX, jint originY) {
    ddjvu_document_t *doc = (ddjvu_document_t *) docl;
    ddjvu_page_t *page = (ddjvu_page_t *) pagel;

    int ret;
    void *pixels;


#ifdef ORION_FOR_ANDROID
    AndroidBitmapInfo info;
    LOGI("Rendering page=%dx%d patch=[%d,%d,%d,%d]",
         bitmapWidth, bitmapHeight, patchX, patchY, patchW, patchH);

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return 0;
    }

    LOGI("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888! Format is %i", info.format);
        //return 0;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return 0;
    }
#endif

    int pageWidth = ddjvu_page_get_width(page);
    int pageHeight = ddjvu_page_get_height(page);

    ddjvu_rect_t pageRect;
    pageRect.x = 0;
    pageRect.y = 0;
    pageRect.w = (unsigned int) round(zoom * (unsigned int) pageWidth);
    pageRect.h = (unsigned int) round(zoom * (unsigned int) pageHeight);
    LOGI("Original page=%dx%d patch=[%d,%d,%d,%d]",
         pageWidth, pageHeight, pageRect.x, pageRect.y, pageRect.w, pageRect.h);

    ddjvu_rect_t targetRect;
    targetRect.x = patchX;
    targetRect.y = patchY;
    targetRect.w = (unsigned int)patchW;
    targetRect.h = (unsigned int)patchH;

    int shift = originX + originY * bitmapWidth;

    if (pageRect.w < targetRect.x + targetRect.w) {
        targetRect.w = targetRect.w - (targetRect.x + targetRect.w - pageRect.w);
    }
    if (pageRect.h < targetRect.y + targetRect.h) {
        targetRect.h = targetRect.h - (targetRect.y + targetRect.h - pageRect.h);
    }

    LOGI("Target rect=%dx%d patch=[%d,%d,%d,%d], shift=%d zoom=%f",
         patchW, patchH, targetRect.x, targetRect.y, targetRect.w, targetRect.h, shift, zoom);

    unsigned int masks[4] = {0xff, 0xff00, 0xff0000, 0xff000000};
    ddjvu_format_t *pixelFormat = ddjvu_format_create(DDJVU_FORMAT_RGBMASK32, 4, masks);

    ddjvu_format_set_row_order(pixelFormat, TRUE);

    ddjvu_format_set_y_direction(pixelFormat, TRUE);

    char *buffer = &(((char *) pixels)[shift * 4]);
    /*jboolean result = */ddjvu_page_render(page, DDJVU_RENDER_COLOR, &pageRect, &targetRect,
                                        pixelFormat, info.stride, buffer);

    ddjvu_format_release(pixelFormat);

    orion_updateContrast((unsigned char *) pixels, originY, originX,
                         originY + targetRect.h, originX + targetRect.w, bitmapWidth);


#ifdef ORION_FOR_ANDROID
    AndroidBitmap_unlockPixels(env, bitmap);
#endif

    return 1;
}


JNIEXPORT void JNICALL JNI_FN(DjvuDocument_destroy)(JNIEnv *env, jclass type, jlong context, jlong doc) {
    LOGI("Closing doc...");

    if (doc != 0) {
        ddjvu_document_release((ddjvu_document_t *) doc);
    }

    LOGI("Closing context...");
    if (context != 0) {
        ddjvu_context_release((ddjvu_context_t *) context);
    }
}

JNIEXPORT void JNICALL JNI_FN(DjvuDocument_releasePage)(JNIEnv *env, jclass clazz, jlong page) {
    if (page != 0) {
        ddjvu_page_release((ddjvu_page_t *) page);
    }
}


struct list_el {
    jobject item;
    struct list_el *next;
};

typedef struct list_el list_item;

struct list_st {
    list_item *head;
    list_item *tail;
};

typedef struct list_st list;

struct OutlineItem_s {
    const char *title;
    int level;
    int page;
};
typedef struct OutlineItem_s OutlineItem;


int buildTOC(ddjvu_document_t *doc, miniexp_t expr, list *myList, jint level, JNIEnv *env, jclass olClass) {
    while (miniexp_consp(expr)) {
        miniexp_t s = miniexp_car(expr);
        expr = miniexp_cdr(expr);
        if (miniexp_consp(s) &&
            miniexp_consp(miniexp_cdr(s)) &&
            miniexp_stringp(miniexp_car(s)) &&
            miniexp_stringp(miniexp_cadr(s))) {
            // fill item
            const char *name = miniexp_to_str(miniexp_car(s));
            const char *page = miniexp_to_str(miniexp_cadr(s));
            //starts with #

            int pageno = -1;
            if (page[0] == '#') {
                pageno = ddjvu_document_search_pageno(doc, &page[1]);
            }

            if (pageno < 0) {
                LOGI("Page %s", page);
            }

            if (name == NULL) { return -1; }

            OutlineItem *element = (OutlineItem *) malloc(sizeof(OutlineItem));
            element->title = name;
            element->page = pageno;
            element->level = level;

            list_item *next = (list_item *) malloc(sizeof(list_item));
            next->item = element;
            next->next = NULL;
            myList->tail->next = next;
            myList->tail = next;

            // recursion
            buildTOC(doc, miniexp_cddr(s), myList, level + 1, env, olClass);
        }
    }
    return 0;
}

#ifdef ORION_FOR_ANDROID
JNIEXPORT jobjectArray JNICALL JNI_FN(DjvuDocument_getOutline)(JNIEnv *env, jclass clazz, jlong docl) {
    ddjvu_document_t *doc = (ddjvu_document_t *) docl;
    miniexp_t outline = ddjvu_document_get_outline(doc);

    if (outline == miniexp_dummy || outline == NULL) {
        return NULL;
    }

    if (!miniexp_consp(outline) || miniexp_car(outline) != miniexp_symbol("bookmarks")) {
        LOGI("Outlines is empty");
    }

    list_item *root = (list_item *) malloc(sizeof(list_item));
    root->next = NULL;

    list *myList = (list *) malloc(sizeof(list));
    myList->head = root;
    myList->tail = root;

    jclass olClass;
    jmethodID ctor;

#ifdef ORION_FOR_ANDROID
    olClass = (*env)->FindClass(env, "universe/constellation/orion/viewer/document/OutlineItem");
    if (olClass == NULL) return NULL;
    ctor = (*env)->GetMethodID(env, olClass, "<init>", "(ILjava/lang/String;I)V");
    if (ctor == NULL) return NULL;
#endif

    buildTOC(doc, miniexp_cdr(outline), myList, 0, env, olClass);

    list_item *next = myList->head;
    int size = 0;
    while (next->next != NULL) {
        next = next->next;
        size++;
    }

    LOGI("Outline has %i entries", size);

    jobjectArray arr = (*env)->NewObjectArray(env, size, olClass, NULL);
    if (arr == NULL) {
        return NULL;
    }

    next = root->next;
    int pos = 0;

    while (next != NULL) {
        OutlineItem *item = next->item;
#ifdef ORION_FOR_ANDROID
        jstring title = (*env)->NewStringUTF(env, item->title);
        //shift pageno to zero based
        jobject element = (*env)->NewObject(env, olClass, ctor, item->level, title, item->page);
        (*env)->SetObjectArrayElement(env, arr, pos, element);
        (*env)->DeleteLocalRef(env, title);
        (*env)->DeleteLocalRef(env, element);
#endif
        free(item);
        list_item *next2 = next->next;
        free(next);
        next = next2;
        pos++;
    }

    free(root);
    free(myList);

    return arr;
}
#endif

//sumatrapdf code
int extractText(JNIEnv* env, miniexp_t item, jobject textBuilder, int height) {
    miniexp_t type = miniexp_car(item);
    if (!miniexp_symbolp(type)) {
        return 0;
    }
    item = miniexp_cdr(item);

    if (!miniexp_numberp(miniexp_car(item))) {
        return 0;
    }
    int x0 = miniexp_to_int(miniexp_car(item));
    item = miniexp_cdr(item);
    if (!miniexp_numberp(miniexp_car(item))) {
        return 0;
    }
    int y1 = height - miniexp_to_int(miniexp_car(item));
    item = miniexp_cdr(item);
    if (!miniexp_numberp(miniexp_car(item))) {
        return 0;
    }
    int x1 = miniexp_to_int(miniexp_car(item));
    item = miniexp_cdr(item);
    if (!miniexp_numberp(miniexp_car(item))) {
        return 0;
    }

    int y0 = height - miniexp_to_int(miniexp_car(item));
    item = miniexp_cdr(item);

    miniexp_t str = miniexp_car(item);

    if (miniexp_stringp(str) && !miniexp_cdr(item)) {
        const char* content = miniexp_to_str(str);
        if (type != miniexp_symbol("char") && type != miniexp_symbol("word")) {
            (*env)->CallVoidMethod(env, textBuilder, newLineId);
        }

        if (content) {
            jstring string = (*env)->NewStringUTF(env, content);
            (*env)->CallVoidMethod(env, textBuilder, addWordId, string, x0, y0, x1, y1);
            (*env)->DeleteLocalRef(env, string);
        }

        if (miniexp_symbol("word") == type) {
            (*env)->CallVoidMethod(env, textBuilder, addSpaceId);
            //coords.Append(Rect(rect.x + rect.dx, rect.y, 2, rect.dy));
        }
        item = miniexp_cdr(item);
    }

    while (miniexp_consp(str)) {
        extractText(env, str, textBuilder, height);
        item = miniexp_cdr(item);
        str = miniexp_car(item);
    }

    return !item;
}


JNIEXPORT jstring JNICALL
JNI_FN(DjvuDocument_getText)(JNIEnv *env, jclass clazz, jlong contextl, jlong docl, jint pageNumber,
                             jobject textBuilder) {
    ddjvu_context_t *context = (ddjvu_context_t *) contextl;
    ddjvu_document_t *doc = (ddjvu_document_t *) docl;

    miniexp_t pagetext;
    while ((pagetext = ddjvu_document_get_pagetext(doc, pageNumber, 0)) == miniexp_dummy) {
        handle_ddjvu_messages_and_wait_stub(env, context);
    }

    if (miniexp_nil == pagetext) {
        return NULL;
    }

    ddjvu_status_t status;
    ddjvu_pageinfo_t info;
    while ((status = ddjvu_document_get_pageinfo(doc, pageNumber, &info)) < DDJVU_JOB_OK) {
        handle_ddjvu_messages_and_wait_stub(env, context);
    }

    resolveErrorIfPossibleStub(env, context, status);
    if (status != DDJVU_JOB_OK) {
        return NULL;
    }

    extractText(env, pagetext, textBuilder, info.height);
    return textBuilder;
}

static int qMax(int a, int b) {
    if (a >= b) {
        return a;
    }
    return b;
}

static int miniexp_get_int(miniexp_t *r, int *x) {
    if (!miniexp_numberp(miniexp_car(*r)))
        return 0;

    *x = miniexp_to_int(miniexp_car(*r));
    *r = miniexp_cdr(*r);

    return 1;
}

static int
parse_text_type(miniexp_t exp) {
    static const char *names[] = {
            "char", "word", "line", "para", "region", "column", "page"
    };
    static int const nsymbs = 7;
    static miniexp_t symbs[7];
    if (!symbs[0]) {
        int i;
        for (i = 0; i < nsymbs; i++)
            symbs[i] = miniexp_symbol(names[i]);
    }

    int i;
    for (i = 0; i < nsymbs; i++)
        if (exp == symbs[i])
            return i;
    return -1;
}


static jobject
miniexp_get_rect(miniexp_t *r, JNIEnv *env, jclass rectFClass, jmethodID ctor, int pageHeight) {
    int x1, y1, x2, y2;
    if (!(miniexp_get_int(r, &x1) && miniexp_get_int(r, &y1) &&
          miniexp_get_int(r, &x2) && miniexp_get_int(r, &y2)))
        return NULL;

    if (x2 < x1 || y2 < y1)
        return NULL;

    jobject rectF;

#ifdef ORION_FOR_ANDROID
    rectF = (*env)->NewObject(env, rectFClass, ctor,
                              (float) x1, (float) (pageHeight - y2), (float) x2,
                              (float) pageHeight - y1);
#endif
    if (rectF == NULL) return NULL;

    //(*env)->DeleteLocalRef(env, rectF);

    return rectF;
}

#ifdef ORION_FOR_ANDROID

static jboolean
miniexp_get_text(JNIEnv *env, miniexp_t exp, jobject stringBuilder, jobject positions, int *state,
                 jclass rectFClass, jmethodID ctor, jmethodID addToList, int pageHeight) {

    miniexp_t type = miniexp_car(exp);
    int typenum = parse_text_type(type);
    miniexp_t r = exp = miniexp_cdr(exp);
    if (!miniexp_symbolp(type))
        return 0;

    jobject rect = miniexp_get_rect(&r, env, rectFClass, ctor, pageHeight);
    if (rect == NULL)
        return 0;

    miniexp_t s = miniexp_car(r);
    *state = qMax(*state, typenum);


    jstring space = (*env)->NewStringUTF(env, " ");
    jstring newLine = (*env)->NewStringUTF(env, "\n");

    if (miniexp_stringp(s) && !miniexp_cdr(r)) {
        //result += (state >= 2) ? "\n" : (state >= 1) ? " " : "";
        if (*state >= 2) {
            (*env)->CallBooleanMethod(env, positions, addToList, rect);
            (*env)->CallBooleanMethod(env, stringBuilder, addToList, newLine);
        } else if (*state >= 1) {
            (*env)->CallBooleanMethod(env, positions, addToList, rect);
            (*env)->CallBooleanMethod(env, stringBuilder, addToList, newLine);
        } else {
            //add empty?
        }
        *state = -1;

        (*env)->CallBooleanMethod(env, positions, addToList, rect);

        jstring string = (*env)->NewStringUTF(env, miniexp_to_str(s));
        (*env)->CallBooleanMethod(env, stringBuilder, addToList, string);
        (*env)->DeleteLocalRef(env, string);

        r = miniexp_cdr(r);
    }

    (*env)->DeleteLocalRef(env, space);
    (*env)->DeleteLocalRef(env, newLine);
    (*env)->DeleteLocalRef(env, rect);

    while (miniexp_consp(s)) {
        miniexp_get_text(env, s, stringBuilder, positions, state, rectFClass, ctor, addToList,
                         pageHeight);
        r = miniexp_cdr(r);
        s = miniexp_car(r);
    }

    if (r)
        return 0;

    *state = qMax(*state, typenum);
    return 1;
}

JNIEXPORT jboolean JNICALL
JNI_FN(DjvuDocument_getPageText)(JNIEnv *env, jclass type, jlong contextl, jlong docl, jint pageNumber, jobject stringBuilder,
                    jobject positionList) {
    LOGI("Start Page Text Extraction %i", pageNumber);
    ddjvu_context_t *context = (ddjvu_context_t *) contextl;
    ddjvu_document_t *doc = (ddjvu_document_t *) docl;

    miniexp_t pagetext;
    while ((pagetext = ddjvu_document_get_pagetext(doc, pageNumber, "word")) == miniexp_dummy) {
        handle_ddjvu_messages_and_wait_stub(env, context);
    }

    if (pagetext == NULL) {
        LOGI("no text on page %i", pageNumber);
        return 0;
    }

    jclass listClass;
    jmethodID addToList;

    listClass = (*env)->FindClass(env, "java/util/ArrayList");
    if (listClass == NULL) return 0;

    addToList = (*env)->GetMethodID(env, listClass, "add", "(Ljava/lang/Object;)Z");
    if (addToList == NULL) return 0;

    jclass rectFClass;
    jmethodID ctor;

    rectFClass = (*env)->FindClass(env, "android/graphics/RectF");
    if (rectFClass == NULL) return 0;

    ctor = (*env)->GetMethodID(env, rectFClass, "<init>", "(FFFF)V");
    if (ctor == NULL) return 0;

    ddjvu_status_t status;
    ddjvu_pageinfo_t dinfo;
    while ((status = ddjvu_document_get_pageinfo(doc, pageNumber, &dinfo)) < DDJVU_JOB_OK) {
        handle_ddjvu_messages_and_wait_stub(env, context);
    }

    resolveErrorIfPossibleStub(env, context, status);
    if (status == DDJVU_JOB_OK) {
        int state = -1;
        return miniexp_get_text(env, pagetext, stringBuilder, positionList, &state, rectFClass,
                                ctor,
                                addToList, dinfo.height);
    } else {
        return 0;
    }
}

#endif
