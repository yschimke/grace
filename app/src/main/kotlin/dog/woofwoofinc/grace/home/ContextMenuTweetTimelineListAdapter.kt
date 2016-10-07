package dog.woofwoofinc.grace.home

import android.content.Context
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import com.twitter.sdk.android.core.models.Tweet
import com.twitter.sdk.android.tweetui.*

class ContextMenuTweetTimelineListAdapter(
    context: Context,
    timeline: Timeline<Tweet>?,
    val viewStyleResId: Int,
    val createContextMenuListener: OnCreateContextMenuListener?
) : TweetTimelineListAdapter(context, timeline) {

    /**
     * Override the TweetTimelineListAdapter with a duplicate that inserts a
     * setOnCreateContextMenuListener call to setup the long click menu for
     * Tweet UI views.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val tweet = getItem(position)

        var rowView: View? = convertView
        if (rowView == null) {
            rowView = CompactTweetView(context, tweet, viewStyleResId)
        }

        (rowView as BaseTweetView).tweet = tweet

        if (createContextMenuListener != null) {
            rowView.setOnCreateContextMenuListener(createContextMenuListener)
        }

        return rowView
    }

    class Builder(private val context: Context) {
        private var timeline: Timeline<Tweet>? = null
        private var styleResId = R.style.tw__TweetLightStyle
        private var createContextMenuListener: OnCreateContextMenuListener? = null

        fun setTimeline(timeline: Timeline<Tweet>): Builder {
            this.timeline = timeline
            return this
        }

        fun setViewStyle(styleResId: Int): Builder {
            this.styleResId = styleResId
            return this
        }

        fun setOnCreateContextMenuListener(createContextMenuListener: OnCreateContextMenuListener): Builder {
            this.createContextMenuListener = createContextMenuListener
            return this
        }

        fun build(): TweetTimelineListAdapter {
            return ContextMenuTweetTimelineListAdapter(context, timeline, styleResId, createContextMenuListener)
        }
    }
}
