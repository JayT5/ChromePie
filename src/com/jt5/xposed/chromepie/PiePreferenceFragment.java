package com.jt5.xposed.chromepie;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import com.jt5.xposed.chromepie.preference.PieMainPreference;

public class PiePreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private SharedPreferences mSharedPrefs;
    private PreferenceCategory pieSlicesCat;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.main_preferences);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!mSharedPrefs.contains("screen_slice_1")) {
            loadDefaultValues(false);
        }

        pieSlicesCat = (PreferenceCategory) findPreference("pie_slices_cat");
        final Preference killChrome = findPreference("kill_chrome");
        killChrome.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((PieSettings) getActivity()).killChrome(true);
                return true;
            }
        });

        final Preference newSlice = findPreference("new_slice");
        newSlice.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (pieSlicesCat.getPreferenceCount() < 6) {
                    PieMainPreference mainPref = new PieMainPreference(getActivity(), null, (pieSlicesCat.getPreferenceCount() + 1));
                    pieSlicesCat.addPreference(mainPref);
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
            for (int i = 1; i < 6; i++) {
                editor.putBoolean("screen_slice_" + i, true);
            }
            editor.putBoolean("screen_slice_6", false).apply();
        }
    }

    private void loadPreferences() {
        Map<String, ?> keys = mSharedPrefs.getAll();

        for (Map.Entry<String,?> entry : keys.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("screen_") && (Boolean) entry.getValue()) {
                int slice = Character.getNumericValue(key.charAt(key.length() - 1));
                PieMainPreference mainPref = new PieMainPreference(getActivity(), null, slice);
                pieSlicesCat.addPreference(mainPref);
            }
        }

        final ListPreference triggerSide = (ListPreference) findPreference("trigger_side");
        triggerSide.setSummary(triggerSide.getEntry());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load_defaults:
                pieSlicesCat.removeAll();
                loadDefaultValues(true);
                loadPreferences();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PieMainPreference) {
            Bundle extras = new Bundle();
            extras.putInt("slice", ((PieMainPreference) preference).getSlice());
            extras.putInt("count", pieSlicesCat.getPreferenceCount());
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
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key != null && key.equals("trigger_side")) {
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setSummary(pref.getEntry());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(null);
    }

}
