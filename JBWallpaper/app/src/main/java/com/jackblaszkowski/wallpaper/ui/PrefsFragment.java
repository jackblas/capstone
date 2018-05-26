package com.jackblaszkowski.wallpaper.ui;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.jackblaszkowski.wallpaper.R;
import com.jackblaszkowski.wallpaper.services.APODService;
import com.jackblaszkowski.wallpaper.utils.Utils;


public class PrefsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "PrefsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setHasOptionsMenu(true);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show the Up button in the action bar.
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.action_settings);

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);
        // Hide options menu in this fragment
        menu.setGroupVisible(R.id.menu_details_group,false);
        menu.setGroupVisible(R.id.menu_group,false);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // Hide the Up button in the action bar.
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(R.string.app_name);
        }
    }

    @Override
    public void onPause() {

        super.onPause();
        scheduleTasks();
    }

    private void scheduleTasks(){

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Boolean sendNotifications = preferences.getBoolean(getResources().getString(R.string.pref_notifications_key),false);
        Boolean setWallpaper = preferences.getBoolean(getResources().getString(R.string.pref_set_wallpaper_key),false);

        // Schedule daily task:
        if (!sendNotifications & !setWallpaper) {
            // If both are false, disable scheduling
            APODService.cancelDailyTask(getContext());

        } else {

            // Check JobScheduler for pending jobs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!(Utils.isJobServiceOn(getContext()))) {
                    APODService.scheduleDailyTask(getContext());
                }
            } else {
                // Check not really needed for AlarmManager since it will override with the same time anyway
                APODService.scheduleDailyTask(getContext());
            }

        }

    }
}
