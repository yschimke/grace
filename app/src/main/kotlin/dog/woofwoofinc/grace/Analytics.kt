package dog.woofwoofinc.grace

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.LoginEvent
import com.twitter.sdk.android.core.TwitterSession

class Analytics(private val answers: Answers) {

    /*
     * User login events.
     */

    fun notLoggedIn() {
        answers.logCustom(CustomEvent("notloggedin"))
    }

    fun loggedIn(session: TwitterSession) {
        answers.logCustom(
                CustomEvent("session").putCustomAttribute("userid", session.userId)
        )
    }

    fun login(session: TwitterSession) {
        answers.logLogin(
                LoginEvent()
                        .putSuccess(true)
                        .putCustomAttribute("userid", session.userId)
        )
    }

    fun failedLogin() {
        answers.logLogin(
                LoginEvent().putSuccess(false)
        )
    }


    /*
     * Content events.
     */

    fun timelineImpression(timeline: Timeline) {
        answers.logContentView(
                ContentViewEvent()
                        .putContentName(timeline.name)
                        .putContentType("Collection")
                        .putContentId("${timeline.id}")
        )
    }


    /*
     * Application events.
     */

    fun foreground() {
        answers.logCustom(CustomEvent("foreground"))
    }

    fun background() {
        answers.logCustom(CustomEvent("background"))
    }


    /*
     * Twitter API events.
     */

    fun getCollectionsList() {
        answers.logCustom(CustomEvent("getcollectionslist"))
    }

    fun failedGetCollectionsList(userId: Long) {
        val event = CustomEvent("failedgetcollectionslist")
                .putCustomAttribute("userid", userId)
        answers.logCustom(event)
    }


    /*
     * Parse errors.
     */

    fun failedParseCollectionsList(userId: Long) {
        val event = CustomEvent("failedparsecollectionslist")
            .putCustomAttribute("userid", userId)
        answers.logCustom(event)
    }
}
