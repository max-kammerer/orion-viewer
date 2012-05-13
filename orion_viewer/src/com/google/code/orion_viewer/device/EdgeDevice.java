package com.google.code.orion_viewer.device;

/*
 * Orion Viewer is a pdf and djvu viewer for android devices
 *
 * Copyright (C) 2011-2012  Michael Bogdanov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.OrionBaseActivity;
import com.google.code.orion_viewer.OrionView;
import universe.constellation.orion.viewer.R;

import java.io.*;

/**
 * User: mike
 * Date: 12.03.12
 * Time: 12:48
 */
public class EdgeDevice extends AndroidDevice {

    private EdgeFB fb;

    public EdgeDevice() {
        try {
            fb = new EdgeFB(2);
        } catch (Exception e) {
            Common.d(e);
        }
    }

    @Override
    public void onSetContentView() {
        if (fb != null) {
            if (activity.getViewerType() == VIEWER_ACTIVITY) {
                View view = activity.findViewById(R.id.view);
                ViewGroup parent = (ViewGroup) view.getParent();
                parent.removeView(view);

                HorizontalScrollView hsv = new HorizontalScrollView(activity);
                hsv.setHorizontalFadingEdgeEnabled(false);
                ViewGroup.LayoutParams lp2 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                parent.addView(hsv, lp2);

                lp2 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                ScrollView vsv = new ScrollView(activity);
                vsv.setVerticalFadingEdgeEnabled(false);
                hsv.addView(vsv, lp2);

                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.width = fb.getWidth();
                lp.height = fb.getHeight();
                view.setMinimumWidth(lp.width);
                view.setMinimumHeight(lp.height);
                view.setLayoutParams(lp);
                vsv.addView(view, lp);
            }
        }
//        if (activity.getViewerType() == VIEWER_ACTIVITY) {
//            View view = activity.findViewById(R.id.view);
//            ViewGroup.LayoutParams lp = view.getLayoutParams();
//            lp.width = 300;
//            lp.height = 300;
//            view.setLayoutParams(lp);
//        }
    }

    @Override
    public void flushBitmap(int delay) {
        if (fb != null && activity.getViewerType() == VIEWER_ACTIVITY) {
            Bitmap bm = ((OrionView) activity.getView()).getBitmap();
            if (bm != null && !bm.isRecycled()) {
                try {
                    fb.transfer(bm, false);
                } catch (IOException e) {
                    Common.d(e);
                }
            }
        }
        super.flushBitmap(delay);
    }


    /*
    * Copyright (C) 2011 vldmr
    *
    *  Class implementing access to entourage edge frame buffer.
    *  Based on "Send to Framebuffer" application by Sven Killig
    *  http://sven.killig.de/android/N1/2.2/usb_host
    */
    public class EdgeFB {

        private int framebuffer, width, height, rotate, stride;

        public EdgeFB(int framebuffer) throws IOException {
            this.framebuffer = framebuffer;
            BufferedReader buf = new BufferedReader(new FileReader("/sys/class/graphics/fb" + framebuffer + "/rotate"));
            String s = buf.readLine();
            buf.close();
            rotate = Integer.parseInt(s);

            buf = new BufferedReader(new FileReader("/sys/class/graphics/fb" + framebuffer + "/stride"));
            s = buf.readLine();
            buf.close();
            stride = Integer.parseInt(s);

            buf = new BufferedReader(new FileReader("/sys/class/graphics/fb" + framebuffer + "/virtual_size"));
            String[] as = buf.readLine().split(",");
            buf.close();
            width = Integer.parseInt(as[0]);
            height = Integer.parseInt(as[1]);

            if (rotate == 90 || rotate == 270)
                if (width == 800)
                    width = 600; // edgejr
                else {
                    width = 825; // edge
                    height = 1200; // edge
                }

            if (rotate == 0 || rotate == 180)
                if (height == 800)
                    height = 600; // edgejr
                else {
                    height = 825; // edge
                    width = 1200;
                }
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getRotate() {
            return rotate;
        }

        public void transfer(Bitmap bitmap, boolean recycle) throws IOException {
            OutputStream os = new FileOutputStream("/dev/graphics/fb" + framebuffer);
            int WIDTH = bitmap.getWidth(), HEIGHT = bitmap.getHeight();
            int dim = WIDTH * HEIGHT;
            int[] pixels = new int[dim];
            bitmap.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);

            if (recycle)
                bitmap.recycle();

            byte[] buffer = new byte[HEIGHT * stride];

            int co;

            int o = 0;

            for (int i = 0; i < HEIGHT; i++)
                for (int j = 0; j < WIDTH; j++) {
                    buffer[i * stride + j] = (byte) (Color.red(pixels[o++]) & 0xff);
                }

            os.write(buffer);
            os.close();
            renew(HEIGHT);
        }

        public void renew(int height) throws IOException {
            InputStream fs = new FileInputStream("/dev/graphics/fb" + framebuffer);
            byte[] buffer = new byte[height * stride];
            byte[] buffer2 = new byte[500 * stride];
            fs.read(buffer);
            fs.close();
            System.arraycopy(buffer, 0, buffer2, 0, buffer2.length);
            OutputStream os = new FileOutputStream("/dev/graphics/fb" + framebuffer);
            os.write(buffer2);
            os.close();
        }
    }


}
