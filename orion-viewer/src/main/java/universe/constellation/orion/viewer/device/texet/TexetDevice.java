package universe.constellation.orion.viewer.device.texet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import universe.constellation.orion.viewer.document.Document;
import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.PageInfo;
import universe.constellation.orion.viewer.device.EInkDevice;
import universe.constellation.orion.viewer.document.DocumentWithCaching;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * Created by mike on 9/16/14.
 */
public class TexetDevice extends EInkDevice {


    @Override
    public void onNewBook(LastPageInfo info, Document document) {
        try {
            String coverFileName = getIconFileName(info.simpleFileName, info.fileSize);
            shtampTexetFile(info.openingFileName, info.simpleFileName, "", "" + info.totalPages, "" + info.pageNumber, coverFileName);
            rememberCover(coverFileName, document);
        } catch (Exception e) {
            log(e);
            Toast.makeText(activity, "Error on new book parameters update: " + e.getMessage(), Toast.LENGTH_SHORT).show();;
        }
    }

    @Override
    public void onBookClose(int currentPage, int pageCount) {
        super.onBookClose(currentPage, pageCount);

        try {
            shtampTexetFile(null, null, null, "" + pageCount, "" + currentPage + 1, null);
        } catch (Exception e) {
            log(e);
            Toast.makeText(activity, "Error on parameters update on book close: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //code provided by texet
    public void shtampTexetFile(String cPath, String cTitle, String cAuthor, String cAllPage, String cCurPage, String cCover) throws Exception {
        if (cCover != null)
            Log.e("COVER", cCover);

        Time myTime = new Time(Time.getCurrentTimezone());
        myTime.setToNow();
        String cDate = String.format("%02d/%02d/%02d %02d:%02d",
                myTime.monthDay, myTime.month + 1, myTime.year, myTime.hour, myTime.minute, myTime.second);

        Context bmkContext =
                activity.getApplicationContext().createPackageContext("com.android.systemui", Context.CONTEXT_IGNORE_SECURITY);


        SharedPreferences settings =
                bmkContext.getSharedPreferences("MyPrefsFile",
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor editor;

        if (cPath == null) {
            editor = settings.edit();

            editor.putString("FirstRecentTotalPage", cAllPage);
            editor.putString("FirstRecentPage", cCurPage);
            editor.putString("FirstRecentReadDate", cDate);

            editor.commit();
            return;
        }

        String curRecentReadingPath =
                settings.getString("FirstRecentPath", "");
        if (cPath.contentEquals(curRecentReadingPath)) {
            return;
        }

        editor = settings.edit();

        String firstRecentPath = settings.getString("FirstRecentPath",
                "");
        String firstRecentTitle =
                settings.getString("FirstRecentTitle", "");
        String firstRecentAuthor =
                settings.getString("FirstRecentAuthor", "");
        String firstRecentTotalPage =
                settings.getString("FirstRecentTotalPage", "");
        String firstRecentPage = settings.getString("FirstRecentPage", "");
        String firstRecentDate =
                settings.getString("FirstRecentReadDate", "");
        String firstBookCover = settings.getString("FirstBookCover", "");

        String secondRecentPath =
                settings.getString("SecondRecentPath", "");
        String secondRecentTitle =
                settings.getString("SecondRecentTitle", "");
        String secondRecentAuthor =
                settings.getString("SecondRecentAuthor", "");
        String secondRecentTotalPage =
                settings.getString("SecondRecentTotalPage", "");
        String secondRecentPage =
                settings.getString("SecondRecentPage", "");
        String secondRecentDate =
                settings.getString("SecondRecentReadDate", "");
        String secondBookCover = settings.getString("SecondBookCover", "");

        String thirdRecentPath = settings.getString("ThirdRecentPath",
                "");
        String thirdRecentTitle =
                settings.getString("ThirdRecentTitle", "");
        String thirdRecentAuthor =
                settings.getString("ThirdRecentAuthor", "");
        String thirdRecentTotalPage =
                settings.getString("ThirdRecentTotalPage", "");
        String thirdRecentPage = settings.getString("ThirdRecentPage",
                "");
        String thirdRecentDate =
                settings.getString("ThirdRecentReadDate", "");
        String thirdBookCover = settings.getString("ThirdBookCover", "");

        if (cPath.contentEquals(secondRecentPath)) {
            secondRecentPath = firstRecentPath;
            secondRecentTitle = firstRecentTitle;
            secondRecentAuthor = firstRecentAuthor;
            secondRecentTotalPage = firstRecentTotalPage;
            secondRecentPage = firstRecentPage;
            secondRecentDate = firstRecentDate;
            secondBookCover = firstBookCover;
        } else {
            thirdRecentPath = secondRecentPath;
            thirdRecentTitle = secondRecentTitle;
            thirdRecentAuthor = secondRecentAuthor;
            thirdRecentTotalPage = secondRecentTotalPage;
            thirdRecentPage = secondRecentPage;
            thirdRecentDate = secondRecentDate;
            thirdBookCover = secondBookCover;

            secondRecentPath = firstRecentPath;
            secondRecentTitle = firstRecentTitle;
            secondRecentAuthor = firstRecentAuthor;
            secondRecentTotalPage = firstRecentTotalPage;
            secondRecentPage = firstRecentPage;
            secondRecentDate = firstRecentDate;
            secondBookCover = firstBookCover;
        }

        firstRecentPath = cPath;
        firstRecentTitle = cTitle;
        firstRecentAuthor = cAuthor;
        firstRecentTotalPage = cAllPage;
        firstRecentPage = cCurPage;
        firstRecentDate = cDate;
        firstBookCover = cCover;

        editor.putString("FirstRecentPath", firstRecentPath);
        editor.putString("FirstRecentTitle", firstRecentTitle);
        editor.putString("FirstRecentAuthor", firstRecentAuthor);
        editor.putString("FirstRecentTotalPage", firstRecentTotalPage);
        editor.putString("FirstRecentPage", firstRecentPage);
        editor.putString("FirstRecentReadDate", firstRecentDate);
        editor.putString("FirstBookCover", firstBookCover);

        editor.putString("SecondRecentPath", secondRecentPath);
        editor.putString("SecondRecentTitle", secondRecentTitle);
        editor.putString("SecondRecentAuthor", secondRecentAuthor);
        editor.putString("SecondRecentTotalPage", secondRecentTotalPage);
        editor.putString("SecondRecentPage", secondRecentPage);
        editor.putString("SecondRecentReadDate", secondRecentDate);
        editor.putString("SecondBookCover", secondBookCover);

        editor.putString("ThirdRecentPath", thirdRecentPath);
        editor.putString("ThirdRecentTitle", thirdRecentTitle);
        editor.putString("ThirdRecentAuthor", thirdRecentAuthor);
        editor.putString("ThirdRecentTotalPage", thirdRecentTotalPage);
        editor.putString("ThirdRecentPage", thirdRecentPage);
        editor.putString("ThirdRecentReadDate", thirdRecentDate);
        editor.putString("ThirdBookCover", thirdBookCover);

        editor.commit();
    }

    public String getIconFileName(String simpleFileName, long fileSize) {
        return "";
    }

    private void writeCover(Bitmap originalBitmap, String newFileName) throws FileNotFoundException {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(newFileName);
            originalBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log(e);
                }
            }
        }
    }

    public void rememberCover(final String coverFileName, final Document doc) {
        if (coverFileName != null && coverFileName.length() != 0 && !new File(coverFileName).exists()) {
            log("Writing cover to " + coverFileName);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Document originalDoc = doc;
                        if (doc instanceof DocumentWithCaching) {
                            originalDoc = ((DocumentWithCaching) doc).getDoc();
                        }

                        if (originalDoc.getPageCount() <= 0) {
                            log("No pages in document");
                            return;
                        }
                        int defaultHeight = 320;

                        log("Extracting cover info ...");

                        PageInfo pageInfo = originalDoc.getPageInfo(0, 0);
                        if (pageInfo.width <= 0 || pageInfo.height <= 0) {
                            log("Wrong page defaultHeight: " + pageInfo.width + "x" + pageInfo.height);
                        }

                        float zoom = Math.min(1.0f * defaultHeight / pageInfo.width, 1.0f * defaultHeight / pageInfo.height);
                        int sizeX = (int) (zoom * pageInfo.width);
                        int sizeY = (int) (zoom * pageInfo.height);
                        Bitmap bm = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
                        int xDelta = -(sizeX - (int)(zoom * pageInfo.width))/2;
                        int yDelta = -(sizeY - (int)(zoom * pageInfo.height))/2;
                        log("Cover info " + zoom + " xD: " + xDelta + " yD: " + yDelta + " bm: " + sizeX + "x" + sizeY);
                        originalDoc.renderPage(0, bm, zoom, xDelta, yDelta, sizeX + xDelta, sizeY + yDelta);
                        writeCover(bm, coverFileName);
                    } catch (FileNotFoundException e) {
                        log(e);
                    }
                }
            }).run();
        }
    }
}
