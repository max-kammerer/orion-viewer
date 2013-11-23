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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.internal.view.SupportMenuItem;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import org.holoeverywhere.widget.AdapterView;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.CheckBox;
import org.holoeverywhere.widget.CheckedTextView;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ImageButton;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.RadioButton;
import org.holoeverywhere.widget.SeekBar;
import org.holoeverywhere.widget.Spinner;
import universe.constellation.orion.viewer.dialog.TapHelpDialog;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.prefs.OrionPreferenceActivity;
import universe.constellation.orion.viewer.selection.SelectedTextActions;
import universe.constellation.orion.viewer.selection.SelectionAutomata;
import universe.constellation.orion.viewer.selection.TouchAutomata;

import java.io.*;

public class OrionViewerActivity extends OrionBaseActivity {

    private Dialog dialog;

    public static final int OPEN_BOOKMARK_ACTIVITY_RESULT = 1;

    public static final int ROTATION_SCREEN = 0;

    public static final int MAIN_SCREEN = 0;

    public static final int PAGE_SCREEN = 1;

    public static final int ZOOM_SCREEN = 2;

    public static final int CROP_SCREEN = 3;

    public static final int PAGE_LAYOUT_SCREEN = 4;

    public static final int ADD_BOOKMARK_SCREEN = 5;

    public static final int HELP_SCREEN = 100;

    public static final int CROP_RESTRICTION_MIN = -10;

    private static final int CROP_DELTA = 10;

    public static final int CROP_RESTRICTION_MAX = 40;

    private final SubscriptionManager manager = new SubscriptionManager();

    private OrionView view;

    private ViewAnimator animator;

    private LastPageInfo lastPageInfo;

    //left, right, top, bottom
    private int [] cropBorders = new int[6];

    private Controller controller;

    private OperationHolder operation = new OperationHolder();

    private GlobalOptions globalOptions;

    private Intent myIntent;

    public boolean isResumed;

    private boolean selectionMode = false;

    private SelectionAutomata textSelection;

    private SelectedTextActions selectedTextActions;

    //new for new devices)
    private TouchAutomata touchListener;

    private boolean hasActionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        loadGlobalOptions();

        getOrionContext().setViewActivity(this);
        OptionActions.FULL_SCREEN.doAction(this, !globalOptions.isFullScreen(), globalOptions.isFullScreen());
        super.onCreate(savedInstanceState);

        //OptionActions.SHOW_ACTION_BAR.doAction(this, !globalOptions.isActionBarVisible(), globalOptions.isActionBarVisible());
        hasActionBar = globalOptions.isActionBarVisible();
        setContentView(device.getLayoutId());
        view = (OrionView) findViewById(R.id.view);

        OptionActions.SHOW_STATUS_BAR.doAction(this, !globalOptions.isStatusBarVisible(), globalOptions.isStatusBarVisible());
        OptionActions.SHOW_OFFSET_ON_STATUS_BAR.doAction(this, !globalOptions.isShowOffsetOnStatusBar(), globalOptions.isShowOffsetOnStatusBar());
        String mode = getOrionContext().getOptions().getStringProperty(GlobalOptions.DAY_NIGHT_MODE, "DAY");
        view.setNightMode("NIGHT".equals(mode));

        initOptionDialog();
        initRotationScreen();

        //page chooser
        initPagePeekerScreen();

        initZoomScreen();

        initCropScreen();

        initPageLayoutScreen();

        initAddBookmarkScreen();

