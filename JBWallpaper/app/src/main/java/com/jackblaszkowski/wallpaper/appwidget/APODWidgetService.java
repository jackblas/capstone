package com.jackblaszkowski.wallpaper.appwidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.jackblaszkowski.wallpaper.R;
import com.jackblaszkowski.wallpaper.data.APODContract;
import com.jackblaszkowski.wallpaper.ui.DetailsActivity;
import com.jackblaszkowski.wallpaper.ui.DetailsFragment;
import com.jackblaszkowski.wallpaper.ui.MainActivity;
import com.jackblaszkowski.wallpaper.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.ParseException;


public class APODWidgetService extends IntentService {

    private static final String TAG = "APODWidgetService";
    private static final int TABLET_WIDTH = 600;

    private boolean isNewImage;

    public APODWidgetService() {
        super("APODWidgetService");
    }

    @Override

    protected void onHandleIntent(@Nullable Intent intent) {
        Log.v(TAG," In the onHandleIntent()");

        String imageTitle = "";
        String imageDate = "";
        String imageUrl = "";
        String formattedDate="";

        isNewImage=true;

        // Retrieve all of the widget ids:
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                APDOWidget.class));

        // Show progress bar
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_apod);
            views.setViewVisibility(R.id.widget_button, View.GONE);
            views.setViewVisibility(R.id.widget_spinner, View.VISIBLE);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        // Get cursor from the ContentProvider
        Uri imagesUri = APODContract.PictureEntry.buildLastItemUri();

        Cursor cursor = getContentResolver().query(
                imagesUri,
                DetailsFragment.IMAGE_ITEM_COLUMNS,
                null,
                null,
                null);

        if (cursor == null) {
            Log.d(TAG,"Cursor in null.");
        }
        if (cursor !=null && (!cursor.moveToFirst())) {
            Log.d(TAG,"Cursor is empty.");
        }

        if(cursor!=null && cursor.moveToFirst()) {

            // Check APOD media type:
            if((cursor.getString(DetailsFragment.COL_MEDIA_TYPE)).equals("image")) {

                // Get data from cursor:
                imageTitle = cursor.getString(DetailsFragment.COL_TITLE);
                imageDate = cursor.getString(DetailsFragment.COL_DATE);
                imageUrl = cursor.getString(DetailsFragment.COL_URL);

                // Format date
                try {
                    formattedDate = Utils.formatDate(imageDate);

                } catch (ParseException e) {
                    formattedDate="";
                    Log.e(TAG, "Error parsing date.",e);
                }

                Log.d(TAG,"Image Url:" + imageUrl);

            } else {
                Log.v(TAG, "No image today.");
                imageTitle=getString(R.string.app_widget_no_image);
                isNewImage=false;

            }

            // Update widgets:
            for (int appWidgetId : appWidgetIds) {

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_apod);
                //views.setViewVisibility(R.id.widget_spinner, View.VISIBLE);

                //if(cursor==null || (!cursor.moveToFirst())){
                //    Log.w(TAG,"Widget not updated. Connection problems.");
                //    continue;
                //}

                if(isNewImage) {

                    PendingIntent pendingIntent;

                    Configuration configuration = getApplicationContext().getResources().getConfiguration();
                    int deviceSmallestWidth = configuration.smallestScreenWidthDp;

                    if ((deviceSmallestWidth < TABLET_WIDTH)) {
                        // Create an Intent to launch Details
                        Intent detailsIntent = new Intent(getApplicationContext(), DetailsActivity.class);
                        detailsIntent.putExtra(DetailsFragment.ARG_ITEM_ID, imageDate);

                        // Create a fresh task and back stack.
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                        stackBuilder.addNextIntentWithParentStack(detailsIntent);
                        pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                    } else {
                        // Tablets
                        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                        pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, mainIntent, 0);

                    }

                    views.setTextViewText(R.id.widget_date, formattedDate);
                    //views.setTextViewText(R.id.widget_title, imageTitle);
                    // Attach an on-click listener to the image
                    views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

                    try {
                        Bitmap bitmap = Picasso.with(getApplicationContext())
                                .load(imageUrl)
                                .resize(180, 180).centerCrop().get();

                        views.setImageViewBitmap(R.id.widget_image, bitmap);

                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image.", e);
                    }

                }

                // If it is not an image, show message only and enable refresh button
                views.setTextViewText(R.id.widget_title, imageTitle);
                // Attach an on-click listener to the button
                views.setOnClickPendingIntent(R.id.widget_button, getRefreshIntent());

                // Hide progress bar
                views.setViewVisibility(R.id.widget_spinner, View.GONE);
                views.setViewVisibility(R.id.widget_button, View.VISIBLE);

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views);

            }

        } else { // cursor is null or empty
            Log.w(TAG,"Widget not updated. Connection problems.");

            for (int appWidgetId : appWidgetIds) {

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_apod);

                views.setTextViewText(R.id.widget_title, getString(R.string.app_widget_error));
                // Attach an on-click listener to the button
                views.setOnClickPendingIntent(R.id.widget_button, getRefreshIntent());

                // Hide progress bar
                views.setViewVisibility(R.id.widget_spinner, View.GONE);
                views.setViewVisibility(R.id.widget_button, View.VISIBLE);

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views);

            }
        }


        if (cursor != null) {
            cursor.close();
            return;
        }

    }

    private PendingIntent getRefreshIntent() {
        Intent intent = new Intent();
        intent.setAction(Utils.APPWIDGET_REFRESH);
        return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}
