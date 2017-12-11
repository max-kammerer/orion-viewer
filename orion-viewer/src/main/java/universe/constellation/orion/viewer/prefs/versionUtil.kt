package universe.constellation.orion.viewer.prefs

import universe.constellation.orion.viewer.log

fun isVersionEquals(constVersion: String, checkingVersion: String) = constVersion == checkingVersion

fun isVersionLess(constVersion: String, checkingVersion: String): Boolean {
    if (constVersion.isEmpty() || checkingVersion.isEmpty()) return false

    try {
        val number = constVersion.substringBefore('.').toInt()
        val checkingNumber = checkingVersion.substringBefore('.').toInt()
        if (checkingNumber < number) return true

        if (checkingNumber == number) {
            return isVersionLess(constVersion.substringAfter('.', ""), checkingVersion.substringAfter('.', ""))
        }

    } catch (e: NumberFormatException) {
        log(e)
    }

    return false
}