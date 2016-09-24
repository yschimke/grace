package dog.woofwoofinc.grace.repository

import android.util.Log

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.twitter.sdk.android.core.OAuthSigning
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterAuthToken
import com.twitter.sdk.android.core.TwitterSession
import dog.woofwoofinc.grace.*

import java.io.IOException

import okhttp3.OkHttpClient
import okhttp3.Request

object TwitterApi {

    private val TAG = TwitterApi::class.asTag()

    private val analytics: Analytics
    private val twitterAuthConfig: TwitterAuthConfig
    private val client: OkHttpClient

    init {
        this.analytics = Analytics(Answers.getInstance())
        this.twitterAuthConfig = GraceApplication.instance!!.twitterAuthConfig
        this.client = OkHttpClient()
    }

    private fun buildGetRequest(url: String, authToken: TwitterAuthToken): Request {
        val signer = OAuthSigning(twitterAuthConfig, authToken)
        val authorization = signer.getAuthorizationHeader("GET", url, null)

        return Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", authorization)
            .build()
    }

    fun getCollectionsList(session: TwitterSession): String? {
        analytics.getCollectionsList()

        val url = "https://api.twitter.com/1.1/collections/list.json?user_id=${session.userId}"

        try {
            val request = buildGetRequest(url, session.authToken)
            val response = client.newCall(request).execute()

            val body = response.body().string()

            body?.let {
                return body
            }
        } catch (ioe: IOException) {
            Crashlytics.logException(ioe)
        }

        analytics.failedGetCollectionsList(session.userId)
        Log.d(TAG, "Failed to fetch collections/list Json.")

        return null
    }
}
