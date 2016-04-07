package com.twitter.tweetducker

import android.util.Log

import com.crashlytics.android.Crashlytics
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import java.io.IOException
import java.util.LinkedList
import kotlin.comparisons.compareBy

object JSON {

    private val TAG = JSON::class.asTag()

    private fun parseUser(user: JsonNode): User = User(
            user.get("id").asLong(),
            user.get("name").asText(),
            user.get("screen_name").asText(),
            user.get("description").asText(),
            user.get("profile_image_url_https").asText()
    )

    private fun parseTimeline(timeline: JsonNode): Timeline = Timeline(
            timeline.get("name").asText(),
            timeline.get("description").asText(),
            timeline.get("collection_url").asText(),
            timeline.get("visibility").asText()
    )


    fun parseCollectionsList(json: String, userId: Long): CollectionsList? {
        try {
            val mapper = ObjectMapper()
            val root = mapper.readValue(json, JsonNode::class.java)

            root?.let {
                val userJson = root.path("objects").path("users").path("" + userId)
                val timelinesJson = root.path("objects").path("timelines")

                if (!userJson.isMissingNode && !timelinesJson.isMissingNode) {
                    val user = parseUser(userJson)

                    val timelines = LinkedList<Timeline>()
                    for (timeline in timelinesJson) {
                        timelines.add(parseTimeline(timeline))
                    }

                    val sortedTimelines = timelines.sortedWith(compareBy({ it.name }))

                    return CollectionsList(user, sortedTimelines)
                }
            }
        } catch (ioe: IOException) {
            Crashlytics.logException(ioe)
            Log.d(TAG, "IOException parsing JSON.", ioe)
        }

        return null
    }
}
