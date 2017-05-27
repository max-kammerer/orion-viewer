# Orion Viewer: Android Pdf and Djvu reader

Orion Viewer is pdf, djvu, xps, cbz and tiff file viewer for Android
devices based on
[mupdf](http://mupdf.com/docs/how-to-build-mupdf-for-android) and
[DjVuLibre](https://sourceforge.net/p/djvu/djvulibre-git/ci/master/tree/)
libraries 

To build Orion Viewer you will need:

 * android-sdk-r23+
 * gradle 3.3+

To compile native libs you also need:

 * android-ndk-r15b
 * make, git 1.9+

Note: compiled native libs could be downloaded by 'thirdparty_download.gradle' script

Thirdparty build scripted in [thirdparty_build.gradle](thirdparty_build.gradle) file
(details in [orion-viewer/jni/README](orion-viewer/jni/README)).  It should be executed once to build native libs.

Main project build logic scripted in build.gradle.

Specify path to android-sdk, android-ndk and folder to checkout third-party libs in
'local.properties' (use 'local.properties.sample' as example).
