#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include <miniexp.h>
#include <ddjvuapi.h>
#include "debug.h"

#include "list.c"
#include "base_geometry.c"

#define LOG_TAG "djvulib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern void orion_updateContrast(unsigned char *, int);

/* Globals */
//ddjvu_context_t *context = NULL;
//ddjvu_document_t *doc = NULL;
//ddjvu_page_t *page = NULL;

static inline jlong jlong_cast(void *p)
{
    return (jlong)(intptr_t)p;
}


JNIEXPORT jlong JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_initContext(JNIEnv * env, jobject thiz)
{
    LOGI("Creating context");
    return jlong_cast(ddjvu_context_create("orion"));
}

JNIEXPORT jlong JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_openFile(JNIEnv * env, jobject thiz, jstring jfileName, jobject docInfo, jlong contextl) {
    ddjvu_context_t *context = (ddjvu_context_t *) contextl;
    const char *fileName = (*env)->GetStringUTFChars(env, jfileName, 0);

    LOGI("Opening document: %s", fileName);
    ddjvu_document_t *doc = ddjvu_document_create_by_filename_utf8(context, fileName, 0);

    LOGI("Start decoding document: %p", doc);
    ddjvu_status_t r;
    while ((r = ddjvu_document_decoding_status(doc)) < DDJVU_JOB_OK);

    if (r == DDJVU_JOB_OK) {
        LOGI("Doc opened successfully: %p", doc);

        int pageNum = 0;

        if (doc) {
            pageNum = ddjvu_document_get_pagenum(doc);
        }

        LOGI("Page count = %i", pageNum);

        jclass cls = (*env)->GetObjectClass(env, docInfo);
        jfieldID pageCountF = (*env)->GetFieldID(env, cls, "pageCount", "I");
        //jfieldID titleF = (*env)->GetFieldID(env, cls, "title", "Ljava/lang/String;");
        (*env)->SetIntField(env, docInfo, pageCountF, pageNum);
        //    if (title) {
        //        (*env)->SetObjectField(env, docInfo, titleF, ((*env)->NewStringUTF(env, title)));
        //    }
    } else {
        LOGI("Error during document opening: %p", doc);
        ddjvu_document_release(doc);
        doc = NULL;
    }

    return jlong_cast(doc);
}


JNIEXPORT jlong JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_gotoPageInternal(JNIEnv *env, jobject thiz, jlong docl, int pageNum)
{
    ddjvu_document_t * doc = (ddjvu_document_t *) docl;
	LOGI("Opening page: %d", pageNum);
    ddjvu_page_t * page = ddjvu_page_create_by_pageno(doc, pageNum);

    LOGI("Start decoding page: %p", page);
	while(!ddjvu_page_decoding_done(page));
	LOGI("End decoding page: %p", page);

    return jlong_cast(page);
}

JNIEXPORT void JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getPageInfo(JNIEnv *env, jobject thiz, jlong docl, int pageNum, jobject info)
{
	
	clock_t start, end;
	start = clock();

	/*
	ddjvu_page_t * mypage = ddjvu_page_create_by_pageno(doc, pageNum);
	int pageWidth =  ddjvu_page_get_width(mypage);
	int pageHeight = ddjvu_page_get_height(mypage);
	//LOGI("mypage: %p", mypage);
	
	ddjvu_page_release(mypage);
	*/

    ddjvu_document_t * doc = (ddjvu_document_t *) docl;
    ddjvu_status_t r;
	ddjvu_pageinfo_t dinfo;
	while ((r=ddjvu_document_get_pageinfo(doc, pageNum, &dinfo))<DDJVU_JOB_OK) {}
         //handle_ddjvu_messages(ctx, TRUE);

    if (r>=DDJVU_JOB_FAILED) {
        LOGI("Page info get fail!");
        //signal_error();
        return;
    }
	
	jclass cls = (*env)->GetObjectClass(env, info);
	jfieldID width = (*env)->GetFieldID(env, cls, "width", "I");
	jfieldID height = (*env)->GetFieldID(env, cls, "height", "I");
	(*env)->SetIntField(env, info, width, dinfo.width);
	(*env)->SetIntField(env, info, height, dinfo.height);
	 
	end = clock();	
	
	LOGI("Page info get %lf s; page size = %ix%i", ((double) (end - start)) / CLOCKS_PER_SEC, dinfo.width, dinfo.height);
}

