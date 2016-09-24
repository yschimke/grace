package dog.woofwoofinc.grace.repository

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import dog.woofwoofinc.grace.Analytics
import dog.woofwoofinc.grace.CollectionsList
import dog.woofwoofinc.grace.Timeline
import dog.woofwoofinc.grace.User

import java.io.IOException

import kotlin.comparisons.compareBy

object Json {

    private val analytics: Analytics

    init {
        this.analytics = Analytics(Answers.getInstance())
    }

    private fun parseUser(user: JsonNode): User = User(
        user.get("id").asLong(),
        user.get("name").asText(),
        user.get("screen_name").asText(),
        user.get("description").asText(),
        user.get("profile_image_url_https").asText()
    )

    private fun parseTimeline(timeline: JsonNode): Timeline = Timeline(
        timeline.get("name").asText(),
        timeline.get("description")?.asText(), // Not all Collections have descriptions.
        timeline.get("collection_url").asText(),
        timeline.get("visibility").asText()
    )

    fun parseCollectionsList(json: String, userId: Long): CollectionsList? {
        try {
            val mapper = ObjectMapper()
            val root = mapper.readValue(json, JsonNode::class.java)

            root?.let {
                val userJson = root.path("objects").path("users").path("${userId}")
                val timelinesJson = root.path("objects").path("timelines")

                if (!userJson.isMissingNode && !timelinesJson.isMissingNode) {
                    val user = parseUser(userJson)

                    val timelines = mutableListOf<Timeline>()
                    for (timeline in timelinesJson) {
                        timelines.add(parseTimeline(timeline))
                    }

                    val sortedTimelines = timelines.sortedWith(compareBy { it.name.toLowerCase() })

                    return CollectionsList(user, sortedTimelines)
                }
            }
        } catch (ioe: IOException) {
            Crashlytics.logException(ioe)
        }

        analytics.failedParseCollectionsList()

        return null
    }
}
