package com.twitter.tweetducker.model;

public class Timeline {
    public String name;
    public String description;
    public String collectionUrl;
    public String visibility;

    public String toString() {
        return String.format(
                "Timeline[name=%s, description=%s, collectionUrl=%s, visibility=%s]",
                name,
                description,
                collectionUrl,
                visibility
        );
    }
}
