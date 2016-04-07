package com.twitter.tweetducker.model;

public class User {
    public long id;
    public String name;
    public String screenName;
    public String description;
    public String profileImageUrlHttps;

    public String getAvatarUrl() {
        if (profileImageUrlHttps == null) {
            return null;
        }

        // Be better than this... :(
        return profileImageUrlHttps.replace("_normal.", "_400x400.");
    }

    public String toString() {
        return String.format(
                "User[id=%d, name=%s, screenName=%s, description=%s, profileImageUrlHttps=%s]",
                id,
                name,
                screenName,
                description,
                profileImageUrlHttps
        );
    }
}
