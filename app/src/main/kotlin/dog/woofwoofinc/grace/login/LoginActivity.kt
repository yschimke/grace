package dog.woofwoofinc.grace.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.startActivity
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import dog.woofwoofinc.grace.Analytics
import dog.woofwoofinc.grace.R
import dog.woofwoofinc.grace.home.HomeActivity

import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Organise the UI
        setContentView(R.layout.activity_login)

        twitter_login_button.callback = object : Callback<TwitterSession>() {
            val analytics = Analytics(Answers.getInstance())

            override fun success(result: Result<TwitterSession>) {
                analytics.login()

                // Start the home activity.
                startActivity<HomeActivity>()

                // Finish this activity so back button doesn't return here.
                finish()
            }

            override fun failure(exception: TwitterException) {
                Crashlytics.logException(exception)

                longToast("Authentication Failure")
                Log.d("TwitterKit", "Authentication Failure", exception)

                analytics.failedLogin()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Ensure the login button gets results for any activities it triggers.
        twitter_login_button.onActivityResult(requestCode, resultCode, data)
    }
}
