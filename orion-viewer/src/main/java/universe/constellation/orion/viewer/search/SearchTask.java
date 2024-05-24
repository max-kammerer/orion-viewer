package universe.constellation.orion.viewer.search;

import static universe.constellation.orion.viewer.LoggerKt.log;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Handler;

import com.artifex.mupdfdemo.SearchTaskResult;

import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.document.Document;
import universe.constellation.orion.viewer.document.PageWithAutoCrop;
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy;
import universe.constellation.orion.viewer.view.AutoCropKt;
import universe.constellation.orion.viewer.view.CorePageView;

public abstract class SearchTask {

    static class ProgressDialogX extends ProgressDialog {
        ProgressDialogX(Context context) {
            super(context);
        }

        private boolean mCancelled = false;

        boolean isCancelled() {
            return mCancelled;
        }

        @Override
        public void cancel() {
            mCancelled = true;
            super.cancel();
        }
    }

    private static final int SEARCH_PROGRESS_DELAY = 200;
    private final Context mContext;
    private final Controller controller;
    private final Handler mHandler;
    private final AlertDialog.Builder mAlertBuilder;
    private AsyncTask<Void, Integer, SearchTaskResult> mSearchTask;

    protected SearchTask(Context context, Controller controller) {
        mContext = context;
        this.controller = controller;
        mHandler = new Handler();
        mAlertBuilder = new AlertDialog.Builder(context);
    }

    protected abstract void onResult(boolean isSuccessful, SearchTaskResult result);

    public void stop() {
        if (mSearchTask != null) {
            log("Stopping search thread");
            mSearchTask.cancel(true);
            mSearchTask = null;
        }
    }

    public void go(final String text, int direction, int displayPage, int searchPage, SimpleLayoutStrategy layoutStrategy) {
        if (controller == null)
            return;
        stop();
        Document document = controller.getDocument();

        final int increment = direction;
        final int startIndex = searchPage == -1 ? displayPage : searchPage + increment;

        final ProgressDialogX progressDialog = new ProgressDialogX(mContext);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(mContext.getString(R.string.msg_searching));
        progressDialog.setOnCancelListener(dialog -> stop());
        progressDialog.setMax(document.getPageCount());

        mSearchTask = new AsyncTask<>() {
            @Override
            protected SearchTaskResult doInBackground(Void... params) {
                int index = startIndex;

                while (0 <= index && index < document.getPageCount() && !isCancelled()) {
                    publishProgress(index);
                    PageWithAutoCrop page = document.getOrCreatePageAdapter(index);

                    RectF searchHits[] = page.searchText(text);
                    if (searchHits != null && searchHits.length > 0) {
                        page.readPageDataForRendering();
                        CorePageView pageView = new CorePageView(index, document, controller, controller.getRootJob(), page);
                        return new SearchTaskResult(text, index, searchHits, AutoCropKt.getPageInfoFromSearch(pageView, layoutStrategy), page);
                    }
                    page.destroy();
                    index += increment;
                }
                return null;
            }

            @Override
            protected void onPostExecute(SearchTaskResult result) {
                progressDialog.cancel();
                if (result != null) {
                    log("On result");
                    onResult(true, result);
                } else {
                    log("fail");
                    mAlertBuilder.setTitle(R.string.warn_text_not_found);
                    AlertDialog alert = mAlertBuilder.create();
                    alert.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getString(R.string.msg_dialog_dismis),
                            (DialogInterface.OnClickListener) null);
                    alert.show();
                }
            }

            @Override
            protected void onCancelled() {
                progressDialog.cancel();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressDialog.setProgress(values[0]);
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mHandler.postDelayed(() -> {
                    if (!progressDialog.isCancelled()) {
                        progressDialog.show();
                        progressDialog.setProgress(startIndex);
                    }
                }, SEARCH_PROGRESS_DELAY);
            }
        };

        mSearchTask.execute();
    }
}