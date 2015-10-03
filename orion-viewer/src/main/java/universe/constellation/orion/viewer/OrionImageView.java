package universe.constellation.orion.viewer;

import android.graphics.Bitmap;

import java.util.concurrent.CountDownLatch;

/**
 * User: mike
 * Date: 20.10.13
 * Time: 9:21
 */
public interface OrionImageView {

    void onNewImage(Bitmap bitmap, LayoutPosition info, CountDownLatch latch);

    void onNewBook(String title, int pageCount);
}
