package universe.constellation.orion.viewer.geometry

/* A plain class rather than an alias of android.graphics.Point: K2 doesn't let Kotlin
 * properties be actualized by Java fields, and nothing needs the Android type here. */
data class Point(var x: Int, var y: Int)