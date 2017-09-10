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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.ViewAnimator;

import java.io.File;

import universe.constellation.orion.viewer.android.FileUtils;
import universe.constellation.orion.viewer.device.Device;
import universe.constellation.orion.viewer.dialog.CropDialog;
import universe.constellation.orion.viewer.dialog.CropDialogBuilderKt;
import universe.constellation.orion.viewer.dialog.SearchDialog;
import universe.constellation.orion.viewer.dialog.TapHelpDialog;
import universe.constellation.orion.viewer.document.Document;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.selection.NewTouchProcessor;
import universe.constellation.orion.viewer.selection.NewTouchProcessorWithScale;
import universe.constellation.orion.viewer.selection.SelectionAutomata;
import universe.constellation.orion.viewer.view.FullScene;
import universe.constellation.orion.viewer.view.OrionStatusBarHelper;

public class OrionViewerActivity extends OrionBaseActivity {

    private AppCompatDialog dialog;

    public static final int OPEN_BOOKMARK_ACTIVITY_RESULT = 1;

    public static final int ROTATION_SCREEN = 0;

    public static final int PAGE_SCREEN = 1;

    public static final int ZOOM_SCREEN = 2;

    public static final int CROP_SCREEN = 3;

    public static final int PAGE_LAYOUT_SCREEN = 4;

    public static final int ADD_BOOKMARK_SCREEN = 5;

    public static final int CROP_RESTRICTION_MIN = -10;

    public static final int CROP_RESTRICTION_MAX = 40;

    private final SubscriptionManager manager = new SubscriptionManager();

    private ViewAnimator animator;

    private LastPageInfo lastPageInfo;

    private Controller controller;

    private OperationHolder operation = new OperationHolder();

    private GlobalOptions globalOptions;

    private Intent myIntent;

    public boolean isResumed;

    private SelectionAutomata selectionAutomata;

    private NewTouchProcessor newTouchProcessor;

    private boolean hasActionBar;

    private FullScene fullScene;

    private boolean hasReadPermissions = false;

    private Intent lastIntent = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Common.d("Creating file manager");

        loadGlobalOptions();

        getOrionContext().setViewActivity(this);
        OptionActions.FULL_SCREEN.doAction(this, !globalOptions.isFullScreen(), globalOptions.isFullScreen());
        super.onOrionCreate(savedInstanceState, R.layout.main_view);

        hasActionBar = globalOptions.isActionBarVisible();
        OptionActions.SHOW_ACTION_BAR.doAction(this, !hasActionBar, hasActionBar);

        OrionScene view = (OrionScene) findViewById(R.id.view);
        fullScene = new FullScene((ViewGroup) findViewById(R.id.orion_full_scene), view, (ViewGroup) findViewById(R.id.orion_status_bar), getOrionContext());

        OptionActions.SHOW_STATUS_BAR.doAction(this, !globalOptions.isStatusBarVisible(), globalOptions.isStatusBarVisible());
        OptionActions.SHOW_OFFSET_ON_STATUS_BAR.doAction(this, !globalOptions.isShowOffsetOnStatusBar(), globalOptions.isShowOffsetOnStatusBar());
        getFullScene().setDrawOffPage(globalOptions.isDrawOffPage());

        initDialogs();

        myIntent = getIntent();
        newTouchProcessor = getOrionContext().getSdkVersion() >= Build.VERSION_CODES.FROYO ?
                new NewTouchProcessorWithScale(view, this) : new NewTouchProcessor(view, this);

