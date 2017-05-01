/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.android

import android.content.Context
import android.preference.*

abstract class DSLPreferenceActivity : PreferenceActivity() {

    //TODO use tree
    @PublishedApi
    internal var preferencesToAdd: MutableList<Pair<PreferenceGroup, Preference>>? = null

    fun rootScreen(init: PreferenceScreen.() -> Unit): PreferenceScreen {
        assert(preferencesToAdd == null) {"rootScreen couldn't be nested"}
        return preferenceManager.createPreferenceScreen(context).also {
            preferencesToAdd = arrayListOf()
            it.init()

            preferencesToAdd!!.forEach { (parent, child)  -> parent.addPreference(child) }
            preferencesToAdd = null
            preferenceScreen = it
        }
    }

    inline fun PreferenceGroup.category(init: PreferenceCategory.() -> Unit) = preference(init)

    inline fun PreferenceGroup.screen(init: PreferenceScreen.() -> Unit) = preference(init)

    inline fun PreferenceGroup.editText(init: EditTextPreference.() -> Unit) = preference(init)

    inline fun PreferenceGroup.list(init: ListPreference.() -> Unit) = preference(init)

    inline fun <reified T : Preference> PreferenceGroup.preference(init: T.() -> Unit): T {
        return T::class.java.getDeclaredConstructor(Context::class.java).
                newInstance(this.context).also { newChild ->
            preferencesToAdd!!.add(this to newChild)
            newChild.init()
        }
    }

    val Int.stringRes
        get() = context.resources.getString(this)

    val Int.intArray
        get() = context.resources.getIntArray(this)

    val Int.stringArray
        get() = context.resources.getStringArray(this)

    val context = applicationContext!!
}

