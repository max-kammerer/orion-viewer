package universe.constellation.orion.viewer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

import universe.constellation.orion.viewer.L;
import universe.constellation.orion.viewer.util.ColorUtil;
import universe.constellation.orion.viewer.util.DensityUtil;

/**
 * Created by mike on 9/14/14.
 */
public class ColorStuff {

    public final Paint backgroundPaint = new Paint();

    public final Paint borderPaint = new Paint();
    public final BitmapDrawable bd;
    private Drawable colorDrawable = DrawableCompat.wrap(new ColorDrawable(Color.WHITE));
    private boolean renderOffPage;

    public ColorStuff(Context context) {
        int dim = 64;
        Bitmap bitmap = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint p = new Paint();
        p.setColor(Color.rgb(223, 223, 223));
        int gradsize = 1 << (int) (Math.log(DensityUtil.calcScreenSize(2, context)) / Math.log(2) + 0.1);
        if (gradsize < 2) {
            gradsize = 2;
        }
        L.log("Grad size is " + gradsize);
        p.setShader(new LinearGradient(0, 0, 0, gradsize, Color.rgb(223, 223, 223), Color.rgb(240, 240, 240), Shader.TileMode.MIRROR));
        canvas.drawRect(0, 0, dim, dim, p);

        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        backgroundPaint.setShader(bitmapShader);
        bd = new BitmapDrawable(context.getResources(), bitmap);
        bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(2);
        borderPaint.setStyle(Paint.Style.STROKE);
    }

    public void setColorMatrix(View view, float[] colorMatrix) {
        if (colorMatrix != null) {
            ColorMatrix matrix = new ColorMatrix(colorMatrix);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            bd.setColorFilter(filter);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //ugly hack
                colorDrawable = new ColorDrawable(ColorUtil.transformColor(Color.WHITE, matrix));
            }
            colorDrawable.setColorFilter(filter);
            borderPaint.setColorFilter(filter);
        } else {
            bd.setColorFilter(null);
            colorDrawable.setColorFilter(null);
            borderPaint.setColorFilter(null);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //ugly hack
                colorDrawable = new ColorDrawable(Color.WHITE);
            }
        }
        renderOffPage(view, renderOffPage);
    }

    public void renderOffPage(View view, boolean on) {
        renderOffPage = on;
        Drawable currentPaint = on ? bd : colorDrawable;
        view.setBackgroundDrawable(currentPaint);
    }
}
