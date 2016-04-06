package com.twitter.tweetducker;

import android.app.Application;
import android.support.annotation.Nullable;
import android.util.Log;

import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
            Log.d(TAG, "Failed to load app.properties.", ioe);
        } catch (IllegalArgumentException iae) {
            Log.d(TAG, "Missing key/secret in app.properties.", iae);
        }

        return null;
    }
}