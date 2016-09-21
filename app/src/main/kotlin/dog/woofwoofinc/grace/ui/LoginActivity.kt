package dog.woofwoofinc.grace.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import dog.woofwoofinc.grace.Analytics
import dog.woofwoofinc.grace.R

import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val analytics = Analytics(Answers.getInstance())

        // Organise the UI
        setContentView(R.layout.activity_login)

        twitter_login_button.callback = object : Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>) {
                val session = result.data
                analytics.login(session)

                // Start the main activity.
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)

                // Finish this activity so back button doesn't return here.
                finish()
            }

            override fun failure(exception: TwitterException) {
                Crashlytics.logException(exception)

                val msg = "Authentication Failure"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()

                Log.d("TwitterKit", msg, exception)
                analytics.failedLogin()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Make sure that the loginButton hears the result from any Activity that it triggered.
        twitter_login_button.onActivityResult(requestCode, resultCode, data)
    }
}
