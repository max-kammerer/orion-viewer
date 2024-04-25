![GitHub code size](https://img.shields.io/github/languages/code-size/max-kammerer/orion-viewer.svg)
![GitHub Releases Downloads](https://img.shields.io/github/downloads/max-kammerer/orion-viewer/total.svg?label=GitHub%20Releases%20Downloads)
![GitHub release](https://img.shields.io/github/release/max-kammerer/orion-viewer.svg)

Orion Viewer is *pdf*, *djvu*, *xps*, *cbz* and *tiff* file viewer for Android
devices based on
[MuPDF](https://mupdf.com) and
[DjVuLibre](https://sourceforge.net/p/djvu/djvulibre-git/ci/master/tree/)
libraries

### Application features
* Outline navigation
* Bookmarks support
* Page navigation by screen taps + Tap Zones + Key binding
* Text selection
* Single word selection by double tap with translation in external dictionary
* Custom zoom
* Custom border crop
* Portrait/landscape orientation
* Support different navigation patterns inside page (left to right, right to left)
* External Dictionaries support
* Built-in file manager with recently opened file view

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/universe.constellation.orion.viewer/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=universe.constellation.orion.viewer)

## Contibution

Whatever you want! Project is open to any ideas and discussions

### Translations

Project is fully or partially translated into Chinese, English, French, German, Hebrew, Italian, Russian, Spanish and Ukrainian languages.
It would be highly desirable to eliminate gaps and errors in existing translations and to translate project into new languages.

Take a look into
[translations](https://github.com/max-kammerer/orion-viewer/wiki/Translations) page on wiki.

## How to build project?

To build `Orion Viewer` you will need:

 * Android Studio 2023.1+ (for development)
 * android-sdk 33+
 * android-ndk 23.2.8568313
 * make and python3 for mupdf
 * git

 * downloaded native libs (mupdf, djvu):

    `./gradlew -b  thirdparty_build.gradle downloadDjvu downloadAndMakeMupdf`

    Build scripts for them are defined in `externalNativeBuild` section in gradle build files
    (for details see `djvuModule/build.gradle` and `mupdfModule/build.gradle`).
    Native libs are checked out into `nativeLibs/djvu` and `nativeLibs/mupdf` folders.

 * specify path to android-sdk in `local.properties` (use `local.properties.sample` as example).

 To build Android apk files run:

 `./gradlew :orion-viewer:assembleDebug` (or `assembleRelease`)

 It will generate `apk` artifacts suitable for Android 4.1+ devices.

 For Android 4.0.x devices use next build commands (it also requires android-ndk 17):

 ```
 ./gradlew :nativeLibs:djvuModule:clean :nativeLibs:mupdfModule:clean
 ./gradlew :orion-viewer:assembleDebug -Porion.build.android40=true
 ```

Pre 0.80 versions are compatible with Android 2.1+ devices

###Troubleshooting

In case of getting error about absent libtinfo.so.5, please install ncurses lib (libncurses5).