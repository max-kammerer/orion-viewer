package universe.constellation.orion.viewer;

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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.*;


import universe.constellation.orion.viewer.device.EdgeDevice;
import universe.constellation.orion.viewer.djvu.DjvuDocument;
import universe.constellation.orion.viewer.pdf.PdfDocument;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.prefs.OrionPreferenceActivity;
import universe.constellation.orion.viewer.prefs.OrionTapActivity;
import pl.polidea.customwidget.TheMissingTabHost;

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

    private boolean isFullScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        loadGlobalOptions();
        isFullScreen = globalOptions.isFullScreen();
        if (isFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        setContentView(device.getLayoutId());

        view = (OrionView) findViewById(R.id.view);
        String mode = getOrionContext().getOptions().getStringProperty(GlobalOptions.DAY_NIGHT_MODE, "DAY");
        view.setNightMode("NIGHT".equals(mode));

        if (!device.optionViaDialog()) {
            initAnimator();
            initMainScreen();
            //initHelpScreen();
        } else {
            initOptionDialog();
            initRotationScreen();
        }

        //page chooser
        initPagePeekerScreen();

        initZoomScreen();

        initCropScreen();

        initPageLayoutScreen();

        initAddBookmarkScreen();

        myIntent = getIntent();
    }



    protected void initHelpScreen() {
        TheMissingTabHost host = (TheMissingTabHost) findMyViewById(R.id.helptab);

        host.setup();

        TheMissingTabHost.TheMissingTabSpec spec = host.newTabSpec("general_help");
        spec.setContent(R.id.general_help);
        spec.setIndicator("", getResources().getDrawable(R.drawable.help));
        host.addTab(spec);
        TheMissingTabHost.TheMissingTabSpec recent = host.newTabSpec("app_info");
        recent.setContent(R.id.app_info);
        recent.setIndicator("", getResources().getDrawable(R.drawable.info));
        host.addTab(recent);
        host.setCurrentTab(0);

        ImageButton btn = (ImageButton) findMyViewById(R.id.help_close);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //animator.setDisplayedChild(MAIN_SCREEN);
                onAnimatorCancel();
            }
        });

        btn = (ImageButton) findMyViewById(R.id.info_close);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAnimatorCancel();
                //animator.setDisplayedChild(MAIN_SCREEN);
            }
        });
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
        int did = controller.getDirection();
        int lid = controller.getLayout();
        ((RadioGroup) findMyViewById(R.id.layoutGroup)).check(lid == 0 ? R.id.layout1 : lid == 1 ? R.id.layout2 : R.id.layout3);
        ((RadioGroup) findMyViewById(R.id.directionGroup)).check(did == 0 ? R.id.direction1 : R.id.direction2);
    }

    protected void onNewIntent(Intent intent) {
        Common.d("Runtime.getRuntime().totalMemory() = " + Runtime.getRuntime().totalMemory());
        Common.d("Debug.getNativeHeapSize() = " + Debug.getNativeHeapSize());
        Uri uri = intent.getData();
        Common.d("File URI  = " + uri.toString());
        String file = uri.getPath();

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

        if (intent.getData() != null) {
            Common.stopLogger();
            openFile(file);
        } else /*if (intent.getAction().endsWith("MAIN"))*/ {
            //TODO error
        }
    }

    public void openFile(String filePath) {
        DocumentWrapper doc = null;
        Common.d("File URI  = " + filePath);
        Common.startLogger(filePath + ".trace");
        getOrionContext().onNewBook(filePath);
        try {
            String filePAthLowCase = filePath.toLowerCase();
            if (filePAthLowCase.endsWith("pdf") || filePAthLowCase.endsWith("xps") || filePAthLowCase.endsWith("cbz")) {
                doc = new PdfDocument(filePath);
            } else {
                doc = new DjvuDocument(filePath);
            }

            LayoutStrategy str = new SimpleLayoutStrategy(doc, device.getDeviceSize());
            int idx = filePath.lastIndexOf('/');

            lastPageInfo = LastPageInfo.loadBookParameters(this, filePath);
            controller = new Controller(this, doc, str, view);

            controller.changeOrinatation(lastPageInfo.screenOrientation);

            controller.init(lastPageInfo);

            getSubscriptionManager().sendDocOpenedNotification(controller);

            getView().setController(controller);

            controller.drawPage();

            String title = doc.getTitle();
            if (title == null || "".equals(title)) {
                title = filePath.substring(idx + 1);
                title = title.substring(0, title.lastIndexOf("."));
            }

            device.updateTitle(title);
            globalOptions.addRecentEntry(new GlobalOptions.RecentEntry(new File(filePath).getAbsolutePath()));
        } catch (Exception e) {
            Common.d(e);
            if (doc != null) {
                doc.destroy();
            }
            finish();
        }
    }


    public void onPause() {
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
                pageSeek.incrementProgressBy(1);
            }
        });

        ImageButton minus = (ImageButton) findMyViewById(R.id.page_picker_minus);
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pageSeek.getProgress() != 0) {
                    pageSeek.incrementProgressBy(-1);
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
    }

    public void initZoomScreen() {
        //zoom screen

        final Spinner sp = (Spinner) findMyViewById(R.id.zoom_spinner);

        final TextView zoomText = (TextView) findMyViewById(R.id.zoom_picker_message);

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
                zoomSeek.incrementProgressBy(1);
            }
        });

        final ImageButton zminus = (ImageButton) findMyViewById(R.id.zoom_picker_minus);
        zminus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (zoomSeek.getProgress() != 0) {
                    zoomSeek.incrementProgressBy(-1);
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



        //sp.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, getResources().getTextArray(R.array.fits)));
        sp.setAdapter(new MyArrayAdapter());
//        sp.setAdapter(new MyArrayAdapter(this, R.layout.zoom_spinner, R.id.spinner_text, getResources().getTextArray(R.array.fits)));
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean disable = position != 0;
                int oldZoomInternal = zoomInternal;
                if (zoomInternal != 2) {
                    zoomInternal = 1;
                    if (disable) {
                        zoomText.setText((String)parent.getAdapter().getItem(position));
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
                int did = ((RadioGroup) findMyViewById(R.id.directionGroup)).getCheckedRadioButtonId();
                int lid = ((RadioGroup) findMyViewById(R.id.layoutGroup)).getCheckedRadioButtonId();
                controller.setDirectionAndLayout(did == R.id.direction1 ? 0 : 1, lid == R.id.layout1 ? 0 : lid == R.id.layout2 ? 1 : 2);
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
                insertBookmark(controller.getCurrentPage(), text.getText().toString());
                onApplyAction(true);
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

    private void initMainScreen() {
        ImageButton btn = (ImageButton) findMyViewById(R.id.exit);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //controller.destroy();
                finish();
            }
        });
        btn = (ImageButton) findMyViewById(R.id.prev_page);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changePage(-1);
            }
        });

        btn.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View v) {
                //page seeker
                animator.setDisplayedChild(PAGE_SCREEN);
                return true;
            }
        });

        btn = (ImageButton) findMyViewById(R.id.next_page);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changePage(1);
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View v) {
                //page seeker
                Action.OPEN_BOOKMARKS.doAction(controller, OrionViewerActivity.this);
                //animator.setDisplayedChild(PAGE_SCREEN);
                return true;
            }
        });

        btn = (ImageButton) findMyViewById(R.id.switch_page);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                controller.setRotation((controller.getRotation() - 1) % 2);
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                controller.setRotation((controller.getRotation() + 1) % 2);
                return true;
            }
        });

        btn = (ImageButton) findMyViewById(R.id.zoom);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOrionDialog(ZOOM_SCREEN, null);
            }
        });

        btn = (ImageButton) findMyViewById(R.id.crop_menu);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateCrops();
                animator.setDisplayedChild(CROP_SCREEN);
            }
        });

        btn = (ImageButton) findMyViewById(R.id.help);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //animator.setDisplayedChild(HELP_SCREEN);
                Intent intent = new Intent();
                intent.setClass(OrionViewerActivity.this, OrionHelpActivity.class);
                startActivity(intent);
            }
        });


        btn = (ImageButton) findMyViewById(R.id.navigation);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updatePageLayout();
                animator.setDisplayedChild(PAGE_LAYOUT_SCREEN);
            }
        });

        btn = (ImageButton) findMyViewById(R.id.options);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent in = new Intent(OrionViewerActivity.this, OrionPreferenceActivity.class);
                startActivity(in);
            }
        });


    }

    protected void onResume() {
        super.onResume();
        updateBrightness();
        boolean newFullScreen = globalOptions.isFullScreen();
        if (isFullScreen != newFullScreen) {
            if (newFullScreen) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            isFullScreen = newFullScreen;
        }

        Common.d("onResume");
        if (myIntent != null) {
            //starting creation intent
            onNewIntent(myIntent);
            myIntent = null;
        } else {
            if (controller != null) {
                controller.changeOverlap(globalOptions.getHorizontalOverlapping(), globalOptions.getVerticalOverlapping());
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
//                ObjectOutputStream out =
//                    new ObjectOutputStream(OrionViewerActivity.this.openFileOutput(lastPageInfo.fileData,
//                        Context.MODE_PRIVATE));
//                out.writeObject(lastPageInfo);
//                out.close();
            } catch (Exception ex) {
                Log.e(Common.LOGTAG, ex.getMessage(), ex);
            }
       }
        saveGlobalOptions();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        System.out.println("key " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!device.optionViaDialog() && animator.getDisplayedChild() != MAIN_SCREEN) {
                onAnimatorCancel();
                return true;
            }
        }

        int actionCode = getOrionContext().getKeyBinding().getInt("" + keyCode, -1);
        if (actionCode != -1) {
            Action action = Action.getAction(actionCode);
            switch (action) {
                case PREV: case NEXT: changePage(action == Action.PREV ? Device.PREV : Device.NEXT); return true;
                case NONE: break;
                default: doAction(action); return true;
            }
        }

        if (device.onKeyDown(keyCode, event, operation)) {
            changePage(operation.value);
            return true;
        }

        return super.onKeyDown(keyCode,  event);
    }

    public void changePage(int operation) {
        boolean swapKeys = globalOptions.isSwapKeys();
        int width = getView().getWidth();
        int height = getView().getHeight();
        boolean landscape = width > height;
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
        boolean result = super.onCreateOptionsMenu(menu);
        if (result) {
            getMenuInflater().inflate(R.menu.menu, menu);
        }
        return result;
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
            case R.id.navigation_menu_item: showOrionDialog(PAGE_LAYOUT_SCREEN, null);
                return true;

            case R.id.rotation_menu_item: action = Action.ROTATION; break;

            case R.id.options_menu_item: action = Action.OPTIONS; break;

            case R.id.book_options_menu_item: action = Action.OPTIONS; break;

            case R.id.tap_menu_item:
                Intent tap = new Intent(this, OrionTapActivity.class);
                startActivity(tap);
                return true;

            case R.id.open_menu_item: action = Action.OPEN_BOOK; break;
            case R.id.open_dictionary_menu_item: action = Action.DICTIONARY; break;
            case R.id.day_night_menu_item:  action = Action.DAY_NIGHT; break;

            case R.id.bookmarks_menu_item:  action = Action.OPEN_BOOKMARKS; break;
        }

        if (Action.NONE != action) {
            doAction(action);
        } else {
            Intent intent = new Intent();
            intent.setClass(this, OrionHelpActivity.class);
            startActivity(intent);
        }
        return true;
    }

    public void initAnimator() {
        animator = (ViewAnimator) findMyViewById(R.id.viewanim);
        getSubscriptionManager().addDocListeners(new DocumentViewAdapter() {
            @Override
            public void documentOpened(Controller controller) {
                animator.setDisplayedChild(MAIN_SCREEN);
            }

            public void pageChanged(final int newPage, final int pageCount) {
                TextView tv = (TextView) findMyViewById(R.id.page_number_view);
                tv.setText(newPage + 1 + "/" + pageCount);
                device.updatePageNumber(newPage + 1, pageCount);
            }
        });
    }

    public SubscriptionManager getSubscriptionManager() {
        return manager;
    }

    public void initOptionDialog() {
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.android_dialog);
        animator = ((ViewAnimator)dialog.findViewById(R.id.viewanim));

        getView().setOnTouchListener(new View.OnTouchListener() {
            private int lastX = -1;
            private int lastY = -1;
            long startTime = 0;
            private static final long TIME_DELTA = 600;
            public boolean onTouch(View v, MotionEvent event) {
                //Common.d("Event " + event.getAction() + ": "  + (SystemClock.uptimeMillis() - startTime));
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //Common.d("DOWN " + event.getAction());
                    startTime = SystemClock.uptimeMillis();
                    lastX = (int) event.getX();
                    lastY = (int) event.getY();
                    return true;
                } else {
//                    Common.d("ev " + event.getAction());
                    boolean doAction = false;
                    if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            Common.d("UP " + event.getAction());
                            doAction = true;
                        } else {
                            if (lastX != -1 && lastY != -1) {
                                boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;
                                doAction = isLongClick;
                            }
                        }

                        if (doAction) {
                            Common.d("Check event action " + event.getAction());
                            boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;

                            if (lastX != -1 && lastY != -1) {
                                int width = getView().getWidth();
                                int height = getView().getHeight();

                                int i = 3 * lastY / height;
                                int j = 3 * lastX / width;

                                int code = globalOptions.getActionCode(i, j, isLongClick);
                                doAction(code);

                                startTime = 0;
                                lastX = -1;
                                lastY = -1;
                            }

                        }
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                        startTime = 0;
                        lastX = -1;
                        lastY = -1;
                    }
                }
                return true;
            }
        });

