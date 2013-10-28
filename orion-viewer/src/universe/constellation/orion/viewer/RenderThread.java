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

package universe.constellation.orion.viewer;

import android.content.Context;
import android.graphics.*;
import android.os.Debug;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import universe.constellation.orion.viewer.view.Renderer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * User: mike
 * Date: 19.10.11
 * Time: 9:52
 */
public class RenderThread extends Thread implements Renderer {

    protected LayoutStrategy layout;

    private LinkedList<CacheInfo> cachedBitmaps = new LinkedList<CacheInfo>();

    private OrionImageView view;

    private LayoutPosition currentPosition;

    private LayoutPosition lastEvent;

    protected DocumentWrapper doc;

    private int CACHE_SIZE = 4;

    private int FUTURE_COUNT = 1;

    private Canvas cacheCanvas = new Canvas();

    private final Bitmap.Config bitmapConfig;
    private boolean executeInSeparateThread;

    private boolean clearCache;

    private boolean stopped;

    private boolean paused;

    private OrionViewerActivity activity;

    public RenderThread(OrionViewerActivity activity, OrionImageView view, LayoutStrategy layout, DocumentWrapper doc) {
        this(activity, view, layout, doc, createBitmapConfig(activity), true);
    }

    public RenderThread(OrionViewerActivity activity, OrionImageView view, LayoutStrategy layout, DocumentWrapper doc, Bitmap.Config config, boolean executeInSeparateThread) {
        this.view = view;
        this.layout = layout;
        this.doc = doc;
        this.activity = activity;

        this.bitmapConfig = config;
        this.executeInSeparateThread = executeInSeparateThread;

        Common.d("BitmapConfig is " +  bitmapConfig);
    }

    public static Bitmap.Config createBitmapConfig(OrionViewerActivity activity) {
        WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return Bitmap.Config.ARGB_8888;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);

        Common.d("PixelFormat is " +  manager.getDefaultDisplay().getPixelFormat());

