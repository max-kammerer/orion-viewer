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

import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.HashMap;

/**
 * User: mike
 * Date: 20.05.12
 * Time: 16:41
 */
public enum OptionActions {

    NONE("NONE"),

    FULL_SCREEN("FULL_SCREEN") {
        public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {
            activity.getWindow().setFlags(newValue ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getDevice().fullScreen(newValue, activity);
        }
    },

    SHOW_ACTION_BAR("SHOW_ACTION_BAR") {
        public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {
            Toolbar toolbar = activity.getToolbar();
            ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
            if (newValue) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                layoutParams.height = 0;
            }
            toolbar.setLayoutParams(layoutParams);
            Menu menu = toolbar.getMenu();
            menu.clear();
            activity.onCreateOptionsMenu(menu);
            if (!newValue) {
                for (int i = 0; i < menu.size(); i++) {
                    SupportMenuItem item = (SupportMenuItem) menu.getItem(i);
                    item.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }
    },

    SHOW_STATUS_BAR("SHOW_ACTION_BAR") {
        public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {
            activity.getStatusBarHelper().setShowStatusBar(newValue);
        }
    },

    SHOW_OFFSET_ON_STATUS_BAR("SHOW_OFFSET_ON_STATUS_BAR") {
        public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {
            activity.getStatusBarHelper().setShowOffset(newValue);
        }
    },

    SCREEN_OVERLAPPING_HORIZONTAL("SCREEN_OVERLAPPING_HORIZONTAL") {
        public void doAction(OrionViewerActivity activity, int hor, int ver) {
            activity.getController().changeOverlap(hor, ver);
        }
    },

    SCREEN_OVERLAPPING_VERTICAL("SCREEN_OVERLAPPING_VERTICAL") {
        public void doAction(OrionViewerActivity activity, int hor, int ver) {
            activity.getController().changeOverlap(hor, ver);
        }
    },

    SET_CONTRAST("contrast") {
        public void doAction(OrionViewerActivity activity, int oldValue, int newValue) {
            Controller controller = activity.getController();
            if (controller != null) {
                controller.changeContrast(newValue);
            }
        }
    },

    SET_THRESHOLD("threshold") {
        public void doAction(OrionViewerActivity activity, int oldValue, int newValue) {
            Controller controller = activity.getController();
            if (controller != null) {
                controller.changeThreshhold(newValue);
            }
        }
    },

    DEBUG("DEBUG") {
        public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {
            if (newValue) {
                AndroidLogger.startLogger(activity.getOrionContext().getCurrentBookParameters().openingFileName + ".trace");
            } else {
                AndroidLogger.stopLogger();
            }
        }
    };


    private static final HashMap<String, OptionActions> actions = new HashMap<String, OptionActions>();

    static {
        OptionActions[] values = values();
        for (OptionActions value : values) {
            actions.put(value.getKey(), value);
        }
    }

    public static OptionActions getAction(String key) {
        OptionActions result = actions.get(key);
        return result != null ? result : NONE;
    }

    private String key;

    OptionActions(String key) {
        this.key = key;
    }

    public void doAction(OrionViewerActivity activity, int oldValue, int newValue) {

    }

    public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {

    }

    public void doAction(OrionViewerActivity activity, String oldValue, String newValue) {

    }

    public String getKey() {
        return key;
    }
}
