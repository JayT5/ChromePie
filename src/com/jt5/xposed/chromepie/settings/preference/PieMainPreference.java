package com.jt5.xposed.chromepie.settings.preference;

import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.View;

import com.jt5.xposed.chromepie.PieControl;
import com.jt5.xposed.chromepie.R;

public class PieMainPreference extends Preference implements View.OnClickListener {

    private PreferenceCategory mPreferenceCategory;
    private int mSlice;
    private SharedPreferences mSharedPrefs;
    private Boolean mIsRemoved = false;
    private final Resources mResources;

    public PieMainPreference(Context context, int slice) {
        super(context);
        mSlice = slice;
        mResources = getContext().getResources();
        initialise();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View remover = view.findViewById(R.id.click_remove);
        remover.setOnClickListener(this);
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        preferenceManager.setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        mSharedPrefs = getContext().getSharedPreferences(
                preferenceManager.getSharedPreferencesName(), Context.MODE_WORLD_READABLE);
        persistBoolean(true);
        mPreferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pie_slices_cat");
        preferenceManager.findPreference("new_slice").setEnabled(
                mPreferenceCategory.getPreferenceCount() < PieControl.MAX_SLICES);
        updateSummary();
    }

    @Override
    public void onClick(View v) {
        if (!mIsRemoved) {
            mIsRemoved = true;
            v.setOnClickListener(null);
            mPreferenceCategory.removePreference(this);
        }
    }

    private void initialise() {
        setKey("screen_slice_" + mSlice);
        setTitle(mResources.getString(R.string.slice) + " " + mSlice);
        setSummary(mResources.getString(R.string.none));
        setOrder(mSlice);
        setWidgetLayoutResource(R.layout.mainpref_remove);
    }

    private void updateSummary() {
        List<String> values = Arrays.asList(mResources.getStringArray(R.array.pie_item_values));
        String value = mSharedPrefs.getString("slice_" + mSlice + "_item_" + mSlice, "none");
        int index = values.indexOf(value);
        if (index >= 0) {
            String[] entries = mResources.getStringArray(R.array.pie_item_entries);
            setSummary(entries[index]);
        }
    }

    public int getSlice() {
        return mSlice;
    }

    private void preferenceRemoved(int newSlice) {
        mSlice = newSlice;
        setKey("screen_slice_" + mSlice);
        setTitle(mResources.getString(R.string.slice) + " " + mSlice);
        setOrder(mSlice);
        updateSummary();
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        Editor editor = mSharedPrefs.edit();
        int count = mPreferenceCategory.getPreferenceCount();

        for (int i = mSlice; i < count; i++) {
            for (int j = 1; j <= PieControl.MAX_SLICES; j++) {
                editor.putString("slice_" + i + "_item_" + j,
                        mSharedPrefs.getString("slice_" + (i + 1) + "_item_" + j, "none"));
            }

            // apply changes now so that the summary can be updated
            editor.apply();
            PieMainPreference mainPref = (PieMainPreference) mPreferenceCategory.getPreference(i);
            mainPref.preferenceRemoved(i);
        }

        // remove final screen's preferences after they have been shifted
        for (int i = 1; i <= PieControl.MAX_SLICES; i++) {
            editor.remove("slice_" + count + "_item_" + i);
        }
        editor.putBoolean("screen_slice_" + count, false).apply();

        getPreferenceManager().findPreference("new_slice").setEnabled(
                mPreferenceCategory.getPreferenceCount() <= PieControl.MAX_SLICES);
    }

}
