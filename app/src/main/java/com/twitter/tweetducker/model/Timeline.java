package com.twitter.tweetducker.model;

public class Timeline {
    public String name;
    public String description;
    public String collectionUrl;
    public String visibility;

    public String getIdString() {
        // You are also better than this... :(
        // collectionUrl looks like "https://twitter.com/<string>/timelines/<long>"
        String id = collectionUrl.substring(collectionUrl.lastIndexOf('/') + 1).trim();

        if (!id.isEmpty()) {
            return id;
        }

        return null;
    }

    public Long getId() {
        try {
            return Long.parseLong(getIdString());
        } catch (Exception e) {
            // Suppress
        }

        return null;
    }

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
