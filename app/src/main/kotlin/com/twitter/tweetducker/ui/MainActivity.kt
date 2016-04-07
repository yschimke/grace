package com.twitter.tweetducker.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import com.crashlytics.android.answers.Answers
import com.jenzz.appstate.AppState
import com.jenzz.appstate.RxAppState
import com.squareup.picasso.Picasso
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.tweetui.CollectionTimeline
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter
import com.twitter.tweetducker.*
import com.twitter.tweetducker.rx.ObserverAdapter

import java.util.concurrent.atomic.AtomicReference

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var analytics: Analytics? = null
    private var appStateSubscription: Subscription? = null
    private var collectionsListSubscription: Subscription? = null
    private val collectionsListReference = AtomicReference<CollectionsList>()
    private val currentTimeline = AtomicReference<Timeline>()

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
            analytics!!.loggedIn(session)
        }
    }

    private fun setCollection(timeline: Timeline) {
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

            currentTimeline.set(timeline)
            analytics!!.timelineImpression(timeline)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = Analytics(Answers.getInstance())

        checkLoggedIn()

        val session = Twitter.getInstance().getSession()
        session?.let {
            // Why can the session be null here if checkLoggedIn finish()-ed?

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

            navigation_view.setNavigationItemSelectedListener(this)

            // Subscribe for application background and foreground actions.
            appStateSubscription = RxAppState.monitor(application).subscribe(object : ObserverAdapter<AppState>() {
                override fun onNext(state: AppState) {
                    when (state) {
                        AppState.FOREGROUND -> analytics!!.foreground()
                        AppState.BACKGROUND -> analytics!!.background()
                    }
                }
            })

            // Start listening to Twitter API responses on the UI thread to apply updates.
            val api = TwitterAPI(session)

            collectionsListSubscription = api.getCollectionsListObservable()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : ObserverAdapter<CollectionsList>() {

                        override fun onNext(collectionsList: CollectionsList) {
                            // Update the user details in the navigation drawer.
                            val user = collectionsList.user

                            screen_name_text_view.text = "@${user.screenName}"
                            name_text_view.text = user.name

                            Picasso.with(applicationContext)
                                    .load(user.avatarUrl)
                                    .resizeDimen(R.dimen.avatar_width, R.dimen.avatar_height)
                                    .transform(RoundedCornersTransformation(10, 0))
                                    .into(avatar_image_view)

                            // Update the collections list in the navigation drawer.
                            val menu = navigation_view.getMenu()

                            menu.clear()
                            for (timeline in collectionsList.timelines) {
                                menu.add(timeline.name)
                            }

                            // Keep the collections list for handling onNavigationItemSelected.
                            collectionsListReference.set(collectionsList)

                            // Display the first collection if none visible.
                            if (currentTimeline.get() == null && !collectionsList.timelines.isEmpty()) {
                                setCollection(collectionsList.timelines[0])
                            }
                        }
                    })

            // Trigger API data fetch to provide or update cached data.
            api.refreshCollectionsList()
        }
    }

    override fun onDestroy() {
        // Don't leak the Rx subscriptions.
        appStateSubscription?.let {
            appStateSubscription!!.unsubscribe()
        }
        collectionsListSubscription?.let  {
            collectionsListSubscription!!.unsubscribe()
        }

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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val title = item.title.toString()

        val collectionsList = collectionsListReference.get()
        collectionsList?.let {
            val timeline = collectionsList.findTimelineByName(title)
            timeline?.let {
                setCollection(timeline)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)

        return true
    }
}
