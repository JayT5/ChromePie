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

import android.view.View;
import android.widget.ImageView;

import com.jt5.xposed.chromepie.view.PieMenu.PieView;

/**
 * Pie menu item
 */
public class BaseItem {

    private final View mView;
    private PieView mPieView;
    private final int level;
    private float start;
    private float sweep;
    private float animate;
    private int inner;
    private int outer;
    private boolean mSelected;
    private boolean mEnabled;
    private List<BaseItem> mItems;
    private int mMenuActionId;
    private String mId;

    public BaseItem(View view, String id) {
        this(view, id, 0);
    }

    protected BaseItem(View view, String id, int action) {
        this(view, id, action, 1);
    }

    private BaseItem(View view, String id, int action, int level) {
        mView = view;
        mId = id;
        mMenuActionId = action;
        this.level = level;
        mEnabled = true;
    }

    public BaseItem(View view, int level, PieView sym) {
        mView = view;
        this.level = level;
        mPieView = sym;
        mEnabled = false;
    }

    public boolean hasItems() {
        return mItems != null;
    }

    public List<BaseItem> getItems() {
        return mItems;
    }

    public void addItem(BaseItem item) {
        if (mItems == null) {
            mItems = new ArrayList<BaseItem>();
        }
        mItems.add(item);
    }

    @SuppressWarnings("deprecation")
    public void setAlpha(float alpha) {
        final int alphaInt = Math.round(alpha * (mEnabled ? 255 : 77));
        if (mView != null) {
            ((ImageView) mView).setAlpha(alphaInt);
        }
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

    public int getMenuActionId() {
        return mMenuActionId;
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
