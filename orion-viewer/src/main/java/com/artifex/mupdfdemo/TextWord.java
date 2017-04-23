package com.artifex.mupdfdemo;

import android.graphics.RectF;

public class TextWord extends RectF {
	private StringBuilder w;

	public TextWord() {
		super();
		w = new StringBuilder();
	}

	public void Add(TextChar tc) {
		super.union(tc);
		w = w.append(tc.c);
	}

    public StringBuilder getText() {
        return w;
    }

    public int textLength() {
        return w.length();
    }
}
