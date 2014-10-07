package universe.constellation.orion.viewer.device;

/**
 * Nook Touch EPD controller interface wrapper.
 * @author DairyKnight <dairyknight@gmail.com>
 * http://forum.xda-developers.com/showthread.php?t=1183173
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Device;

import android.app.Activity;

public class Nook2Util {

    public static final int REGION_APP_1 = 0;
    public static final int REGION_APP_2 = 1;
    public static final int REGION_APP_3 = 2;
    public static final int REGION_APP_4 = 3;

    public static final int WAVE_GC = 0;
    public static final int WAVE_GU = 1;
    public static final int WAVE_DU = 2;
    public static final int WAVE_A2 = 3;
    public static final int WAVE_GL16 = 4;
    public static final int WAVE_AUTO = 5;

    public static final int MODE_BLINK = 0;
    public static final int MODE_ACTIVE = 1;
    public static final int MODE_ONESHOT = 2;
    public static final int MODE_CLEAR = 3;
    public static final int MODE_ACTIVE_ALL = 4;
    public static final int MODE_ONESHOT_ALL = 5;

    private static final boolean NOOK_12_13 = Device.Info.NOOK2 && (Device.Info.getVersion().startsWith("1.2") || Device.Info.getVersion().startsWith("1.3"));

    private static Class epdControllerClass;

    private  static Class epdControllerRegionClass;
    private  static Class epdControllerRegionParamsClass;
    private  static Class epdControllerWaveClass;
    private  static Class epdControllerModeClass;

    private static Constructor regionParamsConstructor;
    private static Method epdControllerSetRegionMethod;

    private static Object mEpdController = null;

    private static Object[] waveEnums;

    private static Object[] regionEnums;

    private static Object[] modeEnums;

    private static boolean successful = false;
    
    private static final int width ;
    
    private static final int height;


    static {
        try {
            epdControllerClass = Class
                        .forName("android.hardware.EpdController");
            epdControllerRegionClass = Class
                    .forName("android.hardware.EpdController$Region");
            if (NOOK_12_13) {
            	epdControllerRegionParamsClass = Class
            			.forName("android.hardware.EpdRegionParams");
            	epdControllerWaveClass = Class
    					.forName("android.hardware.EpdRegionParams$Wave");
            } else {
            	epdControllerRegionParamsClass = Class
            			.forName("android.hardware.EpdController$RegionParams");
            	 epdControllerWaveClass = Class
                         .forName("android.hardware.EpdController$Wave");
            }
            epdControllerModeClass = Class
                    .forName("android.hardware.EpdController$Mode");

            if (epdControllerWaveClass.isEnum()) {
                Common.d("EpdController Wave Enum successfully retrived.");
				waveEnums = epdControllerWaveClass.getEnumConstants();
			}

			if (epdControllerRegionClass.isEnum()) {
				Common.d("EpdController Region Enum successfully retrived.");
				regionEnums = epdControllerRegionClass.getEnumConstants();
			}


            if (epdControllerModeClass.isEnum()) {
				Common.d("EpdController Region Enum successfully retrived.");
				modeEnums = epdControllerModeClass.getEnumConstants();
                Common.d(modeEnums.toString());
			}

            regionParamsConstructor = epdControllerRegionParamsClass
                    .getConstructor(new Class[] { Integer.TYPE, Integer.TYPE,
                            Integer.TYPE, Integer.TYPE, epdControllerWaveClass});

            epdControllerSetRegionMethod = epdControllerClass.getMethod(
                    "setRegion", new Class[] { String.class,
                            epdControllerRegionClass,
                            epdControllerRegionParamsClass, epdControllerModeClass });

            successful = true;
        } catch (Exception e) {
            Common.d(e); //To change body of catch statement use File | Settings | File Templates.
        }

        if ("BNRV500".equals(Device.Info.MODEL)) {
            width = 758;
            height = 1024;
        } else {
            width = 600;
            height = 800;
        }
    }

    public static void setGL16Mode(Activity activity) {
        setMode(REGION_APP_3, WAVE_GU, MODE_ONESHOT, activity);
    }

    public static void setFullUpdate(Activity activity) {
        setMode(REGION_APP_3, WAVE_GC, MODE_ONESHOT_ALL, activity);
    }

    public static void setMode(int region, int wave, int mode, Activity activity) {
		Common.d("Nook screen update: region " + region + ", wave " + wave + ", " + mode);
		try {
            if (successful) {
            	if (NOOK_12_13 && mEpdController == null) {
            		Constructor[] EpdControllerConstructors = epdControllerClass.getConstructors();
					mEpdController = EpdControllerConstructors[0].newInstance(new Object[] { activity });
				}

                Object localRegionParams = regionParamsConstructor
                        .newInstance(new Object[] { 0, 0, width, height, waveEnums[wave]});

                if (NOOK_12_13) {
                	epdControllerSetRegionMethod
					.invoke(mEpdController, new Object[] { "Orion",
                                regionEnums[region], localRegionParams, modeEnums[mode] });
                } else {
	                epdControllerSetRegionMethod
	                        .invoke(null, new Object[] { "Orion",
	                                regionEnums[region], localRegionParams, modeEnums[mode] });
                }
            }
		} catch (Exception e) {
			Common.d(e);
		}
	}

}
