package com.jackblaszkowski.wallpaper.remote;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.jackblaszkowski.wallpaper.BuildConfig;
import com.jackblaszkowski.wallpaper.data.APODContract;
import com.jackblaszkowski.wallpaper.ui.DetailsFragment;
import com.jackblaszkowski.wallpaper.ui.ThumbnailsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class APODApi {

    private static final String LOG_TAG = "APODApi";

    private static final String BASE_URL = "https://api.nasa.gov/planetary/";

    private static final int ITEM_PROJECTION_LENGHT = 8;
    private static final int DIR_PROJECTION_LENGHT = 5;

    // API params names:
    // If start_date is specified without an end_date then end_date defaults to the current date.
    private static final String API_KEY_URL_PARAM = "api_key";
    private static final String DATE_URL_PARAM = "date";
    private static final String START_DATE_URL_PARAM = "start_date";
    private static final String END_DATE_URL_PARAM = "end_date";

    //JSON keys:
    private static final String JSON_COPYRIGHT = "copyright";
    private static final String JSON_DATE = "date";
    private static final String JSON_EXPLANATION = "explanation";
    private static final String JSON_HDURL = "hdurl";
    private static final String JSON_MEDIA_TYPE = "media_type";
    private static final String JSON_SERVICE_VERSION = "service_version";
    private static final String JSON_TITLE = "title";
    private static final String JSON_URL = "url";

    private static final String IMAGE_MEDIA = "image";
    private boolean getLast = false;

    public APODApi() {
    }

    public Cursor query(String path, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.v(LOG_TAG, "In the query()");

        // Build Uri - path=apod
        Uri builtUri;
        getLast = false;

        Log.v(LOG_TAG, "In the query() getLast=" + getLast);

        if (selectionArgs != null) {

            if (selection != null && selection.equals(APODContract.PATH_START_DATE)) {

                // Get images for a date range. Path: apod/start_date/*
                // Today is implied end_date
                String startDate = selectionArgs[1];

                builtUri = Uri.parse(BASE_URL).buildUpon().appendPath(path)
                        .appendQueryParameter(API_KEY_URL_PARAM, BuildConfig.APOD_API_KEY)
                        .appendQueryParameter(START_DATE_URL_PARAM, startDate)
                        .build();


            } else {

                // Get images for a date range. Path: apod/*/*
                if (selectionArgs.length > 1) {

                    String startDate = selectionArgs[0];
                    String endDate = selectionArgs[1];

                    builtUri = Uri.parse(BASE_URL).buildUpon().appendPath(path)
                            .appendQueryParameter(API_KEY_URL_PARAM, BuildConfig.APOD_API_KEY)
                            .appendQueryParameter(START_DATE_URL_PARAM, startDate)
                            .appendQueryParameter(END_DATE_URL_PARAM, endDate)
                            .build();


                } else {
                    // Get image by date. Path: apod/*
                    String date = selectionArgs[0];

                    builtUri = Uri.parse(BASE_URL).buildUpon().appendPath(path)
                            .appendQueryParameter(API_KEY_URL_PARAM, BuildConfig.APOD_API_KEY)
                            .appendQueryParameter(DATE_URL_PARAM, date)
                            .build();

                }

            }


        } else {
            // No args? Get last image. Path: apod/
            getLast = true;
            builtUri = Uri.parse(BASE_URL).buildUpon().appendPath(path)
                    .appendQueryParameter(API_KEY_URL_PARAM, BuildConfig.APOD_API_KEY)
                    .build();
        }


        Cursor cursor = null;

        try {

            // Get JSON
            Log.d(LOG_TAG, "Built Uri = " + builtUri);

            String jsonString = getJSONOK(builtUri);

            //Log.v(LOG_TAG, "Returned Json string: " + jsonString);

            // Build a cursor
            cursor = jsonToCursor(jsonString, projection);

        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error - Invalid URL: ", e);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Connection problems or timeout: ", e);

        }

        return cursor;
    }


    private String getJSONOK(Uri uri) throws IOException {
        Log.v(LOG_TAG, "In the getJSONOK()");

        URL url = new URL(uri.toString());
        //OkHttpClient client = new OkHttpClient();

        // Solution for
        // java.net.SocketTimeoutException: timeout
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS);

        OkHttpClient client = builder.build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();

        long elapsedTime = response.sentRequestAtMillis() - response.receivedResponseAtMillis();

        Log.d(LOG_TAG, "Response:: X-RateLimit-Remaining =  " + response.header("X-RateLimit-Remaining", "Unknown"));
        Log.d(LOG_TAG, "Response time:  " + elapsedTime);

        return response.body().string();

    }

    private Cursor jsonToCursor(String jsonStr, String[] projection) {

        long rowIndex = 0L;
        JSONArray resultsArray;

        MatrixCursor matrixCursor = new MatrixCursor(projection);
        matrixCursor.moveToFirst();

        try {
            JSONTokener tokener = new JSONTokener(jsonStr);
            Object val = tokener.nextValue();
            if (!(val instanceof JSONArray)) {
                resultsArray = new JSONArray().put(val);
            } else {
                resultsArray = new JSONArray(jsonStr);
            }

            // APOD API returns older images first.
            // To show newest first, read array in reverse.
            boolean readReverse = true;
            for (int i = 0; i < resultsArray.length(); i++) {

                int index;
                if (readReverse) {
                    index = resultsArray.length() - (i + 1);
                } else {
                    index = i;
                }

                Object[] columnValues = new Object[projection.length];
                // Get the JSON object representing am APOD
                JSONObject apod = resultsArray.getJSONObject(index);

                // Add only images to the list.
                // If getting latest item only, get it regardless of the type,
                // it will be dealt with later.
                if (!getLast) {
                    if (!((apod.getString(JSON_MEDIA_TYPE)).equals(IMAGE_MEDIA))) {
                        continue;
                    }
                }

                columnValues[ThumbnailsFragment.COL_ID] = rowIndex++;

                if (projection.length == DIR_PROJECTION_LENGHT) {
                    // Cursor for dir
                    columnValues[ThumbnailsFragment.COL_DATE] = apod.getString(JSON_DATE);
                    columnValues[ThumbnailsFragment.COL_TYPE] = apod.getString(JSON_MEDIA_TYPE);
                    columnValues[ThumbnailsFragment.COL_TITLE] = apod.getString(JSON_TITLE);
                    columnValues[ThumbnailsFragment.COL_URL] = apod.getString(JSON_URL);

                } else {
                    // Cursor for item
                    if (apod.isNull(JSON_COPYRIGHT)) {
                        // For Json objects without copyright mapping
                        columnValues[DetailsFragment.COL_COPYRIGHT] = null;
                    } else {
                        columnValues[DetailsFragment.COL_COPYRIGHT] = apod.getString(JSON_COPYRIGHT);
                    }
                    columnValues[DetailsFragment.COL_DATE] = apod.getString(JSON_DATE);
                    columnValues[DetailsFragment.COL_EXPLANATION] = apod.getString(JSON_EXPLANATION);

                    // Some media types don't have hdurl
                    if (apod.isNull(JSON_HDURL)) {
                        columnValues[DetailsFragment.COL_HDURL] = null;
                    } else {
                        columnValues[DetailsFragment.COL_HDURL] = apod.getString(JSON_HDURL);
                    }

                    columnValues[DetailsFragment.COL_MEDIA_TYPE] = apod.getString(JSON_MEDIA_TYPE);
                    columnValues[DetailsFragment.COL_TITLE] = apod.getString(JSON_TITLE);
                    columnValues[DetailsFragment.COL_URL] = apod.getString(JSON_URL);

                }

                matrixCursor.addRow(columnValues);
            }
        } catch (JSONException e) {
            Log.d(LOG_TAG, "Error creating MatrixCursor.", e);
        }

        return matrixCursor;
    }
}
