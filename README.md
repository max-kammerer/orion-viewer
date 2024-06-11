![GitHub code size](https://img.shields.io/github/languages/code-size/max-kammerer/orion-viewer.svg)
![GitHub Releases Downloads](https://img.shields.io/github/downloads/max-kammerer/orion-viewer/total.svg?label=GitHub%20Releases%20Downloads)
![GitHub release](https://img.shields.io/github/release/max-kammerer/orion-viewer.svg)

Orion Viewer is *pdf*, *djvu*, *xps*, *tiff* and comic book (*cbr*, *cbz*, *cbt*) file viewer for Android
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

### App usage analytics

From version `0.91.0` Orion Viewer automatically collects app crashes and app usage statistics to improve application quality.
Orion Viewer doesn't collect any personal information (and doesn't pass it to any third parties).

## Contribution

Contributions are always welcome! Feel free to open any issue, send pull request or suggest any idea

### Translations

Project is fully or partially translated into Chinese, English, French, German, Hebrew, Italian, Russian, Spanish, Turkish and Ukrainian languages.
It would be highly desirable to eliminate gaps and errors in existing translations and add translations to other languages.

Take a look into
[translations](https://github.com/max-kammerer/orion-viewer/wiki/Translations) page on wiki.

## Setting up build and working environment

To work with and build *Orion Viewer* project you will need next tools installed:

 * [Android Studio](https://developer.android.com/studio) 2023.1+ integrated development environment
 * [git tool](https://git-scm.com/downloads) to work with source repository
 * *make* tool and [python 3](https://www.python.org/downloads/) compiler for building *mupdf* library  

1. Checkout repository sources via *git*:

   `git clone https://github.com/max-kammerer/orion-viewer.git`  
or if you have [configured ssh access](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent)  
   `git clone git@github.com:max-kammerer/orion-viewer.git`

2. Setup native libraries (*mupdf*, *djvu*) via next command:

    `./gradlew -b thirdparty_build.gradle downloadDjvu downloadAndMakeMupdf`
    
    Build scripts for them are defined in *externalNativeBuild* section in gradle build files
    (for details see *djvuModule/build.gradle* and *mupdfModule/build.gradle* files).
    Native libs are checked out into *nativeLibs/djvu* and *nativeLibs/mupdf* folders.

3. Open project in Android Studio (AS): *Main Menu/File/Open...* and select project folder to open. 

4. Now you can build project within AS and run it in Android emulator
 
You can also build Android 'apk' artifacts via next command:   
    
`./gradlew :orion-viewer:assembleDebug` (or `assembleRelease`) 

It will generate *apk* artifacts suitable for Android 4.1+ devices.

For Android 4.0.x devices use next build commands (it also requires android-ndk 17):

```
./gradlew :nativeLibs:djvuModule:clean :nativeLibs:mupdfModule:clean
./gradlew :orion-viewer:assembleDebug -Porion.build.android40=true
```

Pre 0.80 versions are compatible with Android 2.1+ devices

    
### Troubleshooting

In case of getting error about absent libtinfo.so.5, please install ncurses lib (libncurses5).