        hasReadPermissions = Permissions.checkReadPermission(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Permissions.ORION_ASK_PERMISSION_CODE == requestCode && !hasReadPermissions) {
            System.out.println("Permission callback...");
            int i = 0;
            while(i < permissions.length) {
                if (android.Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    hasReadPermissions = true;
                    onNewIntent(lastIntent);
                    lastIntent = null;
                    return;
                }
                i++;
            }
        }
    }


    private void initDialogs() {
        initOptionDialog();
        initRotationScreen();

        //page chooser
        initPagePeekerScreen();

        initZoomScreen();

        initPageLayoutScreen();

        initAddBookmarkScreen();
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
        Common.d("OVA: on new intent " + intent);
        if (!hasReadPermissions) {
            Common.d("OVA: Waiting for read permissions");
            lastIntent = intent;
            return;
        }

        Uri uri = intent.getData();
        if (uri != null) {
            Common.d("File URI  = " + uri.toString());
            String path = null;
            try {
                if ("content".equalsIgnoreCase(uri.getScheme())) {
                    path = FileUtils.getPath(this, uri);
                }

                String file = path != null ? path : uri.getPath();

                if (controller != null) {
                    if (lastPageInfo != null) {
                        if (lastPageInfo.openingFileName.equals(file)) {
                            //keep controller
                            controller.drawPage();
                            return;
                        }
                    }

                    destroyControllerAndBook();
                }

                Common.stopLogger();
                openFile(file);
            } catch (Exception e) {
                showAlertWithExceptionThrow(intent, e);
            }
        } else /*if (intent.getAction().endsWith("MAIN"))*/ {
            //TODO error
        }
    }

    private Document openFile(String filePath) throws Exception {
        Document doc = null;
        Common.d("Trying to open file: " + filePath);

        getOrionContext().onNewBook(filePath);
        try {
            lastPageInfo = LastPageInfo.loadBookParameters(this, filePath);
            getOrionContext().setCurrentBookParameters(lastPageInfo);
            OptionActions.DEBUG.doAction(this, false, getGlobalOptions().getBooleanProperty("DEBUG", false));

            doc = FileUtil.openFile(filePath);

            LayoutStrategy layoutStrategy = SimpleLayoutStrategy.create(doc);

            RenderThread renderer = new RenderThread(this, layoutStrategy, doc, fullScene);

            controller = new Controller(this, doc, layoutStrategy, renderer);

            controller.changeOrinatation(lastPageInfo.screenOrientation);

            OrionScene drawView = fullScene.getDrawView();
            controller.init(lastPageInfo, new Point(drawView.getSceneWidth(), drawView.getSceneHeight()));

            getSubscriptionManager().sendDocOpenedNotification(controller);

            getView().setDimensionAware(controller);

            controller.drawPage();

            String title = doc.getTitle();
            if (title == null || "".equals(title)) {
                int idx = filePath.lastIndexOf('/');
                title = filePath.substring(idx + 1);
                title = title.substring(0, title.lastIndexOf("."));
            }


            fullScene.onNewBook(title, controller.getPageCount());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            globalOptions.addRecentEntry(new GlobalOptions.RecentEntry(new File(filePath).getAbsolutePath()));

            lastPageInfo.totalPages = doc.getPageCount();
            device.onNewBook(lastPageInfo, doc);

            askPassword(controller);

        } catch (Exception e) {
            Common.d(e);
            if (doc != null) {
                doc.destroy();
            }
            throw e;
        }
        return doc;
    }

    private void showAlertWithExceptionThrow(final Intent intent, final Exception e) {
        AlertDialog.Builder themedAlertBuilder = createThemedAlertBuilder().setMessage("Error while opening " + intent + ": " + e.getMessage() + " cause of " + e.getCause());
        themedAlertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                throw new RuntimeException("Exception on processing " + intent, e);
            }
        });
        themedAlertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
                throw new RuntimeException("Exception on processing " + intent, e);
            }
        });
        themedAlertBuilder.create().show();
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
                        controller.drawPage(parsedInput - 1);
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
        int spinnerIndex;
        zoomInternal = 1;
        try {
            int zoom = controller.getZoom10000Factor();
            if (zoom <= 0) {
                spinnerIndex = -zoom + 1;
                zoom = (int) (10000 * controller.getCurrentPageZoom());
            } else {
                spinnerIndex = 0;
                textView.setText(String.valueOf(zoom / 100f));
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
                onAnimatorCancel();
                updatePageLayout();
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


   private void initAddBookmarkScreen() {
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
                    AlertDialog.Builder buider = createThemedAlertBuilder();
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

        destroyControllerAndBook();

        if (dialog != null) {
            dialog.dismiss();
        }
        getOrionContext().destroyDb();
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
        L.log("onKeyUp key = " + keyCode + " " + event.isCanceled() + " " + doTrack(keyCode));
        if(event.isCanceled()) {
            L.log("Tracking = " + keyCode);
            return super.onKeyUp(keyCode,  event);
        }

        return processKey(keyCode, event, false) || super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        L.log("onKeyDown = " + keyCode + " " + event.isCanceled() + " " + doTrack(keyCode)) ;
        if (doTrack(keyCode)) {
            L.log("Tracking = " + keyCode);
            event.startTracking();
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

    private void changePage(int operation) {
        boolean swapKeys = globalOptions.isSwapKeys();
        int width = getView().getSceneWidth();
        int height = getView().getSceneHeight();
        boolean landscape = width > height || controller.getRotation() != 0; /*second condition for nook and alex*/
        if (controller != null) {
            if (operation == Device.NEXT && (!landscape || !swapKeys) || swapKeys && operation == Device.PREV && landscape) {
                controller.drawNext();
            } else {
                controller.drawPrev();
            }
        }
    }

    private void loadGlobalOptions() {
        globalOptions = getOrionContext().getOptions();
    }

    private void saveGlobalOptions() {
        Common.d("Saving global options...");
        globalOptions.saveRecents();
        Common.d("Done!");
    }

    public OrionScene getView() {
        return fullScene.getDrawView();
    }

    public FullScene getFullScene() {
        return fullScene;
    }

    OrionStatusBarHelper getStatusBarHelper() {
        return fullScene.getStatusBarHelper();
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        if (!hasActionBar) {
            for (int i = 0; i < menu.size(); i++) {
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

            case R.id.search_menu_item: action = Action.SEARCH; break;
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

    SubscriptionManager getSubscriptionManager() {
        return manager;
    }

    private void initOptionDialog() {
        dialog = new AppCompatDialog(this);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.options_dialog);
        animator = ((ViewAnimator)dialog.findViewById(R.id.viewanim));

        getView().setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return newTouchProcessor.onTouch(event);
            }
        });

        dialog.setCanceledOnTouchOutside(true);
    }

    public void doAction(int code) {
        Action action = Action.getAction(code);
        doAction(action);
        Common.d("Code action " + code);
    }


    private void doAction(Action action) {
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

    private void onApplyAction(boolean close) {
        if (close || globalOptions.isApplyAndClose()) {
            onAnimatorCancel();
        }
    }

    private void initRotationScreen() {
        RadioGroup rotationGroup = (RadioGroup) findMyViewById(R.id.rotationGroup);
        rotationGroup.setVisibility(View.GONE);

        final ListView list = (ListView) findMyViewById(R.id.rotationList);

        //set choices and replace 0 one with Application Default
        boolean isLevel9 = getOrionContext().getSdkVersion() >= 9;
        CharSequence[] values = getResources().getTextArray(isLevel9 ? R.array.screen_orientation_full_desc : R.array.screen_orientation_desc);
        CharSequence[] newValues = new CharSequence[values.length];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[0] = getResources().getString(R.string.orientation_default_rotation);

        list.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, newValues));

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

        ImageButton cancel = (ImageButton) findMyViewById(R.id.rotation_close);
            cancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    onAnimatorCancel();
                    updateRotation();
                }
            });
    }

    private void updateRotation() {
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

    private void updateBrightness() {
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

    private long insertOrGetBookId() {
        LastPageInfo info = lastPageInfo;
        Long bookId = getOrionContext().getTempOptions().bookId;
        if (bookId == null || bookId == -1) {
            bookId = getOrionContext().getBookmarkAccessor().insertOrUpdate(info.simpleFileName, info.fileSize);
            getOrionContext().getTempOptions().bookId = bookId;
        }
        return bookId.intValue();
    }

    private boolean insertBookmark(int page, String text) {
        long id = insertOrGetBookId();
        if (id != -1) {
            long bokmarkId = getOrionContext().getBookmarkAccessor().insertOrUpdateBookmark(id, page, text);
            return bokmarkId != -1;
        }
        return false;
    }

    public void doubleClickAction(int x, int y) {
        SelectionAutomata.selectText(
                this, true, true, getSelectionAutomata().dialog,
                SelectionAutomata.getSelectionRectangle(x, y, 0, 0, true)
        );
    }


    long getBookId() {
        Common.d("Selecting book id...");
        LastPageInfo info = lastPageInfo;
        Long bookId = getOrionContext().getTempOptions().bookId;
        if (bookId == null || bookId == -1) {
            bookId = getOrionContext().getBookmarkAccessor().selectBookId(info.simpleFileName, info.fileSize);
            getOrionContext().getTempOptions().bookId = bookId;
        }
        Common.d("...book id = " + bookId);
        return bookId;
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

    void showOrionDialog(int screenId, Action action, Object parameter) {
        if (screenId == CROP_SCREEN) {
            CropDialog cropDialog = CropDialogBuilderKt.create(this, controller.getMargins());
            cropDialog.show();
            return;
        }
        if (screenId != -1) {
            switch (screenId) {
                case ROTATION_SCREEN: updateRotation(); break;
                case PAGE_LAYOUT_SCREEN: updatePageLayout();
                case PAGE_SCREEN: updatePageSeeker(); break;
                case ZOOM_SCREEN: updateZoom(); break;
            }

            if (action == Action.ADD_BOOKMARK) {
                String parameterText = (String) parameter;

                int page = controller.getCurrentPage();
                String newText = getOrionContext().getBookmarkAccessor().selectExistingBookmark(getBookId(), page, parameterText);

                boolean notOverride = parameterText == null || parameterText.equals(newText);
                findMyViewById(R.id.warn_text_override).setVisibility(notOverride ? View.GONE : View.VISIBLE);

                ((EditText)findMyViewById(R.id.add_bookmark_text)).setText(notOverride ? newText : parameterText);
            }

            animator.setDisplayedChild(screenId);
            dialog.show();
        }
    }


    void textSelectionMode(boolean isSingleSelection, boolean translate) {
        getSelectionAutomata().startSelection(isSingleSelection, translate);
    }

    private SelectionAutomata getSelectionAutomata() {
        if (selectionAutomata == null) {
            selectionAutomata = new SelectionAutomata(this);
        }
        return selectionAutomata;
    }

    private class MyArrayAdapter extends ArrayAdapter implements SpinnerAdapter {

        MyArrayAdapter() {
            super(OrionViewerActivity.this, R.layout.support_simple_spinner_dropdown_item, OrionViewerActivity.this.getResources().getTextArray(R.array.fits));
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

    private static class Nook2ListAdapter extends ArrayAdapter  {

        private int color;

        Nook2ListAdapter(Context context, int textViewResourceId, Object[] objects, TextView view) {
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
            AlertDialog.Builder builder = createThemedAlertBuilder();
            builder.setTitle("Password");

            final EditText input = new EditText(this);
            input.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            input.setTransformationMethod(new PasswordTransformationMethod());
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (controller.authenticate(input.getText().toString())) {
                        dialog.dismiss();
                    } else {
                        askPassword(controller);
                        showWarning("Wrong password!");
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    }

    //big hack
    void myprocessOnActivityVisible() {
        if (getGlobalOptions().isShowTapHelp() && !getOrionContext().isTesting) {
            getGlobalOptions().saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, false);
            new TapHelpDialog(this).showDialog();
        }
    }

    void startSearch() {
        SearchDialog.newInstance().show(getSupportFragmentManager(), "search");
    }

    private void destroyControllerAndBook() {
        if (lastPageInfo != null) {
            device.onBookClose(lastPageInfo);
        }
        if (controller != null) {
            controller.destroy();
            controller = null;
        }
    }
}
