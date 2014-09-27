/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jt5.xposed.chromepie.view;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jt5.xposed.chromepie.view.PieMenu.PieView;

/**
 * Pie menu item
 */
public class PieItem {

    private final View mView;
    private PieView mPieView;
    private final int level;
    private float start;
    private float sweep;
    private float animate;
    private float mAlpha;
    private int inner;
    private int outer;
    private boolean mSelected;
    private boolean mEnabled;
    private List<PieItem> mItems;
    private String mAction;
    private String mId;

    public PieItem(View view, String id, String action, int level) {
        mView = view;
        mId = id;
        mAction = action;
        this.level = level;
        mEnabled = true;
    }

    public PieItem(View view, int level, PieView sym) {
        mView = view;
        this.level = level;
        mPieView = sym;
        mEnabled = false;
    }

    boolean hasItems() {
        return mItems != null;
    }

    public List<PieItem> getItems() {
        return mItems;
    }

    public void addItem(PieItem item) {
        if (mItems == null) {
            mItems = new ArrayList<PieItem>();
        }
        mItems.add(item);
    }

    @SuppressWarnings("deprecation")
    public void setAlpha(float alpha) {
        mAlpha = alpha;
        if (mView != null) {
            if (mId.equals("show_tabs") ) {
                final ImageView iv = (ImageView) ((ViewGroup) mView).getChildAt(0);
                final TextView tv = (TextView) ((ViewGroup) mView).getChildAt(1);
                final int alphaInt = Math.round(alpha * 255);
                iv.setAlpha(alphaInt);
                tv.setTextColor(Color.argb(alphaInt, 255, 255, 255));
                tv.getBackground().setAlpha(alphaInt);
            } else {
                ((ImageView) mView).setAlpha(Math.round(alpha * (mEnabled ? 255 : 77)));
            }
        }
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setAnimationAngle(float a) {
        animate = a;
    }

    public float getAnimationAngle() {
        return animate;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
    
    public boolean isEnabled() {
        return mEnabled;
    }

    public String getId() {
        return mId;
    }
    
    public String getAction() {
        return mAction;
    }

    public void setSelected(boolean s) {
        mSelected = s;
        if (mView != null) {
            mView.setSelected(s);
        }
    }

    public boolean isSelected() {
        return mSelected;
    }

    public int getLevel() {
        return level;
    }

    void setGeometry(float st, float sw, int inside, int outside) {
        start = st;
        sweep = sw;
        inner = inside;
        outer = outside;
    }

    public float getStart() {
        return start;
    }

    public float getStartAngle() {
        return start + animate;
    }

    public float getSweep() {
        return sweep;
    }

    public int getInnerRadius() {
        return inner;
    }

    public int getOuterRadius() {
        return outer;
    }

    public boolean isPieView() {
        return (mPieView != null);
    }

    public View getView() {
        return mView;
    }

    public void setPieView(PieView sym) {
        mPieView = sym;
    }

    public PieView getPieView() {
        if (mEnabled) {
            return mPieView;
        }
        return null;
    }

}
