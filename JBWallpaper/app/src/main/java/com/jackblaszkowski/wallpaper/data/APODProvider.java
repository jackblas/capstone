package com.jackblaszkowski.wallpaper.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jackblaszkowski.wallpaper.remote.APODApi;
import com.jackblaszkowski.wallpaper.utils.Utils;


public class APODProvider extends ContentProvider {

    private static final String TAG = "APODProvider";

    // URI Matcher used by this content provider.
    private static final int IMAGE = 100;
    private static final int IMAGE_WITH_ID = 101;
    private static final int FROM_DATE = 201;
    private static final int DATE_RANGE = 202;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = APODContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, APODContract.PATH_IMAGE, IMAGE);
        matcher.addURI(authority, APODContract.PATH_IMAGE + "/*", IMAGE_WITH_ID);
        matcher.addURI(authority, APODContract.PATH_IMAGE + "/" + APODContract.PATH_START_DATE + "/*", FROM_DATE);
        matcher.addURI(authority, APODContract.PATH_IMAGE + "/*/*", DATE_RANGE);

        return matcher;
    }

    // INSTANCE VARIABLES:

    @Override
    public boolean onCreate() {

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        Cursor retCursor;

        if (!(Utils.isOnline(getContext()))) {
            Log.w(TAG, "Device Not Online!");
            retCursor = null;
            return retCursor;
        }

        switch (sUriMatcher.match(uri)) {
            // "apod/*"
            case IMAGE_WITH_ID: {
                // Get image by id/date
                String imageDate = APODContract.PictureEntry.getItemId(uri);

                selectionArgs = new String[]{imageDate};
                //selection=sImageDateSelection;

                // public static Cursor query(String path, String[] projection, String selection, String[] selectionArgs,String sortOrder);

                retCursor = new APODApi().query(APODContract.PATH_IMAGE,
                        projection,
                        null,
                        selectionArgs,
                        null);
                break;
            }
            // "apod"
            case IMAGE: {

                // Get last image
                retCursor = new APODApi().query(APODContract.PATH_IMAGE,
                        projection,
                        null,
                        null,
                        null);
                break;

            }
            // "apod/start_date/*
            case FROM_DATE: {
                // Get images in date range (provide start date only; assumed end day is today)
                String startDate = APODContract.PictureEntry.getItemId(uri);

                selectionArgs = new String[]{startDate};

                retCursor = new APODApi().query(APODContract.PATH_IMAGE,
                        projection,
                        APODContract.PATH_START_DATE,
                        selectionArgs,
                        null);

                break;
            }

            // "apod/*/*
            case DATE_RANGE: {
                // Get images in date range
                String startDate = APODContract.PictureEntry.getStartDate(uri);
                String endDate = APODContract.PictureEntry.getStartEnd(uri);

                selectionArgs = new String[]{startDate, endDate};

                retCursor = new APODApi().query(APODContract.PATH_IMAGE,
                        projection,
                        APODContract.PATH_START_DATE,
                        selectionArgs,
                        null);

                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        //Register to watch a content URI for changes:
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FROM_DATE:
            case DATE_RANGE:
                return APODContract.PictureEntry.CONTENT_TYPE;
            case IMAGE:
            case IMAGE_WITH_ID:
                return APODContract.PictureEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
