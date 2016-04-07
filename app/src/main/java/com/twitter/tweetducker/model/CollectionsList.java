package com.twitter.tweetducker.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

public class CollectionsList {
    public User user;
    public List<Timeline> timelines;

    @Nullable
    public Timeline findTimelineByName(@NonNull String name) {
        for (Timeline timeline : timelines) {
            if (timeline.name.equalsIgnoreCase(name)) {
                return timeline;
            }
        }

        return null;
    }
}
