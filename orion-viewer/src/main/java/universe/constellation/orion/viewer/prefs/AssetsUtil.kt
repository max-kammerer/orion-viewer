package universe.constellation.orion.viewer.prefs

import android.content.res.AssetManager
import java.io.File

fun copyResIfNotExists(assets: AssetManager, path: String, targetRoot: File) {
    val res = assets.list(path)
    res?.forEach {
        val relPath = "$path/$it"
        if (it.endsWith(".xml")) {
            val newFile = File(targetRoot, relPath)
            try {
                if (newFile.exists()) return
                newFile.parentFile?.mkdirs()
                assets.open(relPath).use { input ->
                    newFile.outputStream().use { output ->
                        println("Copy " + newFile.absolutePath)
                        input.copyTo(output)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                newFile.delete()
            }
        } else {
            copyResIfNotExists(assets, relPath, targetRoot)
        }
    }
}