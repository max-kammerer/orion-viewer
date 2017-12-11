package universe.constellation.orion.viewer.search;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Handler;
import com.artifex.mupdfdemo.SearchTaskResult;

import universe.constellation.orion.viewer.document.Document;
import universe.constellation.orion.viewer.R;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
* User: mike
* Date: 11.11.13
* Time: 20:48
*/
public abstract class SearchTask {

    static class ProgressDialogX extends ProgressDialog {
        public ProgressDialogX(Context context) {
            super(context);
        }

        private boolean mCancelled = false;

        public boolean isCancelled() {
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
    private final Document document;
    private final Handler mHandler;
    private final AlertDialog.Builder mAlertBuilder;
    private AsyncTask<Void, Integer, SearchTaskResult> mSearchTask;

    public SearchTask(Context context, Document document) {
        mContext = context;
        this.document = document;
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

    public void go(final String text, int direction, int displayPage, int searchPage) {
        if (document == null)
            return;
        stop();

        final int increment = direction;
        final int startIndex = searchPage == -1 ? displayPage : searchPage + increment;

        final ProgressDialogX progressDialog = new ProgressDialogX(mContext);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(mContext.getString(R.string.msg_searching));
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                stop();
            }
        });
        progressDialog.setMax(document.getPageCount());

        mSearchTask = new AsyncTask<Void, Integer, SearchTaskResult>() {
            @Override
            protected SearchTaskResult doInBackground(Void... params) {
                int index = startIndex;

                while (0 <= index && index < document.getPageCount() && !isCancelled()) {
                    publishProgress(index);
                    RectF searchHits[] = document.searchPage(index, text);

                    if (searchHits != null && searchHits.length > 0)
                        return new SearchTaskResult(text, index, searchHits);

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
                    mAlertBuilder.setTitle(SearchTaskResult.get() == null ? R.string.warn_text_not_found: R.string.warn_no_further_occurrences_found);
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
                progressDialog.setProgress(values[0].intValue());
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        if (!progressDialog.isCancelled()) {
                            progressDialog.show();
                            progressDialog.setProgress(startIndex);
                        }
                    }
                }, SEARCH_PROGRESS_DELAY);
            }
        };

        mSearchTask.execute();
    }
}