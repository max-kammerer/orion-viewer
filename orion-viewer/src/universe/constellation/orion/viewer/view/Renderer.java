package universe.constellation.orion.viewer.view;

import universe.constellation.orion.viewer.LayoutPosition;

/**
 * User: mike
 * Date: 19.10.13
 * Time: 20:38
 */
public interface Renderer {

    void invalidateCache();

    void cleanCache();

    void stopRenderer();

    void onPause();

    void onResume();

    void render(LayoutPosition lastInfo);

    void startRenreder();
}
