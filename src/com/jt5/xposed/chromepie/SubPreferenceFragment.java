package com.jt5.xposed.chromepie;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.jt5.xposed.chromepie.preference.PieListPreference;

public class SubPreferenceFragment extends PreferenceFragment {

    private int mSlice;
    private int mCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_screen);
        Bundle extras = getArguments();
        mSlice = extras.getInt("slice");
        mCount = extras.getInt("count");
        setHasOptionsMenu(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle(getResources().getString(R.string.slice) + " " + mSlice);
        loadPreferences();
    }

    private void loadPreferences() {
        PreferenceScreen screen = getPreferenceScreen();
        // add main item first so that dependencies can be set
        PieListPreference listPref = new PieListPreference(getActivity(), mSlice, mSlice);
        screen.addPreference(listPref);

        for (int item = 1; item <= mCount; item++) {
            if (item == mSlice) {
                continue;
            }
            listPref = new PieListPreference(getActivity(), item, mSlice);
            screen.addPreference(listPref);
            listPref.setDependency("slice_" + mSlice + "_item_" + mSlice);
        }
    }

    void finishFragment() {
        Intent data = new Intent();
        data.putExtra("slice", mSlice);
        data.putExtra("entry", ((ListPreference) findPreference("slice_" + mSlice + "_item_" + mSlice)).getEntry());
        ((PreferenceActivity) getActivity()).finishPreferencePanel(this, Activity.RESULT_OK, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishFragment();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
