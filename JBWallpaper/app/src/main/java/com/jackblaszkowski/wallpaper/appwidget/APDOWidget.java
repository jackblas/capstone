package com.jackblaszkowski.wallpaper.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jackblaszkowski.wallpaper.utils.Utils;


public class APDOWidget extends AppWidgetProvider {

    private static final String TAG = "APDOWidget";

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        if (Utils.APPWIDGET_REFRESH.equals(intent.getAction())) {
            //Start IntentService to refresh data
            context.startService(new Intent(context, APODWidgetService.class));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.v(TAG, " In the onUpdate");

        //Start IntentService to get data
        context.startService(new Intent(context, APODWidgetService.class));

    }

}
