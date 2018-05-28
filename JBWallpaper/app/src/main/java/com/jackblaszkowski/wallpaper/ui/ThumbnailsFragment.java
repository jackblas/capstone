package com.jackblaszkowski.wallpaper.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jackblaszkowski.wallpaper.R;
import com.jackblaszkowski.wallpaper.data.APODContract;
import com.jackblaszkowski.wallpaper.utils.Utils;
import com.jackblaszkowski.wallpaper.widget.EndlessRecyclerViewScrollListener;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by jackb on 4/6/2018.
 */

public class ThumbnailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // These indices are tied to IMAGE_COLUMNS.
    public static final int COL_ID = 0;
    public static final int COL_DATE = 1;
    public static final int COL_TYPE = 2;
    public static final int COL_TITLE = 3;
    public static final int COL_URL = 4;
    private static final String LOG_TAG = "ThumbnailsFragment";
    // Class variables
    private static final int IMAGE_LOADER = 0;
    private static final int IMAGES_LOADED = 40;
    private static final int SPAN_COUNT = 2;
    private static final String[] IMAGE_DIR_COLUMNS = {
            APODContract.PictureEntry._ID,
            APODContract.PictureEntry.COLUMN_DATE,
            APODContract.PictureEntry.COLUMN_MEDIA_TYPE,
            APODContract.PictureEntry.COLUMN_TITLE,
            APODContract.PictureEntry.COLUMN_URL

    };
    // Instance variables
    //private Cursor mCursor;
    private RVAdapter rvAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private View rootView;
    private AdView mAdView;

    private String startDate;
    private int mPosition = -1;

    private OnFragmentInteractionListener mCallback;
    // Store a member variable for the listener
    private EndlessRecyclerViewScrollListener scrollListener;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get current date in format used by APOD Api
        SimpleDateFormat dateFormat = new SimpleDateFormat(APODContract.APOD_DATE_PATTERN);
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getDefault());
        now.add(Calendar.DATE, -IMAGES_LOADED);

        startDate = dateFormat.format(now.getTime());

        // Initialize adMob
        MobileAds.initialize(getActivity(), getString(R.string.banner_ad_unit_id));
        MobileAds.initialize(getActivity());

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        StaggeredGridLayoutManager sglm = null;

        //Inflate layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_list, container, false);

        // Set adapter, layout manager and scroll Listener for the recycler view:
        mRecyclerView = rootView.findViewById(R.id.my_recycler_view);
        rvAdapter = new RVAdapter(null);
        rvAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(rvAdapter);

        Configuration configuration = getResources().getConfiguration();
        int deviceSmallestWidth = configuration.smallestScreenWidthDp;

        if ((deviceSmallestWidth < 600)) {
            sglm = new StaggeredGridLayoutManager(SPAN_COUNT, StaggeredGridLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(sglm);
        } else {
            sglm = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(sglm);

        }

        // Retain an instance so that you can call `resetState()` for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(mRecyclerView.getLayoutManager()) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list

                Log.e(LOG_TAG, "onLoadMore()- page = " + page);
                Log.e(LOG_TAG, "onLoadMore()- totalItemsCount = " + totalItemsCount);

                loadNextDataFromApi(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        mRecyclerView.addOnScrollListener(scrollListener);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mCallback = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        // Pause the AdView.
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Destroy the AdView.
        mAdView.destroy();

        super.onDestroy();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Loader loader = getLoaderManager().initLoader(IMAGE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = null;
        Uri imagesUri = APODContract.PictureEntry.buildDirUri(startDate);

        return new CursorLoader(getActivity(),
                imagesUri,
                IMAGE_DIR_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(LOG_TAG, "In the onLoadFinished().");

        View intro = rootView.findViewById(R.id.intro);
        if (intro != null) {
            intro.setVisibility(View.GONE);
        }

        if (cursor != null) {

            if (cursor.getCount() < 1) {
                mCallback.showServerErrorAlert();
            }

            // Here:
            //RVAdapter adapter = new RVAdapter(null);
            rvAdapter.setCursor(cursor);
            mRecyclerView.swapAdapter(rvAdapter, false);


        } else {
            Log.d(LOG_TAG, "In the onLoadFinished(): cursor is null");
        }

        showAdd();

    }

    private void showAdd() {
        Log.d(LOG_TAG, "AdMob: in showAdd().");
        mAdView = getActivity().findViewById(R.id.adView);
        if (mAdView == null) {
            Log.d(LOG_TAG, "AdView is null.");
        }

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                //.addTestDevice(getString(R.string.your_physical_device_id))
                .build();

        mAdView.loadAd(adRequest);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);

    }


    private void loadNextDataFromApi(int offset) {
        Log.d(LOG_TAG, "In the loadNextDataFromApi()");
        Log.d(LOG_TAG, "Query startDate = " + startDate);

        SimpleDateFormat dateFormat = new SimpleDateFormat(APODContract.APOD_DATE_PATTERN);
        Calendar calendar = Calendar.getInstance();

        try {
            // Recalculate startDate to get more images
            calendar.setTime(dateFormat.parse(startDate));
            calendar.add(Calendar.DATE, -IMAGES_LOADED);

            startDate = dateFormat.format(calendar.getTime());

        } catch (ParseException e) {
            e.printStackTrace();
        }


        getLoaderManager().restartLoader(IMAGE_LOADER, null, this);

    }

    /**
     * This interface allows interaction between fragments
     * and the activity.
     */
    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(String id, int position);

        void showServerErrorAlert();

    }

    // RECYCLER VIEW ADAPTER CLASS

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.list_item_title);
            imageView = itemView.findViewById(R.id.list_item_image);

        }
    }

    private class RVAdapter extends RecyclerView.Adapter<ViewHolder> {

        //private ViewHolder holder;
        //private int position;
        private Cursor mCursor;
        private Context context;


        RVAdapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

            // create a new view
            context = parent.getContext();
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            // create a View Holder
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
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
                        //Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                        mCursor.moveToPosition(vh.getAdapterPosition());
                        String date = mCursor.getString(COL_DATE);

                        mCallback.onFragmentInteraction(date, vh.getAdapterPosition());

                        mPosition = vh.getAdapterPosition();
                    }

                }
            });


            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            mCursor.moveToPosition(position);

            holder.titleTextView.setText(mCursor.getString(COL_TITLE));

            Picasso.with(context).load(mCursor.getString(COL_URL)).resize(150, 0).into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            if (mCursor == null)
                return 0;
            return mCursor.getCount();
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(mCursor.getColumnIndex(APODContract.PictureEntry._ID));
        }

        void setCursor(Cursor cursor) {
            mCursor = cursor;
        }
    }

}
