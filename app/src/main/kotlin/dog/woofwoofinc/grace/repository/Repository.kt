package dog.woofwoofinc.grace.repository

import com.twitter.sdk.android.core.TwitterSession

import dog.woofwoofinc.grace.CollectionsList

import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

object Repository {

    private val collectionsListObservable: PublishSubject<CollectionsList>

    init {
        this.collectionsListObservable = PublishSubject.create<CollectionsList>()
    }

    /**
     * I/O operation results are returned by Rx Observable channels. Get the
     * Observable channel for the user's list of available collections. Clients
     * should subscribe and refresh the UI for all received notifications.
     */
    fun getCollectionsListObservable(): Observable<CollectionsList> {
        // Exposed as an Observable interface, not as BehaviourSubject.
        return collectionsListObservable
    }

    /**
     * Trigger a reload of the user's list of available collections from cached
     * storage and push onto the Rx Observable channel. Clients should
     * subscribe to the Observable returned by getCollectionsListObservable in
     * order to receive the result.
     */
    fun requestCachedCollectionsList(session: TwitterSession) {
        val json = SharedPreferencesCache.getCollectionsList(session.userId)
        json?.let {
            val collections = Json.parseCollectionsList(json, session.userId)

            collections?.let {
                collectionsListObservable.onNext(collections)
            }
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
        val task = Observable.defer {

            val json = TwitterApi.getCollectionsList(session)
            json?.let {
                val collections = Json.parseCollectionsList(json, session.userId)

                collections?.let {
                    SharedPreferencesCache.setCollectionsList(session.userId, json)
                    collectionsListObservable.onNext(collections)
                }
            }

            Observable.empty<String>()
        }

        // Subscribe to the task (on an IO thread) to trigger execution.
        task.subscribeOn(Schedulers.io()).subscribe(ObserverAdapter<String>())
    }
}
