package com.twitter.tweetducker

import android.text.Spannable
import android.text.TextPaint
import android.text.style.URLSpan
import android.widget.TextView

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

/**
 * From http://stackoverflow.com/a/4463535. Remove the URL underline in a TextView.
 */
fun TextView.removeLinkUnderline() {

    class URLSpanNoUnderline(url: String) : URLSpan(url) {
        override fun updateDrawState(drawState: TextPaint) {
            super.updateDrawState(drawState)
            drawState.isUnderlineText = false
        }
    }

    val spannable = this.text as Spannable

    val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    for (span in spans) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        spannable.removeSpan(span)

        val spanNoUnderline = URLSpanNoUnderline(span.url)
        spannable.setSpan(spanNoUnderline, start, end, 0)
    }

    this.text = spannable
}


