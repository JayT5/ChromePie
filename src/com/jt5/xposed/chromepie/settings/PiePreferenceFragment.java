package com.jt5.xposed.chromepie.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import com.jt5.xposed.chromepie.R;

public class PiePreferenceFragment extends PreferenceFragment {

    private SharedPreferences mPrefs;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.main_preferences);
        mPrefs = getActivity().getSharedPreferences(
                getPreferenceManager().getSharedPreferencesName(), Context.MODE_WORLD_READABLE);

        // Committing a value to shared preferences ensures they are
        // world readable in case readability was reset somehow
        boolean readable = mPrefs.getBoolean("readable", true);
        mPrefs.edit().putBoolean("readable", !readable).commit();
        setHasOptionsMenu(true);

        // If SharedPreferences does not contain this preference,
        // we can presume this is a new install
        if (!mPrefs.contains("screen_slice_1")) {
            PreferenceManager.setDefaultValues(getActivity(), getPreferenceManager().getSharedPreferencesName(),
                    Context.MODE_WORLD_READABLE, R.xml.aosp_preferences, false);
        }

        final Preference killChrome = findPreference("kill_chrome");
        killChrome.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((PieSettings) getActivity()).killProcesses(mPrefs, true);
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
                        "com.jt5.xposed.chromepie.settings.PieSettings_Alias"), state, PackageManager.DONT_KILL_APP);
                return true;
            }
        });

        final Preference editMenu = findPreference("edit_pie_menu");
        editMenu.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((PreferenceActivity) getActivity()).startWithFragment(
                        MenuPreferenceFragment.class.getName(), null, null, Activity.RESULT_CANCELED);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionbar_kill:
                ((PieSettings) getActivity()).killProcesses(mPrefs, false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.menu_load_defaults);
    }

}
