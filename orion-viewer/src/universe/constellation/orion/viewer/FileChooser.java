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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * User: mike
 * Date: 19.10.11
 * Time: 13:02
 */
public class FileChooser extends ArrayAdapter {

    private List currentList;

    private File parentFile = new File("..");

    private File currentFolder;

    private static FilenameFilter filter;

    public static FilenameFilter DEFAULT_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String filename) {
        if (new File(dir, filename).isDirectory()) {
            return true;
        }
        String name = filename.toLowerCase();
        return name.endsWith(".pdf") || name.endsWith(".djvu") || name.endsWith(".djv") || name.endsWith(".xps") || name.endsWith(".oxps") || name.endsWith(".cbz")
                /*|| name.endsWith(".tiff") || name.endsWith(".tif") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".jpg")*/;
        }
    };

    public FileChooser(Context context, String folder) {
        this(context, folder, DEFAULT_FILTER);
    }


    public FileChooser(Context context, String folder, FilenameFilter filter) {
        super(context, R.layout.file_entry, R.id.fileName);
        currentList = new ArrayList();
        this.filter = filter;
        changeFolder(new File(folder));
    }

    public FileChooser(Context context, List<GlobalOptions.RecentEntry> enties) {
        super(context, R.layout.file_entry, R.id.fileName);
        currentList = new ArrayList();
        currentList.addAll(enties);
    }

    public File changeFolder(File file) {
        File newFolder = changeFolderInner(file);
        this.notifyDataSetChanged();
        return newFolder;
    }

    public void goToParent() {
        if (!currentList.isEmpty()) {
            if (currentList.get(0) == parentFile) {
                changeFolder(parentFile);
            }
        }
        this.notifyDataSetChanged();
    }

    private File changeFolderInner(File file) {
        currentList.clear();

        if (file == parentFile) {
            file = currentFolder.getParentFile();
        }

        currentFolder = file;
        if (file.getParent() != null) {
            currentList.add(parentFile);
        }
        File [] files = file.listFiles(filter);
        if (files != null) {
            currentList.addAll(Arrays.asList(files));
        }
        Collections.sort(currentList, new Comparator<File>() {
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory() || !f1.isDirectory() && f2.isDirectory()) {
                    return f1.isDirectory() ? -1 : 1;
                }
                return f1.getName().compareTo(f2.getName());
            }
        });
        return currentFolder;
    }

    public int getCount() {
        return currentList.size();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);
        Object item = getItem(position);
        boolean isDirectory = false;
        String name = item.toString();
        if (item != null) {
            if (item instanceof File) {
                File data = (File) getItem(position);
                isDirectory = data.isDirectory();
                name = data.getName();
            } else if (item instanceof GlobalOptions.RecentEntry) {
                name = ((GlobalOptions.RecentEntry) item).getLastPathElement();
            }


            ImageView fileIcon = (ImageView) convertView.findViewById(R.id.fileImage);
            int icon = R.drawable.djvu;
            String extName = name.toLowerCase();
            if (isDirectory) {
                icon = R.drawable.folder;
            } else if (extName.endsWith("pdf")) {
                icon = R.drawable.pdf;
            }else if (extName.endsWith("djvu")) {
                icon = R.drawable.djvu;
            } else if (extName.endsWith("cbz")) {
                icon = R.drawable.cbz;
            } else if (extName.endsWith("xps") || extName.endsWith("oxps")) {
                icon = R.drawable.xps;
            } else if (extName.endsWith("xml")) {
                icon = R.drawable.xml;
            }

            fileIcon.setImageResource(icon);
            TextView fileName = (TextView) convertView.findViewById(R.id.fileName);
            fileName.setText(name);
        }

        return convertView;
    }

    public Object getItem(int position) {
        return currentList.get(position);
    }

    public File getCurrentFolder() {
        return currentFolder;
    }
}
