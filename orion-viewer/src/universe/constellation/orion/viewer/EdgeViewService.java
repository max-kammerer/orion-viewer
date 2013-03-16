/**
 * 
 */
package universe.constellation.orion.viewer;

import android.graphics.Point;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.app.Notification;
import android.app.PendingIntent;

import universe.constellation.orion.viewer.device.EdgeDevice;
import universe.constellation.orion.viewer.device.KeyEventProducer;
import universe.constellation.orion.viewer.djvu.DjvuDocument;
import universe.constellation.orion.viewer.pdf.PdfDocument;
import universe.constellation.orion.viewer.device.EdgeKbdThread;

/**
 * @author vldmr
 *
 */
public class EdgeViewService extends Service {
    protected EdgeDevice device ;

    // OrionViewerActivity
    private LastPageInfo lastPageInfo;
    
    // Controller
    private DocumentWrapper doc;
    
    private LayoutStrategy layout;

    private LayoutPosition layoutInfo;

    private RenderThread renderer;

    private String screenOrientation;

    private Point lastScreenSize;
    
    private EdgeKbdThread kbd;

	/**
	 * 
	 */
	public EdgeViewService() {
		device = (EdgeDevice) Common.createDevice();
        device.startKeyboardListener(new KeyEventProducer() {
            @Override
            public void nextPage() {
                EdgeViewService.this.drawNext();
            }

            @Override
            public void prevPage() {
                EdgeViewService.this.drawPrev();
            }
        });
        device.setPortrait(true);
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
        Uri uri = intent.getData();
        Common.d("File URI  = " + uri.toString());
        String filePath = uri.getPath();
        Common.d("Trying to open file: " + filePath);
	    
        this.doc = null;
        try {
            // OrionViewerActivity.openFile
            String filePathLowCase = filePath.toLowerCase();
            if (filePathLowCase.endsWith("pdf") || filePathLowCase.endsWith("xps") || filePathLowCase.endsWith("cbz")) {
                this.doc = new PdfDocument(filePath);
            } else {
                this.doc = new DjvuDocument(filePath);
            }

            this.layout = new SimpleLayoutStrategy(doc, null);
            Point dim = device.getDeviceSize();
            this.layout.setDimension(dim.x, dim.y);
            // Controller.Controller
            this.renderer = new RenderThread(this.device, layout, doc);
            this.renderer.start();
            
            
            this.lastPageInfo = LastPageInfo.loadBookParameters(null, filePath);

            OrionBaseActivity.getOrionContext().setCurrentBookParameters(this.lastPageInfo);
            
            // Controller.init
            this.doc.setContrast(this.lastPageInfo.contrast);
            this.doc.setThreshold(this.lastPageInfo.threshold);

            this.layout.init(lastPageInfo, OrionBaseActivity.getOrionContext().getOptions());
            this.layoutInfo = new LayoutPosition();
            this.layout.reset(this.layoutInfo, this.lastPageInfo.pageNumber);
            layoutInfo.x.offset = this.lastPageInfo.newOffsetX;
            layoutInfo.y.offset = this.lastPageInfo.newOffsetY;
    
            this.lastScreenSize = new Point(this.lastPageInfo.screenWidth, this.lastPageInfo.screenHeight);
            this.screenOrientation = this.lastPageInfo.screenOrientation;
            
            this.drawPage();
            
        } catch (Exception e) {
            Common.d(e);
            if (doc != null) {
                doc.destroy();
            }
            //finish();
        }
        
        // Some nonsence to increase service priority by placing it to foreground
        Notification note=new Notification(R.drawable.djvu,
            "Starting Orion Reader Service for Edge",
             System.currentTimeMillis());
        Intent i=new Intent(this, OrionFileManagerActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
            Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this, 0, i, 0);
        
        note.setLatestEventInfo(this, "Orion Reader: Viewing file", filePath, pi);
        note.flags|=Notification.FLAG_NO_CLEAR;
        startForeground(7777, note);
        
        return START_STICKY;
	}
	
    @Override
    public void onDestroy() {
        stopForeground(true);
    }
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

    public void drawPage(int page) {
        this.layout.reset(layoutInfo, page);
        this.drawPage();
    }

    public void drawPage() {
        this.renderer.render(this.layoutInfo);
    }

    public void drawNext() {
        layout.nextPage(layoutInfo);
        drawPage();
    }

    public void drawPrev() {
        layout.prevPage(layoutInfo);
        drawPage();
    }
}
