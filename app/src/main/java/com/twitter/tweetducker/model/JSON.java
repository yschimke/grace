package com.twitter.tweetducker.model;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class JSON {

    private static final String TAG = JSON.class.getSimpleName();

    private static User parseUser(JsonNode user) {
        User result = null;

        if (user != null) {
            result = new User();
            result.id = user.get("id").asLong();
            result.name = user.get("name").asText();
            result.screenName = user.get("screen_name").asText();
            result.description = user.get("description").asText();
            result.profileImageUrlHttps = user.get("profile_image_url_https").asText();
        }

        return result;
    }

    private static Timeline parseTimeline(JsonNode timeline) {
        Timeline result = null;

        if (timeline != null) {
            result = new Timeline();
            result.name = timeline.get("name").asText();
            result.description = timeline.get("description").asText();
            result.collectionUrl = timeline.get("collection_url").asText();
            result.visibility = timeline.get("visibility").asText();
        }

        return result;
    }

    public static CollectionsList parseCollectionsList(String json, long userId) {
        CollectionsList result = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readValue(json, JsonNode.class);

            if (root != null) {
                result = new CollectionsList();

                JsonNode user = root.path("objects").path("users").path("" + userId);
                if (!user.isMissingNode()) {
                    result.user = parseUser(user);
                }

                JsonNode timelines = root.path("objects").path("timelines");
                if (!timelines.isMissingNode()) {
                    List<Timeline> tmp = new LinkedList<>();
                    for (JsonNode timeline : timelines) {
                        tmp.add(parseTimeline(timeline));
                    }

                    result.timelines = tmp;
                }
            }
        } catch (IOException ioe) {
            Crashlytics.logException(ioe);
            Log.d(TAG, "IOException parsing JSON.", ioe);
        }

        return result;
    }
}
