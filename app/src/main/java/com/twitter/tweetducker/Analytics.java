package com.twitter.tweetducker;

import android.support.annotation.NonNull;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class Analytics {

    private Answers answers;

    public Analytics(@NonNull Answers answers) {
        this.answers = answers;
    }

    public void applicationStart() {
        CustomEvent event = new CustomEvent("start");
        answers.logCustom(event);
    }

    public void applicationEntersForeground() {
        CustomEvent event = new CustomEvent("foreground");
        answers.logCustom(event);
    }

    public void applicationEntersBackground() {
        CustomEvent event = new CustomEvent("background");
        answers.logCustom(event);
    }
}