        switch (manager.getDefaultDisplay().getPixelFormat()) {
            case PixelFormat.A_8:
                return Bitmap.Config.ALPHA_8;
            case PixelFormat.RGB_565:
                return  Bitmap.Config.RGB_565;
            case PixelFormat.RGBA_4444:
                return  Bitmap.Config.ARGB_4444;
            case PixelFormat.RGBA_8888:
            default:
                return  Bitmap.Config.ARGB_8888;
        }
    }

    @Override
    public void invalidateCache() {
        synchronized (this) {
            for (Iterator<CacheInfo> iterator = cachedBitmaps.iterator(); iterator.hasNext(); ) {
                CacheInfo next = iterator.next();
                next.setValid(false);
            }
            Common.d("Cache invalidated");
        }
    }

    @Override
    public void startRenreder() {
        start();
    }

    @Override
    public void cleanCache() {
        synchronized (this) {
            //if(clearCache) {
              //  clearCache = false;
                for (Iterator<CacheInfo> iterator = cachedBitmaps.iterator(); iterator.hasNext(); ) {
                    CacheInfo next = iterator.next();
                    next.bitmap.recycle();
                    next.bitmap = null;
                }

                Common.d("Allocated heap size " + (Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize())/ 1024 / 1024 + "Mb");
                cachedBitmaps.clear();
                Common.d("Cache is cleared!");
            //}
            currentPosition = null;
            //clearCache = true;
        }
    }

    @Override
    public void stopRenderer() {
        synchronized (this) {
            stopped = true;
            cleanCache();
            notify();
        }
    }

    @Override
    public void onPause() {
//        synchronized (this) {
//            paused = true;
//        }
    }

    @Override
    public void onResume() {
        synchronized (this) {
            paused = false;
            notify();
        }
    }

    public void run() {
        int futureIndex = 0;
        LayoutPosition curPos = null;

        while (!stopped) {

            Common.d("Allocated heap size " + (Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize())/ 1024 / 1024 + "Mb");

            int rotation;
            synchronized (this) {
//                if (paused) {
//                    try {
//                        wait();
//                    } catch (InterruptedException e) {
//                        Common.d(e);
//                    }
//                    continue;
//                }

                if (lastEvent != null) {
                    currentPosition = lastEvent;
                    lastEvent = null;
                    futureIndex = 0;
                    curPos = currentPosition;
                }

                //keep it here
                rotation = layout.getRotation();

                if (currentPosition == null || futureIndex > FUTURE_COUNT || currentPosition.screenWidth == 0 || currentPosition.screenHeight == 0) {
                    try {
                        Common.d("WAITING...");
                        wait();
                    } catch (InterruptedException e) {
                        Common.d(e);
                    }
                    Common.d("AWAKENING!!!");
                    continue;
                } else {
                    //will cache next page
                    Common.d("Future index is " + futureIndex);
                    if (futureIndex != 0) {
                        curPos = curPos.clone();
                        layout.nextPage(curPos);
                    }
                }
            }

            Common.d("rotation = " + rotation);
            renderInCurrentThread(futureIndex == 0, curPos, rotation);
            futureIndex++;
        }
    }

    protected Bitmap renderInCurrentThread(boolean flushBitmap, LayoutPosition curPos, int rotation) {
        CacheInfo resultEntry = null;
        Common.d("Orion: rendering " + curPos.toString());
        if (curPos != null) {
            //try to find result in cache
            for (Iterator<CacheInfo> iterator = cachedBitmaps.iterator(); iterator.hasNext(); ) {
                CacheInfo cacheInfo =  iterator.next();
                if (cacheInfo.isValid() && cacheInfo.info.equals(curPos)) {
                    resultEntry = cacheInfo;
                    //relocate info to end of cache
                    iterator.remove();
                    cachedBitmaps.add(cacheInfo);
                    break;
                }
            }


            if (resultEntry == null) {
                //render page
                resultEntry = render(curPos, rotation);

                synchronized (this) {
                    cachedBitmaps.add(resultEntry);
                }
            }


            if (flushBitmap) {
                final Bitmap bitmap = resultEntry.bitmap;
                Common.d("Sending Bitmap");
                final CountDownLatch mutex = new CountDownLatch(1);

                final LayoutPosition info = curPos;
                if (!executeInSeparateThread) {
                    view.onNewImage(bitmap, info, mutex);
                    activity.getDevice().flushBitmap(0);
                    mutex.countDown();
                } else {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            view.onNewImage(bitmap, info, mutex);
                            //view.invalidate();
                            activity.getDevice().flushBitmap(0);
                        }
                    });
                }

                try {
                    mutex.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Common.d(e);
                }
            }
        }

        return resultEntry.bitmap;
    }

    private CacheInfo render(LayoutPosition curPos, int rotation) {
        CacheInfo resultEntry;
        int width = curPos.x.screenDimension;
        int height = curPos.y.screenDimension;

        int screenWidth = curPos.screenWidth;
        int screenHeight = curPos.screenHeight;

        Bitmap bitmap = null;
        if (cachedBitmaps.size() >= CACHE_SIZE) {
            CacheInfo info = cachedBitmaps.removeFirst();
            info.setValid(true);

            if (screenWidth == info.bitmap.getWidth() && screenHeight == info.bitmap.getHeight() /*|| rotation != 0 && width == info.bitmap.getHeight() && height == info.bitmap.getWidth()*/) {
                bitmap = info.bitmap;
            } else {
                info.bitmap.recycle(); //todo recycle from ui
                info.bitmap = null;
            }
        }
        if (bitmap == null) {
            Common.d("Creating Bitmap " + bitmapConfig + " " + screenWidth + "x" + screenHeight + "...");
            bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        } else {
            Common.d("Cached ");
        }

//        cacheCanvas.setMatrix(null);
//        if (rotation != 0) {
//            int rotationShift = (screenHeight - screenWidth) / 2;
//            cacheCanvas.rotate(-rotation * 90, screenHeight / 2, screenWidth / 2);
//            cacheCanvas.translate(-rotation * rotationShift, -rotation * rotationShift);
//        }

        Point leftTopCorner = layout.convertToPoint(curPos);


        doc.renderPage(curPos.pageNumber, bitmap, curPos.docZoom, width, height, leftTopCorner.x, leftTopCorner.y, leftTopCorner.x + width, leftTopCorner.y + height);

//        long startTime = System.currentTimeMillis();

//        cacheCanvas.setBitmap(bitmap);
//        Rect src = new Rect(0, 0, width, height);
//        Rect dest = new Rect(0, 0, width, height);
//        cacheCanvas.drawBitmap(renderBitmap, src, dest, new Paint());
//
//        long endTime = System.currentTimeMillis();
//        Common.d("Drawing bitmap in cache " + 0.001 * (endTime - startTime) + " s");

        resultEntry = new CacheInfo(curPos, bitmap);
        return resultEntry;
    }

    @Override
    public void render(LayoutPosition lastInfo) {
        lastInfo = lastInfo.clone();
        synchronized (this) {
            lastEvent = lastInfo;
            notify();
        }
    }

    static class CacheInfo {

        public CacheInfo(LayoutPosition info, Bitmap bitmap) {
            this.info  = info;
            this.bitmap = bitmap;
        }

        private LayoutPosition info;
        private Bitmap bitmap;

        private boolean valid = true;

        public LayoutPosition getInfo() {
            return info;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
