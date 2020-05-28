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

package universe.constellation.orion.viewer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class OrionHelpActivity : OrionBaseActivity(false) {

    class InfoFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.general_help, container, false)
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            val btn = activity!!.findViewById<View>(R.id.help_close) as ImageButton
            btn.setOnClickListener { activity!!.finish() }
        }
    }


    class AboutFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.about, container, false)
            val viewById = view.findViewById<View>(R.id.about_version_name) as TextView
            viewById.text = BuildConfig.VERSION_NAME
            return view
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            val btn = activity!!.findViewById<View>(R.id.info_close) as ImageButton
            btn.setOnClickListener { activity!!.finish() }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.file_manager)
        initHelpScreen()
        chooseTab(intent)
    }

    private fun initHelpScreen() {
        val pagerAdapter = HelpSimplePagerAdapter(supportFragmentManager, 2)
        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)

        val help = tabLayout.getTabAt(0)
        help?.setIcon(R.drawable.help)
        val about = tabLayout.getTabAt(1)
        about?.setIcon(R.drawable.info)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        chooseTab(intent)
    }

    private fun chooseTab(intent: Intent?) {
        val index = if (intent?.getBooleanExtra(OPEN_ABOUT_TAB, false) == true) 1 else 0
        findViewById<ViewPager>(R.id.viewpager).setCurrentItem(index, false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_manager_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exit_menu_item -> {
                finish()
                return true
            }
        }
        return false
    }

    companion object {
        const val OPEN_ABOUT_TAB = "OPEN_ABOUT";
    }

}


internal class HelpSimplePagerAdapter(fm: androidx.fragment.app.FragmentManager, private val pageCount: Int) : FragmentStatePagerAdapter(fm) {

    override fun getItem(i: Int): Fragment {
        return if (i == 0)
            OrionHelpActivity.InfoFragment()
        else
            OrionHelpActivity.AboutFragment()
    }

    override fun getCount(): Int {
        return pageCount
    }

}
