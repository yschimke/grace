package com.twitter.tweetducker.model;

public class User {
    public long id;
    public String name;
    public String screenname;
    public String description;
    public String profileImageUrlHttps;

    public String toString() {
        return String.format(
                "User[id=%d, name=%s, screenname=%s, description=%s, profileImageUrlHttps=%s]",
                id,
                name,
                screenname,
                description,
                profileImageUrlHttps
        );
    }
}
