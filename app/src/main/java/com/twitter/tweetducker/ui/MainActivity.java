package com.twitter.tweetducker.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.jenzz.appstate.AppState;
import com.jenzz.appstate.RxAppState;
import com.squareup.picasso.Picasso;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.tweetui.CollectionTimeline;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;
import com.twitter.tweetducker.Analytics;
import com.twitter.tweetducker.R;
import com.twitter.tweetducker.TwitterAPI;
import com.twitter.tweetducker.model.CollectionsList;
import com.twitter.tweetducker.model.Timeline;
import com.twitter.tweetducker.model.User;
import com.twitter.tweetducker.rx.ObserverAdapter;

import java.util.concurrent.atomic.AtomicReference;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Analytics analytics;
    private Subscription appStateSubscription;
    private Subscription collectionsListSubscription;
    private final AtomicReference<CollectionsList> collectionsListReference = new AtomicReference<>();
    private final AtomicReference<Timeline> currentTimeline = new AtomicReference<>();

    private void checkLoggedIn() {
        TwitterSession session = Twitter.getInstance().core.getSessionManager().getActiveSession();
        if (session == null) {
            analytics.notLoggedIn();

            // Start the login activity.
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

            // Finish this activity so the back button doesn't return here.
            finish();
        } else {
            analytics.loggedIn(session);
        }
    }

    private void setCollection(@NonNull Timeline timeline) {
        if (timeline != null) {
            Long id = timeline.getId();

            if (id != null) {
                // Set the toolbar title.
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                toolbar.setTitle(timeline.name);

                // Set the timeline in the main content view.
                ListView view = (ListView) findViewById(R.id.collection);

                CollectionTimeline collection = new CollectionTimeline.Builder()
                        .id(id)
                        .build();

                TweetTimelineListAdapter adapter = new TweetTimelineListAdapter.Builder(this)
                        .setTimeline(collection)
                        .setViewStyle(R.style.tw__TweetLightWithActionsStyle)
                        .build();

                view.setAdapter(adapter);

                currentTimeline.set(timeline);
                analytics.timelineImpression(timeline);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        analytics = new Analytics(Answers.getInstance());

        checkLoggedIn();

        TwitterSession session = Twitter.getInstance().core.getSessionManager().getActiveSession();
        if (session != null) {
            // Why can the session be null here if checkLoggedIn finish()-ed?

            // Organise the UI.
            setContentView(R.layout.activity_main);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });

            // Hide the floating action bar until it has a use.
            fab.hide();

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            // Subscribe for application background and foreground actions.
            appStateSubscription = RxAppState.monitor(getApplication())
                    .subscribe(new ObserverAdapter<AppState>() {
                        @Override
                        public void onNext(AppState state) {
                            switch (state) {
                                case FOREGROUND:
                                    analytics.foreground();
                                    break;

                                case BACKGROUND:
                                    analytics.background();
                                    break;
                            }
                        }
                    });

            // Start listening to Twitter API responses on the UI thread to apply updates.
            TwitterAPI api = new TwitterAPI(session);

            View header = navigationView.getHeaderView(0);
            final TextView screenName = (TextView) header.findViewById(R.id.screen_name);
            final TextView name = (TextView) header.findViewById(R.id.name);
            final ImageView avatar = (ImageView) header.findViewById(R.id.avatar);

            final Menu menu = navigationView.getMenu();

            final Context context = getApplicationContext();
            collectionsListSubscription = api.getCollectionsListObservable()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ObserverAdapter<CollectionsList>() {
                        public void onNext(CollectionsList collectionsList) {
                            // Update the user details in the navigation drawer.
                            User user = collectionsList.user;

                            String screenNameText = "@" + user.screenName;
                            if (user.screenName == null) {
                                screenNameText = "";
                            }

                            String nameText = user.name;
                            if (nameText == null) {
                                nameText = "";
                            }

                            screenName.setText(screenNameText);
                            name.setText(nameText);

                            Picasso.with(context)
                                    .load(user.getAvatarUrl())
                                    .resizeDimen(R.dimen.avatar_width, R.dimen.avatar_height)
                                    .transform(new RoundedCornersTransformation(10, 0))
                                    .into(avatar);

                            // Update the collections list in the navigation drawer.
                            menu.clear();
                            for (Timeline timeline : collectionsList.timelines) {
                                menu.add(timeline.name);
                            }

                            // Keep the collections list for handling onNavigationItemSelected.
                            collectionsListReference.set(collectionsList);

                            // Display the first collection if none visible.
                            if (currentTimeline.get() == null && !collectionsList.timelines.isEmpty()) {
                                setCollection(collectionsList.timelines.get(0));
                            }
                        }
                    });

            // Trigger API data fetch to provide or update cached data.
            api.refreshCollectionsList();
        }
    }

    @Override
    protected void onDestroy() {
        // Don't leak the Rx subscriptions.
        if (appStateSubscription != null) {
            appStateSubscription.unsubscribe();
        }
        if (collectionsListSubscription != null) {
            collectionsListSubscription.unsubscribe();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        String title = item.getTitle().toString();

        CollectionsList collectionsList = collectionsListReference.get();
        if (collectionsList != null) {
            Timeline timeline = collectionsList.findTimelineByName(title);
            if (timeline != null) {
                setCollection(timeline);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }
}
