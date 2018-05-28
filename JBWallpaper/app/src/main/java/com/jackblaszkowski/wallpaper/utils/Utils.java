package com.jackblaszkowski.wallpaper.utils;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;

import com.jackblaszkowski.wallpaper.data.APODContract;
import com.jackblaszkowski.wallpaper.services.APODService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jackb on 5/7/2018.
 */

public class Utils {

    public static final String APPWIDGET_REFRESH = "com.jackblaszkowski.wallpaper.APPWIDGET_REFRESH";

    /**
     * This utility method checks to see whether a network connection is available
     * before the app attempts to connect to the network.
     */
    public static boolean isOnline(Context context) {

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();

    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isJobServiceOn(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == APODService.JOB_ID) {
                hasBeenScheduled = true;
                break;
            }
        }

        return hasBeenScheduled;
    }

    /**
     * This utility method converts dp to physical px
     */

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * Format Apod date string into display format
     */

    public static String formatDate(String dateIn)
            throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(APODContract.APOD_DATE_PATTERN);

        Date date = new Date();
        date = sdf.parse(dateIn);

        return DateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault()).format(date);
    }

}
