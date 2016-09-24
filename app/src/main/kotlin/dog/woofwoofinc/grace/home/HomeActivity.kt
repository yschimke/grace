package dog.woofwoofinc.grace.home

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView

import com.crashlytics.android.answers.Answers
import com.jakewharton.rxbinding.support.design.widget.itemSelections
import com.jenzz.appstate.AppState
import com.jenzz.appstate.RxAppState
import com.squareup.picasso.Picasso
import com.tumblr.remember.Remember
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.tweetui.CollectionTimeline
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter

import dog.woofwoofinc.grace.*
import dog.woofwoofinc.grace.login.LoginActivity
import dog.woofwoofinc.grace.repository.ObserverAdapter
import dog.woofwoofinc.grace.repository.Repository

import java.util.concurrent.atomic.AtomicReference

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class HomeActivity : AppCompatActivity() {

    private var analytics: Analytics? = null
    private val collectionsListReference = AtomicReference<CollectionsList>()

    private var appStateSubscription: Subscription? = null
    private var collectionsListSubscription: Subscription? = null
    private var navigationViewSubscription: Subscription? = null

    private fun checkLoggedIn() {
        val session = Twitter.getInstance().getSession()
        if (session == null) {
            analytics!!.notLoggedIn()

            // Start the login activity.
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // Finish this activity so the back button doesn't return here.
            finish()
        }

        session?.let {
            analytics!!.loggedIn()
        }
    }

    private fun setCollection(timeline: Timeline) {
        // Don't set a collection if it is already being shown.
        if (collection_list_view.tag != timeline.collectionUrl) {
            val id = timeline.id
            id?.let {
                // Set the toolbar title.
                toolbar.title = timeline.name

                // Set the timeline in the main content view.
                val collection = CollectionTimeline.Builder()
                        .id(id)
                        .build()

                val adapter = TweetTimelineListAdapter.Builder(this)
                        .setTimeline(collection)
                        .setViewStyle(R.style.tw__TweetLightWithActionsStyle)
                        .build()

                collection_list_view.adapter = adapter

                Remember.putString("selected-timeline", timeline.collectionUrl)

                // Tag the collection_list_view so we can determine if it is still showing
                // the collection we want after app running state changes, etc.
                collection_list_view.tag = timeline.collectionUrl

                analytics!!.timelineImpression(timeline)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = Analytics(Answers.getInstance())

        checkLoggedIn()

        val session = Twitter.getInstance().getSession()
        session?.let {
            // Why could the session have been null if checkLoggedIn finish()-ed?

            // Organise the UI.
            setContentView(R.layout.activity_main)
            setSupportActionBar(toolbar)

            floating_action_button.setOnClickListener { view ->
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show()
            }

            // Hide the floating action bar until it has a use.
            floating_action_button.hide()

            val toggle = ActionBarDrawerToggle(
                    this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer_layout.addDrawerListener(toggle)
            toggle.syncState()

            // Subscribe for navigation drawer menu item selections on the UI thread to apply updates.
            navigationViewSubscription = navigation_view.itemSelections()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : ObserverAdapter<MenuItem>() {
                        override fun onNext(item: MenuItem) {
                            val collectionsList = collectionsListReference.get()
                            collectionsList?.let {
                                val title = item.title.toString()

                                val timeline = collectionsList.findTimelineByName(title)
                                timeline?.let {
                                    setCollection(timeline)
                                }
                            }

                            drawer_layout.closeDrawer(GravityCompat.START)
                        }
                    })

            // Subscribe for application background and foreground actions.
            appStateSubscription = RxAppState.monitor(application)
                    .observeOn(Schedulers.io())
                    .subscribe(object : ObserverAdapter<AppState>() {
                        override fun onNext(state: AppState) {
                            when (state) {
                                AppState.FOREGROUND -> analytics!!.foreground()
                                AppState.BACKGROUND -> analytics!!.background()
                            }
                        }
                    })

            // The Kotlin Android Extension synthetic views (navigation_view, screen_name_text_view,
            // name_text_view, avatar_image_view) are null after changes to the app running state.
            // e.g. swipe it out of the recent apps list and restart. Also displays some problems on
            // rotation. Use findViewById for now.
            val navigationView = findViewById(R.id.navigation_view) as NavigationView
            val header = navigationView.getHeaderView(0)
            val screenNameTextView = header.findViewById(R.id.screen_name_text_view) as TextView
            val nameTextView = header.findViewById(R.id.name_text_view) as TextView
            val avatarImageView = header.findViewById(R.id.avatar_image_view) as ImageView

            // Subscribe for Twitter API responses on the UI thread to apply updates.
            collectionsListSubscription = Repository.getCollectionsListObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : ObserverAdapter<CollectionsList>() {
                        override fun onNext(collectionsList: CollectionsList) {
                            // Update the user details in the navigation drawer.
                            val user = collectionsList.user

                            screenNameTextView.text = "@${user.screenName}"
                            nameTextView.text = user.name

                            Picasso.with(applicationContext)
                                    .load(user.avatarUrl)
                                    .resizeDimen(R.dimen.avatar_width, R.dimen.avatar_height)
                                    .transform(RoundedCornersTransformation(10, 0))
                                    .into(avatarImageView)

                            // Update the collections list in the navigation drawer.
                            val menu = navigationView.getMenu()

                            menu.clear()
                            for (timeline in collectionsList.timelines) {
                                menu.add(timeline.name)
                            }

                            // Keep the collections list for handling onNavigationItemSelected.
                            collectionsListReference.set(collectionsList)

                            // Must display a collection if at least one is available.
                            if (!collectionsList.timelines.isEmpty()) {
                                // Get selected collection or fallback to first collection in list.
                                val firstCollectionUrl = collectionsList.timelines[0].collectionUrl
                                val selectedCollectionUrl = Remember.getString("selected-timeline", firstCollectionUrl)

                                val currentTimeline = collectionsList.findTimelineByCollectionUrl(selectedCollectionUrl)
                                currentTimeline?.let {
                                    setCollection(currentTimeline)
                                }
                            }
                        }
                    })

            // Trigger API data fetch to provide updated Collections data.
            Repository.requestCachedCollectionsList(session)
            Repository.requestCollectionsList(session)
        }
    }

    override fun onDestroy() {
        // Don't leak the Rx subscriptions.
        navigationViewSubscription?.unsubscribe()
        appStateSubscription?.unsubscribe()
        collectionsListSubscription?.unsubscribe()

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
