package com.jackblaszkowski.wallpaper.ui;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.jackblaszkowski.wallpaper.R;
import com.jackblaszkowski.wallpaper.data.APODContract;
import com.jackblaszkowski.wallpaper.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;

public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";
    public static final String[] IMAGE_ITEM_COLUMNS = {
            APODContract.PictureEntry._ID,
            APODContract.PictureEntry.COLUMN_COPYRIGHT,
            APODContract.PictureEntry.COLUMN_DATE,
            APODContract.PictureEntry.COLUMN_EXPLANATION,
            APODContract.PictureEntry.COLUMN_HDURL,
            APODContract.PictureEntry.COLUMN_MEDIA_TYPE,
            APODContract.PictureEntry.COLUMN_TITLE,
            APODContract.PictureEntry.COLUMN_URL

    };
    // These indices are tied to IMAGE_COLUMNS.
    public static final int COL_ID = 0;
    public static final int COL_COPYRIGHT = 1;
    public static final int COL_DATE = 2;
    public static final int COL_EXPLANATION = 3;
    public static final int COL_HDURL = 4;
    public static final int COL_MEDIA_TYPE = 5;
    public static final int COL_TITLE = 6;
    public static final int COL_URL = 7;
    private static final String LOG_TAG = "DetailsFragment";
    // Class variables
    private static final int IMAGE_LOADER = 1;
    private static final String WEBSITE_BASE_URL = "https://apod.nasa.gov/apod/";
    // Instance variables
    private ThumbnailsFragment.OnFragmentInteractionListener mCallback;

    private String mImageDate;
    private View mRootView;
    private Cursor mCursor;
    private String mItemId;

    private String mImagePageUrl = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {

            mItemId = getArguments().getString(ARG_ITEM_ID);
            Log.d(LOG_TAG, "In onCreate()-mItemId: " + mItemId);

            mImagePageUrl = getImagePageUrl(mItemId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(LOG_TAG, "In onCreateView().");

        //Inflate layout for this fragment
        mRootView = inflater.inflate(R.layout.fragment_details, container, false);

        //bindViews();
        return mRootView;
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(IMAGE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ThumbnailsFragment.OnFragmentInteractionListener) {
            mCallback = (ThumbnailsFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_details, menu);

        // Set Share action:
        ShareActionProvider mShareActionProvider;
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mImagePageUrl);

        mShareActionProvider.setShareIntent(shareIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_set:
                setWallpaper();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = null;
        Uri imagesUri;

        if (mItemId != null) {
            imagesUri = APODContract.PictureEntry.buildItemUri(mItemId);
        } else {
            imagesUri = APODContract.PictureEntry.buildLastItemUri();
        }
        Log.v(LOG_TAG, "In onCreateLoader():: imagesUri =" + imagesUri.toString());

        return new CursorLoader(getActivity(),
                imagesUri,
                IMAGE_ITEM_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (!isAdded()) {
            if (data != null) {
                data.close();
            }
            return;
        }

        mCursor = data;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(LOG_TAG, "Error reading item detail cursor");

            mCallback.showServerErrorAlert();

            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mCursor = null;
        bindViews();
    }

    private void bindViews() {
        Log.d(LOG_TAG, "In onLoadFinished()-bindViews().");
        if (mRootView == null) {
            return;
        }
        //Find views
        TextView creditsView = mRootView.findViewById(R.id.details_credits);
        TextView titleView = mRootView.findViewById(R.id.details_title);
        TextView explanationView = mRootView.findViewById(R.id.details_explanation);
        View bylineView = mRootView.findViewById(R.id.details_byline);

        // Handheld:
        //ImageView toolbarImage = (ImageView) mRootView.findViewById(R.id.toolbar_image);
        Activity activity = this.getActivity();
        CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
        ImageView toolbarImage = activity.findViewById(R.id.toolbar_image);
        CoordinatorLayout coordinatorLayout = activity.findViewById(R.id.details_layout);

        // Tablet:
        FrameLayout frameLayout = activity.findViewById(R.id.details_image_frame);
        TextView dateView = mRootView.findViewById(R.id.details_date);
        ImageView imageView = mRootView.findViewById(R.id.details_image);

        //Bind views
        if (mCursor != null) {
            // Format date
            try {

                mImageDate = Utils.formatDate(mCursor.getString(COL_DATE));

            } catch (ParseException e) {
                mImageDate = "";
                Log.e(LOG_TAG, "Error parsing date.", e);
            }

            // Title:
            titleView.setText(mCursor.getString(COL_TITLE));

            // Credits:
            if (mCursor.getString(COL_COPYRIGHT) == null) {
                bylineView.setVisibility(View.GONE);
            } else {
                creditsView.setText(mCursor.getString(COL_COPYRIGHT));
                bylineView.setVisibility(View.VISIBLE);
            }
            // Explanation:
            explanationView.setText(mCursor.getString(COL_EXPLANATION));

            // Handheld layout
            if (coordinatorLayout != null) {

                // Image
                if (toolbarImage != null) {
                    //Glide.with(getContext()).load(mCursor.getString(COL_URL)).thumbnail(0.1f).into(toolbarImage);

                    Picasso.with(getContext()).load(mCursor.getString(COL_URL)).into(toolbarImage);
                }

                // Date
                if (appBarLayout != null) {
                    //mImageTitle=mCursor.getString(COL_TITLE);
                    appBarLayout.setTitle(mImageDate);
                }


                // FAB
                FloatingActionButton fab = coordinatorLayout.findViewById(R.id.fab);

                if (fab != null) {
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (!(Utils.isOnline(getActivity()))) {
                                // Show alert
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                                alertBuilder.setTitle(R.string.no_connection_alert_title);
                                alertBuilder.setMessage(R.string.no_connection_alert_message_three);
                                alertBuilder.setPositiveButton(android.R.string.ok, null);
                                alertBuilder.create().show();
                            } else {

                                Bundle arguments = new Bundle();
                                arguments.putString(FullScreenFragment.ARG_URL, mCursor.getString(COL_HDURL));
                                arguments.putString(FullScreenFragment.ARG_TITLE, mCursor.getString(COL_TITLE));
                                arguments.putString(FullScreenFragment.ARG_DATE, mImageDate);
                                FullScreenFragment fullScreenFragment = new FullScreenFragment();
                                fullScreenFragment.setArguments(arguments);

                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.details_container, fullScreenFragment)
                                        .addToBackStack(null)
                                        .commit();

                            }

                        }
                    });
                }
            }


            // Tablet layout
            if (frameLayout != null) {

                dateView.setText(mImageDate);
                Picasso.with(getContext()).load(mCursor.getString(COL_URL))
                        .resize(600, 0)
                        .into(imageView);

                // FAB
                FloatingActionButton fab = frameLayout.findViewById(R.id.fab);

                if (fab != null) {
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (!(Utils.isOnline(getActivity()))) {
                                // Show alert
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                                alertBuilder.setTitle(R.string.no_connection_alert_title);
                                alertBuilder.setMessage(R.string.no_connection_alert_message_three);
                                alertBuilder.setPositiveButton(android.R.string.ok, null);
                                alertBuilder.create().show();
                            } else {
                                Bundle arguments = new Bundle();
                                arguments.putString(FullScreenFragment.ARG_URL, mCursor.getString(COL_HDURL));
                                arguments.putString(FullScreenFragment.ARG_TITLE, mCursor.getString(COL_TITLE));
                                arguments.putString(FullScreenFragment.ARG_DATE, mImageDate);
                                FullScreenFragment fullScreenFragment = new FullScreenFragment();
                                fullScreenFragment.setArguments(arguments);

                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.frameLayout, fullScreenFragment)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        }
                    });
                }
            }


        } else {  // mCursor = null
            mRootView.setVisibility(View.GONE);
        }

    }

    // Convert date string "YYYY-MM-DD" into html
    // page name - "apYYMMDD.html" and add to base Url
    // Used by Share intent
    private String getImagePageUrl(String date) {

        String imagePage = "ap" + date.substring(2, 4)
                + date.substring(5, 7)
                + date.substring(8, 10)
                + ".html";

        return WEBSITE_BASE_URL + imagePage;
    }

    private void setWallpaper() {

        if (!(Utils.isOnline(getActivity()))) {
            // Show alert
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(R.string.no_connection_alert_title);
            alertBuilder.setMessage(R.string.no_connection_alert_message_three);
            alertBuilder.setPositiveButton(android.R.string.ok, null);
            alertBuilder.create().show();
            return;
        }

        new SetWallpaperTask(getContext(), mCursor.getString(DetailsFragment.COL_HDURL), mCursor.getString(DetailsFragment.COL_URL))
                .execute();
    }

    private static class SetWallpaperTask extends AsyncTask<Void, Void, Integer> {
        private final String hdUrl;
        private final String url;
        private WeakReference<Context> contextRef;

        // only retain a weak reference to the context
        SetWallpaperTask(Context context, String primaryUrl, String secondryUrl) {
            contextRef = new WeakReference<>(context);
            hdUrl = primaryUrl;
            url = secondryUrl;
        }


        protected void onPreExecute() {
            Toast.makeText(contextRef.get(), contextRef.get().getString(R.string.setting_wallpaper), Toast.LENGTH_LONG).show();
        }

        protected Integer doInBackground(Void... unused) {
            Integer status = 0;

            Configuration configuration = contextRef.get().getResources().getConfiguration();
            int deviceSmallestWidth = configuration.smallestScreenWidthDp;

            float widthInpx = Utils.convertDpToPixel(deviceSmallestWidth, contextRef.get());
            int imageHeight = (int) widthInpx;

            try {

                Bitmap bitmap = Picasso.with(contextRef.get())
                        .load(hdUrl).resize(0, imageHeight).get();

                WallpaperManager wallpaperManager = WallpaperManager.getInstance(contextRef.get());
                wallpaperManager.setBitmap(bitmap);

                status = Integer.valueOf(1);

            } catch (IOException e) {
                Log.w(LOG_TAG, "IOException getting HD image");
                Log.w(LOG_TAG, "HD image URL =" + hdUrl);
                try {

                    Bitmap bitmap = Picasso.with(contextRef.get())
                            .load(url).resize(0, imageHeight).get();

                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(contextRef.get());
                    wallpaperManager.setBitmap(bitmap);

                    status = Integer.valueOf(1);

                } catch (IOException e1) {
                    Log.e(LOG_TAG, "Error getting image for wallpaper", e1);
                }
            }
            return status;
        }

        protected void onPostExecute(Integer status) {
            if (status == 1) {
                Toast.makeText(contextRef.get(), contextRef.get().getString(R.string.wallpaper_set), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(contextRef.get(), contextRef.get().getString(R.string.wallpaper_error), Toast.LENGTH_SHORT).show();
            }

            //Log Firebase event:
            Bundle params = new Bundle();
            params.putString("image_url", url);
            FirebaseAnalytics.getInstance(contextRef.get()).logEvent("wallpaper_set_from_menu", params);
        }
    }


}
