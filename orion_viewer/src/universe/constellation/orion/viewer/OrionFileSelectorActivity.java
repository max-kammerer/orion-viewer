/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
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

package universe.constellation.orion.viewer;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: mike
 * Date: 25.09.12
 * Time: 21:14
 */
public class OrionFileSelectorActivity extends OrionFileManagerActivity {

    public static final String RESULT_FILE_NAME = "fileName";

    @Override
    protected void onNewIntent(Intent intent) {
        //do nothing
    }

    @Override
    public boolean showRecentsAndSavePath() {
        return false;
    }

    @Override
    protected void openFile(File file) {
        Intent result = new Intent();
        result.putExtra(RESULT_FILE_NAME, file.getAbsolutePath());
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public FilenameFilter getFileNameFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {

                if (new File(dir, filename).isDirectory()) {
                    return true;
                }

                return filename.toLowerCase().endsWith(".xml");
            }
        };
    }
}
