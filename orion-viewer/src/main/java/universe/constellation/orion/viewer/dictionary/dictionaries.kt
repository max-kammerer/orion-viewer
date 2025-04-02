package universe.constellation.orion.viewer.dictionary

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.log

enum class Dictionary(
    val action: String,
    val queryKey: String,
    val packageName: String? = null,
    val className: String? = null
) {
    FORA("com.ngc.fora.action.LOOKUP", "HEADWORD"),
    COLORDICT("colordict.intent.action.SEARCH", "EXTRA_QUERY"),
    AARD(
        Intent.ACTION_SEARCH,
        SearchManager.QUERY,
        "aarddict.android",
        "aarddict.android.LookupActivity"
    ),
    AARD2("aard2.lookup", SearchManager.QUERY),
    LINGVO(
        "com.abbyy.mobile.lingvo.intent.action.TRANSLATE",
        "com.abbyy.mobile.lingvo.intent.extra.TEXT",
        "com.abbyy.mobile.lingvo.market"
    ),
    ONYX(
        Intent.ACTION_VIEW,
        Intent.ACTION_SEARCH,
        "com.onyx.dict",
        "com.onyx.dict.main.ui.DictMainActivity"
    ),
    LEO(
        Intent.ACTION_SEARCH,
        SearchManager.QUERY,
        "org.leo.android.dict",
        "org.leo.android.dict.LeoDict"
    ),
    POPUP(Intent.ACTION_VIEW, SearchManager.QUERY, "com.barisatamer.popupdictionary.MainActivity"),
    DICTAN(Intent.ACTION_VIEW, "article.word", "info.softex.dictan", null),
    GOOGLE(
        Intent.ACTION_SEND,
        Intent.EXTRA_TEXT,
        "com.google.android.apps.translate",
        "com.google.android.apps.translate.TranslateActivity"
    ),
    YANDEX(
        Intent.ACTION_SEND,
        Intent.EXTRA_TEXT,
        "ru.yandex.translate",
        "ru.yandex.translate.ui.activities.MainActivity"
    ),
    WIKIPEDIA(
        Intent.ACTION_SEND,
        SearchManager.QUERY,
        "org.wikipedia",
        "org.wikipedia.search.SearchActivity"
    );
}


internal fun openDictionary(
    parameter: String?,
    activity: OrionViewerActivity,
    dictionary: String
) {
    val intent = createIntent(parameter, Dictionary.valueOf(dictionary))

    try {
        activity.startActivity(intent)
    } catch (ex: ActivityNotFoundException) {
        log(ex)
        val errorMessage =
            activity.getString(R.string.warn_msg_no_dictionary) + ": " + dictionary + ": " + ex.message
        activity.showWarning(errorMessage)
    }
}

private fun createIntent(
    parameter: String?,
    dictionary: Dictionary
): Intent {
    val intent = Intent().apply {
        action = dictionary.action
        dictionary.packageName?.let { setPackage(it) }
        dictionary.className?.let { setClassName(dictionary.packageName!!, it) }
        (safeParameter(parameter) as? String)?.let {
            putExtra(dictionary.queryKey, it as String?)
        }

        when (dictionary) {
            Dictionary.DICTAN -> {
                //setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addCategory("info.softex.dictan.EXTERNAL_DISPATCHER")
            }

            Dictionary.GOOGLE, Dictionary.YANDEX -> {
                setType("text/plain")
            }

            else -> {}
        }
    }
    return intent
}

private fun safeParameter(parameter: Any?): Any {
    return parameter ?: ""
}