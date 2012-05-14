package pl.polidea.customwidget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import pl.polidea.demo.R;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * 
 * Displays a list of tab labels representing each page in the parent's tab
 * collection. The container object for this widget is {@link TheMissingTabHost
 * TheMissingTabHost}. When the user selects a tab, this object sends a message
 * to the parent container, TabHost, to tell it to switch the displayed page.
 * You typically won't use many methods directly on this object. The container
 * TabHost is used to add labels, add the callback handler, and manage
 * callbacks. You might call this object to iterate the list of tabs, or to
 * tweak the layout of the tab list, but most methods should be called on the
 * containing TabHost object.
 * 
 * @attr ref android.R.styleable#TabWidget_divider
 * @attr ref android.R.styleable#TabWidget_tabStripEnabled
 * @attr ref android.R.styleable#TabWidget_tabStripLeft
 * @attr ref android.R.styleable#TabWidget_tabStripRight
 */
public class TheMissingTabWidget extends LinearLayout implements OnFocusChangeListener {
    private OnTabSelectionChanged mSelectionChangedListener;

    private int mSelectedTab = 0;

    private Drawable mLeftStrip;
    private Drawable mRightStrip;

    private boolean mDrawBottomStrips = true;
    private boolean mStripMoved;

    private Drawable mDividerDrawable;

    private final Rect mBounds = new Rect();

    private int orientation;
    private boolean alwaysLandscape;

    public TheMissingTabWidget(final Context context) {
        this(context, null);
    }

