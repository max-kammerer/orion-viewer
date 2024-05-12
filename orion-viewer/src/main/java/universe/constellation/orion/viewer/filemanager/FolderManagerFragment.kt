package universe.constellation.orion.viewer.filemanager

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import universe.constellation.orion.viewer.Permissions
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.R
import java.io.File

class FolderManagerFragment : Fragment(R.layout.folder_view) {

    private fun getStartFolder(storages: List<Storage>): File {
        val lastOpenedDir =
            (activity as OrionFileManagerActivityBase).globalOptions.lastOpenedDirectory

        if (lastOpenedDir != null && File(lastOpenedDir).exists()) {
            return File(lastOpenedDir)
        }

        val startFolder = (storages.flatMap { it.folders + it }
            .map { it.file } + File("/system/media/sdcard/")).firstOrNull { it.exists() }

        if (startFolder != null) {
            return startFolder
        }

        return Environment.getRootDirectory()
    }

    private lateinit var listView: ListView

    private lateinit var pathView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = view.findViewById(R.id.folderList)
        pathView = view.findViewById(R.id.path)

        val myActivity = requireActivity() as OrionFileManagerActivityBase

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val file = parent.getItemAtPosition(position) as File
                if (file.isDirectory) {
                    changeFolder(file)
                } else {
                    if (myActivity.showRecentsAndSavePath) {
                        val absolutePath = file.parentFile?.absolutePath
                        myActivity.prefs!!.edit().putString(OrionFileManagerActivity.LAST_OPENED_DIRECTORY, absolutePath)
                            .apply()
                    }
                    myActivity.openFile(file)
                }
            }
        val storages = requireContext().describeStorages()
        val startFolder = getStartFolder(storages)
        pathView.text = startFolder.absolutePath

        listView.adapter =
            FileChooserAdapter(requireActivity(), startFolder, myActivity.fileNameFilter)

        val panel = view.findViewById<LinearLayout>(R.id.file_manager_goto_panel)
        storages.flatMap { it.folders + it }.map {
            setupGotoButtonIfExists(panel, it)
        }

        val grant = requireView().findViewById<Button>(R.id.file_manager_grant)
        grant.setOnClickListener {
            requireActivity().checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
        }
    }

    private fun setupGotoButtonIfExists(panel: LinearLayout, folder: Folder) {
        if (folder.file.exists()) {
            val goto = AppCompatButton(requireContext())
            goto.text = folder.description
            goto.setOnClickListener {
                (requireActivity() as? OrionFileManagerActivityBase)?.analytics?.action("folderNavButton")
                changeFolder(folder.file)
            }
            panel.addView(goto)
        }
    }

    internal fun changeFolder(
        file: File
    ) {
        val newFolder = (listView.adapter as FileChooserAdapter).changeFolder(file)
        pathView.text = newFolder.absolutePath
    }

    override fun onResume() {
        super.onResume()
        if (activity !is OrionFileManagerActivity) {
            view?.findViewById<TextView>(R.id.file_manager_tip)?.visibility = View.GONE
        }
        view?.findViewById<TextView>(R.id.file_manager_grant)?.visibility =
            if (Permissions.hasReadStoragePermission(requireActivity())) View.GONE else View.VISIBLE
    }
}