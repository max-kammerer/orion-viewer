package universe.constellation.orion.viewer.filemanager

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import java.io.File

class RecentListFragment : Fragment(R.layout.history_view) {

    private lateinit var listView: ListView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = view.findViewById(R.id.list)
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as GlobalOptions.RecentEntry
            val file = File(entry.path)
            if (file.exists()) {
                (requireActivity() as OrionFileManagerActivityBase).openFile(file)
            } else {
                Toast.makeText(
                    parent.context,
                    getString(R.string.recent_book_not_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        view.findViewById<ImageView>(R.id.enableTrash).setOnClickListener {
            (listView.adapter as? RecentListAdapter)?.showTrashButton = !((listView.adapter as? RecentListAdapter)?.showTrashButton ?: false)
            (listView.adapter as? RecentListAdapter)?.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        updateRecentListAdapter()
    }

    private fun updateRecentListAdapter() {
        listView.adapter = RecentListAdapter(
            requireActivity(),
            (requireActivity() as OrionFileManagerActivityBase).globalOptions
        )
    }

}