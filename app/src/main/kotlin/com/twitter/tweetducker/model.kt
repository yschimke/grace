package com.twitter.tweetducker

data class CollectionsList(val user: User, val timelines: List<Timeline>) {

    fun findTimelineByName(name: String): Timeline? {
        for (timeline in timelines) {
            if (timeline.name.equals(name, ignoreCase = true)) {
                return timeline
            }
        }

        return null
    }
}


data class User(val id: Long, val name: String, val screenName: String, val description: String, val profileImageUrlHttps: String) {

    // Be better than this... :(
    val avatarUrl: String get() = profileImageUrlHttps.replace("_normal.", "_400x400.")
}


data class Timeline(val name: String, val description: String, val collectionUrl: String, val visibility: String) {

    val idString: String?
        get() {
            // You are also better than this... :(
            // collectionUrl looks like "https://twitter.com/<string>/timelines/<long>"

            val id = collectionUrl.substring(collectionUrl.lastIndexOf('/') + 1).trim({ it <= ' ' })

            if (!id.isEmpty()) {
                return id
            }

            return null
        }

    val id: Long?
        get() {
            try {
                return java.lang.Long.parseLong(idString)
            } catch (e: Exception) {
                // Suppress
            }

            return null
        }
}