JNIEXPORT jboolean JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_drawPage(JNIEnv *env, jobject thiz, jlong docl, jlong pagel, jobject bitmap,
        float zoom,
		int pageW, int pageH, int patchX, int patchY, int patchW, int patchH)
{
    ddjvu_document_t * doc = (ddjvu_document_t *) docl;
    ddjvu_page_t * page = (ddjvu_page_t *) pagel;

	LOGI("==================Start Rendering==============");
	int ret;
	AndroidBitmapInfo info;
	void *pixels;
	int num_pixels = pageW * pageH;

	LOGI("Rendering page=%dx%d patch=[%d,%d,%d,%d]",
			pageW, pageH, patchX, patchY, patchW, patchH);
    LOGI("page: %p", page);

    LOGI("In native method\n");
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return 0;
    }

    LOGI("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888! Format is %i", info.format);
        //return 0;
    }

    LOGI("locking pixels\n");
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return 0;
    }

			
    //float zoom = 0.0001f * zoom10000;
	
	int pageWidth =  ddjvu_page_get_width(page);
	int pageHeight = ddjvu_page_get_height(page);
	
    ddjvu_rect_t pageRect;
    pageRect.x = 0;
    pageRect.y = 0;
    pageRect.w = round(zoom * pageWidth);
    pageRect.h = round(zoom * pageHeight);
	LOGI("Original page=%dx%d patch=[%d,%d,%d,%d]",
			pageWidth, pageHeight, pageRect.x, pageRect.y, pageRect.w, pageRect.h);
    ddjvu_rect_t targetRect;
    targetRect.x = patchX;
    targetRect.y = patchY;
    targetRect.w = patchW;
    targetRect.h = patchH;
	
	int shift = 0;
	if (targetRect.x < 0) {
	   shift = -targetRect.x;
	   targetRect.w += targetRect.x;
	   targetRect.x = 0;
	}
	if (targetRect.y < 0) {
	    shift +=  -targetRect.y * pageW;
		targetRect.h += targetRect.y;
		targetRect.y = 0;
	}
	
	
	if (pageRect.w <  targetRect.x + targetRect.w) {
	  targetRect.w = targetRect.w - (targetRect.x + targetRect.w - pageRect.w);
	}
	if (pageRect.h <  targetRect.y + targetRect.h) {
	    targetRect.h = targetRect.h - (targetRect.y + targetRect.h - pageRect.h);
	}
	memset(pixels, 255, num_pixels * 4);
	 
	LOGI("Rendering page=%dx%d patch=[%d,%d,%d,%d]",
			patchW, patchH, targetRect.x, targetRect.y, targetRect.w, targetRect.h);
	 
    unsigned int masks[4] = { 0xff, 0xff00, 0xff0000, 0xff000000 };
    ddjvu_format_t* pixelFormat = ddjvu_format_create(DDJVU_FORMAT_RGBMASK32, 4, masks);

    LOGI("zoom=%f ", zoom);

    ddjvu_format_set_row_order(pixelFormat, TRUE);

    ddjvu_format_set_y_direction(pixelFormat, TRUE);

    char * buffer = &(((char *)pixels)[shift*4]);
    jboolean result = ddjvu_page_render(page, DDJVU_RENDER_COLOR, &pageRect, &targetRect, pixelFormat, pageW * 4, buffer);

	ddjvu_format_release(pixelFormat);

	orion_updateContrast((unsigned char *) pixels, num_pixels*4);
    AndroidBitmap_unlockPixels(env, bitmap);
	LOGI("...Rendered");
	
    return 1;
}


JNIEXPORT void JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_destroying(JNIEnv * env, jobject thiz, jlong doc, jlong context)
{
	LOGI("Closing doc...");	

	if (doc != 0) {
		ddjvu_document_release((ddjvu_document_t *) doc);
	}

    LOGI("Closing context...");
	if (context != 0) {
		ddjvu_context_release((ddjvu_context_t *) context);
	}
}

JNIEXPORT void JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_releasePage(JNIEnv * env, jobject thiz, jlong page)
{
    if (page != 0) {
        ddjvu_page_release((ddjvu_page_t *)page);
    }
}


struct list_el {  
  jobject item;
  struct list_el * next;
};

typedef struct list_el list_item;

struct list_st {  
  list_item * head;
  list_item * tail;
};

typedef struct list_st list;

struct OutlineItem_s {  
  const char * title;
  int level;
  int page;
};
typedef struct OutlineItem_s OutlineItem;



