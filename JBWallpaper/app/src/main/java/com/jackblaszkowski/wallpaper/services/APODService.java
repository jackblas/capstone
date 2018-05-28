package com.jackblaszkowski.wallpaper.services;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.jackblaszkowski.wallpaper.R;
import com.jackblaszkowski.wallpaper.data.APODContract;
import com.jackblaszkowski.wallpaper.ui.DetailsActivity;
import com.jackblaszkowski.wallpaper.ui.DetailsFragment;
import com.jackblaszkowski.wallpaper.ui.MainActivity;
import com.jackblaszkowski.wallpaper.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;


public class APODService extends IntentService {
    //For testing: 15min
    //private static final long PERIOD = 15*60000L;
    public static final int JOB_ID = 1;
    private static final String TAG = "APODService";
    private static final int TABLET_WIDTH = 600;
    private static final String IMAGE_MEDIA = "image";
    private static final long DAILY = TimeUnit.DAYS.toMillis(1L);
    private static final long PERIOD = 12 * 60 * 60000L;
    private boolean mNewImage = false;
    private Bitmap mAPODImage;
    private String mAPODUrl;
    private String mAPODHDUrl;
    private String mAPODDate;
    private String mNotificationText;

    public APODService() {
        super(TAG);
    }

    public static void scheduleDailyTask(Context context) {
        Log.v(TAG, "In the scheduleDailyTask().");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If running on something older than API level 21

            // Enable BootReceiver
            //BootReceiver default - android:enabled="false".
            ComponentName receiver = new ComponentName(context, BootReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            // Set alarms
            setAlarmManager(context);
        } else {
            setJobScheduler(context);
        }
    }

    public static void cancelDailyTask(Context context) {
        Log.v(TAG, "In the cancelDailyTask().");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            // Disable BootReceiver
            ComponentName receiver = new ComponentName(context, BootReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            // Cancel alarms
            Intent intent = new Intent(context, APODService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

        } else {
            // Cancel JobService
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(JOB_ID);
        }
    }

    // Set Job Scheduler to schedule this service
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void setJobScheduler(Context context) {
        Log.v(TAG, "In the setJobScheduler().");

        ComponentName serviceName = new ComponentName(context, APODJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, serviceName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(PERIOD)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfo);

    }

    // Set Alarm Manager to schedule this service
    static void setAlarmManager(Context context) {
        Log.v(TAG, "In the setAlarmManager().");

        // Set the alarm to start at approximately 8:00 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 8);

        Intent intent = new Intent(context, APODService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

        //FOR TESTING ONLY:
        // Wake up the device to fire the alarm in 10 minutes, and every 20 minutes after that:
        //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
        //        SystemClock.elapsedRealtime() + (10*60000L),
        //        (20*60000L), pendingIntent);

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.v(TAG, "In the onHandleIntent().");

        // New API 26 requirement:
        // The app must call service's startForeground()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(100, new Notification());
        }

        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Uri imagesUri = APODContract.PictureEntry.buildLastItemUri();

        Cursor cursor = contentResolver.query(
                imagesUri,
                DetailsFragment.IMAGE_ITEM_COLUMNS,
                null,
                null,
                null);

        // Get preferences:
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean sendNotifications = preferences.getBoolean(getResources().getString(R.string.pref_notifications_key), false);
        Boolean setWallpaper = preferences.getBoolean(getResources().getString(R.string.pref_set_wallpaper_key), false);

        if (cursor == null) {
            Log.w(TAG, "Cursor is null. Wallpaper will not be set.");

            mNotificationText = getString(R.string.notification_no_connection);
            setWallpaper = false;

        } else if (cursor != null && !cursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor.  Wallpaper will not be set.");

            mNotificationText = getString(R.string.notification_server_error);
            setWallpaper = false;

            cursor.close();

        } else {

            //Get data from cursor
            if ((cursor.getString(DetailsFragment.COL_MEDIA_TYPE)).equals(IMAGE_MEDIA)) {
                mNewImage = true;
                // Get data from cursor:
                mNotificationText = cursor.getString(DetailsFragment.COL_TITLE);
                mAPODDate = cursor.getString(DetailsFragment.COL_DATE);
                mAPODHDUrl = cursor.getString(DetailsFragment.COL_HDURL);
                mAPODUrl = cursor.getString(DetailsFragment.COL_URL);

                try {

                    mAPODImage = Picasso.with(getApplicationContext())
                            .load(cursor.getString(DetailsFragment.COL_URL))
                            .resize(50, 50).centerCrop().get();

                } catch (IOException e) {
                    Log.e(TAG, "Error loading image.", e);
                }

            } else {
                mNotificationText = getString(R.string.notification_no_image);
            }

        }

        if (sendNotifications) {
            sendNotification(getApplicationContext());
        }

        if (mNewImage && setWallpaper) {
            setWallpaper(getApplicationContext());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }

    }

    private void sendNotification(Context context) {
        Log.v(TAG, "In the sendNotification().");

        Intent intent;
        PendingIntent pendingIntent;

        Configuration configuration = context.getResources().getConfiguration();
        int deviceSmallestWidth = configuration.smallestScreenWidthDp;

        if ((deviceSmallestWidth < TABLET_WIDTH) && (mNewImage)) {
            intent = new Intent(context, DetailsActivity.class);
            intent.putExtra(DetailsFragment.ARG_ITEM_ID, mAPODDate);

            // Create a fresh task and back stack.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(intent);

            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            // For tablets and handhelds if there is no new image
            // set action to Main Activity
            intent = new Intent(context, MainActivity.class);
            // use System.currentTimeMillis() to have a unique ID for the pending intent
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getString(R.string.channel_id))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(mNotificationText)
                .setSmallIcon(R.drawable.ic_stat_apod)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (mNewImage) {

            builder.setLargeIcon(mAPODImage);
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, builder.build());

    }

    private void setWallpaper(Context context) {
        Log.v(TAG, "In the setWallpaper().");

        Configuration configuration = getResources().getConfiguration();
        int deviceSmallestWidth = configuration.smallestScreenWidthDp;

        float widthInpx = Utils.convertDpToPixel(deviceSmallestWidth, context);
        int imageHeight = (int) widthInpx;

        try {

            Bitmap bitmap = Picasso.with(context)
                    .load(mAPODHDUrl).resize(0, imageHeight).get();

            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            wallpaperManager.setBitmap(bitmap);

        } catch (IOException e) {
            Log.w(TAG, "IOException getting HD image");
            Log.w(TAG, "HD image URL =" + mAPODHDUrl);

            // Try smaller image:
            try {

                Bitmap bitmap = Picasso.with(context)
                        .load(mAPODUrl).resize(0, imageHeight).get();

                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                wallpaperManager.setBitmap(bitmap);

            } catch (IOException e1) {
                Log.e(TAG, "Error getting image for wallpaper", e1);
            }
        }
    }
}
