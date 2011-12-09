package com.google.code.orion_viewer;

/*Orion Viewer is a pdf viewer for Nook Classic based on mupdf

Copyright (C) 2011  Michael Bogdanov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

import android.app.Activity;
import android.view.KeyEvent;

/**
 * User: mike
 * Date: 18.10.11
 * Time: 11:13
 */
public interface Device {

    final int NEXT = 1;

    final int PREV = -1;

    final int ESC = 10;

    void updateTitle(String title);

    boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder operation);

    void onCreate(Activity activity);

    void onPause();

    void onResume();

    void onUserInteraction();

    void updatePageNumber(int current, int max);

    void flush();

    public int getLayoutId();

    public String getDefaultDirectory();

    public int getViewWidth();

    public int getViewHeight();
}