        myIntent = getIntent();
        touchListener = new TouchAutomata(this, view);

    }

    public void updateCrops() {
        controller.getMargins(cropBorders);
        TableLayout cropTable = (TableLayout) findMyViewById(R.id.crop_borders);
        for (int i = 0; i < cropTable.getChildCount(); i++) {
            TableRow row = (TableRow) cropTable.getChildAt(i);
            TextView valueView = (TextView) row.findViewById(R.id.crop_value);
            valueView.setText(cropBorders[i] + "%");
        }

        TableLayout cropTable2 = (TableLayout) findMyViewById(R.id.crop_borders_even);
        int index = 4;
        for (int i = 0; i < cropTable2.getChildCount(); i++) {
            if (cropTable2.getChildAt(i) instanceof  TableRow) {
                TableRow row = (TableRow) cropTable2.getChildAt(i);
                TextView valueView = (TextView) row.findViewById(R.id.crop_value);
                valueView.setText(cropBorders[index] + "%");
                index++;
            }
        }
        ((CheckBox)findMyViewById(R.id.crop_even_flag)).setChecked(controller.isEvenCropEnabled());
    }

    public void updatePageLayout() {
        String walkOrder = controller.getDirection();
        int lid = controller.getLayout();
        ((RadioGroup) findMyViewById(R.id.layoutGroup)).check(lid == 0 ? R.id.layout1 : lid == 1 ? R.id.layout2 : R.id.layout3);
        //((RadioGroup) findMyViewById(R.id.directionGroup)).check(did == 0 ? R.id.direction1 : did == 1 ? R.id.direction2 : R.id.direction3);

        RadioGroup group = (RadioGroup) findMyViewById(R.id.directionGroup);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof universe.constellation.orion.viewer.android.RadioButton) {
                universe.constellation.orion.viewer.android.RadioButton button = (universe.constellation.orion.viewer.android.RadioButton) child;
                if (walkOrder.equals(button.getWalkOrder())) {
                    group.check(button.getId());
                }
            }
        }
    }

    protected void onNewIntent(Intent intent) {
        Common.d("Runtime.getRuntime().totalMemory() = " + Runtime.getRuntime().totalMemory());
        Common.d("Debug.getNativeHeapSize() = " + Debug.getNativeHeapSize());
        String file =  null;
        Uri uri = intent.getData();
        if (uri != null) {
            Common.d("File URI  = " + uri.toString());
            file = uri.getPath();

            if (controller != null) {
                if (lastPageInfo!= null) {
                    if (lastPageInfo.openingFileName.equals(file)) {
                        //keep controller
                        controller.drawPage();
                        return;
                    }
                }

                controller.destroy();
                controller = null;
            }

            Common.stopLogger();
            openFile(file);
        } else /*if (intent.getAction().endsWith("MAIN"))*/ {
            //TODO error
        }
    }

    public DocumentWrapper openFile(String filePath) {
        DocumentWrapper doc = null;
        Common.d("Trying to open file: " + filePath);

        getOrionContext().onNewBook(filePath);
        try {
            lastPageInfo = LastPageInfo.loadBookParameters(this, filePath);
            getOrionContext().setCurrentBookParameters(lastPageInfo);
            OptionActions.DEBUG.doAction(this, false, getGlobalOptions().getBooleanProperty("DEBUG", false));

            doc = FileUtil.openFile(filePath);

            LayoutStrategy layoutStrategy = new SimpleLayoutStrategy(doc, device.getDeviceSize());

            RenderThread renderer = new RenderThread(this, view, layoutStrategy, doc);

            controller = new Controller(this, doc, layoutStrategy, renderer);

            controller.changeOrinatation(lastPageInfo.screenOrientation);

            controller.init(lastPageInfo, view.getRenderingSize());

            getSubscriptionManager().sendDocOpenedNotification(controller);

            getView().setDimensionAware(controller);

            controller.drawPage();

            String title = doc.getTitle();
            if (title == null || "".equals(title)) {
                int idx = filePath.lastIndexOf('/');
                title = filePath.substring(idx + 1);
                title = title.substring(0, title.lastIndexOf("."));
            }

            device.updateTitle(title);
            view.onNewBook(title, controller.getPageCount());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            globalOptions.addRecentEntry(new GlobalOptions.RecentEntry(new File(filePath).getAbsolutePath()));
            askPassword(controller);

        } catch (Exception e) {
            Common.d(e);
            if (doc != null) {
                doc.destroy();
            }
            finish();
        }
        return doc;
    }


    public void onPause() {
        isResumed = false;
        super.onPause();
        if (controller != null) {
            controller.onPause();
            saveData();
        }
    }

    public void initPagePeekerScreen() {
        final SeekBar pageSeek = (SeekBar) findMyViewById(R.id.page_picker_seeker);

        getSubscriptionManager().addDocListeners(new DocumentViewAdapter() {
            @Override
            public void documentOpened(Controller controller) {
                pageSeek.setMax(controller.getPageCount() - 1);
                pageSeek.setProgress(controller.getCurrentPage());
            }

            @Override
            public void pageChanged(int newPage, int pageCount) {
                pageSeek.setProgress(newPage);
            }
        });


        final TextView pageNumberText = (TextView) findMyViewById(R.id.page_picker_message);
        //initial state
        pageNumberText.setText("" + 1);

        pageSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pageNumberText.setText("" + (progress + 1));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ImageButton closePagePeeker = (ImageButton) findMyViewById(R.id.page_picker_close);

        ImageButton plus = (ImageButton) findMyViewById(R.id.page_picker_plus);
        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pageSeek.incrementProgress(1);
            }
        });

        ImageButton minus = (ImageButton) findMyViewById(R.id.page_picker_minus);
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pageSeek.getProgress() != 0) {
                    pageSeek.incrementProgress(-1);
                }
            }
        });

        closePagePeeker.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //controller.drawPage(Integer.valueOf(pageNumberText.getText().toString()) - 1);
                //main menu
                onAnimatorCancel();
                updatePageSeeker();
                //animator.setDisplayedChild(MAIN_SCREEN);
            }
        });

        ImageButton page_preview = (ImageButton) findMyViewById(R.id.page_preview);
        page_preview.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onApplyAction();
                if (!"".equals(pageNumberText.getText())) {
                    try {
                        int parsedInput = Integer.valueOf(pageNumberText.getText().toString());
                        controller.drawPage(parsedInput -1);
                    } catch (NumberFormatException ex) {
                        showError("Couldn't parse " + pageNumberText.getText(), ex);
                    }
                }
            }
        });
    }

    public void updatePageSeeker() {
        SeekBar pageSeek = (SeekBar) findMyViewById(R.id.page_picker_seeker);
        pageSeek.setProgress(controller.getCurrentPage());
        TextView view = (TextView) findMyViewById(R.id.page_picker_message);
        view.setText("" + (controller.getCurrentPage() + 1));
        view.clearFocus();
        view.requestFocus();

    }

    public void initZoomScreen() {
        //zoom screen

        final Spinner sp = (Spinner) findMyViewById(R.id.zoom_spinner);

        final EditText zoomText = (EditText) findMyViewById(R.id.zoom_picker_message);

        final SeekBar zoomSeek = (SeekBar) findMyViewById(R.id.zoom_picker_seeker);

        if (zoomSeek != null) {
            zoomSeek.setMax(300);
            zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (zoomInternal != 1) {
                        zoomText.setText("" + progress);
                        if (sp.getSelectedItemPosition() != 0) {
                            int oldInternal = zoomInternal;
                            zoomInternal = 2;
                            sp.setSelection(0);
                            zoomInternal = oldInternal;
                        }
                    }
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        getSubscriptionManager().addDocListeners(new DocumentViewAdapter(){
            @Override
            public void documentOpened(Controller controller) {
                updateZoom();
            }
        });

        final ImageButton zplus = (ImageButton) findMyViewById(R.id.zoom_picker_plus);
        zplus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomSeek.incrementProgress(1);
            }
        });

        final ImageButton zminus = (ImageButton) findMyViewById(R.id.zoom_picker_minus);
        zminus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (zoomSeek.getProgress() != 0) {
                    zoomSeek.incrementProgress(-1);
                }
            }
        });

        ImageButton closeZoomPeeker = (ImageButton) findMyViewById(R.id.zoom_picker_close);
        closeZoomPeeker.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //main menu
                onAnimatorCancel();
                //updateZoom();
            }
        });

        ImageButton zoom_preview = (ImageButton) findMyViewById(R.id.zoom_preview);
        zoom_preview.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onApplyAction();
                int index = sp.getSelectedItemPosition();
                controller.changeZoom(index == 0 ? (int)(Float.parseFloat(zoomText.getText().toString()) * 100) : -1 *(index-1));
                updateZoom();
            }
        });

        sp.setAdapter(new MyArrayAdapter());
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean disable = position != 0;
                int oldZoomInternal = zoomInternal;
                if (zoomInternal != 2) {
                    zoomInternal = 1;
                    if (disable) {
                        zoomText.setText((String) parent.getAdapter().getItem(position));
                    } else {
                        zoomText.setText("" + ((int) (controller.getCurrentPageZoom() * 10000)) / 100f);
                        zoomSeek.setProgress((int) (controller.getCurrentPageZoom() * 100));
                    }
                    zoomInternal = oldZoomInternal;
                }

                zminus.setVisibility(disable ? View.GONE : View.VISIBLE);
                zplus.setVisibility(disable ? View.GONE : View.VISIBLE);

                zoomText.setFocusable(!disable);
                zoomText.setFocusableInTouchMode(!disable);

                final LinearLayout parent1 = (LinearLayout) zoomText.getParent();

                parent1.post(new Runnable() {
                    @Override
                    public void run() {
                        parent1.requestLayout();
                    }
                });
            }

            public void onNothingSelected(AdapterView<?> parent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        //by width
        sp.setSelection(1);

    }

    private int zoomInternal = 0;

    public void updateZoom() {
        SeekBar zoomSeek = (SeekBar) findMyViewById(R.id.zoom_picker_seeker);
        TextView textView = (TextView) findMyViewById(R.id.zoom_picker_message);

        Spinner sp = (Spinner) findMyViewById(R.id.zoom_spinner);
        int spinnerIndex = sp.getSelectedItemPosition();
        zoomInternal = 1;
        try {
            int zoom = controller.getZoom10000Factor();
            if (zoom <= 0) {
                spinnerIndex = -zoom + 1;
                zoom = (int) (10000 * controller.getCurrentPageZoom());
            } else {
                spinnerIndex = 0;
                textView.setText("" + (zoom /100f));
            }
            zoomSeek.setProgress(zoom / 100);
            sp.setSelection(spinnerIndex);
        } finally {
            zoomInternal = 0;
        }
    }


    public void initPageLayoutScreen() {
        ImageButton close = (ImageButton) findMyViewById(R.id.options_close);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                int did = ((RadioGroup) findMyViewById(R.id.directionGroup)).getCheckedRadioButtonId();
//                int lid = ((RadioGroup) findMyViewById(R.id.layoutGroup)).getCheckedRadioButtonId();
//                controller.setDirectionAndLayout(did == R.id.direction1 ? 0 : 1, lid == R.id.layout1 ? 0 : lid == R.id.layout2 ? 1 : 2);
                //main menu
                onAnimatorCancel();
                updatePageLayout();
                //animator.setDisplayedChild(MAIN_SCREEN);
            }
        });


        ImageButton view = (ImageButton) findMyViewById(R.id.options_apply);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onApplyAction();
                RadioGroup group = ((RadioGroup) findMyViewById(R.id.directionGroup));
                int walkOrderButtonId = group.getCheckedRadioButtonId();
                universe.constellation.orion.viewer.android.RadioButton button = (universe.constellation.orion.viewer.android.RadioButton) group.findViewById(walkOrderButtonId);
                int lid = ((RadioGroup) findMyViewById(R.id.layoutGroup)).getCheckedRadioButtonId();
                controller.setDirectionAndLayout(button.getWalkOrder(), lid == R.id.layout1 ? 0 : lid == R.id.layout2 ? 1 : 2);
            }
        });

        getSubscriptionManager().addDocListeners(new DocumentViewAdapter() {
            public void documentOpened(Controller controller) {
                updatePageLayout();
            }
        });
    }


   public void initAddBookmarkScreen() {
        ImageButton close = (ImageButton) findMyViewById(R.id.add_bookmark_close);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //main menu
                onAnimatorCancel();
            }
        });


        ImageButton view = (ImageButton) findMyViewById(R.id.add_bookmark_apply);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText text = (EditText) findMyViewById(R.id.add_bookmark_text);
                try {
                    insertBookmark(controller.getCurrentPage(), text.getText().toString());
                    onApplyAction(true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    OrionViewerActivity activity = OrionViewerActivity.this;
                    AlertDialog.Builder buider = new AlertDialog.Builder(activity);
                    buider.setTitle(activity.getResources().getString(R.string.ex_msg_operation_failed));

                    final EditText input = new EditText(activity);
                    input.setText(e.getMessage());
                    buider.setView(input);

                    buider.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    buider.create().show();
                }
            }
        });

    }


    public void initCropScreen() {
        TableLayout cropTable = (TableLayout) findMyViewById(R.id.crop_borders);

        getSubscriptionManager().addDocListeners(new DocumentViewAdapter(){
            @Override
            public void documentOpened(Controller controller) {
                updateCrops();
            }
        });

        for (int i = 0; i < cropTable.getChildCount(); i++) {
            TableRow row = (TableRow) cropTable.getChildAt(i);
            row.findViewById(R.id.crop_plus);

            TextView valueView = (TextView) row.findViewById(R.id.crop_value);
            ImageButton plus = (ImageButton) row.findViewById(R.id.crop_plus);
            ImageButton minus = (ImageButton) row.findViewById(R.id.crop_minus);
            linkCropButtonsAndText(minus, plus, valueView, i);
        }

        //even cropping
        int index = 4;
        final TableLayout cropTable2 = (TableLayout) findMyViewById(R.id.crop_borders_even);
        for (int i = 0; i < cropTable2.getChildCount(); i++) {
            View child = cropTable2.getChildAt(i);
            if (child instanceof  TableRow) {
                TableRow row = (TableRow) child;
                row.findViewById(R.id.crop_plus);
                TextView valueView = (TextView) row.findViewById(R.id.crop_value);
                ImageButton plus = (ImageButton) row.findViewById(R.id.crop_plus);
                ImageButton minus = (ImageButton) row.findViewById(R.id.crop_minus);
                linkCropButtonsAndText(minus, plus, valueView, index);
                index++;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View v = row.getChildAt(j);
                    v.setEnabled(false);
                }
            }
        }


        final ImageButton switchEven = (ImageButton) findMyViewById(R.id.crop_even_button);
        if (switchEven != null) {
            final ViewAnimator cropAnim = (ViewAnimator) findMyViewById(R.id.crop_animator);
            switchEven.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cropAnim.setDisplayedChild((cropAnim.getDisplayedChild() + 1) % 2);
                    switchEven.setImageResource(cropAnim.getDisplayedChild() == 0 ? R.drawable.next : R.drawable.prev);
                }
            });
        }

        final CheckBox checkBox = (CheckBox) findMyViewById(R.id.crop_even_flag);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (int i = 0; i < cropTable2.getChildCount(); i++) {
                    View child = cropTable2.getChildAt(i);
                    if (child instanceof  TableRow) {
                        TableRow row = (TableRow) child;
                        for (int j = 0; j < row.getChildCount(); j++) {
                            View rowChild = row.getChildAt(j);
                            rowChild.setEnabled(isChecked);
                        }
                    }
                }
            }
        });

