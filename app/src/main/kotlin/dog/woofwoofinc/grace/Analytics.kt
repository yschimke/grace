package dog.woofwoofinc.grace

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.LoginEvent

class Analytics(private val answers: Answers) {

    /*
     * User login events.
     */

    fun notLoggedIn() {
        answers.logCustom(
            CustomEvent("notloggedin")
        )
    }

    fun loggedIn() {
        answers.logCustom(
            CustomEvent("loggedin")
        )
    }

    fun login() {
        answers.logLogin(
            LoginEvent()
                .putSuccess(true)
        )
    }

    fun failedLogin() {
        answers.logLogin(
            LoginEvent()
                .putSuccess(false)
        )
    }


    /*
     * Content events.
     */

    fun timelineImpression(timeline: Timeline) {
        answers.logContentView(
            ContentViewEvent()
                .putContentType("Collection")
                .putContentId("${timeline.id}")
        )
    }


    /*
     * Application events.
     */

    fun foreground() {
        answers.logCustom(
            CustomEvent("foreground")
        )
    }

    fun background() {
        answers.logCustom(
            CustomEvent("background")
        )
    }


    /*
     * Twitter API events.
     */

    fun getCollectionsList() {
        answers.logCustom(
            CustomEvent("getcollectionslist")
        )
    }

    fun failedGetCollectionsList() {
        answers.logCustom(
            CustomEvent("failedgetcollectionslist")
        )
    }

    fun failedRefresh() {
        answers.logCustom(
            CustomEvent("failedrefresh")
        )
    }


    /*
     * Parse errors.
     */

    fun failedParseCollectionsList() {
        answers.logCustom(
            CustomEvent("failedparsecollectionslist")
        )
    }
}
