package universe.constellation.orion.viewer.device.texet;

import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import universe.constellation.orion.viewer.OperationHolder;

import static universe.constellation.orion.viewer.LoggerKt.log;

public class TexetTB576HDDevice extends TexetDevice {

    private static Method texetBacklight0;

    private static Method texetBacklight1;

    private static Method invalidateScreen;

    private static boolean isMethodFound = false;

    static {
        try {
            texetBacklight0 = PowerManager.class.getMethod("setBacklight", int.class, Context.class);
        } catch (Exception e) {
            log(e);
        }

        try {
            texetBacklight1 = PowerManager.class.getMethod("setBacklight", int.class);
        } catch (Exception e) {
            log(e);
        }

        try {
            invalidateScreen = View.class.getMethod("postInvalidate", int.class);
            isMethodFound = true;
        } catch (Exception e) {
            log(e);
        }
    }

    @Override
    public boolean isLightingSupported() {
        return true;
    }

    @Override
    public int doLighting(int delta) throws Exception {
        int i = Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);

        i += delta;

        if (i < 0)
            i = 0;
        if (i > 255)
            i = 255;

        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);

        if (texetBacklight0 != null) {
            texetBacklight0.invoke(pm, i, getActivity());
        } else if (texetBacklight1 != null){
            texetBacklight1.invoke(pm, i);
        } else {
            Log.e("texet backlight",  "not found");
        }

        Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, i);

        return i;
    }

    @Override
    public boolean onKeyUp(int keyCode, boolean isLongPress, @NotNull OperationHolder holder) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            holder.value = isLongPress ? PREV : NEXT;
            return true;
        }
        return super.onKeyUp(keyCode, isLongPress, holder);
    }

    @Override
    public String getIconFileName(String simpleFileName, long fileSize) {
        int i = simpleFileName.lastIndexOf(".");
        if (i > 0) {
            simpleFileName = simpleFileName.substring(0, i);
        }
        return "/mnt/storage/BookCover/" + simpleFileName + "." + fileSize + ".png.bnv";
    }

    @Override
    public void doFullUpdate(View view) {
        if (isMethodFound) {
            try {
                invalidateScreen.invoke(view, 98);
            } catch (Exception e) {
                log(e);
                super.doFullUpdate(view);
            }
        } else {
            super.doFullUpdate(view);
        }
    }

    @Override
    public void doDefaultUpdate(View view) {
        doFullUpdate(view);
    }
}
