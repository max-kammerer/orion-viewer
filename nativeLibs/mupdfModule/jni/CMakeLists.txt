# Mention the cmake version name.
cmake_minimum_required(VERSION 3.4.1)
set(MUPDF_PATH ../../mupdf)
set(LIBARCH ../../libarchive)
set(XZ_PATH ../../xz)
set(WEBP_PATH ../../libwebp)
set(BROTLI ${MUPDF_PATH}/thirdparty/brotli/c)

# Thirdparty source lists are parsed out of mupdf's own Makelists so they
# stay in sync with the mupdf checkout instead of being maintained by hand.
set(MUPDF_MAKELISTS ${CMAKE_CURRENT_SOURCE_DIR}/${MUPDF_PATH}/Makelists)
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS ${MUPDF_MAKELISTS})

function(makelists_sources var prefix)
    file(STRINGS ${MUPDF_MAKELISTS} lines REGEX "^${prefix}_SRC \\+= ")
    set(result "")
    foreach(line IN LISTS lines)
        string(REGEX REPLACE "^${prefix}_SRC \\+= *" "" path "${line}")
        string(STRIP "${path}" path)
        if(path MATCHES "\\$")
            message(FATAL_ERROR "${prefix}_SRC entry needs make expansion, cannot parse: ${line}")
        endif()
        if(NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/${MUPDF_PATH}/${path})
            message(FATAL_ERROR "${prefix}_SRC entry does not exist: ${path}")
        endif()
        list(APPEND result ${MUPDF_PATH}/${path})
    endforeach()
    if(NOT result)
        message(FATAL_ERROR "no ${prefix}_SRC entries found in ${MUPDF_MAKELISTS}")
    endif()
    set(${var} ${result} PARENT_SCOPE)
endfunction()

