package com.jackblaszkowski.wallpaper.ui;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.jackblaszkowski.wallpaper.R;
import com.jackblaszkowski.wallpaper.utils.Utils;


/**
 * An activity representing a list of APOD images. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a Details Activity. On tablets, the activity presents
 * the list of items and item details side-by-side using two vertical panes.
 */

public class MainActivity extends AppCompatActivity implements ThumbnailsFragment.OnFragmentInteractionListener {
    // Class variables
    private static final String TAG = "MainActivity";

    // Instance variables
    private boolean mTwoPane;
    // This boolean is used in Master/Details layout to prevent duplicate Alerts
    private boolean mServerErrorShown=false;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getTitle());

        if (findViewById(R.id.details_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts
            mTwoPane = true;
            // Load default item (first) into the details panel
            if(savedInstanceState == null) {
                DetailsFragment fragment = new DetailsFragment();
                //fragment.setArguments(arguments);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.details_container, fragment)
                        .commit();
            }
        }

        // Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel:
        createNotificationChannel();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!(Utils.isOnline(this))) {
            // Show alert
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this, 0);
            //AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setTitle(R.string.no_connection_alert_title);
            alertBuilder.setMessage(R.string.no_connection_alert_message_three);
            alertBuilder.setPositiveButton(android.R.string.ok, null);
            alertBuilder.create().show();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_settings:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayout, new PrefsFragment())
                        .addToBackStack(null)
                        .commit();

                return  true;

            case android.R.id.home:

                FragmentManager fm = getSupportFragmentManager();
                fm.popBackStack();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if(mServerErrorShown) {
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem setWall = menu.findItem(R.id.action_set);

            if(share != null) share.setEnabled(false);
            if(setWall != null) setWall.setEnabled(false);
        }

        return true;
    }

    /**
     * OnFragmentInteractionListener interface method implementation
     */
    @Override
    public void onFragmentInteraction(String itemId, int position) {

        if (mTwoPane) {
            mServerErrorShown=false; // reset the flag in case it has been shown
            Bundle arguments = new Bundle();
            arguments.putString(DetailsFragment.ARG_ITEM_ID, itemId);
            DetailsFragment fragment = new DetailsFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_container, fragment)
                    .commit();


        } else {
            Context context = this;
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra(DetailsFragment.ARG_ITEM_ID, itemId);

            context.startActivity(intent);
        }

    }

    /**
     * OnFragmentInteractionListener interface method implementation
     */
    @Override
    public void showServerErrorAlert() {

        if(!mServerErrorShown) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this, 0);
            //AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setTitle(R.string.server_error_alert_title);
            alertBuilder.setMessage(R.string.unable_to_parse_alert_message);
            alertBuilder.setPositiveButton(android.R.string.ok, null);
            alertBuilder.create().show();

            mServerErrorShown=true;
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+
        // The NotificationChannel class is new and not in the support library

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channelId = getString(R.string.channel_id);
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = 	getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
