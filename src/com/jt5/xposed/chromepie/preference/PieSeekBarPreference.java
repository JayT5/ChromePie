package com.jt5.xposed.chromepie.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jt5.xposed.chromepie.R;

public class PieSeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private int mProgress;
    private int mOldValue;
    private int mMax;
    private int mMin;
    private boolean mTrackingTouch;
    private TextView mTextValue;

    public PieSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttributes(attrs);
        setDialogLayoutResource(R.layout.seekbar_preference);
    }

    private void loadAttributes(AttributeSet attrs) {
        mMin = attrs.getAttributeIntValue(null, "minimum", 0);
        int max = attrs.getAttributeIntValue(null, "maximum", 100) - mMin;
        setMax(max);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTextValue = (TextView) view.findViewById(R.id.seekbar_pref_value);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar_pref_seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(mMax);
        seekBar.setProgress(mProgress);
        mOldValue = mProgress + mMin;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
        setSummary((mProgress + mMin) + "dp");
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public void setMax(int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public void setProgress(int progress) {
        setProgress(progress - mMin, true);
    }

    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(mProgress + mMin);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return mProgress;
    }

    /**
     * Persist the seekBar's progress value if callChangeListener
     * returns true, otherwise set the seekBar's progress to the stored value
     */
    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) {
            setProgress(mOldValue);
        }
        setSummary((mProgress + mMin) + "dp");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !mTrackingTouch) {
            syncProgress(seekBar);
        }
        mTextValue.setText((progress + mMin) + "dp");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
    }

}
