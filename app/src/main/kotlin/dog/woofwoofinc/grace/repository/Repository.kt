package dog.woofwoofinc.grace.repository

import com.twitter.sdk.android.core.TwitterSession

import dog.woofwoofinc.grace.CollectionsList
import dog.woofwoofinc.grace.TimelineOrder

import rx.Observable
import rx.lang.kotlin.deferredObservable
import rx.lang.kotlin.emptyObservable
import rx.lang.kotlin.PublishSubject
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

object Repository {

    private val collectionsListObservable: PublishSubject<CollectionsList>

    init {
        this.collectionsListObservable = PublishSubject<CollectionsList>()
    }

    fun getSelectedCollectionUrl(): String? {
        return SharedPreferences.getSelectedCollectionUrl()
    }

    fun setSelectedCollectionUrl(url: String) {
        SharedPreferences.setSelectedCollectionUrl(url)
    }

    /**
     * I/O operation results are returned by Rx Observable channels. Get the
     * Observable channel for the user's list of available collections. Clients
     * should subscribe and refresh the UI for all received notifications.
     */
    fun getCollectionsListObservable(): Observable<CollectionsList> {
        // Exposed as an Observable interface, not as PublishSubject.
        return collectionsListObservable
    }

    fun getCachedCollectionsList(session: TwitterSession): CollectionsList? {
        val json = SharedPreferences.getCollectionsList(session.userId)
        json?.let {
            return Json.parseCollectionsList(json, session.userId)
        }

        return null
    }

    /**
     * Trigger a reload of the user's list of available collections from cached
     * storage and push onto the Rx Observable channel. Clients should
     * subscribe to the Observable returned by getCollectionsListObservable in
     * order to receive the result.
     */
    fun requestCachedCollectionsList(session: TwitterSession) {
        val collections = getCachedCollectionsList(session)
        collections?.let {
            collectionsListObservable.onNext(collections)
        }
    }

    /**
     * Trigger a reload of the user's list of available collections from the
     * Twitter API and push onto the Rx Observable channel. Clients should
     * subscribe to the Observable returned by getCollectionsListObservable in
     * order to receive the result.
     */
    fun requestCollectionsList(session: TwitterSession) {
        // Use a deferred Observable to move the network call off the current
        // thread and onto a background IO thread.
        val task = deferredObservable {

            val json = TwitterApi.getCollectionsList(session)
            json?.let {
                val collections = Json.parseCollectionsList(json, session.userId)

                collections?.let {
                    SharedPreferences.setCollectionsList(session.userId, json)
                    collectionsListObservable.onNext(collections)
                }
            }

            emptyObservable<String>()
        }

        // Subscribe to the task (on an IO thread) to trigger execution.
        task.subscribeOn(Schedulers.io()).subscribe(emptyObserver())
    }

    /**
     * Remove the given Tweet from the collection with given URL.
     */
    fun removeTweetFromCollection(session: TwitterSession, url: String, tweetId: Long): Observable<Boolean> {
        val collections = getCachedCollectionsList(session)
        val collection = collections?.findTimelineByCollectionUrl(url)

        val observable = PublishSubject<Boolean>()

        collection?.let {
            // Use a deferred Observable to move the network call off the current
            // thread and onto a background IO thread.
            val task = deferredObservable {
                val result = TwitterApi.removeTweetFromCollection(session, collection, tweetId)
                observable.onNext(result)
                observable.onCompleted()

                emptyObservable<String>()
            }

            // Subscribe to the task (on an IO thread) to trigger execution.
            task.subscribeOn(Schedulers.io()).subscribe(emptyObserver())
        }

        return observable
    }

    /**
     * Sort the collection with the given URL.
     */
    fun setCollectionTimelineOrder(session: TwitterSession, url: String, order: TimelineOrder): Observable<Boolean> {
        val collections = getCachedCollectionsList(session)
        val collection = collections?.findTimelineByCollectionUrl(url)

        val observable = PublishSubject<Boolean>()

        collection?.let {
            // Use a deferred Observable to move the network call off the current
            // thread and onto a background IO thread.
            val task = deferredObservable {
                val result = TwitterApi.setCollectionTimelineOrder(session, collection, order)
                observable.onNext(result)
                observable.onCompleted()

                emptyObservable<String>()
            }

            // Subscribe to the task (on an IO thread) to trigger execution.
            task.subscribeOn(Schedulers.io()).subscribe(emptyObserver())
        }

        return observable
    }
}
