package com.google.code.orion_viewer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import com.google.code.orion_viewer.Action;
import com.google.code.orion_viewer.OrionViewerActivity;
import com.google.code.orion_viewer.R;

/**
 * User: mike
 * Date: 10.01.12
 * Time: 13:26
 */
public class ImageButton extends android.widget.ImageButton {

    private int actionCode;

    private Action action;

    public ImageButton(Context context) {
        super(context);
        initListener(null);
    }

    public ImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initListener(attrs);
    }

    public ImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initListener(attrs);
    }

    public void initListener(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.com_google_code_orion_viewer_widget_ImageButton);
            actionCode = a.getInt(R.styleable.com_google_code_orion_viewer_widget_ImageButton_actionId, Action.NONE.getCode());
            action = Action.getAction(actionCode);
            a.recycle();
            setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    action.doAction(((OrionViewerActivity) getContext()).getController(), (OrionViewerActivity) getContext());
                }
            });
        }
    }

}
