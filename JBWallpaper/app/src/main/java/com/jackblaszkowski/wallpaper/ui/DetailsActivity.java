package com.jackblaszkowski.wallpaper.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.jackblaszkowski.wallpaper.R;


public class DetailsActivity extends AppCompatActivity implements ThumbnailsFragment.OnFragmentInteractionListener {
    private static final String LOG_TAG = "DetailsActivity";

    private boolean mServerErrorShown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_details);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Add Details fragment:
        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString(DetailsFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(DetailsFragment.ARG_ITEM_ID));

            DetailsFragment fragment = new DetailsFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.details_container, fragment)
                    .commit();
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mServerErrorShown) {
            menu.findItem(R.id.action_share).setEnabled(false);
            menu.findItem(R.id.action_set).setEnabled(false);
        }

        return true;
    }

    /**
     * OnFragmentInteractionListener interface method implementation
     */
    @Override
    public void onFragmentInteraction(String date, int position) {
        throw new UnsupportedOperationException("This functionality has not been implemented yet.");
    }

    /**
     * OnFragmentInteractionListener interface method implementation
     */
    @Override
    public void showServerErrorAlert() {

        if (!mServerErrorShown) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this, 0);
            alertBuilder.setTitle(R.string.server_error_alert_title);
            alertBuilder.setMessage(R.string.unable_to_parse_alert_message);
            alertBuilder.setPositiveButton(android.R.string.ok, null);
            alertBuilder.create().show();

            mServerErrorShown = true;
        }

    }

}
