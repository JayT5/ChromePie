package com.jt5.xposed.chromepie.preference;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import com.jt5.xposed.chromepie.R;
import com.jt5.xposed.chromepie.SubPreferenceFragment;

public class PieMainPreference extends Preference implements View.OnClickListener {

    private PreferenceCategory mPreferenceCategory;
    private int mSlice;
    private SharedPreferences mSharedPrefs;
    private Boolean mIsRemoved = false;

    public PieMainPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieMainPreference(Context context, AttributeSet attrs, int slice) {
        super(context, attrs);
        mSlice = slice;
        initialise();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View remover = view.findViewById(R.id.click_remove);
        remover.setOnClickListener(this);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        mPreferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pie_slices_cat");
        preferenceManager.findPreference("new_slice").setEnabled(mPreferenceCategory.getPreferenceCount() < 6);
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
        setTitle("Slice " + mSlice);
        setSummary("None");
        setOrder(mSlice);
        setFragment(SubPreferenceFragment.class.getName());
        setWidgetLayoutResource(R.layout.mainpref_remove);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Editor editor = mSharedPrefs.edit();
        editor.putBoolean(getKey(), true).apply();
    }

    private void updateSummary() {
        String[] values = getContext().getResources().getStringArray(R.array.pie_item_values);
        String[] entries = getContext().getResources().getStringArray(R.array.pie_item_entries);
        String value = mSharedPrefs.getString("slice_" + mSlice + "_item_" + mSlice, "none");
        int i = Arrays.asList(values).indexOf(value);
        String summary = Arrays.asList(entries).get(i);
        setSummary(summary);
    }

    public int getSlice() {
        return mSlice;
    }

    private void preferenceRemoved(int newSlice) {
        mSlice = newSlice;
        setKey("screen_slice_" + mSlice);
        setTitle("Slice " + mSlice);
        setOrder(mSlice);
        updateSummary();
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        Editor editor = mSharedPrefs.edit();
        int count = mPreferenceCategory.getPreferenceCount();

        for (int i = mSlice; i < count; i++) {
            for (int j = 1; j < 7; j++) {
                editor.putString("slice_" + i + "_item_" + j, mSharedPrefs.getString("slice_" + (i + 1) + "_item_" + j, "none"));
            }

            // apply changes now so that the summary can be updated
            editor.apply();
            PieMainPreference mainPref = (PieMainPreference) mPreferenceCategory.getPreference(i);
            mainPref.preferenceRemoved(i);
        }

        // remove final screen's preferences after they have been shifted
        for (int i = 1; i < 7; i++) {
            editor.remove("slice_" + count + "_item_" + i);
        }
        editor.putBoolean("screen_slice_" + count, false).apply();

        getPreferenceManager().findPreference("new_slice").setEnabled(mPreferenceCategory.getPreferenceCount() < 7);
    }

}
