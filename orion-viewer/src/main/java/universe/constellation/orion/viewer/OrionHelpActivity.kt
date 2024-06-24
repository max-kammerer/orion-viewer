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
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import java.util.Date

class OrionHelpActivity : OrionBaseActivity() {

    class InfoFragment : Fragment(R.layout.general_help)

    class AboutFragment : Fragment(R.layout.app_about_fragment)

    class ContributionFragment : Fragment(R.layout.app_contribution_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val conent = view.findViewById<ViewGroup>(R.id.content)
            conent.forEach { view ->
                if (view is TextView) {
                    view.movementMethod = LinkMovementMethod.getInstance()
                }
            }

            if (Date().before(Date(2024 - 1900, 8, 10))) {
                val survey = view.findViewById<TextView>(R.id.survey)
                val key = resources.getString(R.string.survey_key)
                val fullPath = "https://docs.google.com/forms/d/e/$key/viewform?usp=sf_link"

                val spannable = SpannableStringBuilder(survey.text)
                val onClick = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val uri = Uri.parse(fullPath)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                        startActivity(intent)
                    }
                }
                spannable.setSpan(onClick, 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                survey.text = spannable
                survey.movementMethod = LinkMovementMethod.getInstance();
                survey.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.app_help_activity, displayHomeAsUpEnabled = true)
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
        help?.setContentDescription(R.string.menu_help_text)

        val about = tabLayout.getTabAt(1)
        about?.setIcon(R.drawable.new_info)
        about?.setContentDescription(R.string.menu_about_text)

        tabLayout.getTabAt(2)?.apply {
            setIcon(R.drawable.contribution)
            setContentDescription(R.string.menu_about_text)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        chooseTab(intent)
    }

    private fun chooseTab(intent: Intent?) {
        val index = if (intent?.getBooleanExtra(OPEN_ABOUT_TAB, false) == true) 1 else 0
        findViewById<ViewPager>(R.id.viewpager).setCurrentItem(index, false)
    }

    companion object {
        const val OPEN_ABOUT_TAB = "OPEN_ABOUT"
    }

}

internal class HelpSimplePagerAdapter(fm: androidx.fragment.app.FragmentManager) : FragmentStatePagerAdapter(fm) {

    private val fragments: MutableList<Fragment> = arrayListOf(OrionHelpActivity.InfoFragment(), OrionHelpActivity.AboutFragment(), OrionHelpActivity.ContributionFragment())

    override fun getItem(i: Int): Fragment {
        return fragments[i]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}