    public TheMissingTabWidget(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.tabWidgetStyle);
    }

    public TheMissingTabWidget(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TheMissingTabWidget, defStyle, 0);

        // attributes
        mDrawBottomStrips = true;
        mDividerDrawable = a.getDrawable(R.styleable.TheMissingTabWidget_android_divider);
        alwaysLandscape = a.getBoolean(R.styleable.TheMissingTabWidget_always_landscape, false);
        mLeftStrip = null;
        mRightStrip = null;
        initTabWidget();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        mStripMoved = true;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected int getChildDrawingOrder(final int childCount, final int i) {
        // Always draw the selected tab last, so that drop shadows are drawn
        // in the correct z-order.
        if (i == childCount - 1) {
            return mSelectedTab;
        } else if (i >= mSelectedTab) {
            return i + 1;
        } else {
            return i;
        }
    }

    private void updateChildrenDrawingOrder() {
        try {
            final Class< ? > clazz = ViewGroup.class;
            final Method m = clazz.getDeclaredMethod("setChildrenDrawingOrderEnabled", boolean.class);
            m.invoke(this, true);
            return;
        } catch (final SecurityException e) {
            // skip - we cannot do much
        } catch (final NoSuchMethodException e) {
            // skip - we cannot do much
        } catch (final IllegalArgumentException e) {
            // skip - we cannot do much
        } catch (final IllegalAccessException e) {
            // skip - we cannot do much
        } catch (final InvocationTargetException e) {
            // skip - we cannot do much
        }

    }

    private void initTabWidget() {
        final int orientation = alwaysLandscape ? Configuration.ORIENTATION_LANDSCAPE : getResources().getConfiguration().orientation;
        switch (orientation) {
        case Configuration.ORIENTATION_LANDSCAPE:
            setOrientation(LinearLayout.VERTICAL);
            this.orientation = Configuration.ORIENTATION_LANDSCAPE;
            break;
        case Configuration.ORIENTATION_PORTRAIT:
        case Configuration.ORIENTATION_SQUARE:
        case Configuration.ORIENTATION_UNDEFINED:
            setOrientation(LinearLayout.HORIZONTAL);
            this.orientation = Configuration.ORIENTATION_PORTRAIT;
            break;
        }
        updateChildrenDrawingOrder();
        final Context context = getContext();
        final Resources resources = context.getResources();
        if (mLeftStrip == null) {
            mLeftStrip = resources.getDrawable(alwaysLandscape ? R.drawable.tab_bottom_left_always_landscape : R.drawable.tab_bottom_left);
        }
        if (mRightStrip == null) {
            mRightStrip = resources.getDrawable(alwaysLandscape ? R.drawable.tab_bottom_right_always_landscape : R.drawable.tab_bottom_right);
        }

        // Deal with focus, as we don't want the focus to go by default
        // to a tab other than the current tab
        setFocusable(true);
        setOnFocusChangeListener(this);
    }

    /**
     * Returns the tab indicator view at the given index.
     * 
     * @param index
     *            the zero-based index of the tab indicator view to return
     * @return the tab indicator view at the given index
     */
    public View getChildTabViewAt(int index) {
        // If we are using dividers, then instead of tab views at 0, 1, 2, ...
        // we have tab views at 0, 2, 4, ...
        if (mDividerDrawable != null) {
            index *= 2;
        }
        return getChildAt(index);
    }

    /**
     * Returns the number of tab indicator views.
     * 
     * @return the number of tab indicator views.
     */
    public int getTabCount() {
        int children = getChildCount();

        // If we have dividers, then we will always have an odd number of
        // children: 1, 3, 5, ... and we want to convert that sequence to
        // this: 1, 2, 3, ...
        if (mDividerDrawable != null) {
            children = (children + 1) / 2;
        }
        return children;
    }

    /**
     * Sets the drawable to use as a divider between the tab indicators.
     * 
     * @param drawable
     *            the divider drawable
     */
    public void setDividerDrawable(final Drawable drawable) {
        mDividerDrawable = drawable;
        requestLayout();
        invalidate();
    }

    /**
     * Sets the drawable to use as a divider between the tab indicators.
     * 
     * @param resId
     *            the resource identifier of the drawable to use as a divider.
     */
    public void setDividerDrawable(final int resId) {
        mDividerDrawable = getContext().getResources().getDrawable(resId);
        requestLayout();
        invalidate();
    }

    /**
     * Sets the drawable to use as the left part of the strip below the tab
     * indicators.
     * 
     * @param drawable
     *            the left strip drawable
     */
    public void setLeftStripDrawable(final Drawable drawable) {
        mLeftStrip = drawable;
        requestLayout();
        invalidate();
    }

    /**
     * Sets the drawable to use as the left part of the strip below the tab
     * indicators.
     * 
     * @param resId
     *            the resource identifier of the drawable to use as the left
     *            strip drawable
     */
    public void setLeftStripDrawable(final int resId) {
        mLeftStrip = getContext().getResources().getDrawable(resId);
        requestLayout();
        invalidate();
    }

    /**
     * Sets the drawable to use as the right part of the strip below the tab
     * indicators.
     * 
     * @param drawable
     *            the right strip drawable
     */
    public void setRightStripDrawable(final Drawable drawable) {
        mRightStrip = drawable;
        requestLayout();
        invalidate();
    }

    /**
     * Sets the drawable to use as the right part of the strip below the tab
     * indicators.
     * 
     * @param resId
     *            the resource identifier of the drawable to use as the right
     *            strip drawable
     */
    public void setRightStripDrawable(final int resId) {
        mRightStrip = getContext().getResources().getDrawable(resId);
        requestLayout();
        invalidate();
    }

    /**
     * Controls whether the bottom strips on the tab indicators are drawn or
     * not. The default is to draw them. If the user specifies a custom view for
     * the tab indicators, then the TheMissingTabHost class calls this method to
     * disable drawing of the bottom strips.
     * 
     * @param stripEnabled
     *            true if the bottom strips should be drawn.
     */
    public void setStripEnabled(final boolean stripEnabled) {
        mDrawBottomStrips = stripEnabled;
        invalidate();
    }

    /**
     * Indicates whether the bottom strips on the tab indicators are drawn or
     * not.
     */
    public boolean isStripEnabled() {
        return mDrawBottomStrips;
    }

    @Override
    public void childDrawableStateChanged(final View child) {
        if (getTabCount() > 0 && child == getChildTabViewAt(mSelectedTab)) {
            // To make sure that the bottom strip is redrawn
            invalidate();
        }
        super.childDrawableStateChanged(child);
    }

    @Override
    public void dispatchDraw(final Canvas canvas) {
        super.dispatchDraw(canvas);

        // Do nothing if there are no tabs.
        if (getTabCount() == 0) {
            return;
        }

        // If the user specified a custom view for the tab indicators, then
        // do not draw the bottom strips.
        if (!mDrawBottomStrips) {
            // Skip drawing the bottom strips.
            return;
        }

        final View selectedChild = getChildTabViewAt(mSelectedTab);

        final Drawable leftStrip = mLeftStrip;
        final Drawable rightStrip = mRightStrip;

        leftStrip.setState(selectedChild.getDrawableState());
        rightStrip.setState(selectedChild.getDrawableState());

        switch (orientation) {
        case Configuration.ORIENTATION_LANDSCAPE:
            if (mStripMoved) {
                final Rect bounds = mBounds;
                bounds.top = selectedChild.getTop();
                bounds.bottom = selectedChild.getBottom();
                final int myWidth = getWidth();
                leftStrip.setBounds(myWidth - leftStrip.getIntrinsicWidth(),
                        Math.min(0, bounds.top - leftStrip.getIntrinsicHeight()), myWidth, bounds.top);
                rightStrip.setBounds(myWidth - rightStrip.getIntrinsicWidth(), bounds.bottom, myWidth,
                        Math.max(getHeight(), bounds.bottom + rightStrip.getIntrinsicHeight()));
                mStripMoved = false;
            }
            leftStrip.draw(canvas);
            rightStrip.draw(canvas);
        case Configuration.ORIENTATION_PORTRAIT:
        default:
            if (mStripMoved) {
                final Rect bounds = mBounds;
                bounds.left = selectedChild.getLeft();
                bounds.right = selectedChild.getRight();
                final int myHeight = getHeight();
                leftStrip.setBounds(Math.min(0, bounds.left - leftStrip.getIntrinsicWidth()),
                        myHeight - leftStrip.getIntrinsicHeight(), bounds.left, myHeight);
                rightStrip.setBounds(bounds.right, myHeight - rightStrip.getIntrinsicHeight(),
                        Math.max(getWidth(), bounds.right + rightStrip.getIntrinsicWidth()), myHeight);
                mStripMoved = false;
            }
            leftStrip.draw(canvas);
            rightStrip.draw(canvas);
        }
    }

    /**
     * Sets the current tab. This method is used to bring a tab to the front of
     * the Widget, and is used to post to the rest of the UI that a different
     * tab has been brought to the foreground.
     * 
     * Note, this is separate from the traditional "focus" that is employed from
     * the view logic.
     * 
     * For instance, if we have a list in a tabbed view, a user may be
     * navigating up and down the list, moving the UI focus (orange
     * highlighting) through the list items. The cursor movement does not effect
     * the "selected" tab though, because what is being scrolled through is all
     * on the same tab. The selected tab only changes when we navigate between
     * tabs (moving from the list view to the next tabbed view, in this
     * example).
     * 
     * To move both the focus AND the selected tab at once, please use
     * {@link #setCurrentTab}. Normally, the view logic takes care of adjusting
     * the focus, so unless you're circumventing the UI, you'll probably just
     * focus your interest here.
     * 
     * @param index
     *            The tab that you want to indicate as the selected tab (tab
     *            brought to the front of the widget)
     * 
     * @see #focusCurrentTab
     */
    public void setCurrentTab(final int index) {
        if (index < 0 || index >= getTabCount()) {
            return;
        }

        getChildTabViewAt(mSelectedTab).setSelected(false);
        mSelectedTab = index;
        getChildTabViewAt(mSelectedTab).setSelected(true);
        mStripMoved = true;
    }

    /**
     * Sets the current tab and focuses the UI on it. This method makes sure
     * that the focused tab matches the selected tab, normally at
     * {@link #setCurrentTab}. Normally this would not be an issue if we go
     * through the UI, since the UI is responsible for calling
     * TabWidget.onFocusChanged(), but in the case where we are selecting the
     * tab programmatically, we'll need to make sure focus keeps up.
     * 
     * @param index
     *            The tab that you want focused (highlighted in orange) and
     *            selected (tab brought to the front of the widget)
     * 
     * @see #setCurrentTab
     */
    public void focusCurrentTab(final int index) {
        final int oldTab = mSelectedTab;

        // set the tab
        setCurrentTab(index);

        // change the focus if applicable.
        if (oldTab != index) {
            getChildTabViewAt(index).requestFocus();
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        final int count = getTabCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildTabViewAt(i);
            child.setEnabled(enabled);
        }
    }

    @Override
    public void addView(final View child) {
        if (child.getLayoutParams() == null) {
            LinearLayout.LayoutParams lp;
            switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                lp = new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 1.0f);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                lp = new LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT, 1.0f);
                break;
            }
            lp.setMargins(0, 0, 0, 0);
            child.setLayoutParams(lp);
        }

        // Ensure you can navigate to the tab with the keyboard, and you can
        // touch it
        child.setFocusable(true);
        child.setClickable(true);

        // If we have dividers between the tabs and we already have at least one
        // tab, then add a divider before adding the next tab.
        if (mDividerDrawable != null && getTabCount() > 0) {
            final ImageView divider = new ImageView(getContext());
            final LinearLayout.LayoutParams lp = new LayoutParams(mDividerDrawable.getIntrinsicWidth(),
                    LayoutParams.FILL_PARENT);
            lp.setMargins(0, 0, 0, 0);
            divider.setLayoutParams(lp);
            divider.setBackgroundDrawable(mDividerDrawable);
            super.addView(divider);
        }
        super.addView(child);

        // TODO: detect this via geometry with a tabwidget listener rather
        // than potentially interfere with the view's listener
        child.setOnClickListener(new TheMissingTabClickListener(getTabCount() - 1));
        child.setOnFocusChangeListener(this);
    }

    /**
     * Provides a way for {@link TheMissingTabHost} to be notified that the user
     * clicked on a tab indicator.
     */
    void setTabSelectionListener(final OnTabSelectionChanged listener) {
        mSelectionChangedListener = listener;
    }

    @Override
    public void onFocusChange(final View v, final boolean hasFocus) {
        if (v == this && hasFocus && getTabCount() > 0) {
            getChildTabViewAt(mSelectedTab).requestFocus();
            return;
        }

        if (hasFocus) {
            int i = 0;
            final int numTabs = getTabCount();
            while (i < numTabs) {
                if (getChildTabViewAt(i) == v) {
                    setCurrentTab(i);
                    mSelectionChangedListener.onTabSelectionChanged(i, false);
                    break;
                }
                i++;
            }
        }
    }

    // registered with each tab indicator so we can notify tab host
    private class TheMissingTabClickListener implements OnClickListener {

        private final int mTabIndex;

        private TheMissingTabClickListener(final int tabIndex) {
            mTabIndex = tabIndex;
        }

        @Override
        public void onClick(final View v) {
            mSelectionChangedListener.onTabSelectionChanged(mTabIndex, true);
        }
    }

    /**
     * Let {@link TheMissingTabHost} know that the user clicked on a tab
     * indicator.
     */
    static interface OnTabSelectionChanged {
        /**
         * Informs the TheMissingTabHost which tab was selected. It also
         * indicates if the tab was clicked/pressed or just focused into.
         * 
         * @param tabIndex
         *            index of the tab that was selected
         * @param clicked
         *            whether the selection changed due to a touch/click or due
         *            to focus entering the tab through navigation. Pass true if
         *            it was due to a press/click and false otherwise.
         */
        void onTabSelectionChanged(int tabIndex, boolean clicked);
    }

}