//        getView().setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                v.get
//                globalOptions.getActionCode()
//                controller.drawNext();
//            }
//        });
//
//        getView().setOnLongClickListener(new View.OnLongClickListener(){
//            public boolean onLongClick(View v) {
//                controller.drawPrev();
//                return true;
//            }
//        });

//        getView().setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View v, MotionEvent event) {
//
//            }
//        });
    }

    private void doAction(int code) {
        Action action = Action.getAction(code);
        doAction(action);
        Common.d("Code action " + code);
    }


    public void doAction(Action action) {
        action.doAction(controller, this);
    }


    protected View findMyViewById(int id) {
        if (device.optionViaDialog()) {
            return dialog.findViewById(id);
        } else {
            return findViewById(id);
        }
    }

    public void onAnimatorCancel() {
        if (!device.optionViaDialog()){
            animator.setDisplayedChild(MAIN_SCREEN);
        } else {
            dialog.cancel();
        }
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

            list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    CheckedTextView check = (CheckedTextView)view;
                    check.setChecked(!check.isChecked());
                }
            });

            final CharSequence[] ORIENTATION_ARRAY = getResources().getTextArray(R.array.screen_orientation_full);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
        LastPageInfo info = lastPageInfo;
        Long bookId = getOrionContext().getTempOptions().bookId;
        if (bookId == null) {
            bookId = getOrionContext().getBookmarkAccessor().selectBookId(info.simpleFileName, info.fileSize);
            getOrionContext().getTempOptions().bookId = bookId;
        }
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

    public void showOrionDialog(int screenId, Action action) {
        if (screenId != -1) {
            switch (screenId) {
                case ROTATION_SCREEN: updateRotation(); break;
                case CROP_SCREEN: updateCrops(); break;
                case PAGE_LAYOUT_SCREEN: updatePageLayout();
                case PAGE_SCREEN: updatePageSeeker(); break;
                case ZOOM_SCREEN: updateZoom(); break;
            }

            if (action == Action.ADD_BOOKMARK) {
                int page = controller.getCurrentPage();
                String text = getOrionContext().getBookmarkAccessor().selectExistingBookmark(getBookId(), page);
                ((EditText)findMyViewById(R.id.add_bookmark_text)).setText(text);
            }

            animator.setDisplayedChild(screenId);

            if  (device.optionViaDialog()) {
                dialog.show();
            }
        }
    }


    public void changeDayNightMode() {
        boolean newMode = !getView().isNightMode();
        getOrionContext().getOptions().saveProperty(GlobalOptions.DAY_NIGHT_MODE, newMode ? "NIGHT" : "DAY");
        getView().setNightMode(newMode);
        getView().invalidate();
    }

    public class MyArrayAdapter extends ArrayAdapter implements SpinnerAdapter {

        public MyArrayAdapter() {
            super(OrionViewerActivity.this, android.R.layout.simple_spinner_dropdown_item, OrionViewerActivity.this.getResources().getTextArray(R.array.fits));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                return  convertView;
            } else {
                TextView view = null;
                    view = new TextView(OrionViewerActivity.this);
                    view.setText("%");
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
}
