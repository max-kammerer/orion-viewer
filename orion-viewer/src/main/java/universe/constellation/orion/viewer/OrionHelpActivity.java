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

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * User: mike
 * Date: 26.12.11
 * Time: 15:08
 */
public class OrionHelpActivity extends OrionBaseActivity {

    public static class InfoFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.general_help, container, false);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ImageButton btn = (ImageButton) getActivity().findViewById(R.id.help_close);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
        }
    }


    public static class AboutFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.about, container, false);
            TextView viewById = (TextView)view.findViewById(R.id.about_version_name);
            viewById.setText(BuildConfig.VERSION_NAME);
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ImageButton btn = (ImageButton) getActivity().findViewById(R.id.info_close);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onOrionCreate(savedInstanceState, R.layout.file_manager);
        initHelpScreen();
    }

    protected void initHelpScreen() {
        HelpSimplePagerAdapter pagerAdapter = new HelpSimplePagerAdapter(getSupportFragmentManager(), 2);
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(pagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        TabLayout.Tab help = tabLayout.getTabAt(0);
        if (help != null) {
            help.setIcon(R.drawable.help);
        }
        TabLayout.Tab about = tabLayout.getTabAt(1);
        if (about != null) {
            about.setIcon(R.drawable.info);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manager_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exit_menu_item:
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean supportDevice() {
        return false;
    }
}


class HelpSimplePagerAdapter extends FragmentStatePagerAdapter {
    private final int pageCount;

    public HelpSimplePagerAdapter(FragmentManager fm, int pageCount) {
        super(fm);
        this.pageCount = pageCount;
    }

    @Override
    public Fragment getItem(int i) {
        return i == 0 ?
                new OrionHelpActivity.InfoFragment() : new OrionHelpActivity.AboutFragment();
    }

    @Override
    public int getCount() {
        return pageCount;
    }

}
