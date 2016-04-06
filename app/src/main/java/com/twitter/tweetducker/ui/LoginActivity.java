package com.twitter.tweetducker.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.tweetducker.Analytics;
import com.twitter.tweetducker.BuildConfig;
import com.twitter.tweetducker.R;

public class LoginActivity extends Activity {

    private TwitterLoginButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Analytics analytics = new Analytics(Answers.getInstance());

        // Organise the UI
        setContentView(R.layout.activity_login);

        TextView view = (TextView) findViewById(R.id.terms_text);
        String html = getApplicationContext().getString(R.string.terms_html);
        view.setText(Html.fromHtml(html));
        view.setMovementMethod(LinkMovementMethod.getInstance());
        RemoveLinkUnderline.remove(view);

        view = (TextView) findViewById(R.id.privacy_text);
        html = getApplicationContext().getString(R.string.privacy_html);
        view.setText(Html.fromHtml(html));
        view.setMovementMethod(LinkMovementMethod.getInstance());
        RemoveLinkUnderline.remove(view);

        view = (TextView) findViewById(R.id.version_text);
        String text = getApplicationContext().getString(R.string.version_text);
        String version = text + " " + BuildConfig.VERSION_NAME;
        view.setText(version);

        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                TwitterSession session = result.data;

                String msg = "@" + session.getUserName() + " logged in! (#" + session.getUserId() + ")";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                analytics.login(session);

                // Start the main activity
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);

                // Finish this activity so back button doesn't return here.
                finish();
            }

            @Override
            public void failure(TwitterException exception) {
                Crashlytics.logException(exception);

                String msg = "Authentication Failure";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                Log.d("TwitterKit", msg, exception);
                analytics.failedLogin();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Make sure that the loginButton hears the result from any
        // Activity that it triggered.
        loginButton.onActivityResult(requestCode, resultCode, data);
    }
}