# --- Build a local static library for core mupdf ---
file(GLOB mupdf_core_SRC
${MUPDF_PATH}/source/fitz/*.c
${MUPDF_PATH}/source/fitz/*.cpp
${MUPDF_PATH}/source/pdf/*.c
${MUPDF_PATH}/source/xps/*.c
${MUPDF_PATH}/source/svg/*.c
${MUPDF_PATH}/source/cbz/*.c
${MUPDF_PATH}/source/html/*.c
${MUPDF_PATH}/generated/resources/fonts/urw/*.c
${MUPDF_PATH}/generated/resources/fonts/sil/*.c
${MUPDF_PATH}/generated/resources/hyphen/*.c
)

file(GLOB LIBARCH_INCLUDE
        ${LIBARCH}/libarchive/*.h
)

add_library(mupdf_core
    STATIC
    ${mupdf_core_SRC}
)

include_directories(
${MUPDF_PATH}/include.mupdf
${MUPDF_PATH}/scripts/freetype
${MUPDF_PATH}/scripts/libjpeg
${MUPDF_PATH}/thirdparty/brotli/c/include
${MUPDF_PATH}/thirdparty/freetype/include
${MUPDF_PATH}/thirdparty/harfbuzz/src
${MUPDF_PATH}/thirdparty/jbig2dec
${MUPDF_PATH}/thirdparty/libjpeg
${MUPDF_PATH}/thirdparty/lcms2/include
${MUPDF_PATH}/thirdparty/mujs
${MUPDF_PATH}/thirdparty/openjpeg/src/lib/openjp2
${MUPDF_PATH}/thirdparty/zlib
${MUPDF_PATH}/thirdparty/extract/include
${MUPDF_PATH}/thirdparty/gumbo-parser/src
${MUPDF_PATH}/thirdparty/cmark-gfm/src
${MUPDF_PATH}/thirdparty/cmark-gfm/extensions
${MUPDF_PATH}/scripts/cmark-gfm
)

#LOCAL_CFLAGS := \
#-ffunction-sections -fdata-sections \
#-D_FILE_OFFSET_BITS=32 \
#-DTOFU_NOTO -DTOFU_CJK \
#-DAA_BITS=8 \
#-DOPJ_STATIC -DOPJ_HAVE_INTTYPES_H -DOPJ_HAVE_STDINT_H \
#-DHAVE_LCMS2MT \

target_compile_definitions(mupdf_core PRIVATE -D_FILE_OFFSET_BITS=32 -DTOFU_NOTO -DTOFU_CJK -DTOFU_SIL -DAA_BITS=8 -DOPJ_STATIC -DOPJ_HAVE_INTTYPES_H -DOPJ_HAVE_STDINT_H -DHAVE_LCMS2MT -DHAVE_LIBARCHIVE -DFZ_ENABLE_BARCODE=0 -DCMARK_GFM_STATIC_DEFINE)
target_compile_options(mupdf_core PRIVATE -ffunction-sections -fdata-sections)

# --- liblzma (decoders only): LZMA/LZMA2 for 7z support in libarchive ---

if(NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/${XZ_PATH}/CMakeLists.txt)
    message(FATAL_ERROR "xz sources not found in nativeLibs/xz: run './gradlew -b thirdparty_build.gradle downloadXz' first")
endif()

set(XZ_TOOL_XZ OFF)
set(XZ_TOOL_XZDEC OFF)
set(XZ_TOOL_LZMADEC OFF)
set(XZ_TOOL_LZMAINFO OFF)
set(XZ_TOOL_SCRIPTS OFF)
set(XZ_NLS OFF)
set(XZ_DOC OFF)
set(XZ_MICROLZMA_ENCODER OFF)
set(XZ_MICROLZMA_DECODER OFF)
set(XZ_LZIP_DECODER OFF)
set(XZ_ENCODERS "")
set(BUILD_TESTING OFF)
add_subdirectory(${XZ_PATH} xz)

# --- libwebp (decoder only): WebP pages in comic archives and standalone WebP images ---

if(NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/${WEBP_PATH}/CMakeLists.txt)
    message(FATAL_ERROR "libwebp sources not found in nativeLibs/libwebp: run './gradlew -b thirdparty_build.gradle downloadLibwebp' first")
endif()

set(WEBP_BUILD_ANIM_UTILS OFF)
set(WEBP_BUILD_CWEBP OFF)
set(WEBP_BUILD_DWEBP OFF)
set(WEBP_BUILD_GIF2WEBP OFF)
set(WEBP_BUILD_IMG2WEBP OFF)
set(WEBP_BUILD_VWEBP OFF)
set(WEBP_BUILD_WEBPINFO OFF)
set(WEBP_BUILD_WEBPMUX OFF)
set(WEBP_BUILD_EXTRAS OFF)
set(WEBP_BUILD_LIBWEBPMUX OFF)
# mupdf decodes an image on one thread; libwebp's own threads would only add pthread use.
set(WEBP_USE_THREAD OFF)
# Only the webpdecoder target is linked, the encoder and demux libraries are never built.
add_subdirectory(${WEBP_PATH} libwebp EXCLUDE_FROM_ALL)

target_compile_definitions(mupdf_core PRIVATE -DHAVE_WEBP)
target_include_directories(mupdf_core PRIVATE ${WEBP_PATH}/src)

# --- Build a local static library for thirdparty libraries ---

file(GLOB LIBARCH_SOURCE
        ${LIBARCH}/libarchive/*.c
)

add_library( libarchive STATIC
    ${LIBARCH_SOURCE}
)

include_directories(
    ${LIBARCH_INCLUDE}
    ${LIBARCH}/contrib/android/include/
)

target_compile_definitions(libarchive PRIVATE
    -DHAVE_INTTYPES_H
    -DOPJ_HAVE_STDINT_H
    -DPLATFORM_CONFIG_H="${LIBARCH}/contrib/android/config/android.h"
    -D__LIBARCHIVE_CONFIG_H_INCLUDED
    -DHAVE_DECL_INT32_MAX
    -DHAVE_DECL_INT32_MIN
    -DHAVE_LZMA_H
    -DHAVE_LIBLZMA
)
target_include_directories(libarchive PRIVATE ${XZ_PATH}/src/liblzma/api)

# --- Build a local static library for thirdparty libraries ---

makelists_sources(FREETYPE_SRC FREETYPE)
makelists_sources(HARFBUZZ_SRC HARFBUZZ)
makelists_sources(JBIG2DEC_SRC JBIG2DEC)
makelists_sources(LIBJPEG_SRC LIBJPEG)
makelists_sources(LCMS2_SRC LCMS2)
makelists_sources(MUJS_SRC MUJS)
makelists_sources(OPENJPEG_SRC OPENJPEG)
makelists_sources(ZLIB_SRC ZLIB)
makelists_sources(EXTRACT_SRC EXTRACT)
makelists_sources(GUMBO_SRC GUMBO)
makelists_sources(BROTLI_SRC BROTLI)

add_library( mupdf_thirdparty
STATIC
${FREETYPE_SRC}
${HARFBUZZ_SRC}
${JBIG2DEC_SRC}
${LIBJPEG_SRC}
${LCMS2_SRC}
${MUJS_SRC}
${OPENJPEG_SRC}
${ZLIB_SRC}
${EXTRACT_SRC}
${GUMBO_SRC}
${BROTLI_SRC}
)

# cmark-gfm is built as a separate target because its internal headers are
# included as <parser.h>/<utf8.h>, which clash with gumbo-parser's headers
# in the global include path; BEFORE puts its own dirs first.
makelists_sources(CMARKGFM_SRC CMARKGFM)

add_library( cmark_gfm STATIC
    ${CMARKGFM_SRC}
)

target_include_directories(cmark_gfm BEFORE PRIVATE
    ${MUPDF_PATH}/thirdparty/cmark-gfm/src
    ${MUPDF_PATH}/thirdparty/cmark-gfm/extensions
    ${MUPDF_PATH}/scripts/cmark-gfm
)

target_compile_definitions(cmark_gfm PRIVATE -DCMARK_GFM_STATIC_DEFINE)
target_compile_options(cmark_gfm PRIVATE -ffunction-sections -fdata-sections)

include_directories(
${MUPDF_PATH}/include
${MUPDF_PATH}/include/mupdf
${MUPDF_PATH}/scripts/freetype
${MUPDF_PATH}/scripts/libjpeg
${MUPDF_PATH}/thirdparty/freetype/include
${MUPDF_PATH}/thirdparty/harfbuzz/src
${MUPDF_PATH}/thirdparty/jbig2dec
${MUPDF_PATH}/thirdparty/libjpeg
${MUPDF_PATH}/thirdparty/lcms2/include
${MUPDF_PATH}/thirdparty/mujs
${MUPDF_PATH}/thirdparty/openjpeg/src/lib/openjp2
${MUPDF_PATH}/thirdparty/zlib
${MUPDF_PATH}/thirdparty/extract/include
${MUPDF_PATH}/thirdparty/gumbo-parser/src
${LIBARCH}/libarchive/
)

#LOCAL_CFLAGS := \
#-ffunction-sections -fdata-sections \
#-DFT2_BUILD_LIBRARY -DDARWIN_NO_CARBON \
#'-DFT_CONFIG_MODULES_H="slimftmodules.h"' \
#'-DFT_CONFIG_OPTIONS_H="slimftoptions.h"' \
#-DHAVE_STDINT_H \
#-DOPJ_STATIC -DOPJ_HAVE_INTTYPES_H -DOPJ_HAVE_STDINT_H \
#
#
#LOCAL_CPPFLAGS := \
#-ffunction-sections -fdata-sections \
#-fno-rtti -fno-exceptions -fvisibility-inlines-hidden \
#-DHAVE_FALLBACK=1 -DHAVE_OT -DHAVE_UCDN -DHB_NO_MT \
#-Dhb_malloc_impl=fz_hb_malloc \
#-Dhb_calloc_impl=fz_hb_calloc \
#-Dhb_realloc_impl=fz_hb_realloc \
#-Dhb_free_impl=fz_hb_free \

target_compile_definitions(mupdf_thirdparty PRIVATE
        -DFT2_BUILD_LIBRARY
        -DDARWIN_NO_CARBON
        -DFT_CONFIG_MODULES_H="slimftmodules.h"
        -DFT_CONFIG_OPTIONS_H="slimftoptions.h"
        -DHAVE_STDINT_H
        -DOPJ_STATIC
        -DOPJ_HAVE_INTTYPES_H
        -DOPJ_HAVE_STDINT_H
        -DHAVE_FALLBACK=1
        -DHAVE_OT
        -DHAVE_UCDN
        -DHAVE_FREETYPE
        -DHB_NO_MT
        -Dhb_malloc_impl=fz_hb_malloc
        -Dhb_calloc_impl=fz_hb_calloc
        -Dhb_realloc_impl=fz_hb_realloc
        -Dhb_free_impl=fz_hb_free
)

target_compile_options(mupdf_thirdparty PRIVATE -ffunction-sections -fdata-sections -fno-rtti -fno-exceptions -fvisibility-inlines-hidden)


# --- Build the final JNI shared library ---

add_library( mupdf_java
            SHARED
../../common/orion_bitmap.c
../../common/list.c
${MUPDF_PATH}/platform/java/mupdf_native.c
)

# NDK 17 (Android 4.0 build) links libgcc, which lacks the compiler-rt helper
# clang emits for 64-bit __builtin_mul_overflow in libarchive on 32-bit ABIs.
if(NOT DEFINED ANDROID_NDK_MAJOR OR ANDROID_NDK_MAJOR LESS 23)
    target_sources(mupdf_java PRIVATE ../../common/compat_mulodi4.c)
endif()


include_directories(
${MUPDF_PATH}/include
${MUPDF_PATH}/platform/java/
${MUPDF_PATH}/include.mupdf
)

target_compile_definitions(mupdf_java PRIVATE -DHAVE_ANDROID -DORION_PDF -DORION_FOR_ANDROID -DFZ_JAVA_STORE_SIZE=64<<20)
target_compile_options(mupdf_java PRIVATE -ffunction-sections -fdata-sections)

add_dependencies(mupdf_java mupdf_core mupdf_thirdparty cmark_gfm libarchive liblzma webpdecoder)
target_link_libraries(mupdf_java mupdf_core mupdf_thirdparty cmark_gfm libarchive liblzma webpdecoder)

#LOCAL_LDLIBS := -ljnigraphics -llog -lm
#
#LOCAL_LDFLAGS := -Wl,--gc-sections
#

target_link_libraries(
        mupdf_java
        -ljnigraphics
        -llog
        -lm
        -Wl,--gc-sections
)

if(ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "x86_64")
    target_link_libraries(
            mupdf_java
            -Wl,-z,max-page-size=16384
    )
endif()
