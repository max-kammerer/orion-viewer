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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

class OrionHelpActivity : OrionBaseActivity() {

    class InfoFragment : Fragment(R.layout.general_help)

    class AboutFragment : Fragment(R.layout.app_about_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            /* The links are <a> tags in string resources; without autoLink they need the movement method. */
            for (id in intArrayOf(R.id.project_ino_2, R.id.discussions, R.id.discussions_ru)) {
                view.findViewById<TextView>(id).movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    class ContributionFragment : Fragment(R.layout.app_contribution_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val conent = view.findViewById<ViewGroup>(R.id.content)
            conent.forEach { view ->
                if (view is TextView) {
                    view.movementMethod = LinkMovementMethod.getInstance()
                }
            }

            if (isSurveyActive()) {
                showSurveyCard(view)
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

        tabLayout.getTabAt(2)?.let { tab ->
            tab.setContentDescription(R.string.menu_about_text)
            globalOptions.OPENED_SURVEY.observe(this) { tab.setIcon(contributionTabIcon(this, it)) }
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

        private val surveyEndDate = GregorianCalendar(2026, Calendar.NOVEMBER, 30).time

        fun isSurveyActive(): Boolean = Date().before(surveyEndDate)

        /**
         * With a "!" while the survey is on and its link hasn't been opened yet ([openedSurvey] is
         * the key of the last one opened): the invitation is on this tab only.
         */
        fun contributionTabIcon(context: Context, openedSurvey: String): Int =
            if (isSurveyActive() && openedSurvey != context.getString(R.string.survey_key)) R.drawable.contribution_survey
            else R.drawable.contribution
    }

}

/**
 * The survey invitation: a card with a button while the survey hasn't been opened, then a
 * thank-you with a link to open it again. Follows the stored key, so it switches at the click.
 */
private fun OrionHelpActivity.ContributionFragment.showSurveyCard(view: View) {
    val activity = requireActivity() as OrionBaseActivity
    val key = getString(R.string.survey_key)
    val card = view.findViewById<View>(R.id.survey_card)
    card.background = surveyCardBackground(card)
    card.visibility = View.VISIBLE

    val open = View.OnClickListener {
        activity.globalOptions.saveStringProperty(activity.globalOptions.OPENED_SURVEY.key, key)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/$key/viewform?usp=sf_link")))
    }
    val title = view.findViewById<TextView>(R.id.survey_title)
    val description = view.findViewById<View>(R.id.survey_text)
    val purpose = view.findViewById<View>(R.id.survey_purpose)
    val take = view.findViewById<View>(R.id.survey_take).apply { setOnClickListener(open) }
    val reopen = view.findViewById<TextView>(R.id.survey_reopen).apply {
        val link = SpannableStringBuilder(text)
        link.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) = open.onClick(widget)
        }, 0, link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        this.text = link
        movementMethod = LinkMovementMethod.getInstance()
    }

    activity.globalOptions.OPENED_SURVEY.observe(viewLifecycleOwner) { opened ->
        val done = opened == key
        title.setText(if (done) R.string.survey_thanks else R.string.survey_title)
        description.visibility = if (done) View.GONE else View.VISIBLE
        purpose.visibility = if (done) View.GONE else View.VISIBLE
        take.visibility = if (done) View.GONE else View.VISIBLE
        reopen.visibility = if (done) View.VISIBLE else View.GONE
    }
}

/* Built in code: a shape in xml can't take a theme color on Android 4. */
private fun surveyCardBackground(card: View): GradientDrawable {
    val value = TypedValue()
    card.context.theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, value, true)
    val accent = value.data
    val density = card.resources.displayMetrics.density
    return GradientDrawable().apply {
        cornerRadius = 8 * density
        setStroke((1.5f * density).toInt().coerceAtLeast(1), accent)
        setColor((accent and 0x00ffffff) or 0x1f000000)
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
