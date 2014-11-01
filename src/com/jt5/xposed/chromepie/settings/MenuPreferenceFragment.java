package com.jt5.xposed.chromepie.settings;

import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import com.jt5.xposed.chromepie.PieControl;
import com.jt5.xposed.chromepie.R;
import com.jt5.xposed.chromepie.settings.preference.PieMainPreference;

public class MenuPreferenceFragment extends PreferenceFragment {

    private SharedPreferences mSharedPrefs;
    private PreferenceCategory mPieMenuCat;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.menu_preferences);
        mSharedPrefs = getActivity().getSharedPreferences(
                getPreferenceManager().getSharedPreferencesName(), Context.MODE_WORLD_READABLE);
        setHasOptionsMenu(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        mPieMenuCat = (PreferenceCategory) findPreference("pie_slices_cat");

        final Preference newSlice = findPreference("new_slice");
        newSlice.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mPieMenuCat.getPreferenceCount() < PieControl.MAX_SLICES) {
                    PieMainPreference mainPref = new PieMainPreference(getActivity(), (mPieMenuCat.getPreferenceCount() + 1));
                    mPieMenuCat.addPreference(mainPref);
                }
                return true;
            }
        });

        loadPreferences();

    }

    @SuppressWarnings("deprecation")
    private void loadDefaultValues(boolean readAgain) {
        PreferenceManager.setDefaultValues(getActivity(), getPreferenceManager().getSharedPreferencesName(),
                Context.MODE_WORLD_READABLE, R.xml.aosp_preferences, readAgain);

        if (readAgain) {
            Editor editor = mSharedPrefs.edit();
            for (int i = 1; i < PieControl.MAX_SLICES; i++) {
                editor.putBoolean("screen_slice_" + i, true);
            }
            editor.putBoolean("screen_slice_" + PieControl.MAX_SLICES, false).apply();
        }
    }

    private void loadPreferences() {
        Map<String, ?> keys = mSharedPrefs.getAll();
        for (Map.Entry<String,?> entry : keys.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("screen_") && (Boolean) entry.getValue()) {
                int slice = Character.getNumericValue(key.charAt(key.length() - 1));
                PieMainPreference mainPref = new PieMainPreference(getActivity(), slice);
                mPieMenuCat.addPreference(mainPref);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PieMainPreference) {
            Bundle extras = new Bundle();
            extras.putInt("slice", ((PieMainPreference) preference).getSlice());
            extras.putInt("count", mPieMenuCat.getPreferenceCount());
            ((PreferenceActivity) getActivity()).startWithFragment(preference.getFragment(), extras, this, 0);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            int slice = data.getIntExtra("slice", 1);
            String summary = data.getStringExtra("entry");
            PieMainPreference pref = (PieMainPreference) findPreference("screen_slice_" + slice);
            pref.setSummary(summary);
        }
        ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load_defaults:
                mPieMenuCat.removeAll();
                loadDefaultValues(true);
                loadPreferences();
                return true;
            case android.R.id.home:
                ((PreferenceActivity) getActivity()).finishPreferencePanel(this, Activity.RESULT_OK, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
