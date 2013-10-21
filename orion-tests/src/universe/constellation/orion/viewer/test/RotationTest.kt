package universe.constellation.orion.viewer.test

import android.view.ViewGroup.LayoutParams
import universe.constellation.orion.viewer.OptionActions
import junit.framework.Assert
import android.view.WindowManager
import android.graphics.PixelFormat
import android.content.Intent
import universe.constellation.orion.viewer.OrionViewerActivity
import android.net.Uri

/**
 * User: mike
 * Date: 21.10.13
 * Time: 7:17
 */
class RotationTest : ActivityBaseTest() {

    private val width = 300;
    private val height = 400;

    override fun setUp() {
        super<ActivityBaseTest>.setUp()
        val params = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION, 0, PixelFormat.OPAQUE)
        getActivity().getWindow()!!.setAttributes(params)
        OptionActions.FULL_SCREEN.doAction(getActivity(), true, false)
    }

    fun testRotation() {
        val file = extractFileFromTestData(BaseTestTrait.SICP)
        val intent = Intent(getInstrumentation()!!.getTargetContext(), javaClass<OrionViewerActivity>());
        intent.setData(Uri.fromFile(file))

        openTestDocument(BaseTestTrait.SICP)
        val view = getActivity().getView()!!
        Assert.assertTrue(view.getWidth() == width)
        Assert.assertTrue(view.getHeight() == height)
    }



    override fun getDataPath(): Uri? {
        return Uri.fromFile(extractFileFromTestData(BaseTestTrait.SICP))
    }
}