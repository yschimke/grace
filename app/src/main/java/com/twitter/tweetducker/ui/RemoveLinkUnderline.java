package com.twitter.tweetducker.ui;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.widget.TextView;

/**
 * From http://stackoverflow.com/a/4463535. Remove the URL underline in a TextView.
 */
public class RemoveLinkUnderline {

    public static void remove(TextView textView) {
        Spannable spannable = (Spannable) textView.getText();

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);

            span = new URLSpanNoUnderline(span.getURL());
            spannable.setSpan(span, start, end, 0);
        }

        textView.setText(spannable);
    }

    private static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override public void updateDrawState(TextPaint drawState) {
            super.updateDrawState(drawState);
            drawState.setUnderlineText(false);
        }
    }
}
