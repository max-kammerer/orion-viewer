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
import android.view.MenuItem
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout

class OrionHelpActivity : OrionBaseActivity(false) {

    class InfoFragment : Fragment(R.layout.general_help)

    class AboutFragment : Fragment(R.layout.app_about_fragment)

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.app_help_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.apply {
            val tintColor = MaterialColors.getColor(toolbar, R.attr.navIconTint)
            DrawableCompat.setTint(this, tintColor)
        }
        initHelpScreen()
        chooseTab(intent)
    }

    private fun initHelpScreen() {
        val pagerAdapter = HelpSimplePagerAdapter(supportFragmentManager)
        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)

        val help = tabLayout.getTabAt(0)
        help?.setIcon(R.drawable.new_help)
        val about = tabLayout.getTabAt(1)
        about?.setIcon(R.drawable.new_info)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        chooseTab(intent)
    }

    private fun chooseTab(intent: Intent?) {
        val index = if (intent?.getBooleanExtra(OPEN_ABOUT_TAB, false) == true) 1 else 0
        findViewById<ViewPager>(R.id.viewpager).setCurrentItem(index, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return false
    }

    companion object {
        const val OPEN_ABOUT_TAB = "OPEN_ABOUT"
    }

}

internal class HelpSimplePagerAdapter(fm: androidx.fragment.app.FragmentManager) : FragmentStatePagerAdapter(fm) {

    private val fragments: MutableList<Fragment> = arrayListOf(OrionHelpActivity.InfoFragment(), OrionHelpActivity.AboutFragment())

    override fun getItem(i: Int): Fragment {
        return fragments[i]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}
