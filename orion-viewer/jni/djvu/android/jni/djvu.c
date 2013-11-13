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
ddjvu_context_t *context = NULL;
ddjvu_document_t *doc = NULL;
ddjvu_page_t *page = NULL;


JNIEXPORT int JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_openFile(JNIEnv * env, jobject thiz, jstring jfileName)
{  
	page = NULL;
	context = ddjvu_context_create("orion");
	const char * fileName = (*env)->GetStringUTFChars(env, jfileName, 0);

	LOGI("Opening document: %s", fileName);
	doc = ddjvu_document_create_by_filename_utf8(context, fileName, 0);
	LOGI("Doc opened: %x", doc);

	int pageNum = 0;
	
	if (doc) {
		pageNum =  ddjvu_document_get_pagenum(doc);
	}

	LOGI("Page count = %i", pageNum);

	return 	pageNum;
}


JNIEXPORT void JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_gotoPageInternal(JNIEnv *env, jobject thiz, int pageNum)
{
	LOGI("Opening page: %d", pageNum);
	if (page != NULL) {
	    ddjvu_page_release(page);
	    page = NULL;
	}
	page = ddjvu_page_create_by_pageno(doc, pageNum);
}

JNIEXPORT void JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getPageInfo(JNIEnv *env, jobject thiz, int pageNum, jobject info)
{
	
	clock_t start, end;
	start = clock();

	/*
	ddjvu_page_t * mypage = ddjvu_page_create_by_pageno(doc, pageNum);
	int pageWidth =  ddjvu_page_get_width(mypage);
	int pageHeight = ddjvu_page_get_height(mypage);
	//LOGI("mypage: %x", mypage);
	
	ddjvu_page_release(mypage);
	*/

	ddjvu_pageinfo_t dinfo;
	ddjvu_document_get_pageinfo(doc, pageNum, &dinfo);
	
	jclass cls = (*env)->GetObjectClass(env, info);
	jfieldID width = (*env)->GetFieldID(env, cls, "width", "I");
	jfieldID height = (*env)->GetFieldID(env, cls, "height", "I");
	(*env)->SetIntField(env, info, width, dinfo.width);
	(*env)->SetIntField(env, info, height, dinfo.height);
	 
	end = clock();	
	
	LOGI("Page info get %lf s; page size = %ix%i", ((double) (end - start)) / CLOCKS_PER_SEC, dinfo.width, dinfo.height);
}

JNIEXPORT jboolean JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_drawPage(JNIEnv *env, jobject thiz, jobject bitmap,
        float zoom,
		int pageW, int pageH, int patchX, int patchY, int patchW, int patchH)
{
	LOGI("==================Start Rendering==============");
	int ret;
	AndroidBitmapInfo info;
	void *pixels;
	int num_pixels = pageW * pageH;

	LOGI("Rendering page=%dx%d patch=[%d,%d,%d,%d]",
			pageW, pageH, patchX, patchY, patchW, patchH);
    LOGI("page: %x", page);

    LOGI("In native method\n");
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return 0;
    }

    LOGI("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return 0;
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

    char * buffer = &(((unsigned char *)pixels)[shift*4]);
    jboolean result = ddjvu_page_render(page, DDJVU_RENDER_COLOR, &pageRect, &targetRect, pixelFormat, pageW * 4, buffer);

	ddjvu_format_release(pixelFormat);

	orion_updateContrast((unsigned char *) pixels, num_pixels*4);
    AndroidBitmap_unlockPixels(env, bitmap);
	LOGI("...Rendered");
	
    return 1;
}


JNIEXPORT void JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_destroying(JNIEnv * env, jobject thiz)
{
	LOGI("Closing doc...");	
	
	if (page != NULL) {
		ddjvu_page_release(page);
		page = NULL;
	}
	if (doc != NULL) {
		ddjvu_document_release(doc);
		doc = NULL;
	}
	if (context != NULL) {
		ddjvu_context_release(context);
		context = context;
	}
}


struct list_el {  
  jobject item;
  struct list_item * next;
};

typedef struct list_el list_item;

struct list_st {  
  list_item * head;
  list_item * tail;
};

typedef struct list_st list;

struct OutlineItem_s {  
  char * title;
  int level;
  int page;  
};
typedef struct OutlineItem_s OutlineItem;



JNIEXPORT jobjectArray JNICALL Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getOutline(JNIEnv * env, jobject thiz) 
{
  miniexp_t outline = ddjvu_document_get_outline(doc);
  
  if (outline == miniexp_dummy || outline == NULL) {    
      return NULL;
  }
    
  if (!miniexp_consp(outline) || miniexp_car(outline) != miniexp_symbol("bookmarks"))
  {
     LOGI("Outlines is empty");
  }
      
  list_item * root = (list_item *) malloc(sizeof(list_item)); 
  root->next = NULL;
  
  list * myList = (list *) malloc(sizeof(list));
  myList->head = root;
  myList->tail = root;
  
  jclass        olClass;
  jmethodID     ctor;

  olClass = (*env)->FindClass(env, "universe/constellation/orion/viewer/outline/OutlineItem");
  if (olClass == NULL) return NULL;
  ctor = (*env)->GetMethodID(env, olClass, "<init>", "(ILjava/lang/String;I)V");
  if (ctor == NULL) return NULL;  
  
  buildTOC(miniexp_cdr(outline), myList, 0, env, olClass, ctor);
  
  list_item * next = myList->head;
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
    OutlineItem * item = next->item;
    jstring title = (*env)->NewStringUTF(env, item->title);
    //shift pageno to zero based
    jobject element = (*env)->NewObject(env, olClass, ctor, item->level, title, item->page - 1);
    (*env)->SetObjectArrayElement(env, arr, pos, element);    
    (*env)->DeleteLocalRef(env, title);    
    (*env)->DeleteLocalRef(env, element);
    
    free(item);    
    list_item * next2 = next->next;
    free(next);
    next = next2;
    pos++;    
  }
  free(root);
  
  return arr;  
}


int buildTOC(miniexp_t expr, list * myList, int level, JNIEnv * env, jclass olClass, jmethodID ctor)
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
//          LOGI("Page %s", page);
          int pageno = atoi(&page[1]);
	  
	  if (name == NULL) {return -1;}
	  
	  OutlineItem * element = (list_item *) malloc(sizeof(OutlineItem));
	  element->title = name;
	  element->page = pageno;
	  element->level = level;
	  
	  list_item * next = (list_item *) malloc(sizeof(list_item));
	  next->item = element;
	  next->next = NULL;
	  myList->tail->next = next;
	  myList->tail = next;
	  	  
          // recursion
          buildTOC(miniexp_cddr(s), myList, level+1, env, olClass, ctor);	  
        }
    }
    return 0;
}



JNIEXPORT jstring JNICALL
Java_universe_constellation_orion_viewer_djvu_DjvuDocument_getText(JNIEnv *env, jobject thiz, int pageNumber,
		int startX, int startY, int width, int height)
{

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
