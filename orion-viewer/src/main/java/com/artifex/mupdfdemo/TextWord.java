package com.artifex.mupdfdemo;

import android.graphics.RectF;

public class TextWord extends RectF {
	private StringBuffer w;

	public TextWord() {
		super();
		w = new StringBuffer();
	}

	public void Add(TextChar tc) {
		super.union(tc);
		w = w.append(tc.c);
	}

    public StringBuffer getText() {
        return w;
    }

    public int textLength() {
        return w.length();
    }
}
