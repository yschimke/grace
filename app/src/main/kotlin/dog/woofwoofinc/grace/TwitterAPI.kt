package dog.woofwoofinc.grace

import android.util.Log

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.tumblr.remember.Remember
import com.twitter.sdk.android.core.OAuthSigning
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterSession
import dog.woofwoofinc.grace.rx.ObserverAdapter

import java.io.IOException

import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

object TwitterAPI {

    private val TAG = TwitterAPI::class.asTag()

    private val analytics: Analytics
    private val twitterAuthConfig: TwitterAuthConfig
    private val client: OkHttpClient
    private val collectionsListObservable: PublishSubject<CollectionsList>

    init {
        this.analytics = Analytics(Answers.getInstance())
        this.twitterAuthConfig = GraceApplication.instance!!.twitterAuthConfig
        this.client = OkHttpClient()
        this.collectionsListObservable = PublishSubject.create<CollectionsList>()
    }

    fun getCollectionsListObservable(): Observable<CollectionsList> {
        // Note, exposed as an Observable interface, not as BehaviourSubject.
        return collectionsListObservable
    }

    fun fetchCachedCollectionsList(session: TwitterSession) {
        // Read latest CollectionsList from cache and onNext to subscribers to preload.
        val url = "https://api.twitter.com/1.1/collections/list.json?user_id=${session.userId}"

        val body = Remember.getString(url, null)
        body?.let {
            Log.d(TAG, "Cached collections/list found. Using to fast populate navigation drawer.")
            analytics.precachedCollectionsList()

            // Parse the JSON into a model object Collections and publish.
            val collections = JSON.parseCollectionsList(body, session.userId)
            collections?.let {
                collectionsListObservable.onNext(collections)
            }
        }
    }

    private fun buildGetRequestWithAuthHeaders(url: String, session: TwitterSession): Request {
        val body = emptyMap<String, String>()

        var builder: Request.Builder = Request.Builder().get().url(url)

        // Add authentication header(s).
        val signer = OAuthSigning(twitterAuthConfig, session.authToken)
        builder.addHeader("Authorization", signer.getAuthorizationHeader("GET", url, body))

        return builder.build()
    }

    fun refreshCollectionsList(session: TwitterSession) {
        // Make the network call in a deferred Observable so we can observe/subscribe
        // to it on an IO thread instead of on the Android UI thread.
        val task = Observable.defer {
            analytics.refreshCollectionsList()

            val url = "https://api.twitter.com/1.1/collections/list.json?user_id=${session.userId}"

            try {
                val request = buildGetRequestWithAuthHeaders(url, session)
                val response = client.newCall(request).execute()

                val body = response.body().string()
                if (body == null) {
                    analytics.failedCollectionsListFetch(session)
                    Log.d(TAG, "Failed to fetch collections/list JSON.")
                }

                body?.let {
                    // Parse the JSON into a model object Collections and publish.
                    val collections = JSON.parseCollectionsList(body, session.userId)
                    if (collections == null) {
                        analytics.failedCollectionsListParse(session)
                        Log.d(TAG, "Failed to parse collections/list JSON response.")
                    }

                    collections?.let {
                        analytics.refreshedCollectionsList()
                        collectionsListObservable.onNext(collections)

                        // Now cache the result in storage.
                        Remember.putString(url, body)
                    }
                }
            } catch (ioe: IOException) {
                Crashlytics.logException(ioe)
            }

            Observable.empty<String>()
        }

        // Subscribe to the task (on an IO thread) to trigger execution.
        task.subscribeOn(Schedulers.io()).subscribe(ObserverAdapter<String>())
    }
}
