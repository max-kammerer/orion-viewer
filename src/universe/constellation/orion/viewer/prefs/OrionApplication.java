package universe.constellation.orion.viewer.prefs;

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
import android.app.Application;
import universe.constellation.orion.viewer.db.BookmarkAccessor;

/**
 * User: mike
 * Date: 23.01.12
 * Time: 20:03
 */
public class OrionApplication extends Application {

    private GlobalOptions options;

    private TemporaryOptions tempOptions;

    public static OrionApplication instance;

    private BookmarkAccessor bookmarkAccessor;


    public void onCreate() {
        instance = this;
        super.onCreate();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public GlobalOptions getOptions() {
        if (options == null) {
            options = new GlobalOptions(this);
        }
        return options;
    }

    public void onNewBook() {
        tempOptions = new TemporaryOptions();
    }

    public TemporaryOptions getTempOptions() {
        return tempOptions;
    }


    public void applyTheme(Activity activity) {
        String theme = getOptions().getApplicationTheme();
        int themeId = android.R.style.Theme_NoTitleBar;
        if ("DARK".equals(theme)) {
            themeId = android.R.style.Theme_Black_NoTitleBar;
        } else if ("LIGHT".equals(theme)) {
            themeId = android.R.style.Theme_Light_NoTitleBar;
        }
        activity.setTheme(themeId);
    }

    public BookmarkAccessor getBookmarkAccessor() {
        if (bookmarkAccessor == null) {
            bookmarkAccessor = new BookmarkAccessor(this);
        }
        return bookmarkAccessor;
    }
}