//        if (Device.Info.NOOK2) {
//            TextView tv = (TextView) findMyViewById(R.id.navigation_title);
//            int color = tv.getTextColors().getDefaultColor();
//            checkBox.setTextColor(color);
//        }


        ImageButton preview = (ImageButton) findMyViewById(R.id.crop_preview);
        preview.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onApplyAction();
                controller.changeMargins(cropBorders[0], cropBorders[2], cropBorders[1], cropBorders[3], checkBox.isChecked(), cropBorders[4], cropBorders[5]);
            }
        });

        ImageButton close = (ImageButton) findMyViewById(R.id.crop_close);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //main menu
                onAnimatorCancel();
                //reset if canceled
                updateCrops();
            }
        });
    }

    public void linkCropButtonsAndText(final ImageButton minus, final ImageButton plus, final TextView text, final int cropIndex) {
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //main menu
                if (cropBorders[cropIndex] != CROP_RESTRICTION_MIN) {
                    cropBorders[cropIndex] = cropBorders[cropIndex] - 1;
                    text.setText(cropBorders[cropIndex] + "%");
                }
            }
        });

        minus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                cropBorders[cropIndex] = cropBorders[cropIndex] - CROP_DELTA;
                if (cropBorders[cropIndex] < CROP_RESTRICTION_MIN) {
                    cropBorders[cropIndex] = CROP_RESTRICTION_MIN;
                }
                text.setText(cropBorders[cropIndex] + "%");
                return true;
            }
        });

        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //main menu
                //int value = Integer.valueOf(text.getText().toString());
                cropBorders[cropIndex] = cropBorders[cropIndex] + 1;
                if (cropBorders[cropIndex] > CROP_RESTRICTION_MAX) {
                    cropBorders[cropIndex] = CROP_RESTRICTION_MAX;
                }
                text.setText(cropBorders[cropIndex]  + "%");
            }
        });

        plus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                cropBorders[cropIndex] = cropBorders[cropIndex] + CROP_DELTA;
                if (cropBorders[cropIndex] > CROP_RESTRICTION_MAX) {
                    cropBorders[cropIndex] = CROP_RESTRICTION_MAX;
                }
                text.setText(cropBorders[cropIndex] + "%");
                return true;
            }
        });
    }

    protected void onResume() {
        isResumed = true;
        super.onResume();
        updateBrightness();

        Common.d("onResume");
        if (myIntent != null) {
            //starting creation intent
            onNewIntent(myIntent);
            myIntent = null;
        } else {
            if (controller != null) {
                controller.processPendingEvents();
                //controller.startRenderer();
                controller.drawPage();
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Common.d("onDestroy");
        Common.stopLogger();
        if (controller != null) {
            controller.destroy();
        }

        if (dialog != null) {
            dialog.dismiss();
        }
        getOrionContext().destroyDb();
        //globalOptions.onDestroy(this);
    }

    private void saveData() {
       if (controller != null) {
            try {
                controller.serialize(lastPageInfo);
                lastPageInfo.save(this);
            } catch (Exception ex) {
                Log.e(Common.LOGTAG, ex.getMessage(), ex);
            }
       }
        saveGlobalOptions();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(isLevel5ApiEnabled() && SafeApi.isCanceled(event)) {
            return super.onKeyUp(keyCode,  event);
        }

        return processKey(keyCode, event, false) ? true : super.onKeyUp(keyCode,  event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isLevel5ApiEnabled() && doTrack(keyCode)) {
            SafeApi.doTrackEvent(event);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean processKey(int keyCode, KeyEvent event, boolean isLong) {
        L.log("key = " + keyCode + " isLong = " + isLong);

        int actionCode = getOrionContext().getKeyBinding().getInt(Common.getPrefKey(keyCode, isLong), -1);
        if (actionCode != -1) {
            Action action = Action.getAction(actionCode);
            switch (action) {
                case PREV: case NEXT: changePage(action == Action.PREV ? Device.PREV : Device.NEXT); return true;
                case NONE: break;
                default: doAction(action); return true;
            }
        }

        if (device.onKeyUp(keyCode, event, operation)) {
            changePage(operation.value);
            return true;
        }
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return processKey(keyCode, event, true);
    }

    public void changePage(int operation) {
        boolean swapKeys = globalOptions.isSwapKeys();
        int width = getView().getWidth();
        int height = getView().getHeight();
        boolean landscape = width > height || controller.getRotation() != 0; /*second condition for nook and alex*/
        if (controller != null) {
            if (operation == Device.NEXT && (!landscape || !swapKeys) || swapKeys && operation == Device.PREV && landscape) {
                controller.drawNext();
            } else {
                controller.drawPrev();
            }
        }
    }

    public void loadGlobalOptions() {
        globalOptions = getOrionContext().getOptions();
    }

    public void saveGlobalOptions() {
        Common.d("Saving global options...");
        globalOptions.saveRecents();
        Common.d("Done!");
    }

    public OrionView getView() {
        return view;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        if (!hasActionBar) {
            for (int i = 0; i < 5; i++) {
                SupportMenuItem item = (SupportMenuItem) menu.getItem(i);
                item.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_NEVER);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Action action = Action.NONE; //will open help

        switch (item.getItemId()) {
            case R.id.exit_menu_item:
                finish();
                return true;

            case R.id.crop_menu_item: action = Action.CROP; break;
            case R.id.zoom_menu_item: action = Action.ZOOM; break;
            case R.id.add_bookmark_menu_item: action = Action.ADD_BOOKMARK; break;
            case R.id.goto_menu_item: action = Action.GOTO; break;
            case R.id.select_text_menu_item: action = Action.SELECT_TEXT; break;
//            case R.id.navigation_menu_item: showOrionDialog(PAGE_LAYOUT_SCREEN, null, null);
//                return true;

//            case R.id.rotation_menu_item: action = Action.ROTATION; break;

            case R.id.options_menu_item: action = Action.OPTIONS; break;

            case R.id.book_options_menu_item: action = Action.BOOK_OPTIONS; break;

//            case R.id.tap_menu_item:
//                Intent tap = new Intent(this, OrionTapActivity.class);
//                startActivity(tap);
//                return true;

            case R.id.outline_menu_item: action = Action.SHOW_OUTLINE; break;
            case R.id.open_menu_item: action = Action.OPEN_BOOK; break;
            case R.id.open_dictionary_menu_item: action = Action.DICTIONARY; break;
            case R.id.day_night_menu_item:  action = Action.DAY_NIGHT; break;

            case R.id.bookmarks_menu_item:  action = Action.OPEN_BOOKMARKS; break;
            case R.id.help_menu_item:
                Intent intent = new Intent();
                intent.setClass(this, OrionHelpActivity.class);
                startActivity(intent);
            break;
        }

        if (Action.NONE != action) {
            doAction(action);
        } else {
//            Intent intent = new Intent();
//            intent.setClass(this, OrionHelpActivity.class);
//            startActivity(intent);
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public SubscriptionManager getSubscriptionManager() {
        return manager;
    }

    public void initOptionDialog() {
        dialog = new org.holoeverywhere.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.android_dialog);
        animator = ((ViewAnimator)dialog.findViewById(R.id.viewanim));

        getView().setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (!selectionMode) {
                    return touchListener.onTouch(event);
                } else {
                    boolean result = textSelection.onTouch(event);
                    if (textSelection.isSuccessful()) {
                        selectionMode = false;
                        String text = controller.selectText(textSelection.getStartX(), textSelection.getStartY(), textSelection.getWidth(), textSelection.getHeight());
                        if (text != null) {
                            if (selectedTextActions == null) {
                                selectedTextActions = new SelectedTextActions(OrionViewerActivity.this);
                            }
                            selectedTextActions.show(text);
                        } else {

                        }
                    }
                    return result;
                }
            }
        });

        dialog.setCanceledOnTouchOutside(true);
    }

    public void doAction(int code) {
        Action action = Action.getAction(code);
        doAction(action);
        Common.d("Code action " + code);
    }


    public void doAction(Action action) {
        action.doAction(controller, this, null);
    }


    protected View findMyViewById(int id) {
        return dialog.findViewById(id);
    }

    public void onAnimatorCancel() {
        dialog.cancel();
    }

    @Override
    protected void onApplyAction() {
        onApplyAction(false);
    }

    protected void onApplyAction(boolean close) {
        if (close || globalOptions.isApplyAndClose()) {
            onAnimatorCancel();
        }
    }

    public void initRotationScreen() {
        //if (getDevice() instanceof EdgeDevice) {
        if (false) {
            final RadioGroup rotationGroup = (RadioGroup) findMyViewById(R.id.rotationGroup);

            rotationGroup.check(R.id.rotate0);

            if (Device.Info.NOOK2) {
                RadioButton r0 = (RadioButton) rotationGroup.findViewById(R.id.rotate0);
                RadioButton r90 = (RadioButton) rotationGroup.findViewById(R.id.rotate90);
                RadioButton r270 = (RadioButton) rotationGroup.findViewById(R.id.rotate270);
                TextView tv = (TextView) findMyViewById(R.id.navigation_title);
                int color = tv.getTextColors().getDefaultColor();
                r0.setTextColor(color);
                r90.setTextColor(color);
                r270.setTextColor(color);
            }

            getSubscriptionManager().addDocListeners(new DocumentViewAdapter() {
                @Override
                public void documentOpened(Controller controller) {
                    updateRotation();
                }
            });


            ImageButton apply = (ImageButton) findMyViewById(R.id.rotation_apply);
            apply.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    onApplyAction(true);
                    int id = rotationGroup.getCheckedRadioButtonId();
                    controller.setRotation(id == R.id.rotate0 ? 0 : id == R.id.rotate90 ? -1 : 1);
                }
            });

            ListView list = (ListView) findMyViewById(R.id.rotationList);
            list.setVisibility(View.GONE);
        } else {
            RadioGroup rotationGroup = (RadioGroup) findMyViewById(R.id.rotationGroup);
            rotationGroup.setVisibility(View.GONE);

            final ListView list = (ListView) findMyViewById(R.id.rotationList);

            //set choices and replace 0 one with Application Default
            boolean isLevel9 = getOrionContext().getSdkVersion() >= 9;
            CharSequence[] values = getResources().getTextArray(isLevel9 ? R.array.screen_orientation_full_desc : R.array.screen_orientation_desc);
            CharSequence[] newValues = new CharSequence[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = values[i];
            }
            newValues[0] = getResources().getString(R.string.orientation_default_rotation);

            list.setAdapter(Device.Info.NOOK2 ?
                    new Nook2ListAdapter(this, android.R.layout.simple_list_item_single_choice, newValues, (TextView) findMyViewById(R.id.navigation_title)) :
                    new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, newValues));

            list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            list.setItemChecked(0, true);

            list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                public void onItemClick(android.widget.AdapterView parent, View view, int position, long id) {
                    CheckedTextView check = (CheckedTextView) view;
                    check.setChecked(!check.isChecked());
                }
            });

            final CharSequence[] ORIENTATION_ARRAY = getResources().getTextArray(R.array.screen_orientation_full);

            list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                public void onItemClick(android.widget.AdapterView parent, View view, int position, long id) {
                    onApplyAction(true);
                    String orientation = ORIENTATION_ARRAY[position].toString();
                    controller.changeOrinatation(orientation);
                }
            });


            ImageButton apply = (ImageButton) findMyViewById(R.id.rotation_apply);
            apply.setVisibility(View.GONE);
