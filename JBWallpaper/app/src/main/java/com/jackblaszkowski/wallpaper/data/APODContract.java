package com.jackblaszkowski.wallpaper.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public final class APODContract {

    //Content Provider constants:
    public static final String CONTENT_AUTHORITY = "com.jackblaszkowski.wallpaper";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //Content paths
    public static final String PATH_IMAGE = "apod";
    public static final String PATH_START_DATE = "start_date";

    public static final String APOD_DATE_PATTERN = "yyyy-MM-dd";

    // Private constructor to prevent someone from
    // accidentally instantiating the contract class,
    private APODContract() {
    }

    /* Inner class that defines APOD record */
    public static final class PictureEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_IMAGE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_IMAGE;

        public static final String TABLE_NAME = "apod";

        public static final String COLUMN_COPYRIGHT = "copyright";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_EXPLANATION = "explanation";
        public static final String COLUMN_HDURL = "hdurl";
        public static final String COLUMN_MEDIA_TYPE = "media_type";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_URL = "url";

        /** Matches: /apod/start_date/* */
        public static Uri buildDirUri(String date) {

            return CONTENT_URI.buildUpon().appendPath(PATH_START_DATE).appendPath(date).build();
        }

        // Matches: /apod/*/*
        public static Uri buildDirRangeUri(String startDate, String endDate) {

            return CONTENT_URI.buildUpon().appendPath(startDate).appendPath(endDate).build();
        }

        /** Matches: /apod/         */
        public static Uri buildLastItemUri() {
            return CONTENT_URI;
        }

        /** Matches: /apod/*     / Here we use date String as id */
        public static Uri buildItemUri(String _id) {
            return CONTENT_URI.buildUpon().appendPath(_id).build();
        }

        /** Read item ID item detail URI. */
        public static String getItemId(Uri itemUri) {
            return itemUri.getPathSegments().get(1);
        }

        public static String getStartDate(Uri itemUri) {
            return itemUri.getPathSegments().get(1);
        }
        public static String getStartEnd(Uri itemUri) {
            return itemUri.getPathSegments().get(2);
        }

    }


}
