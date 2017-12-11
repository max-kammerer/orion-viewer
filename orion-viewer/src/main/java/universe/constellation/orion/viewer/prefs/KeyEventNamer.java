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

package universe.constellation.orion.viewer.prefs;

import android.view.KeyEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 05.04.12
 * Time: 21:42
 */
public class KeyEventNamer {

    private static final Map<Integer, String> key2Name = new HashMap<Integer, String>();
    static {
        try {
            Field[] fields = KeyEvent.class.getFields();
            for (Field field : fields) {
                if (field.getName().startsWith("KEYCODE_")) {
                    int key = field.getInt(null);
                    String name = field.getName().substring("KEYCODE_".length());
                    key2Name.put(key, name);
                }
            }
        } catch (Exception e) {
            log(e);
        }
    }

    public static String getKeyName(int code) {
        String name = key2Name.get(code);
        return name == null ? "keycode " + code : name;
    }

}
