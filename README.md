# Orion Viewer: Android Pdf and Djvu reader

Orion Viewer is pdf, djvu, xps, cbz and tiff file viewer for Android
devices based on
[mupdf](http://mupdf.com/docs/how-to-build-mupdf-for-android) and
[DjVuLibre](https://sourceforge.net/p/djvu/djvulibre-git/ci/master/tree/)
libraries

To build Orion Viewer you will need:

 * android-sdk-r26+
 * gradle 4.10.1+
 * android-ndk-r16b
 * make
 * git

 * downloaded Native Libs [mupdf, djvu]:

    ./gradlew -b  thirdparty_build.gradle downloadAndPatchDjvu downloadAndMakeMupdf

    They are defined in gradle scripts via 'externalNativeBuild' section
    (for details see 'djvuModule/build.gradle' and 'mupdfModule/build.gradle').
    Native libs are checkouted into 'nativeLibs/djvu' and 'nativeLibs/mupdf' folders.

 * specify path to android-sdk, android-ndk in 'local.properties' (use 'local.properties.sample' as example).