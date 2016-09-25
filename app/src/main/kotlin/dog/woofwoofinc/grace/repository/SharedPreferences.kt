package dog.woofwoofinc.grace.repository

import com.tumblr.remember.Remember

object SharedPreferences {

    fun getSelectedCollectionUrl(): String? {
        return Remember.getString("selected-collection-url", null)
    }

    fun setSelectedCollectionUrl(url: String) {
        Remember.putString("selected-collection-url", url)
    }

    fun getCollectionsList(userId: Long): String? {
        val url = "https://api.twitter.com/1.1/collections/list.json?user_id=${userId}"
        return Remember.getString(url, null)
    }

    fun setCollectionsList(userId: Long, json: String) {
        val url = "https://api.twitter.com/1.1/collections/list.json?user_id=${userId}"
        Remember.putString(url, json)
    }
}
