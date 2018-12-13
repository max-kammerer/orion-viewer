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

package universe.constellation.orion.viewer

import android.app.Activity
import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.*
import java.lang.reflect.Modifier

class LastPageInfo private constructor() : ShortFileInfo {

    var screenWidth: Int = 0
    var screenHeight: Int = 0

    override var currentPage: Int = 0
    var rotation: Int = 0

    //application default
    var screenOrientation = "DEFAULT"

    var newOffsetX: Int = 0
    var newOffsetY: Int = 0

    var zoom: Int = 0

    var leftMargin = 0
    var rightMargin = 0
    var topMargin = 0
    var bottomMargin = 0

    var enableEvenCropping = false
    var cropMode = 0
    var leftEvenMargin = 0
    var rightEventMargin = 0

    var pageLayout = 0

    var contrast = 100
    var threshold = 255

    @Transient
    lateinit var fileData: String

    @Transient
    override var fileSize: Long = 0

    @Transient
    override lateinit var simpleFileName: String

    @Transient
    override lateinit var  fileName: String

    @Transient
    var totalPages: Int = 0

    var walkOrder = "ABCD"

    var colorMode = "CM_NORMAL"

    fun save(activity: Activity) {
        var writer: OutputStreamWriter? = null
        try {
            val serializer = Xml.newSerializer()
            writer = OutputStreamWriter(activity.openFileOutput(fileData, Context.MODE_PRIVATE))
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            val nameSpace = ""
            serializer.startTag(nameSpace, "bookParameters")
            serializer.attribute(nameSpace, "version", "$CURRENT_VERSION")

            val fields = this.javaClass.declaredFields
            for (field in fields) {
                try {
                    val modifiers = field.modifiers
                    if (modifiers and (Modifier.TRANSIENT or Modifier.STATIC) == 0) {
                        //System.out.println(field.getName());
                        writeValue(serializer, field.name, field.get(this).toString())
                    }
                } catch (e: IllegalAccessException) {
                    log(e)
                }

            }

            serializer.endTag(nameSpace, "bookParameters")
            serializer.endDocument()
        } catch (e: IOException) {
            log(e)
            showError(activity, "Couldn't save book preferences", e)
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

    private fun load(activity: Activity, filePath: String): Boolean {
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
                            var value: Any?
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
                            javaClass.getField(name).set(this, value)
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
            showError(activity, "Couldn't parse book parameters", e)
        } catch (e: IOException) {
            showError(activity, "Couldn't parse book parameters", e)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        }
        return false
    }

    fun upgrade(fromVersion: Int, name: String, value: Int?): Int? {
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

    companion object {

        const val CURRENT_VERSION = 5

        fun loadBookParameters(activity: Activity, filePath: String, defaultInitializer: Function1<LastPageInfo, Unit>): LastPageInfo {
            val idx = filePath.lastIndexOf('/')
            val file = File(filePath)
            val fileData = filePath.substring(idx + 1) + "." + file.length() + ".xml"
            var lastPageInfo = LastPageInfo()

            var successfull = false
            try {
                successfull = lastPageInfo.load(activity, fileData)
            } catch (e: Exception) {
                log("Error on restore book options", e);
            }

            if (!successfull) {
                lastPageInfo = createDefaultLastPageInfo(defaultInitializer)
            }

            lastPageInfo.fileData = fileData
            lastPageInfo.fileName = filePath
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
        fun writeValue(serializer: XmlSerializer, name: String, value: String) {
            serializer.startTag("", name)
            serializer.attribute("", "value", value)
            serializer.endTag("", name)
        }
    }
}
