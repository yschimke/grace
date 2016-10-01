package dog.woofwoofinc.grace.home

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.jakewharton.rxbinding.support.design.widget.itemSelections
import com.jenzz.appstate.AppState
import com.jenzz.appstate.RxAppState
import com.pawegio.kandroid.WebIntent
import com.pawegio.kandroid.find
import com.pawegio.kandroid.startActivity
import com.squareup.picasso.Picasso
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.models.Tweet
import com.twitter.sdk.android.tweetui.CollectionTimeline
import com.twitter.sdk.android.tweetui.TimelineResult
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter

import dog.woofwoofinc.grace.*
import dog.woofwoofinc.grace.R.anim.abc_fade_in
import dog.woofwoofinc.grace.R.anim.abc_fade_out
import dog.woofwoofinc.grace.login.LoginActivity
import dog.woofwoofinc.grace.repository.ObserverAdapter
import dog.woofwoofinc.grace.repository.Repository

import java.util.concurrent.atomic.AtomicReference

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.content_home.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class HomeActivity : AppCompatActivity() {

    private var analytics: Analytics? = null
    private val collectionsListReference = AtomicReference<CollectionsList>()

    private var appStateSubscription: Subscription? = null
    private var collectionsListSubscription: Subscription? = null
    private var navigationViewSubscription: Subscription? = null

    private val swipeRefreshCallback = object : Callback<TimelineResult<Tweet>>() {
        override fun success(result: Result<TimelineResult<Tweet>>) {
            val swipeRefreshLayout = find<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
            swipeRefreshLayout.isRefreshing = false
        }

        override fun failure(exception: TwitterException) {
            Crashlytics.logException(exception)
            analytics!!.failedRefresh()
        }
    }

    private fun refreshCollection() {
        val swipeRefreshLayout = find<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        val adapter = collection_list_view.adapter as TweetTimelineListAdapter

        swipeRefreshLayout.isRefreshing = true
        adapter.refresh(swipeRefreshCallback)
    }

    private fun checkLoggedIn() {
        val session = Twitter.getInstance().getSession()
        if (session == null) {
            analytics!!.notLoggedIn()

            // Start the login activity.
            startActivity<LoginActivity>()

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

                // Set the timeline in the home content view.
                val collection = CollectionTimeline.Builder()
                        .id(id)
                        .build()

                val adapter = TweetTimelineListAdapter.Builder(this)
                        .setTimeline(collection)
                        .setViewStyle(R.style.tw__TweetLightWithActionsStyle)
                        .build()

                collection_list_view.adapter = adapter

                Repository.setSelectedCollectionUrl(timeline.collectionUrl)

                // Tag the collection_list_view so we can tell if it is showing
                // the correct collection after app running state changes, etc.
                collection_list_view.tag = timeline.collectionUrl

                analytics!!.timelineImpression(timeline)

                // Invalidate the option menu to trigger the refresh which
                // enables the share, publish, etc menu items.
                invalidateOptionsMenu()

                // Swipe to refresh.
                val swipeRefreshLayout = find<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
                swipeRefreshLayout.setOnRefreshListener {
                    refreshCollection()
                }

                // Scroll to top when title clicked.
                val titleView: View? = toolbar.findFirstViewWithText(timeline.name)
                titleView?.setOnClickListener {
                    collection_list_view.smoothScrollToPosition(0)
                }
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
            setContentView(R.layout.activity_home)
            setSupportActionBar(toolbar)

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
            val navigationView = find<NavigationView>(R.id.navigation_view)
            val header = navigationView.getHeaderView(0)
            val screenNameTextView = header.find<TextView>(R.id.screen_name_text_view)
            val nameTextView = header.find<TextView>(R.id.name_text_view)
            val avatarImageView = header.find<ImageView>(R.id.avatar_image_view)

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
                            val menu = navigationView.menu

                            menu.clear()
                            for (timeline in collectionsList.timelines) {
                                menu.add(timeline.name)
                            }

                            // Keep the collections list for handling onNavigationItemSelected.
                            collectionsListReference.set(collectionsList)

                            // Must display a collection if at least one is available.
                            if (!collectionsList.timelines.isEmpty()) {
                                // Get selected collection or fallback to first collection in list.
                                val selectedCollectionUrl = Repository.getSelectedCollectionUrl()
                                val collectionUrl = selectedCollectionUrl ?: collectionsList.timelines[0].collectionUrl

                                val currentTimeline = collectionsList.findTimelineByCollectionUrl(collectionUrl)
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
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val showCollectionOptions = collection_list_view.tag != null

        menu.findItem(R.id.action_share).isVisible = showCollectionOptions
        menu.findItem(R.id.action_refresh).isVisible = showCollectionOptions
        menu.findItem(R.id.action_sort).isVisible = showCollectionOptions
        menu.findItem(R.id.action_publish).isVisible = showCollectionOptions

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_share) {
            val url = collection_list_view.tag.toString()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, url)

            startActivity(Intent.createChooser(intent, "Share"))

            return true
        } else if (id == R.id.action_refresh) {
            refreshCollection()

            return true
        } else if (id == R.id.action_sort) {
            val session = Twitter.getInstance().getSession()
            session?.let {
                val url = collection_list_view.tag.toString()
                val options = resources.getStringArray(R.array.sort_options)

                AlertDialog.Builder(this).
                    setItems(options) { dialog, which ->
                        val order: TimelineOrder? = when(options[which]) {
                            resources.getString(R.string.sort_oldest_first) -> TimelineOrder.OLDEST_FIRST
                            resources.getString(R.string.sort_newest_first) -> TimelineOrder.NEWEST_FIRST
                            resources.getString(R.string.sort_curation_order) -> TimelineOrder.CURATION_ORDER
                            else -> null
                        }

                        order?.let {
                            Repository.setCollectionTimelineOrder(session, url, order).
                                observeOn(AndroidSchedulers.mainThread()).
                                subscribe { success: Boolean ->
                                    if (success) {
                                        refreshCollection()
                                    }
                                }
                        }
                    }.
                    show()
            }

            return true
        } else if (id == R.id.action_publish) {
            val url = collection_list_view.tag.toString()
            val intent = WebIntent("https://publish.twitter.com/?query=${url}")

            startActivity(intent)

            return true
        } else if (id == R.id.action_logout) {
            Twitter.getInstance().logOut()

            startActivity<HomeActivity>()
            overridePendingTransition(abc_fade_in, abc_fade_out)

            // Finish this activity so the back button doesn't return here.
            finish()

            return true
        }

        // Otherwise let the superclass handle the action.
        return super.onOptionsItemSelected(item)
    }
}