//            apply.setOnClickListener(new View.OnClickListener() {
//                public void onClick(View view) {
//                    onApplyAction(true);
//                }
//            });
        }

        ImageButton cancel = (ImageButton) findMyViewById(R.id.rotation_close);
            cancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    onAnimatorCancel();
                    updateRotation();
                }
            });
    }

    void updateRotation() {
        RadioGroup rotationGroup = (RadioGroup) findMyViewById(R.id.rotationGroup);
        if (rotationGroup != null) { //nook case
            rotationGroup.check(controller.getRotation() == 0 ? R.id.rotate0 : controller.getRotation() == -1 ? R.id.rotate90 : R.id.rotate270);
        }
        ListView list = (ListView) findMyViewById(R.id.rotationList);
        if (list != null) {
            int index = getScreenOrientationItemPos(controller.getScreenOrientation());
            list.setItemChecked(index, true);
            list.setSelection(index);
        }
    }

    @Override
    public int getViewerType() {
        return Device.VIEWER_ACTIVITY;
    }

    public GlobalOptions getGlobalOptions() {
        return globalOptions;
    }

    public Controller getController() {
        return controller;
    }

    public void updateBrightness() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        float oldBrightness = params.screenBrightness;
        if (globalOptions.isCustomBrightness()) {
            params.screenBrightness = (float)globalOptions.getBrightness() / 100;
            getWindow().setAttributes(params);
        } else  {
            if (oldBrightness >= 0) {
                params.screenBrightness = -1;
                getWindow().setAttributes(params);
            }
        }
    }

    public long insertOrGetBookId() {
        LastPageInfo info = lastPageInfo;
        Long bookId = getOrionContext().getTempOptions().bookId;
        if (bookId == null || bookId == -1) {
            bookId = getOrionContext().getBookmarkAccessor().insertOrUpdate(info.simpleFileName, info.fileSize);
            getOrionContext().getTempOptions().bookId = bookId;
        }
        return bookId.intValue();
    }

    public boolean insertBookmark(int page, String text) {
        long id = insertOrGetBookId();
        if (id != -1) {
            long bokmarkId = getOrionContext().getBookmarkAccessor().insertOrUpdateBookmark(id, page, text);
            return bokmarkId != -1;
        }
        return false;
    }

    public long getBookId() {
        Common.d("Selecting book id...");
        LastPageInfo info = lastPageInfo;
        Long bookId = getOrionContext().getTempOptions().bookId;
        if (bookId == null || bookId == -1) {
            bookId = getOrionContext().getBookmarkAccessor().selectBookId(info.simpleFileName, info.fileSize);
            getOrionContext().getTempOptions().bookId = bookId;
        }
        Common.d("...book id = " + bookId.longValue());
        return bookId.longValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPEN_BOOKMARK_ACTIVITY_RESULT && resultCode == Activity.RESULT_OK) {
            if (controller != null) {
                int page = data.getIntExtra(OrionBookmarkActivity.OPEN_PAGE, -1);
                if (page != -1) {
                    controller.drawPage(page);
                } else {
                    doAction(Action.GOTO);
                }
            }
        }
    }

    public void showOrionDialog(int screenId, Action action, Object parameter) {
        if (screenId != -1) {
            switch (screenId) {
                case ROTATION_SCREEN: updateRotation(); break;
                case CROP_SCREEN: updateCrops(); break;
                case PAGE_LAYOUT_SCREEN: updatePageLayout();
                case PAGE_SCREEN: updatePageSeeker(); break;
                case ZOOM_SCREEN: updateZoom(); break;
            }

            if (action == Action.ADD_BOOKMARK) {
                String parameterText = (String) parameter;

                int page = controller.getCurrentPage();
                String newText = getOrionContext().getBookmarkAccessor().selectExistingBookmark(getBookId(), page, parameterText);

                boolean notOverride = parameterText == null || parameterText == newText;
                findMyViewById(R.id.warn_text_override).setVisibility(notOverride ? View.GONE : View.VISIBLE);

                ((EditText)findMyViewById(R.id.add_bookmark_text)).setText(notOverride ? newText : parameterText);
            }

            animator.setDisplayedChild(screenId);
            dialog.show();
        }
    }


    public void textSelectionMode() {
        //selectionMode = true;
        if (textSelection == null) {
            textSelection = new SelectionAutomata(this);
        }
        textSelection.startSelection();
    }


    public void changeDayNightMode() {
        boolean newMode = !getView().isNightMode();
        getOrionContext().getOptions().saveProperty(GlobalOptions.DAY_NIGHT_MODE, newMode ? "NIGHT" : "DAY");
        getView().setNightMode(newMode);
        getView().invalidate();
    }

    public class MyArrayAdapter extends ArrayAdapter implements SpinnerAdapter {

        public MyArrayAdapter() {
            super(OrionViewerActivity.this, R.layout.simple_spinner_dropdown_item, OrionViewerActivity.this.getResources().getTextArray(R.array.fits));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                return  convertView;
            } else {
                TextView view = new TextView(OrionViewerActivity.this);
                view.setText(" % ");
                return view;
            }
        }
    }

    public static class Nook2ListAdapter extends ArrayAdapter  {

        private int color;

        public Nook2ListAdapter(Context context, int textViewResourceId, Object[] objects, TextView view) {
            super(context, textViewResourceId,  objects);
            this.color = view.getTextColors().getDefaultColor();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ((CheckedTextView)view).setTextColor(color);
            return view;
        }
    }

    private void askPassword(final Controller controller) {
        if (controller.needPassword()) {
            AlertDialog.Builder buider = new AlertDialog.Builder(this);
            buider.setTitle("Password");

            final EditText input = new EditText(this);
            input.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            input.setTransformationMethod(new PasswordTransformationMethod());
            buider.setView(input);

            buider.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (controller.authentificate(input.getText().toString())) {
                        dialog.dismiss();
                    } else {
                        askPassword(controller);
                        showWarning("Wrong password!");
                    }
                }
            });

            buider.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            buider.create().show();
        }
    }

    //big hack
    protected void myprocessOnActivityVisible() {
        if (getGlobalOptions().isShowTapHelp()) {
            getGlobalOptions().saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, false);
            new TapHelpDialog(this).showDialog();
        }

        if (!hasActionBar) {
            //relayout
            getSupportActionBar().show();
            getSupportActionBar().hide();
        }
    }

}
