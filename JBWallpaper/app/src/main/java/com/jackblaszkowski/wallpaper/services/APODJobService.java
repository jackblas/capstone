package com.jackblaszkowski.wallpaper.services;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class APODJobService extends JobService {
    private static final String LOG_TAG = "APODJobService";

    private Intent intent;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.v(LOG_TAG, "In onStartJob() called");

        intent = new Intent(getApplicationContext(),APODService.class);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            getApplicationContext().startService(intent);

        } else {
            // New API 26 requirement:
            // Service must be started in the foreground
            // or it will not run when the app is in background
            getApplicationContext().startForegroundService(intent);
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (intent != null){
            getApplicationContext().stopService(intent);
        }
        return false;
    }
}
