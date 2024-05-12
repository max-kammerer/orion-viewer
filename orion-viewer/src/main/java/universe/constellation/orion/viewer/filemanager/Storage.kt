package universe.constellation.orion.viewer.filemanager

import android.content.Context
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.os.Environment.getExternalStorageDirectory
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Environment.getExternalStorageState
import android.os.Environment.getStorageState
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastAndroidN
import universe.constellation.orion.viewer.android.isAtLeastAndroidR
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.android.isAtLeastLollipop
import java.io.File

open class Folder(val description: String, val file: File)

class Storage(desc: String, path: File, val isPrimary: Boolean) : Folder(desc, path) {
    var folders = mutableListOf<Folder>()
}

fun Context.describeStorages(
    folders: List<Pair<String, String>> = listOf(
        DIRECTORY_DOCUMENTS to resources.getString(R.string.file_manager_documents),
        DIRECTORY_DOWNLOADS to resources.getString(R.string.file_manager_downloads)
    )
): List<Storage> {
    var storages = if (isAtLeastAndroidN()) {
        val systemService = getSystemService(StorageManager::class.java)
        systemService.listMountedStorages().map {
            var description = it.getDescription(this)
            if (description == "Internal shared storage") {
                description = resources.getString(R.string.file_manager_internal_storage)
            }
            Storage(description, it.getVolumeDirectory()!!, it.isPrimary)
        }
    } else {
        emptyList()
    }

    if (storages.isEmpty()) {
        storages = listOf(Storage(resources.getString(R.string.file_manager_primary_storage), getExternalStorageDirectory(), true))
    }

    val storageMap = storages.associateBy { it.file.absolutePath }

    for ((folder, desc) in folders) {
        val dir = getExternalStoragePublicDirectory(folder)
        //for (dir in externalFilesDirs) {
            for ((k, v) in storageMap) {
                if (dir.absolutePath.startsWith(k)) {
                    v.folders.add(Folder(desc, dir))
                    break
                }
            }
        //}
    }
    return storages
}

fun StorageManager.listMountedStorages(): List<StorageVolume> {
    val allStorages = if (isAtLeastAndroidN()) {
        this.getStorageVolumes()
    } else {
        try {
            val method = StorageManager::class.java.getDeclaredMethod("getVolumeList")
            val volumes = method(this) as? Array<StorageVolume> ?: emptyArray()
            volumes.toList()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    return allStorages.filter {
        it.isMounted() && it.getVolumeDirectory() != null
    }
}

fun StorageVolume.getVolumeDirectory(): File? {
    return if (isAtLeastAndroidR()) {
        directory?.canonicalFile
    } else {
        try {
            val field = StorageVolume::class.java.getDeclaredField("mPath")
            field.isAccessible = true
            when (val value = field[this]) {
                is File -> value
                is String -> File(value).takeIf { it.exists() }
                else -> null
            }

        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}

fun StorageVolume.getVolumeDescription(context: Context): String {
    return if (isAtLeastAndroidR()) {
        this.getDescription(context)
    } else {
        try {
            val field = StorageVolume::class.java.getDeclaredField("mDescription")
            field.isAccessible = true
            field[this] as String? ?: "Unknown"
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }
}


fun StorageVolume.isMounted(): Boolean {
    val state = if (isAtLeastAndroidN()) {
        state
    } else if (isAtLeastLollipop()) {
        getExternalStorageState(this.getVolumeDirectory() ?: return false)
    } else if (isAtLeastKitkat()) {
        getStorageState(this.getVolumeDirectory() ?: return false)
    } else {
        MEDIA_MOUNTED
    }

    return state == MEDIA_MOUNTED || state == MEDIA_MOUNTED_READ_ONLY
}

fun StorageVolume.isPrimaryVolume(): Boolean {
    return if (isAtLeastAndroidN()) {
        isPrimary
    } else {
        getVolumeDirectory() == getExternalStorageDirectory()
    }
}