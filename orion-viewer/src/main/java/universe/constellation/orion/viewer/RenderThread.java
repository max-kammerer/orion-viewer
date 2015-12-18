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

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Debug;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import universe.constellation.orion.viewer.view.FullScene;
import universe.constellation.orion.viewer.view.Renderer;

/**
 * User: mike
 * Date: 19.10.11
 * Time: 9:52
 */
public class RenderThread extends Thread implements Renderer {

    private static final int CACHE_SIZE = 4;

    private static final int FUTURE_COUNT = 1;

    protected LayoutStrategy layout;

    private LinkedList<CacheInfo> cachedBitmaps = new LinkedList<>();

    private LayoutPosition currentPosition;

    private LayoutPosition lastEvent;

    protected DocumentWrapper doc;

    private boolean executeInSeparateThread;

    private boolean stopped;

    private OrionViewerActivity activity;

    private FullScene fullScene;

    public RenderThread(OrionViewerActivity activity, LayoutStrategy layout, DocumentWrapper doc, FullScene fullScene) {
        this(activity, layout, doc, true, fullScene);
    }

    public RenderThread(OrionViewerActivity activity, LayoutStrategy layout, DocumentWrapper doc, boolean executeInSeparateThread, FullScene scene) {
        this.layout = layout;
        this.doc = doc;
        this.activity = activity;
        this.executeInSeparateThread = executeInSeparateThread;
        this.fullScene = scene;
        Common.d("RenderThread was created successfully");
    }

    @Override
    public void invalidateCache() {
        synchronized (this) {
            for (CacheInfo next : cachedBitmaps) {
                next.setValid(false);
            }
            Common.d("Cache invalidated");
        }
    }

    @Override
    public void startRenreder() {
        Common.d("Starting renderer");
        start();
    }

    public void cleanCache() {
        synchronized (this) {
            //if(clearCache) {
              //  clearCache = false;
                for (CacheInfo cacheInfo : cachedBitmaps) {
                    //cacheInfo.bitmap.recycle();
                    cacheInfo.bitmap = null;
                }

                Common.d("Allocated heap size: " + Common.memoryInMB(Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()));
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
            notify();
        }
    }

    public void run() {
        int futureIndex = 0;
        LayoutPosition curPos = null;

        while (!stopped) {

            Common.d("Allocated heap size " + Common.memoryInMB(Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()));

            int rotation;
            synchronized (this) {
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
        Common.d("Orion: rendering position: " + curPos);
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
                    fullScene.onNewImage(bitmap, info, mutex);
                    activity.getDevice().flushBitmap();
                    mutex.countDown();
                } else {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            fullScene.onNewImage(bitmap, info, mutex);
                            activity.getDevice().flushBitmap();
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
            bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        } else {
            Common.d("Using cached bitmap " + bitmap);
        }

        Point leftTopCorner = layout.convertToPoint(curPos);


        doc.renderPage(curPos.pageNumber, bitmap, curPos.docZoom, leftTopCorner.x, leftTopCorner.y, leftTopCorner.x + width, leftTopCorner.y + height);

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
