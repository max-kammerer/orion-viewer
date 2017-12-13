package universe.constellation.orion.viewer.view;

import universe.constellation.orion.viewer.layout.LayoutPosition;

/**
 * User: mike
 * Date: 19.10.13
 * Time: 20:38
 */
public interface Renderer {

    void invalidateCache();

    void stopRenderer();

    void onPause();

    void onResume();

    void render(LayoutPosition lastInfo);

    void startRenderer();
}
