/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.polidea.customwidget;

import java.util.ArrayList;
import java.util.List;

import pl.polidea.demo.R;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Container for a tabbed window view. This object holds two children: a set of
 * tab labels that the user clicks to select a specific tab, and a FrameLayout
 * object that displays the contents of that page. The individual elements are
 * typically controlled using this container object, rather than setting values
 * on the child elements themselves.
 */
public class TheMissingTabHost extends FrameLayout implements ViewTreeObserver.OnTouchModeChangeListener {

    private TheMissingTabWidget mTabWidget;
    private FrameLayout mTabContent;
    private final List<TheMissingTabSpec> mTabSpecs = new ArrayList<TheMissingTabSpec>(2);
    protected int mCurrentTab = -1;
    private View mCurrentView = null;
    private boolean landscapePicturesAboveTitles = false;
    private boolean alwaysLandscape = false;
    protected LocalActivityManager mLocalActivityManager = null;
    private OnTabChangeListener mOnTabChangeListener;
    private OnKeyListener mTabKeyListener;

    public TheMissingTabHost(final Context context) {
        super(context);
        initTabHost();
    }

    public TheMissingTabHost(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context, attrs);
        initTabHost();
    }

    private void parseAttributes(final Context context, final AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TheMissingTabWidget);
        try {
            alwaysLandscape = a.getBoolean(R.styleable.TheMissingTabWidget_always_landscape, false);
            landscapePicturesAboveTitles = a.getBoolean(R.styleable.TheMissingTabWidget_pictures_in_landscape_above,
                    false);
        } finally {
            a.recycle();
        }

    }

    private void initTabHost() {
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        mCurrentTab = -1;
        mCurrentView = null;
    }

    /**
     * Get a new {@link TheMissingTabSpec} associated with this tab host.
     * 
     * @param tag
     *            required tag of tab.
     */
    public TheMissingTabSpec newTabSpec(final String tag) {
        return new TheMissingTabSpec(tag);
    }

    /**
     * Allows to set whether icons should be displayed above titles or left of
     * them in landscape mode. Courtesy of Thierry :
     * http://sites.google.com/site/freeboxrecorder/
     * 
     * @param landscapePicturesAboveTitles
     *            if true, then icons will be displayed above titles in the tab
     *            view. By default it is false which means that the icons will
     *            be displayed left of title.
     */
    public void setLandscapePicturesAboveTitles(final boolean landscapePicturesAboveTitles) {
        this.landscapePicturesAboveTitles = landscapePicturesAboveTitles;
    }

    /**
     * <p>
     * Call setup() before adding tabs if loading TabHost using findViewById().
     * <i><b>However</i></b>: You do not need to call setup() after getTabHost()
     * in {@link android.app.TabActivity TabActivity}. Example:
     * </p>
     * 
     * <pre>
     * mTabHost = (TabHost) findViewById(R.id.tabhost);
     * mTabHost.setup();
     * mTabHost.addTab(TAB_TAG_1, "Hello, world!", "Tab 1");
     */
    public void setup() {
        mTabWidget = (TheMissingTabWidget) findViewById(android.R.id.tabs);
        if (mTabWidget == null) {
            throw new RuntimeException(
                    "Your TabHost must have a TheMissingTabWidget whose id attribute is 'android.R.id.tabs'");
        }

        // KeyListener to attach to all tabs. Detects non-navigation keys
        // and relays them to the tab content.
        mTabKeyListener = new OnKeyListener() {
            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_ENTER:
                    return false;

                }
                mTabContent.requestFocus(View.FOCUS_FORWARD);
                return mTabContent.dispatchKeyEvent(event);
            }

        };

        mTabWidget.setTabSelectionListener(new TheMissingTabWidget.OnTabSelectionChanged() {
            @Override
            public void onTabSelectionChanged(final int tabIndex, final boolean clicked) {
                setCurrentTab(tabIndex);
                if (clicked) {
                    mTabContent.requestFocus(View.FOCUS_FORWARD);
                }
            }
        });

        mTabContent = (FrameLayout) findViewById(android.R.id.tabcontent);
        if (mTabContent == null) {
            throw new RuntimeException("Your TheMissingTabHost must have a FrameLayout whose id attribute is "
                    + "'android.R.id.tabcontent'");
        }
    }

    /**
     * If you are using
     * {@link TheMissingTabSpec#setContent(android.content.Intent)}, this must
     * be called since the activityGroup is needed to launch the local activity.
     * 
     * This is done for you if you extend {@link android.app.TabActivity}.
     * 
     * @param activityGroup
     *            Used to launch activities for tab content.
     */
    public void setup(final LocalActivityManager activityGroup) {
        setup();
        mLocalActivityManager = activityGroup;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final ViewTreeObserver treeObserver = getViewTreeObserver();
        if (treeObserver != null) {
            treeObserver.addOnTouchModeChangeListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        final ViewTreeObserver treeObserver = getViewTreeObserver();
        if (treeObserver != null) {
            treeObserver.removeOnTouchModeChangeListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchModeChanged(final boolean isInTouchMode) {
        if (!isInTouchMode) {
            // leaving touch mode.. if nothing has focus, let's give it to
            // the indicator of the current tab
            if (mCurrentView != null && (!mCurrentView.hasFocus() || mCurrentView.isFocused())) {
                mTabWidget.getChildTabViewAt(mCurrentTab).requestFocus();
            }
        }
    }

    /**
     * Add a tab.
     * 
     * @param tabSpec
     *            Specifies how to create the indicator and content.
     */
    public void addTab(final TheMissingTabSpec tabSpec) {

        if (tabSpec.mIndicatorStrategy == null) {
            throw new IllegalArgumentException("you must specify a way to create the tab indicator.");
        }

        if (tabSpec.mContentStrategy == null) {
            throw new IllegalArgumentException("you must specify a way to create the tab content");
        }
        final View tabIndicator = tabSpec.mIndicatorStrategy.createIndicatorView();
        tabIndicator.setOnKeyListener(mTabKeyListener);

        // If this is a custom view, then do not draw the bottom strips for
        // the tab indicators.
        if (tabSpec.mIndicatorStrategy instanceof ViewIndicatorStrategy) {
            mTabWidget.setStripEnabled(false);
        }
        mTabWidget.addView(tabIndicator);
        mTabSpecs.add(tabSpec);

        if (mCurrentTab == -1) {
            setCurrentTab(0);
        }
    }

    /**
     * Removes all tabs from the tab widget associated with this tab host.
     */
    public void clearAllTabs() {
        mTabWidget.removeAllViews();
        initTabHost();
        mTabContent.removeAllViews();
        mTabSpecs.clear();
        requestLayout();
        invalidate();
    }

    public TheMissingTabWidget getTabWidget() {
        return mTabWidget;
    }

    public int getCurrentTab() {
        return mCurrentTab;
    }

    public String getCurrentTabTag() {
        if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) {
            return mTabSpecs.get(mCurrentTab).getTag();
        }
        return null;
    }

    public View getCurrentTabView() {
        if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) {
            return mTabWidget.getChildTabViewAt(mCurrentTab);
        }
        return null;
    }

    public View getCurrentView() {
        return mCurrentView;
    }

    public void setCurrentTabByTag(final String tag) {
        int i;
        for (i = 0; i < mTabSpecs.size(); i++) {
            if (mTabSpecs.get(i).getTag().equals(tag)) {
                setCurrentTab(i);
                break;
            }
        }
    }

    /**
     * Get the FrameLayout which holds tab content
     */
    public FrameLayout getTabContentView() {
        return mTabContent;
    }

    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        final boolean handled = super.dispatchKeyEvent(event);

        // unhandled key ups change focus to tab indicator for embedded
        // activities
        // when there is nothing that will take focus from default focus
        // searching
        if (!handled && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP
                && mCurrentView != null
                // && mCurrentView.isRootNamespace()
                && mCurrentView.hasFocus() && mCurrentView.findFocus().focusSearch(View.FOCUS_UP) == null) {
            mTabWidget.getChildTabViewAt(mCurrentTab).requestFocus();
            playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
            return true;
        }
        return handled;
    }

    @Override
    public void dispatchWindowFocusChanged(final boolean hasFocus) {
        if (mCurrentView != null) {
            mCurrentView.dispatchWindowFocusChanged(hasFocus);
        }
    }

    public void setCurrentTab(final int index) {
        if (index < 0 || index >= mTabSpecs.size()) {
            return;
        }

        if (index == mCurrentTab) {
            return;
        }

        // notify old tab content
        if (mCurrentTab != -1) {
            mTabSpecs.get(mCurrentTab).mContentStrategy.tabClosed();
        }

        mCurrentTab = index;
        final TheMissingTabHost.TheMissingTabSpec spec = mTabSpecs.get(index);

        // Call the tab widget's focusCurrentTab(), instead of just
        // selecting the tab.
        mTabWidget.focusCurrentTab(mCurrentTab);

        // tab content
        mCurrentView = spec.mContentStrategy.getContentView();

        if (mCurrentView.getParent() == null) {
            mTabContent.addView(mCurrentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT));
        }

        if (!mTabWidget.hasFocus()) {
            // if the tab widget didn't take focus (likely because we're in
            // touch mode)
            // give the current tab content view a shot
            mCurrentView.requestFocus();
        }

        // mTabContent.requestFocus(View.FOCUS_FORWARD);
        invokeOnTabChangeListener();
    }

    /**
     * Register a callback to be invoked when the selected state of any of the
     * items in this list changes
     * 
     * @param l
     *            The callback that will run
     */
    public void setOnTabChangedListener(final OnTabChangeListener l) {
        mOnTabChangeListener = l;
    }

    private void invokeOnTabChangeListener() {
        if (mOnTabChangeListener != null) {
            mOnTabChangeListener.onTabChanged(getCurrentTabTag());
        }
    }

    /**
     * Interface definition for a callback to be invoked when tab changed
     */
    public interface OnTabChangeListener {
        void onTabChanged(String tabId);
    }

    /**
     * Makes the content of a tab when it is selected. Use this if your tab
     * content needs to be created on demand, i.e. you are not showing an
     * existing view or starting an activity.
     */
    public interface TabContentFactory {
        /**
         * Callback to make the tab contents
         * 
         * @param tag
         *            Which tab was selected.
         * @return The view to display the contents of the selected tab.
         */
        View createTabContent(String tag);
    }

    /**
     * A tab has a tab indicator, content, and a tag that is used to keep track
     * of it. This builder helps choose among these options.
     * 
     * For the tab indicator, your choices are: 1) set a label 2) set a label
     * and an icon
     * 
     * For the tab content, your choices are: 1) the id of a {@link View} 2) a
     * {@link TabContentFactory} that creates the {@link View} content. 3) an
     * {@link Intent} that launches an {@link android.app.Activity}.
     */
    public class TheMissingTabSpec {

        private final String mTag;

        private IndicatorStrategy mIndicatorStrategy;
        private ContentStrategy mContentStrategy;

        private TheMissingTabSpec(final String tag) {
            mTag = tag;
        }

        /**
         * Specify a label as the tab indicator.
         */
        public TheMissingTabSpec setIndicator(final CharSequence label) {
            mIndicatorStrategy = new LabelIndicatorStrategy(label);
            return this;
        }

        /**
         * Specify a label and icon as the tab indicator.
         */
        public TheMissingTabSpec setIndicator(final CharSequence label, final Drawable icon) {
            mIndicatorStrategy = new LabelAndIconIndicatorStrategy(label, icon);
            return this;
        }

        /**
         * Specify a view as the tab indicator.
         */
        public TheMissingTabSpec setIndicator(final View view) {
            mIndicatorStrategy = new ViewIndicatorStrategy(view);
            return this;
        }

        /**
         * Specify the id of the view that should be used as the content of the
         * tab.
         */
        public TheMissingTabSpec setContent(final int viewId) {
            mContentStrategy = new ViewIdContentStrategy(viewId);
            return this;
        }

        /**
         * Specify a {@link android.widget.TabHost.TabContentFactory} to use to
         * create the content of the tab.
         */
        public TheMissingTabSpec setContent(final TabContentFactory contentFactory) {
            mContentStrategy = new FactoryContentStrategy(mTag, contentFactory);
            return this;
        }

        /**
         * Specify an intent to use to launch an activity as the tab content.
         */
        public TheMissingTabSpec setContent(final Intent intent) {
            mContentStrategy = new IntentContentStrategy(mTag, intent);
            return this;
        }

        public String getTag() {
            return mTag;
        }
    }

    /**
     * Specifies what you do to create a tab indicator.
     */
    private static interface IndicatorStrategy {

        /**
         * Return the view for the indicator.
         */
        View createIndicatorView();
    }

    /**
     * Specifies what you do to manage the tab content.
     */
    private static interface ContentStrategy {

        /**
         * Return the content view. The view should may be cached locally.
         */
        View getContentView();

        /**
         * Perhaps do something when the tab associated with this content has
         * been closed (i.e make it invisible, or remove it).
         */
        void tabClosed();
    }

    /**
     * How to create a tab indicator that just has a label.
     */
    private class LabelIndicatorStrategy implements IndicatorStrategy {

        private final CharSequence mLabel;

        private LabelIndicatorStrategy(final CharSequence label) {
            mLabel = label;
        }

        @Override
        public View createIndicatorView() {
            final Context context = getContext();
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int indicatorLayout = alwaysLandscape ? R.layout.tab_indicator_always_landscape : R.layout.tab_indicator;
            if (landscapePicturesAboveTitles) {
                indicatorLayout = R.layout.tab_indicator_picture_in_landscape_above;
            }
            final View tabIndicator = inflater.inflate(indicatorLayout, mTabWidget, false);
            final TextView tv = (TextView) tabIndicator.findViewById(android.R.id.title);
            tv.setText(mLabel);
            /*
             * if (context.getApplicationInfo().targetSdkVersion <=
             * Build.VERSION_CODES.DONUT) { // Donut apps get old color scheme
             * tabIndicator.setBackgroundResource(R.drawable.tab_indicator_v4);
             * tv.setTextColor(context.getResources().getColorStateList(
             * R.color.tab_indicator_text_v4)); }
             */
            return tabIndicator;
        }
    }

    /**
     * How we create a tab indicator that has a label and an icon
     */
    private class LabelAndIconIndicatorStrategy implements IndicatorStrategy {

        private final CharSequence mLabel;
        private final Drawable mIcon;

        private LabelAndIconIndicatorStrategy(final CharSequence label, final Drawable icon) {
            mLabel = label;
            mIcon = icon;
        }

        @Override
        public View createIndicatorView() {
            final Context context = getContext();
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            int indicatorLayout = alwaysLandscape ? R.layout.tab_indicator_always_landscape : R.layout.tab_indicator;

            if (landscapePicturesAboveTitles) {
                indicatorLayout = R.layout.tab_indicator_picture_in_landscape_above;
            }
            final View tabIndicator = inflater.inflate(indicatorLayout, mTabWidget, // parent
                    false);

            final TextView tv = (TextView) tabIndicator.findViewById(android.R.id.title);
            tv.setText(mLabel);

            final ImageView iconView = (ImageView) tabIndicator.findViewById(R.id.icon);
            iconView.setImageDrawable(mIcon);

            // if (context.getApplicationInfo().targetSdkVersion <=
            // Build.VERSION_CODES.DONUT) {
            // // Donut apps get old color scheme
            // tabIndicator.setBackgroundResource(R.drawable.tab_indicator_v4);
            // tv.setTextColor(context.getResources().getColorStateList(
            // R.color.tab_indicator_text_v4));
            // }

            return tabIndicator;
        }
    }

    /**
     * How to create a tab indicator by specifying a view.
     */
    private class ViewIndicatorStrategy implements IndicatorStrategy {

        private final View mView;

        private ViewIndicatorStrategy(final View view) {
            mView = view;
        }

        @Override
        public View createIndicatorView() {
            return mView;
        }
    }

    /**
     * How to create the tab content via a view id.
     */
    private class ViewIdContentStrategy implements ContentStrategy {

        private final View mView;

        private ViewIdContentStrategy(final int viewId) {
            mView = mTabContent.findViewById(viewId);
            if (mView != null) {
                mView.setVisibility(View.GONE);
            } else {
                throw new RuntimeException("Could not create tab content because " + "could not find view with id "
                        + viewId);
            }
        }

        @Override
        public View getContentView() {
            mView.setVisibility(View.VISIBLE);
            return mView;
        }

        @Override
        public void tabClosed() {
            mView.setVisibility(View.GONE);
        }
    }

    /**
     * How tab content is managed using {@link TabContentFactory}.
     */
    private class FactoryContentStrategy implements ContentStrategy {
        private View mTabContent;
        private final CharSequence mTag;
        private final TabContentFactory mFactory;

        public FactoryContentStrategy(final CharSequence tag, final TabContentFactory factory) {
            mTag = tag;
            mFactory = factory;
        }

        @Override
        public View getContentView() {
            if (mTabContent == null) {
                mTabContent = mFactory.createTabContent(mTag.toString());
            }
            mTabContent.setVisibility(View.VISIBLE);
            return mTabContent;
        }

        @Override
        public void tabClosed() {
            mTabContent.setVisibility(View.GONE);
        }
    }

    /**
     * How tab content is managed via an {@link Intent}: the content view is the
     * decorview of the launched activity.
     */
    private class IntentContentStrategy implements ContentStrategy {

        private final String mTag;
        private final Intent mIntent;

        private View mLaunchedView;

        private IntentContentStrategy(final String tag, final Intent intent) {
            mTag = tag;
            mIntent = intent;
        }

        @Override
        public View getContentView() {
            if (mLocalActivityManager == null) {
                throw new IllegalStateException(
                        "Did you forget to call 'public void setup(LocalActivityManager activityGroup)'?");
            }
            final Window w = mLocalActivityManager.startActivity(mTag, mIntent);
            final View wd = w != null ? w.getDecorView() : null;
            if (mLaunchedView != wd && mLaunchedView != null) {
                if (mLaunchedView.getParent() != null) {
                    mTabContent.removeView(mLaunchedView);
                }
            }
            mLaunchedView = wd;

            // XXX Set FOCUS_AFTER_DESCENDANTS on embedded activities for now so
            // they can get
            // focus if none of their children have it. They need focus to be
            // able to
            // display menu items.
            //
            // Replace this with something better when Bug 628886 is fixed...
            //
            if (mLaunchedView != null) {
                mLaunchedView.setVisibility(View.VISIBLE);
                mLaunchedView.setFocusableInTouchMode(true);
                ((ViewGroup) mLaunchedView).setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
            }
            return mLaunchedView;
        }

        @Override
        public void tabClosed() {
            if (mLaunchedView != null) {
                mLaunchedView.setVisibility(View.GONE);
            }
        }
    }

}