int buildTOC(ddjvu_document_t * doc, miniexp_t expr, list * myList, int level, JNIEnv * env, jclass olClass, jmethodID ctor)
{
    while(miniexp_consp(expr))
    {
        miniexp_t s = miniexp_car(expr);
        expr = miniexp_cdr(expr);
        if (miniexp_consp(s) &&
            miniexp_consp(miniexp_cdr(s)) &&
            miniexp_stringp(miniexp_car(s)) &&
            miniexp_stringp(miniexp_cadr(s)) )
        {
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

            if (name == NULL) {return -1;}

            OutlineItem * element = (OutlineItem *) malloc(sizeof(OutlineItem));
            element->title = name;
            element->page = pageno;
            element->level = level;

            list_item * next = (list_item *) malloc(sizeof(list_item));
            next->item = element;
            next->next = NULL;
            myList->tail->next = next;
            myList->tail = next;

            // recursion
            buildTOC(doc, miniexp_cddr(s), myList, level+1, env, olClass, ctor);
        }
    }
    return 0;
}


JNIEXPORT jobjectArray JNICALL Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getOutline(JNIEnv * env, jobject thiz, jlong docl)
{
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

    olClass = (*env)->FindClass(env, "universe/constellation/orion/viewer/outline/OutlineItem");
    if (olClass == NULL) return NULL;
    ctor = (*env)->GetMethodID(env, olClass, "<init>", "(ILjava/lang/String;I)V");
    if (ctor == NULL) return NULL;

    buildTOC(doc, miniexp_cdr(outline), myList, 0, env, olClass, ctor);

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
        jstring title = (*env)->NewStringUTF(env, item->title);
        //shift pageno to zero based
        jobject element = (*env)->NewObject(env, olClass, ctor, item->level, title, item->page);
        (*env)->SetObjectArrayElement(env, arr, pos, element);
        (*env)->DeleteLocalRef(env, title);
        (*env)->DeleteLocalRef(env, element);

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



//sumatrapdf code
int extractText(miniexp_t item, Arraylist list, fz_bbox * target) {
    miniexp_t type = miniexp_car(item);

    if (!miniexp_symbolp(type))
        return 0;

    item = miniexp_cdr(item);

    if (!miniexp_numberp(miniexp_car(item))) return 0;
    int x0 = miniexp_to_int(miniexp_car(item)); item = miniexp_cdr(item);
    if (!miniexp_numberp(miniexp_car(item))) return 0;
    int y0 = miniexp_to_int(miniexp_car(item)); item = miniexp_cdr(item);
    if (!miniexp_numberp(miniexp_car(item))) return 0;
    int x1 = miniexp_to_int(miniexp_car(item)); item = miniexp_cdr(item);
    if (!miniexp_numberp(miniexp_car(item))) return 0;
    int y1 = miniexp_to_int(miniexp_car(item)); item = miniexp_cdr(item);
    //RectI rect = RectI::FromXY(x0, y0, x1, y1);
    fz_bbox rect = {x0 , y0 , x1 , y1};

    miniexp_t str = miniexp_car(item);

    if (miniexp_stringp(str) && !miniexp_cdr(item)) {
        fz_bbox inters = fz_intersect_bbox(rect, *target);
        //LOGI("Start text extraction: rectangle=[%d,%d,%d,%d] %s", rect.x0, rect.y0, rect.x1, rect.y1, content);
        if (!fz_is_empty_bbox(inters)) {
            const char *content = miniexp_to_str(str);

            while (*content) {
                arraylist_add(list, *content++);
            }

            //        if (value) {
            //            size_t len = str::Len(value);
            //            // TODO: split the rectangle into individual parts per glyph
            //            for (size_t i = 0; i < len; i++)
            //                coords.Append(RectI(rect.x, rect.y, rect.dx, rect.dy));
            //            extracted.AppendAndFree(value);
            //        }
            if (miniexp_symbol("word") == type) {
                arraylist_add(list, ' ');
                //coords.Append(RectI(rect.x + rect.dx, rect.y, 2, rect.dy));
            }
            else if (miniexp_symbol("char") != type) {
                arraylist_add(list, '\n');
                //            extracted.Append(lineSep);
                //            for (size_t i = 0; i < str::Len(lineSep); i++)
                //                coords.Append(RectI());
            }
        }
        item = miniexp_cdr(item);
    }

    while (miniexp_consp(str)) {
        extractText(str, list, target);
        item = miniexp_cdr(item);
        str = miniexp_car(item);
    }

    return !item;
}


JNIEXPORT jstring JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getText(JNIEnv *env, jobject thiz, jlong docl, int pageNumber,
		int startX, int startY, int width, int height)
{
    ddjvu_document_t * doc = (ddjvu_document_t *) docl;
    LOGI("==================Start Text Extraction==============");

    miniexp_t pagetext;
    while ((pagetext=ddjvu_document_get_pagetext(doc,pageNumber,0))==miniexp_dummy) {
      //handle_ddjvu_messages(ctx, TRUE);
    }

    if (miniexp_nil == pagetext) {
        return NULL;
    }
//
    ddjvu_status_t status;
    ddjvu_pageinfo_t info;
    while ((status = ddjvu_document_get_pageinfo(doc, pageNumber, &info)) < DDJVU_JOB_OK) {
        //nothing
    }

	LOGI("Extraction rectangle=[%d,%d,%d,%d]", startX, startY, width, height);

	Arraylist values = arraylist_create();

    int w = info.width;
    int h = info.height;
    fz_bbox target = {startX, h - startY - height, startX + width, h - startY};
    LOGI("Extraction irectangle=[%d,%d,%d,%d]", target.x0, target.y0, target.x1, target.y1);

	extractText(pagetext, values, &target);

	arraylist_add(values, 0);

    LOGI("Data: %s", arraylist_getData(values));
    jstring result = (*env)->NewStringUTF(env, arraylist_getData(values));
    arraylist_free(values);

    return result;
}

static int qMax(int a, int b) {
    if (a >= b) {
        return a;
    }
    return b;
}

static int miniexp_get_int(miniexp_t * r, int * x)
{
  if (! miniexp_numberp(miniexp_car(*r)))
    return 0;

  *x = miniexp_to_int(miniexp_car(*r));
  *r = miniexp_cdr(*r);

  return 1;
}

static int
parse_text_type(miniexp_t exp)
{
  static const char *names[] = {
    "char", "word", "line", "para", "region", "column", "page"
  };
  static int const nsymbs = 7;
  static miniexp_t symbs[7];
  if (!symbs[0]) {
    int i;
    for (i=0; i<nsymbs; i++)
      symbs[i] = miniexp_symbol(names[i]);
  }

 int i;
  for (i=0; i<nsymbs; i++)
    if (exp == symbs[i])
      return i;
  return -1;
}


static jobject
miniexp_get_rect(miniexp_t *r, JNIEnv * env, jclass rectFClass, jmethodID ctor, int pageHeight)
{
      int x1,y1,x2,y2;
      if (! (miniexp_get_int(r, &x1) && miniexp_get_int(r, &y1) &&
             miniexp_get_int(r, &x2) && miniexp_get_int(r, &y2) ))
            return NULL;

    if (x2<x1 || y2<y1)
        return NULL;

    jobject rectF;
    rectF = (*env)->NewObject(env, rectFClass, ctor,
                    (float)x1, (float)(pageHeight - y2), (float)x2, (float)pageHeight - y1);

    if (rectF == NULL) return NULL;

    //(*env)->DeleteLocalRef(env, rectF);

    return rectF;
}


static jboolean miniexp_get_text(JNIEnv * env, miniexp_t exp, jobject stringBuilder, jobject positions, int *state,
                        jclass rectFClass, jmethodID ctor, jmethodID addToList, int pageHeight)
{

  miniexp_t type = miniexp_car(exp);
  int typenum = parse_text_type(type);
  miniexp_t r = exp = miniexp_cdr(exp);
  if (! miniexp_symbolp(type))
    return 0;

  jobject rect = miniexp_get_rect(&r, env, rectFClass, ctor, pageHeight);
  if (rect == NULL)
    return 0;

  miniexp_t s = miniexp_car(r);
  *state = qMax(*state, typenum);

  jstring space = (*env)->NewStringUTF(env, " ");
  jstring newLine = (*env)->NewStringUTF(env, "\n");

  if (miniexp_stringp(s) && !miniexp_cdr(r))
    {
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

  while(miniexp_consp(s))
    {
      miniexp_get_text(env, s, stringBuilder, positions, state, rectFClass, ctor, addToList, pageHeight);
      r = miniexp_cdr(r);
      s = miniexp_car(r);
    }

  if (r)
    return 0;

  *state = qMax(*state, typenum);
  return 1;
}

JNIEXPORT jboolean JNICALL Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getPageText(JNIEnv *env, jobject thiz, jlong  docl,
                                                                                  jint pageNumber, jobject stringBuilder, jobject positionList)
{
    LOGI("Start Page Text Extraction %i", pageNumber);
    ddjvu_document_t * doc = (ddjvu_document_t *) docl;

    miniexp_t pagetext;
    while ((pagetext=ddjvu_document_get_pagetext(doc, pageNumber, "word"))==miniexp_dummy) {
      //handle_ddjvu_messages(ctx, TRUE);
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

    int state = -1;

    ddjvu_pageinfo_t dinfo;
    ddjvu_status_t r;
    while ((r=ddjvu_document_get_pageinfo(doc, pageNumber, &dinfo))<DDJVU_JOB_OK) {}
         //handle_ddjvu_messages(ctx, TRUE);

    return miniexp_get_text(env, pagetext, stringBuilder, positionList, &state, rectFClass, ctor, addToList, dinfo.height);
}
