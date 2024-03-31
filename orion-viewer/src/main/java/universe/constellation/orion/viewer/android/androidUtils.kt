package universe.constellation.orion.viewer.android;

import android.content.ContentResolver
import android.content.Intent
import android.os.Build

fun isAtLeastKitkat() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

fun Intent.isContentScheme(): Boolean {
    return ContentResolver.SCHEME_CONTENT == scheme
}