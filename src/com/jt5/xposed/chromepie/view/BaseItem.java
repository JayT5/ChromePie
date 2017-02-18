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

/**
 * Pie menu item
 */
public class BaseItem {

    private final View mView;
    private final int level;
    private float start;
    private float sweep;
    private float animate;
    private int inner;
    private int outer;
    private boolean mSelected;
    private boolean mEnabled;
    private List<BaseItem> mItems;
    private final int mMenuActionId;
    private final String mId;

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

    protected boolean hasItems() {
        return mItems != null;
    }

    protected List<BaseItem> getItems() {
        return mItems;
    }

    public void addItem(BaseItem item) {
        if (mItems == null) {
            mItems = new ArrayList<>();
        }
        mItems.add(item);
    }

    public void setAlpha(float alpha) {
        final int alphaInt = Math.round(alpha * (mEnabled ? 255 : 77));
        if (mView != null) {
            ((ImageView) mView).setImageAlpha(alphaInt);
        }
    }

    void setAnimationAngle(float a) {
        animate = a;
    }

    public float getAnimationAngle() {
        return animate;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    protected boolean isEnabled() {
        return mEnabled;
    }

    public String getId() {
        return mId;
    }

    protected int getMenuActionId() {
        return mMenuActionId;
    }

    protected void setSelected(boolean s) {
        mSelected = s;
        if (mView != null) {
            mView.setSelected(s);
        }
    }

    boolean isSelected() {
        return mSelected;
    }

    protected int getLevel() {
        return level;
    }

    void setGeometry(float st, float sw, int inside, int outside) {
        start = st;
        sweep = sw;
        inner = inside;
        outer = outside;
    }

    float getStart() {
        return start;
    }

    protected float getStartAngle() {
        return start + animate;
    }

    float getSweep() {
        return sweep;
    }

    int getInnerRadius() {
        return inner;
    }

    int getOuterRadius() {
        return outer;
    }

    public View getView() {
        return mView;
    }

}
