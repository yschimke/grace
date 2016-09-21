package dog.woofwoofinc.grace

import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.core.TwitterSession

import java.io.InputStream
import java.util.Properties

import kotlin.reflect.KClass

fun <T: Any> KClass<T>.asTag(): String = this.java.simpleName

fun InputStream.readProperties(): Properties {
    val properties = Properties()
    properties.load(this)

    return properties
}

fun Twitter.getSession(): TwitterSession? {
    return Twitter.getInstance().core.sessionManager.activeSession
}
