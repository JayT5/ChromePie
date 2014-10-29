package com.jt5.xposed.chromepie;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;

public class PiePreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private SharedPreferences mSharedPrefs;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.main_preferences);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // If SharedPreferences does not contain this preference,
        // we can presume this is a new install
        if (!mSharedPrefs.contains("screen_slice_1")) {
            PreferenceManager.setDefaultValues(getActivity(), getPreferenceManager().getSharedPreferencesName(),
                    Context.MODE_WORLD_READABLE, R.xml.aosp_preferences, false);
        }

        final Preference killChrome = findPreference("kill_chrome");
        killChrome.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((PieSettings) getActivity()).killChrome(true);
                return true;
            }
        });

        final Preference hideIcon = findPreference("hide_launcher_icon");
        hideIcon.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int state = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                PackageManager pm = getActivity().getPackageManager();
                pm.setComponentEnabledSetting(new ComponentName(getActivity(),
                        "com.jt5.xposed.chromepie.PieSettings_Alias"), state, PackageManager.DONT_KILL_APP);
                return true;
            }
        });

        final ListPreference triggerSide = (ListPreference) findPreference("trigger_side");
        triggerSide.setSummary(triggerSide.getEntry());

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key != null && key.equals("trigger_side")) {
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setSummary(pref.getEntry());
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.menu_load_defaults);
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
