package universe.constellation.orion.viewer.filemanager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

internal class SimplePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    private val fragments: MutableList<Fragment> = arrayListOf(FoldersFragment())

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    override fun getItem(i: Int): Fragment {
        return fragments[i]
    }

    override fun getCount(): Int {
        return fragments.size
    }

}