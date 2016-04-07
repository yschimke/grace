package com.twitter.tweetducker;

import android.support.annotation.NonNull;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.tweetducker.model.Timeline;

public class Analytics {

    private Answers answers;

    public Analytics(@NonNull Answers answers) {
        this.answers = answers;
    }


    /*
     * User login events.
     */

    public void notLoggedIn() {
        CustomEvent event = new CustomEvent("notloggedin");
        answers.logCustom(event);
    }

    public void loggedIn(@NonNull TwitterSession session) {
        CustomEvent event = new CustomEvent("session")
                .putCustomAttribute("userid", session.getUserId());
        answers.logCustom(event);
    }

    public void login(@NonNull TwitterSession session) {
        LoginEvent event = new LoginEvent()
                .putSuccess(true)
                .putCustomAttribute("userid", session.getUserId());
        answers.logLogin(event);
    }

    public void failedLogin() {
        LoginEvent event = new LoginEvent().putSuccess(false);
        answers.logLogin(event);
    }


    /*
     * Content events
     */

    public void timelineImpression(@NonNull Timeline timeline) {
        ContentViewEvent event = new ContentViewEvent()
                .putContentName(timeline.name)
                .putContentType("Collection")
                .putContentId(timeline.getIdString());

        answers.logContentView(event);
    }


    /*
     * Application events.
     */

    public void foreground() {
        CustomEvent event = new CustomEvent("foreground");
        answers.logCustom(event);
    }

    public void background() {
        CustomEvent event = new CustomEvent("background");
        answers.logCustom(event);
    }


    /*
     * Twitter API events.
     */

    public void precachedCollectionsList() {
        CustomEvent event = new CustomEvent("precachedcollections");
        answers.logCustom(event);
    }

    public void refreshCollectionsList() {
        CustomEvent event = new CustomEvent("refreshcollections");
        answers.logCustom(event);
    }

    public void refreshedCollectionsList() {
        CustomEvent event = new CustomEvent("refreshedcollection");
        answers.logCustom(event);
    }
}
