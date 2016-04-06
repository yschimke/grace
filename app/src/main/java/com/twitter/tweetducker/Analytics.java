package com.twitter.tweetducker;

import android.support.annotation.NonNull;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.twitter.sdk.android.core.TwitterSession;

public class Analytics {

    private Answers answers;

    public Analytics(@NonNull Answers answers) {
        this.answers = answers;
    }

    public void foreground() {
        CustomEvent event = new CustomEvent("foreground");
        answers.logCustom(event);
    }

    public void background() {
        CustomEvent event = new CustomEvent("background");
        answers.logCustom(event);
    }

    public void notLoggedIn() {
        CustomEvent event = new CustomEvent("notloggedin");
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

    public void session(@NonNull TwitterSession session) {
        CustomEvent event = new CustomEvent("session")
                .putCustomAttribute("userid", session.getUserId());
        answers.logCustom(event);
    }
}
