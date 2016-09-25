package dog.woofwoofinc.grace

import android.view.View
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.core.TwitterSession

import java.io.InputStream
import java.util.ArrayList
import java.util.Properties

fun InputStream.readProperties(): Properties {
    val properties = Properties()
    properties.load(this)

    return properties
}

fun Twitter.getSession(): TwitterSession? {
    return this.core.sessionManager.activeSession
}

fun Twitter.logOut() {
    return this.core.logOut()
}

fun View.findFirstViewWithText(text: String): View? {
    val views = ArrayList<View>(1)
    findViewsWithText(views, text, View.FIND_VIEWS_WITH_TEXT)

    if (views.size > 0) {
        return views.get(0)
    }

    return null
}
