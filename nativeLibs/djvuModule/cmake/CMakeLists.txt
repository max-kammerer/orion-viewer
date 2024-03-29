#LOCAL_ARM_MODE := arm
cmake_minimum_required(VERSION 3.2.2)
project("orion-djvu-library")
set(MY_ROOT "${PROJECT_SOURCE_DIR}/../../djvu/")
set(ANDROID_ROOT "${PROJECT_SOURCE_DIR}/../jni")

add_library(djvu SHARED
        ${ANDROID_ROOT}/djvu.c
        ${ANDROID_ROOT}/../../common/list.c
        ${ANDROID_ROOT}/../../common/orion_bitmap.c
        )

add_library(djvucore STATIC
        ${MY_ROOT}/libdjvu/JB2EncodeCodec.cpp
        ${MY_ROOT}/libdjvu/JB2Image.cpp
        ${MY_ROOT}/libdjvu/JPEGDecoder.cpp
        ${MY_ROOT}/libdjvu/MMRDecoder.cpp
        ${MY_ROOT}/libdjvu/MMX.cpp
        ${MY_ROOT}/libdjvu/UnicodeByteStream.cpp
        ${MY_ROOT}/libdjvu/XMLParser.cpp
        ${MY_ROOT}/libdjvu/XMLTags.cpp
        ${MY_ROOT}/libdjvu/ZPCodec.cpp
        ${MY_ROOT}/libdjvu/Arrays.cpp
        ${MY_ROOT}/libdjvu/atomic.cpp
        ${MY_ROOT}/libdjvu/BSByteStream.cpp
        ${MY_ROOT}/libdjvu/BSEncodeByteStream.cpp
        ${MY_ROOT}/libdjvu/ByteStream.cpp
        ${MY_ROOT}/libdjvu/DataPool.cpp
        ${MY_ROOT}/libdjvu/ddjvuapi.cpp
        ${MY_ROOT}/libdjvu/debug.cpp
        ${MY_ROOT}/libdjvu/DjVmDir.cpp
        ${MY_ROOT}/libdjvu/DjVmDir0.cpp
        ${MY_ROOT}/libdjvu/DjVmDoc.cpp
        ${MY_ROOT}/libdjvu/DjVmNav.cpp
        ${MY_ROOT}/libdjvu/DjVuAnno.cpp
        ${MY_ROOT}/libdjvu/DjVuDocEditor.cpp
        ${MY_ROOT}/libdjvu/DjVuDocument.cpp
        ${MY_ROOT}/libdjvu/DjVuDumpHelper.cpp
        ${MY_ROOT}/libdjvu/DjVuErrorList.cpp
        ${MY_ROOT}/libdjvu/DjVuFile.cpp
        ${MY_ROOT}/libdjvu/DjVuFileCache.cpp
        ${MY_ROOT}/libdjvu/DjVuGlobal.cpp
        ${MY_ROOT}/libdjvu/DjVuGlobalMemory.cpp
        ${MY_ROOT}/libdjvu/DjVuImage.cpp
        ${MY_ROOT}/libdjvu/DjVuInfo.cpp
        ${MY_ROOT}/libdjvu/DjVuMessage.cpp
        ${MY_ROOT}/libdjvu/DjVuMessageLite.cpp
        ${MY_ROOT}/libdjvu/DjVuNavDir.cpp
        ${MY_ROOT}/libdjvu/DjVuPalette.cpp
        ${MY_ROOT}/libdjvu/DjVuPort.cpp
        ${MY_ROOT}/libdjvu/DjVuText.cpp
        ${MY_ROOT}/libdjvu/DjVuToPS.cpp
        ${MY_ROOT}/libdjvu/GBitmap.cpp
        ${MY_ROOT}/libdjvu/GContainer.cpp
        ${MY_ROOT}/libdjvu/GException.cpp
        ${MY_ROOT}/libdjvu/GIFFManager.cpp
        ${MY_ROOT}/libdjvu/GMapAreas.cpp
        ${MY_ROOT}/libdjvu/GOS.cpp
        ${MY_ROOT}/libdjvu/GPixmap.cpp
        ${MY_ROOT}/libdjvu/GRect.cpp
        ${MY_ROOT}/libdjvu/GScaler.cpp
        ${MY_ROOT}/libdjvu/GSmartPointer.cpp
        ${MY_ROOT}/libdjvu/GString.cpp
        ${MY_ROOT}/libdjvu/GThreads.cpp
        ${MY_ROOT}/libdjvu/GUnicode.cpp
        ${MY_ROOT}/libdjvu/GURL.cpp
        ${MY_ROOT}/libdjvu/IFFByteStream.cpp
        ${MY_ROOT}/libdjvu/IW44EncodeCodec.cpp
        ${MY_ROOT}/libdjvu/IW44Image.cpp
        ${MY_ROOT}/libdjvu/miniexp.cpp
        )

#djvucore lib
target_include_directories(djvucore PUBLIC ${MY_ROOT}/libdjvu/)

# -DDEBUGLVL=6 -DRUNTIME_DEBUG_ONLY
target_compile_definitions(djvucore PRIVATE -DHAVE_PTHREAD -DNO_LIBGCC_HOOKS -DHAVE_STDINT_H -DHAVE_NAMESPACES -DHAVE_STDINCLUDES)
#mbstate and wchar
target_compile_definitions(djvucore PRIVATE -DHAS_WCHAR -DHAVE_WCHAR_H -DHAVE_MBSTATE_T  -DHAS_MBSTATE)
#for ernno
target_compile_definitions(djvucore PRIVATE -DUNIX)


#DJVU Android
target_include_directories(djvu PUBLIC ${MY_ROOT}/libdjvu/)

target_compile_definitions(djvu PRIVATE -DORION_FOR_ANDROID)
target_compile_options(djvu PRIVATE -fexceptions -frtti)


target_link_libraries(djvu djvucore  -lm  -llog -ljnigraphics)