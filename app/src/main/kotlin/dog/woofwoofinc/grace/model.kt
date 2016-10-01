package dog.woofwoofinc.grace

data class CollectionsList(val user: User, val timelines: List<Timeline>) {

    fun findTimelineByName(name: String): Timeline? {
        return timelines.find { timeline ->
            timeline.name.equals(name, ignoreCase = true)
        }
    }

    fun findTimelineByCollectionUrl(collectionUrl: String): Timeline? {
        return timelines.find { timeline ->
            timeline.collectionUrl.equals(collectionUrl, ignoreCase = true)
        }
    }
}


data class User(val id: Long, val name: String, val screenName: String, val description: String, val profileImageUrlHttps: String) {

    // Be better than this... :(
    val avatarUrl: String get() = profileImageUrlHttps.replace("_normal.", "_400x400.")
}


data class Timeline(
    val name: String,
    val description: String?,
    val collectionUrl: String,
    val visibility: String,
    val timelineOrder: String
) {

    val id: Long?
        get() {
            // You are also better than this... :(

            // collectionUrl looks like "https://twitter.com/<string>/timelines/<long>"
            val id = collectionUrl.substringAfterLast('/').trim { it <= ' ' }

            if (!id.isEmpty()) {
                return id.toLong()
            }

            return null
        }
}


enum class TimelineOrder(val param: String) {
    NEWEST_FIRST("tweet_reverse_chron"),
    OLDEST_FIRST("tweet_chron"),
    CURATION_ORDER("curation_reverse_chron")
}
