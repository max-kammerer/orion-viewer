package universe.constellation.orion.viewer;

/**
 * User: mike
 * Date: 20.05.12
 * Time: 16:26
 */
public abstract class AbstractDocumentWrapper implements DocumentWrapper {

    public native void setContrast(int contrast);

    public native void setDayNightMode(boolean isNight);
}
