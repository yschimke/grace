package com.twitter.tweetducker.ui

import android.text.Spannable
import android.text.TextPaint
import android.text.style.URLSpan
import android.widget.TextView

/**
 * From http://stackoverflow.com/a/4463535. Remove the URL underline in a TextView.
 */
object RemoveLinkUnderline {

    fun remove(textView: TextView) {
        val spannable = textView.text as Spannable

        val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
        for (span in spans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            spannable.removeSpan(span)

            val spanNoUnderline = URLSpanNoUnderline(span.url)
            spannable.setSpan(spanNoUnderline, start, end, 0)
        }

        textView.text = spannable
    }

    private class URLSpanNoUnderline(url: String) : URLSpan(url) {

        override fun updateDrawState(drawState: TextPaint) {
            super.updateDrawState(drawState)
            drawState.isUnderlineText = false
        }
    }
}
