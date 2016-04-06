package com.twitter.tweetducker;

import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.tumblr.remember.Remember;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.tweetducker.model.CollectionsList;
import com.twitter.tweetducker.model.JSON;
import com.twitter.tweetducker.rx.ObserverAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class TwitterAPI {

    private static final String TAG = TwitterAPI.class.getSimpleName();

    private Analytics analytics;
    private TwitterSession session;
    private TwitterAuthConfig twitterAuthConfig;

    private OkHttpClient client = new OkHttpClient();

    private BehaviorSubject<CollectionsList> collectionsListObservable;

    public TwitterAPI(@NonNull TwitterSession session) {
        this.analytics = new Analytics(Answers.getInstance());
        this.session = session;
        this.twitterAuthConfig = TweetDuckerApplication.getInstance().getTwitterAuthConfig();
        this.collectionsListObservable = BehaviorSubject.create();

        // Read from cache and onNext into the collectionsListObservable to preload.
        final String url = "https://api.twitter.com/1.1/collections/list.json?user_id=" + session.getUserId();
        String body = Remember.getString(url, null);
        if (body != null) {
            Log.d(TAG, "Cached collections/list found. Using to fast populate navigation drawer.");
            analytics.precachedCollectionsList();

            // Parse the JSON into a model object Collections and publish.
            CollectionsList collections = JSON.parseCollectionsList(body, session.getUserId());
            if (collections != null) {
                collectionsListObservable.onNext(collections);
            }
        }
    }

    @NonNull
    public Observable<CollectionsList> getCollectionsListObservable() {
        // Note, exposed as Observable interface, not as BehaviourSubject.
        return collectionsListObservable;
    }

    private Request buildGetRequestWithAuthHeaders(@NonNull String url) {
        Map<String, String> body = Collections.<String, String>emptyMap();

        Request.Builder builder = new Request.Builder()
                .get()
                .url(url);

        // Add authentication header(s).
        TwitterAuthToken token = session.getAuthToken();
        Map<String, String> authHeaders = token.getAuthHeaders(twitterAuthConfig, "GET", url, body);
        for (Map.Entry<String, String> header : authHeaders.entrySet()) {
            builder = builder.addHeader(header.getKey(), header.getValue());
        }

        return builder.build();
    }

    public void refreshCollectionsList() {
        // Make the network call in a deferred Observable so we can observe/subscribe to it on an IO
        // thread instead of on the Android UI thread.
        Observable<String> task = Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                Log.d(TAG, "Refreshing collections/list.");
                analytics.refreshCollectionsList();

                String url = "https://api.twitter.com/1.1/collections/list.json?user_id=" + session.getUserId();
                String body = null;

                try {
                    Request request = buildGetRequestWithAuthHeaders(url);
                    Response response = client.newCall(request).execute();

                    body = response.body().string();
                } catch (IOException ioe) {
                    Crashlytics.logException(ioe);
                    Log.d(TAG, "IOException in OkHttp call", ioe);
                }

                if (body != null) {
                    // Parse the JSON into a model object Collections and publish.
                    CollectionsList collections = JSON.parseCollectionsList(body, session.getUserId());
                    if (collections != null) {
                        Log.d(TAG, "Refreshed collections/list.");
                        analytics.refreshedCollectionsList();

                        collectionsListObservable.onNext(collections);

                        // Now cache the result in storage.
                        Remember.putString(url, body);
                    }
                }

                return Observable.empty();
            }
        });

        // Subscribe to the task (on an IO thread) to trigger execution.
        task.subscribeOn(Schedulers.io())
                .subscribe(new ObserverAdapter<String>());
    }
}
