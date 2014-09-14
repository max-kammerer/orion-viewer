/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.device;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Device;
import universe.constellation.orion.viewer.view.OrionDrawScene;
import universe.constellation.orion.viewer.R;

import java.io.*;

/**
 * User: mike
 * Date: 12.03.12
 * Time: 12:48
 */
public class EdgeDevice extends AndroidDevice {

    private EdgeFB fb;

    private Boolean portrait;

    private EdgeKbdThread listener;

    public EdgeDevice() {
        super(PowerManager.SCREEN_DIM_WAKE_LOCK);
        try {
            fb = new EdgeFB(2);
        } catch (Exception e) {
            Common.d(e);
        }
    }

    
    public void onDestroy() {
        if (this.listener != null) {
            this.listener.detouch();
            this.listener = null;
        }
    }

    
    //only for edge service
    public void setPortrait(Boolean portrait) {
        this.portrait = portrait;
    }

    public void startKeyboardListener(KeyEventProducer producer) {
        this.listener = new EdgeKbdThread(producer);
        this.listener.setDaemon(true);
        this.listener.start();
    }

    public Point getDeviceSize() {
        return new Point(fb.getWidth(), fb.getHeight());
    }
    
    public void onSetContentView() {
        onSetContentView(true);
    }


    public void onSetContentView(boolean wrap) {
        if (activity != null && activity.getViewerType() == Device.VIEWER_ACTIVITY && listener == null) {
            startKeyboardListener(new KeyEventProducer() {
                @Override
                public void nextPage() {
                    activity.onKeyUp(KeyEvent.KEYCODE_SOFT_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SOFT_LEFT));
                }

                @Override
                public void prevPage() {
                    activity.onKeyUp(KeyEvent.KEYCODE_SOFT_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SOFT_LEFT));
                }
            });
        }

        if (fb != null) {
            Common.d("On set content view ");
            if (activity.getViewerType() == VIEWER_ACTIVITY) {
                final View view = activity.findViewById(R.id.view);
                ScrollView vsv = null;

                ViewGroup parent = (ViewGroup) view.getParent();
                parent.removeView(view);

                if (wrap) {
                    HorizontalScrollView hsv = new HorizontalScrollView(activity) {
                        @Override
                        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                            Common.d("HSV onSizeChanged " + w + "x" + h);
                            if (w != 0 && h != 0 & fb != null) {
                                boolean newPortrait = h > w;
                                if (portrait == null || newPortrait != portrait.booleanValue()) {
                                    portrait = newPortrait;

//                                try {
//                                    Process pr = Runtime.getRuntime().exec("su -c echo " + (h > w ? "270" : "180")  + ">/sys/class/graphics/fb2/rotate");
//                                    pr.waitFor();
//                                    fb.initParameters();
                                    onSetContentView(false);
//                                } catch (IOException e) {
//                                    Common.d(e);
//                                } catch (InterruptedException e) {
//                                    Common.d(e);
                                }
                            }
                        }
                    };
                    hsv.setHorizontalFadingEdgeEnabled(false);
                    ViewGroup.LayoutParams lp2 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                    parent.addView(hsv, lp2);

                    lp2 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                    vsv = new ScrollView(activity);
                    vsv.setVerticalFadingEdgeEnabled(false);
                    hsv.addView(vsv, lp2);
                } else {
                    vsv = (ScrollView) parent;
                }
                int newW = portrait == null || portrait.booleanValue() ? fb.getWidth() : fb.getHeight();
                int newH = portrait == null || portrait.booleanValue() ? fb.getHeight() : fb.getWidth();
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(newW, newH);
                view.setMinimumWidth(newH);
                view.setMinimumHeight(newH);

                Common.d("Setting new lp " + lp.width + "x" + lp.height);

                view.setLayoutParams(lp);
                vsv.addView(view, lp);
            }
        }
    }

    @Override
    public void flushBitmap() {
        if (fb != null && activity.getViewerType() == VIEWER_ACTIVITY) {
            Bitmap bm = ((OrionDrawScene) activity.getView()).getBitmap();
            if (bm != null && !bm.isRecycled()) {
                try {
                    fb.transfer(bm, false);
                } catch (IOException e) {
                    Common.d(e);
                }
            }
        }
        super.flushBitmap();
    }

    public void flushBitmap(Bitmap bitmap) {
        try {
			fb.transfer(bitmap, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
            initParameters();
        }

        protected void initParameters() throws IOException {
            Common.d("Edge: initParameters");
            BufferedReader buf = new BufferedReader(new FileReader("/sys/class/graphics/fb" + framebuffer + "/rotate"));
            String s = buf.readLine();
            buf.close();
            rotate = Integer.parseInt(s);
            Common.d("Edge: rotate = " + rotate);
            buf = new BufferedReader(new FileReader("/sys/class/graphics/fb" + framebuffer + "/stride"));
            s = buf.readLine();
            buf.close();
            stride = Integer.parseInt(s);
            Common.d("Edge: stride = " + stride);

            buf = new BufferedReader(new FileReader("/sys/class/graphics/fb" + framebuffer + "/virtual_size"));
            String[] as = buf.readLine().split(",");
            buf.close();
            width = Integer.parseInt(as[0]);
            height = Integer.parseInt(as[1]);

            //it seems that on this step width and height is same)))) max of them)
            Common.d("Edge: width = " + width);
            Common.d("Edge: height = " + height);

            if (rotate == 90 || rotate == 270)
                if (width == 800) {
                    width = 600; // edgejr
                    height = 800;
                } else {
                    width = 825; // edge
                    height = 1200; // edge
                }

            if (rotate == 0 || rotate == 180)
                if (height == 800) {
                    height = 600; // edgejr
                    width = 800;
                } else {
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
            Common.d("Edge: rotate = " + rotate);
            Common.d("Edge: stride = " + stride);
            Common.d("Edge: width = " + width);
            Common.d("Edge: height = " + height);


            OutputStream os = new FileOutputStream("/dev/graphics/fb" + framebuffer);
            int WIDTH = bitmap.getWidth(), HEIGHT = bitmap.getHeight();
            int dim = WIDTH * HEIGHT;
            int[] pixels = new int[dim];
            bitmap.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);

            if (recycle)
                bitmap.recycle();

            byte[] buffer = new byte[(portrait ? HEIGHT : WIDTH) * stride];

            int o = 0;

            if (portrait) {
                for (int i = 0; i < HEIGHT; i++)
                    for (int j = 0; j < WIDTH; j++) {
                        buffer[i * stride + j] = (byte) (Color.red(pixels[o++]) & 0xff);
                    }
            } else {
                for (int j = 0; j < HEIGHT; j++)
                    for (int i = 0; i < WIDTH; i++) {
                        buffer[(WIDTH - i - 1) * stride + j] = (byte) (Color.red(pixels[o++]) & 0xff);
                    }
            }

            os.write(buffer);
            os.close();
            renew(HEIGHT, WIDTH);
        }

        public void renew(int height, int width) throws IOException {
            InputStream fs = new FileInputStream("/dev/graphics/fb" + framebuffer);
            byte[] buffer = new byte[(portrait ? height : width) * stride];
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
