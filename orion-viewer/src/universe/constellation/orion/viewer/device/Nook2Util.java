package universe.constellation.orion.viewer.device;

/**
 * Nook Touch EPD controller interface wrapper.
 * @author DairyKnight <dairyknight@gmail.com>
 * http://forum.xda-developers.com/showthread.php?t=1183173
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Nook2Util {

    private static Class epdControllerClass;

    private  static Class epdControllerRegionClass;
    private  static Class epdControllerRegionParamsClass;
    private  static Class epdControllerWaveClass;
    private  static Class epdControllerModeClass;

    private static Object[] waveEnums;

    private static Object[] regionEnums;

    private static Object[] modeEnums;

    private static boolean successful = false;
    static {
        try {
            /*
			 * Loading the Epson EPD Controller Classes
			 *
			 * */

            epdControllerClass = Class
                        .forName("android.hardware.EpdController");
            epdControllerRegionClass = Class
                    .forName("android.hardware.EpdController$Region");
            epdControllerRegionParamsClass = Class
                    .forName("android.hardware.EpdController$RegionParams");
            epdControllerWaveClass = Class
                    .forName("android.hardware.EpdController$Wave");
            epdControllerModeClass = Class
                    .forName("android.hardware.EpdController$Mode");

            if (epdControllerWaveClass.isEnum()) {
				System.err.println("EpdController Wave Enum successfully retrived.");
				waveEnums = epdControllerWaveClass.getEnumConstants();
			}

			if (epdControllerRegionClass.isEnum()) {
				System.err.println("EpdController Region Enum successfully retrived.");
				regionEnums = epdControllerRegionClass.getEnumConstants();
			}


            if (epdControllerModeClass.isEnum()) {
				System.err.println("EpdController Region Enum successfully retrived.");
				modeEnums = epdControllerModeClass.getEnumConstants();
				System.err.println(modeEnums);
			}

            successful = true;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

	public static void exitA2Mode() {
		System.err.println("Orion::exitA2Mode");
		try {

			Constructor RegionParamsConstructor = epdControllerRegionParamsClass.getConstructor(new Class[] { Integer.TYPE, Integer.TYPE,
							Integer.TYPE, Integer.TYPE, epdControllerWaveClass, Integer.TYPE });

			Object localRegionParams = RegionParamsConstructor.newInstance(new Object[] { 0, 0, 600, 800, waveEnums[3], 16}); // Wave = A2

			Method epdControllerSetRegionMethod = epdControllerClass.getMethod("setRegion", new Class[] { String.class,
							epdControllerRegionClass,
							epdControllerRegionParamsClass });
			epdControllerSetRegionMethod.invoke(null, new Object[] { "OrionView", regionEnums[2], localRegionParams });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void enterA2Mode() {
		System.err.println("Orion::enterA2Mode");
		try {

			Constructor RegionParamsConstructor = epdControllerRegionParamsClass.getConstructor(new Class[] { Integer.TYPE, Integer.TYPE,
							Integer.TYPE, Integer.TYPE, epdControllerWaveClass, Integer.TYPE });

			Object localRegionParams = RegionParamsConstructor.newInstance(new Object[] { 0, 0, 600, 800, waveEnums[2], 16}); // Wave = DU

			Method epdControllerSetRegionMethod = epdControllerClass.getMethod("setRegion", new Class[] { String.class,
							epdControllerRegionClass,
							epdControllerRegionParamsClass });
			epdControllerSetRegionMethod.invoke(null, new Object[] { "Orion", regionEnums[2], localRegionParams });

			Thread.sleep(100L);
			localRegionParams = RegionParamsConstructor.newInstance(new Object[] { 0, 0, 600, 800, waveEnums[3], 14}); // Wave = A2
			epdControllerSetRegionMethod.invoke(null, new Object[] { "Orion", regionEnums[2], localRegionParams});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void setGL16Mode() {
		System.err.println("Orion::setGL16Mode");
		try {
            if (successful) {
                Constructor RegionParamsConstructor = epdControllerRegionParamsClass
                        .getConstructor(new Class[] { Integer.TYPE, Integer.TYPE,
                                Integer.TYPE, Integer.TYPE, epdControllerWaveClass});

                Object localRegionParams = RegionParamsConstructor
                        .newInstance(new Object[] { 0, 0, 600, 800, waveEnums[1]}); // Wave = GU

                Method epdControllerSetRegionMethod = epdControllerClass.getMethod(
                        "setRegion", new Class[] { String.class,
                                epdControllerRegionClass,
                                epdControllerRegionParamsClass, epdControllerModeClass });
                epdControllerSetRegionMethod
                        .invoke(null, new Object[] { "Orion",
                                regionEnums[2], localRegionParams, modeEnums[2] }); // Mode = ONESHOT_ALL
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setDUMode() {
		System.err.println("Orion::setDUMode");
		try {
            if (successful) {
                Constructor RegionParamsConstructor = epdControllerRegionParamsClass
                        .getConstructor(new Class[] { Integer.TYPE, Integer.TYPE,
                                Integer.TYPE, Integer.TYPE, epdControllerWaveClass,
                                Integer.TYPE });

                Object localRegionParams = RegionParamsConstructor
                        .newInstance(new Object[] { 0, 0, 600, 800, waveEnums[2],
                                14 });

                Method epdControllerSetRegionMethod = epdControllerClass.getMethod(
                        "setRegion", new Class[] { String.class,
                                epdControllerRegionClass,
                                epdControllerRegionParamsClass });
                epdControllerSetRegionMethod
                        .invoke(null, new Object[] { "Orion",
                                regionEnums[2], localRegionParams });
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
