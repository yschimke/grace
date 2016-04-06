package com.twitter.tweetducker;

import android.app.Application;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.tumblr.remember.Remember;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.fabric.sdk.android.Fabric;

public class TweetDuckerApplication extends Application {

    public static final String TAG = TweetDuckerApplication.class.getSimpleName();

    private static TweetDuckerApplication singleton;

    public static TweetDuckerApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        Fabric.with(this, new Crashlytics(), new Twitter(getTwitterAuthConfig()));
        Remember.init(getApplicationContext(), "com.twitter.tweetducker");
    }

    @Nullable
    public TwitterAuthConfig getTwitterAuthConfig() {
        try {
            InputStream is = getAssets().open("app.properties");

            Properties properties = new Properties();
            properties.load(is);

            String key = properties.getProperty("twitter_key");
            String secret = properties.getProperty("twitter_secret");

            return new TwitterAuthConfig(key, secret);
        } catch (IOException ioe) {
            // Crashlytics is not initialised yet to log this exception.
            Log.d(TAG, "Failed to load app.properties.", ioe);
        } catch (IllegalArgumentException iae) {
            // Crashlytics is not initialised yet to log this exception.
            Log.d(TAG, "Missing key/secret in app.properties.", iae);
        }

        return null;
    }
}