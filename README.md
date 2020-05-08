# Orion Viewer: Android Pdf and Djvu reader

Orion Viewer is pdf, djvu, xps, cbz and tiff file viewer for Android
devices based on
[mupdf](http://mupdf.com/) and
[DjVuLibre](https://sourceforge.net/p/djvu/djvulibre-git/ci/master/tree/)
libraries

To build Orion Viewer you will need:

 * Android Studio 3.6.2
 * android-sdk 28+
 * android-ndk 20+
 * gradle 5.6.4+
 * android-ndk-r20+
 * make + python2 for mupdf
 * git

 * downloaded Native Libs [mupdf, djvu]:

    `./gradlew -b  thirdparty_build.gradle downloadAndPatchDjvu downloadAndMakeMupdf`

    They are defined in gradle scripts via 'externalNativeBuild' section
    (for details see 'djvuModule/build.gradle' and 'mupdfModule/build.gradle').
    Native libs are checked out into 'nativeLibs/djvu' and 'nativeLibs/mupdf' folders.

 * specify path to android-sdk in 'local.properties' (use 'local.properties.sample' as example).

 * By default sources for native libs are included to build.
  If you have any freezes with IDE you can exclude them via 'orion.excludeNativeLibsSources'
  flag in local.properties.
