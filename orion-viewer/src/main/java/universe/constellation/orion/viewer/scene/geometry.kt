package universe.constellation.orion.viewer.scene

/**
 * Created by mike on 1/5/17.
 */
data class Position(@JvmField var x: Int, @JvmField var y: Int) {

    /*UI thread*/
    fun translate(x: Int, y: Int) {
        this.x += x
        this.y += y
    }

    fun translate(p: Position) {
        translate(p.x, p.y)
    }
}

data class Dimension(@JvmField var x: Int, @JvmField var y: Int) {

    /*UI thread*/
    fun translate(x: Int, y: Int) {
        this.x += x
        this.y += y
    }
}