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
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView

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
        super.onOrionCreate(savedInstanceState, R.layout.file_manager)
        initHelpScreen()
    }

    private fun initHelpScreen() {
        val pagerAdapter = HelpSimplePagerAdapter(supportFragmentManager, 2)
        val viewPager = findViewById<View>(R.id.viewpager) as ViewPager
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)

        val help = tabLayout.getTabAt(0)
        help?.setIcon(R.drawable.help)
        val about = tabLayout.getTabAt(1)
        about?.setIcon(R.drawable.info)
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

}


internal class HelpSimplePagerAdapter(fm: FragmentManager, private val pageCount: Int) : FragmentStatePagerAdapter(fm) {

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
