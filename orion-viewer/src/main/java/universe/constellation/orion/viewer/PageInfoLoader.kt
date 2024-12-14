package universe.constellation.orion.viewer

import android.app.Activity
import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Modifier

private const val CURRENT_VERSION = 5

fun loadBookParameters(
    activity: Activity,
    filePath: String,
    defaultInitializer: Function1<LastPageInfo, Unit>
): LastPageInfo {
    val idx = filePath.lastIndexOf('/')
    val file = File(filePath)
    val fileData = filePath.substring(idx + 1) + "." + file.length() + ".xml"
    var lastPageInfo = LastPageInfo()
    var successfull = false
    try {
        successfull = lastPageInfo.load(activity, fileData)
    } catch (e: Exception) {
        log("Error on restore book options", e)
    }
    if (!successfull) {
        lastPageInfo = createDefaultLastPageInfo(defaultInitializer)
    }
    lastPageInfo.fileData = fileData
    lastPageInfo.openingFileName = filePath
    lastPageInfo.simpleFileName = filePath.substring(idx + 1)
    lastPageInfo.fileSize = file.length()
    return lastPageInfo
}

fun createDefaultLastPageInfo(defaultInitializer: Function1<LastPageInfo, Unit>): LastPageInfo {
    val lastPageInfo = LastPageInfo()
    defaultInitializer.invoke(lastPageInfo)
    return lastPageInfo
}

@Throws(IOException::class)
fun writeValue(serializer: XmlSerializer, name: String?, value: Int) {
    writeValue(serializer, name, value.toString())
}

@Throws(IOException::class)
fun writeValue(serializer: XmlSerializer, name: String?, value: Boolean) {
    writeValue(serializer, name, java.lang.Boolean.toString(value))
}

@Throws(IOException::class)
fun writeValue(serializer: XmlSerializer, name: String?, value: String?) {
    serializer.startTag("", name)
    serializer.attribute("", "value", value)
    serializer.endTag("", name)
}


fun LastPageInfo.save(activity: OrionBaseActivity) {
    var writer: OutputStreamWriter? = null
    log("Saving book parameters in $fileData")
    try {
        val serializer = Xml.newSerializer()
        writer = OutputStreamWriter(activity.openFileOutput(fileData, Context.MODE_PRIVATE))
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        val nameSpace = ""
        serializer.startTag(nameSpace, "bookParameters")
        serializer.attribute(nameSpace, "version", "" + CURRENT_VERSION)
        val fields = LastPageInfo::class.java.fields
        for (field in fields) {
            try {
                val modifiers = field.modifiers
                if (modifiers and (Modifier.TRANSIENT or Modifier.STATIC) == 0) {
                    writeValue(serializer, field.name, field[this].toString())
                }
            } catch (e: IllegalAccessException) {
                log(e)
            }
        }
        serializer.endTag(nameSpace, "bookParameters")
        serializer.endDocument()
    } catch (e: IOException) {
        activity.analytics.error(e)
        showAndLogError(activity, "Couldn't save book preferences", e)
    } finally {
        if (writer != null) {
            try {
                writer.close()
            } catch (e: IOException) {
                log(e)
            }
        }
    }
}

private fun LastPageInfo.load(activity: Activity, filePath: String): Boolean {
    var reader: InputStreamReader? = null
    try {
        reader = InputStreamReader(activity.openFileInput(filePath))
        val factory = XmlPullParserFactory.newInstance()
        //factory.setNamespaceAware(true);
        val xpp = factory.newPullParser()
        xpp.setInput(reader)
        var fileVersion = -1
        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = xpp.name
                if ("bookParameters" == name) {
                    fileVersion = Integer.valueOf(xpp.getAttributeValue("", "version"))
                } else {
                    try {
                        val rawValue = xpp.getAttributeValue("", "value")
                        val f = javaClass.getField(name)
                        var value: Any
                        val type = f.type
                        if (type == Int::class.javaPrimitiveType) {
                            value = Integer.valueOf(rawValue)
                            value = upgrade(fileVersion, name, value)
                        } else if (type == Boolean::class.javaPrimitiveType) {
                            value = java.lang.Boolean.valueOf(rawValue)
                        } else if (type == String::class.java) {
                            value = rawValue
                        } else {
                            log("Error on deserializing field $name = $rawValue")
                            continue
                        }
                        javaClass.getField(name)[this] = value
                    } catch (e: IllegalAccessException) {
                        log(e)
                    } catch (e: NoSuchFieldException) {
                        //skip
                        log(e)
                    } catch (e: NumberFormatException) {
                        log(e)
                    }
                }
            }
            eventType = xpp.next()
        }
        return true
    } catch (e: FileNotFoundException) {
        //do nothing
    } catch (e: XmlPullParserException) {
        showAndLogError(activity, "Couldn't parse book parameters", e)
    } catch (e: IOException) {
        showAndLogError(activity, "Couldn't parse book parameters", e)
    } finally {
        if (reader != null) {
            try {
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace() //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
    return false
}

fun LastPageInfo.upgrade(fromVersion: Int, name: String, value: Int): Int {
    var value = value
    var localVersion = fromVersion
    if (localVersion < 2) {
        if ("zoom" == name) {
            println("Property $name upgraded")
            localVersion = 2
            value = 0
        }
    }
    if (localVersion < 3) {
        if ("rotation" == name) {
            println("Property $name upgraded")
            localVersion = 3
            value = 0
        }
    }
    if (localVersion < 4) {
        if ("navigation" == name) {
            println("Property $name upgraded")
            localVersion = 4
            if (value == 1) {
                walkOrder = "ACBD"
            }
        }
    }
    if (localVersion < 5) {
        if ("contrast" == name) {
            println("Property $name upgraded")
            localVersion = 5
            value = 100
        }
    }
    return value
}

