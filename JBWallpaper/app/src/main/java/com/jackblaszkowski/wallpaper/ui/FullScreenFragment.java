package com.jackblaszkowski.wallpaper.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jackblaszkowski.wallpaper.R;
import com.squareup.picasso.Picasso;


public class FullScreenFragment extends Fragment {

    public static final String ARG_URL = "item_url";
    public static final String ARG_TITLE = "item_title";
    public static final String ARG_DATE = "state_date";

    private String mUrl;
    private String mTitle;
    private String mDate;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null && getArguments().containsKey(ARG_URL)) {

            mUrl = getArguments().getString(ARG_URL);
            mDate = getArguments().getString(ARG_DATE);
            mTitle = getArguments().getString(ARG_TITLE);
        }


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Inflate layout for this fragment
        View mRootView = inflater.inflate(R.layout.fragment_full, container, false);

        ImageView imageView = mRootView.findViewById(R.id.full_image);

        Picasso.with(getContext()).load(mUrl).into(imageView);

        TextView titleView = mRootView.findViewById(R.id.full_title);
        titleView.setText(mTitle);

        return mRootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setView();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        reSetView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);
        // Hide options menu in this fragment
        menu.setGroupVisible(R.id.menu_details_group, false);
        menu.setGroupVisible(R.id.menu_group, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back to Details
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setView() {

        CoordinatorLayout coordinatorLayout = (getActivity()).findViewById(R.id.details_layout);

        if (coordinatorLayout != null) {
            // Handheld layout

            AppBarLayout appBarLayout = getActivity().findViewById(R.id.details_app_bar);
            if (appBarLayout != null) {
                appBarLayout.setExpanded(false);
            }

            CollapsingToolbarLayout toolbarLayout = getActivity().findViewById(R.id.toolbar_layout);
            toolbarLayout.setTitle(mDate);

            FloatingActionButton fab = coordinatorLayout.findViewById(R.id.fab);
            fab.setVisibility(View.INVISIBLE);

        } else {
            // Tablet
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(getString(R.string.app_name) + " - " + mDate);
            }

        }
    }


    private void reSetView() {

        CoordinatorLayout coordinatorLayout = (getActivity()).findViewById(R.id.details_layout);

        if (coordinatorLayout != null) {
            // Handheld
            AppBarLayout appBarLayout = getActivity().findViewById(R.id.details_app_bar);

            if (appBarLayout != null) {
                // Handheld layout
                appBarLayout.setExpanded(true);
            }

            FloatingActionButton fab = coordinatorLayout.findViewById(R.id.fab);
            fab.setVisibility(View.VISIBLE);

        } else {
            // Tablet
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(R.string.app_name);
            }
        }
    }

}
