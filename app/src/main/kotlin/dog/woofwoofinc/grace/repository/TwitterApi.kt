package dog.woofwoofinc.grace.repository

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.pawegio.kandroid.d
import com.twitter.sdk.android.core.OAuthSigning
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterAuthToken
import com.twitter.sdk.android.core.TwitterSession
import dog.woofwoofinc.grace.*

import java.io.IOException

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

object TwitterApi {

    private val analytics: Analytics
    private val twitterAuthConfig: TwitterAuthConfig
    private val client: OkHttpClient

    init {
        this.analytics = Analytics(Answers.getInstance())
        this.twitterAuthConfig = GraceApplication.instance!!.twitterAuthConfig
        this.client = OkHttpClient()
    }

    private fun buildGetRequest(authToken: TwitterAuthToken, url: String): Request {
        val signer = OAuthSigning(twitterAuthConfig, authToken)
        val authorization = signer.getAuthorizationHeader("GET", url, null)

        return Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", authorization)
            .build()
    }

    private fun buildPostRequest(authToken: TwitterAuthToken, url: String, params: Map<String, String>): Request {
        val signer = OAuthSigning(twitterAuthConfig, authToken)
        val authorization = signer.getAuthorizationHeader("POST", url, params)

        val body = FormBody.Builder()
        for ((key, value) in params) {
            body.add(key, value)
        }

        return Request.Builder()
            .post(body.build())
            .url(url)
            .addHeader("Authorization", authorization)
            .build()
    }

    fun getCollectionsList(session: TwitterSession): String? {
        analytics.getCollectionsList()

        val url = "https://api.twitter.com/1.1/collections/list.json?user_id=${session.userId}"

        try {
            val request = buildGetRequest(session.authToken, url)
            val response = client.newCall(request).execute()

            val body = response.body().string()

            body?.let {
                return body
            }
        } catch (ioe: IOException) {
            Crashlytics.logException(ioe)
        }

        analytics.failedGetCollectionsList()
        d("Failed to fetch collections/list Json.")

        return null
    }

    fun setCollectionTimelineOrder(session: TwitterSession, collection: Timeline, order: TimelineOrder): Boolean {
        analytics.setCollectionTimelineOrder()

        val url = "https://api.twitter.com/1.1/collections/update.json"

        var params = mapOf<String, String>(
            Pair("id", "custom-${collection.id}"),
            Pair("name", collection.name),
            Pair("timeline_order", order.param)
        )

        // Add description too if present or it will be deleted.
        collection.description?.let {
            params += Pair("description", collection.description)
        }

        try {
            val request = buildPostRequest(session.authToken, url, params)
            val response = client.newCall(request).execute()

            response.body().close()

            if (response.isSuccessful) {
                return true
            }
        } catch (ioe: IOException) {
            Crashlytics.logException(ioe)
        }

        analytics.failedSetCollectionTimelineOrder()
        d("Failed to set collection timeline order.")

        return false
    }
}
