package universe.constellation.orion.viewer.view

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Created by mike on 10/11/15.
 */
class OrionScene(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(context, attrs, defStyle, 0) {}

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    constructor(context: Context) : this(context, null) {}

}