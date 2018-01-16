/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jt5.xposed.chromepie;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.jt5.xposed.chromepie.view.BaseItem;
import com.jt5.xposed.chromepie.view.PieMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Controller for Quick Controls pie menu
 */
public class PieControl implements PieMenu.PieController {

    private static final String TAG = "ChromePie:PieControl: ";
    public static final int MAX_SLICES = 6;

    private static final List<String> NO_TAB_ACTIONS = Collections.unmodifiableList(
            Arrays.asList("new_tab", "new_incognito_tab", "fullscreen", "settings", "exit",
                    "go_to_home", "show_tabs", "recent_apps", "toggle_data_saver", "downloads",
                    "expand_notifications", "bookmarks", "history", "most_visited", "recent_tabs"));
    private static final List<String> CUSTOM_TAB_ACTIONS = Collections.unmodifiableList(
            Arrays.asList("back", "forward", "refresh", "close_tab", "add_bookmark", "add_to_home",
                    "find_in_page", "desktop_site", "fullscreen", "scroll_to_top", "show_tabs",
                    "scroll_to_bottom", "share", "direct_share", "print", "settings", "go_to_home",
                    "exit", "reader_mode", "recent_apps", "toggle_data_saver",
                    "expand_notifications"));

    private final Activity mActivity;
    private final ChromeHelper mHelper;
    private PieMenu mPie;
    private final Resources mXResources;
    private final XSharedPreferences mXPreferences;
    private Unhook mFinishPageLoadHook;
    private final List<PieMenu.Trigger> mEnabledTriggers;
    private final boolean mApplyThemeColor;
    private int mThemeColor;

    PieControl(Activity activity, Resources res, XSharedPreferences prefs, ClassLoader classLoader) {
        mActivity = activity;
        Utils.initialise(classLoader);
        if (Utils.isDocumentModeEnabled(mActivity, classLoader)) {
            mHelper = new ChromeDocumentHelper(mActivity, classLoader);
        } else {
            mHelper = new ChromeHelper(mActivity, classLoader);
        }
        mXResources = res;
        mXPreferences = prefs;
        Utils.reloadPreferences(mXPreferences);
        mEnabledTriggers = initTriggerPositions();
        mApplyThemeColor = mXPreferences.getBoolean("apply_theme_color", true);
        applyFullscreen();
    }

    void attachToContainer(ViewGroup container) {
        if (mPie == null) {
            mPie = new PieMenu(mActivity, mXResources, mXPreferences);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            mPie.setLayoutParams(lp);
            populateMenu();
            mPie.setController(this);
            mPie.setDefaultColors(mXResources);
        }
        container.addView(mPie);
    }

    private void removeFromParent() {
        if (mPie.getParent() != null) {
            ((ViewGroup) mPie.getParent()).removeView(mPie);
        }
    }

    void destroy() {
        removeFromParent();
        mPie.clearItems();
        mPie = null;
        if (mFinishPageLoadHook != null) {
            mFinishPageLoadHook.unhook();
            mFinishPageLoadHook = null;
        }
    }

    private void applyFullscreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean defValue = false;
        if (prefs.contains("chromepie_apply_fullscreen")) {
            defValue = prefs.getBoolean("chromepie_apply_fullscreen", false);
            prefs.edit().remove("chromepie_apply_fullscreen").apply();
        }
        mHelper.setFullscreen(mXPreferences.getBoolean("launch_in_fullscreen", defValue));
    }

    private List<PieMenu.Trigger> initTriggerPositions() {
        Set<String> triggerSet = mXPreferences.getStringSet("trigger_side_set",
                new HashSet<>(Arrays.asList("0", "1", "2")));
        List<PieMenu.Trigger> triggerList = new ArrayList<>();
        for (String trigger : triggerSet) {
            triggerList.add(PieMenu.Trigger.values()[Integer.valueOf(trigger)]);
        }
        return triggerList;
    }

    @Override
    public void onOpen() {
        if (mHelper.getCurrentTab() == null) {
            if (mHelper.isDocumentMode()) {
                return;
            }
        } else if (mFinishPageLoadHook == null) {
            hookFinishPageLoad();
        }

        if (mApplyThemeColor) {
            int color = mHelper.getThemeColor();
            if (mThemeColor != color) {
                mThemeColor = color;
                if (mHelper.shouldUseThemeColor(mThemeColor)) {
                    mPie.setThemeColors(color);
                } else {
                    mPie.setDefaultColors(mXResources);
                }
            }
        }

        final int tabCount = mHelper.getTabCount();
        final List<BaseItem> items = mPie.getItems();
        for (BaseItem item : items) {
            boolean shouldEnable = mHelper.isCustomTabs() ? CUSTOM_TAB_ACTIONS.contains(item.getId())
                    : tabCount != 0 || NO_TAB_ACTIONS.contains(item.getId());
            item.setEnabled(shouldEnable);
            if (!shouldEnable || item.getView() == null) {
                continue;
            }
            ((PieItem) item).onOpen(mHelper, mXResources);
        }
    }

    @Override
    public void onClick(PieItem item) {
        item.onClick(mHelper);
    }

    @Override
    public boolean shouldShowMenu(PieMenu.Trigger triggerPosition) {
        return mEnabledTriggers.contains(triggerPosition)
                && !mHelper.isInFullscreenVideo()
                && !mHelper.touchScrollInProgress()
                && (mHelper.isCustomTabs() || ((mHelper.isInOverview() == (mHelper.getTabCount() == 0))
                && (mHelper.getUrlBar() == null || !mHelper.getUrlBar().hasFocus())));
    }

    @Override
    public void requestTabFocus() {
        mHelper.requestTabFocus();
    }

    @Override
    public int getTopControlsHeight() {
        return mHelper.getTopControlsHeight();
    }

    private void populateMenu() {
        final List<String> values = Arrays.asList(mXResources.getStringArray(R.array.pie_item_values));
        final TypedArray drawables = mXResources.obtainTypedArray(R.array.pie_item_dark_drawables);
        final String[] actions = mXResources.getStringArray(R.array.pie_item_actions);
        mPie.clearItems();
        Map<String, ?> keyMap = mXPreferences.getAll();
        if (keyMap.isEmpty()) {
            XposedBridge.log(TAG + "Failed to load preferences, using default values");
            keyMap = Utils.createDefaultsMap(mXResources);
        }
        for (int i = 1; i <= MAX_SLICES; i++) {
            Boolean enabled = (Boolean) keyMap.get("screen_slice_" + i);
            if (enabled != null && enabled) {
                String value = (String) keyMap.get("slice_" + i + "_item_" + i);
                BaseItem item = initItem(values, drawables, actions, value);
                mPie.addItem(item);
                for (int j = 1; j <= MAX_SLICES; j++) {
                    if (i == j) {
                        continue;
                    }
                    value = (String) keyMap.get("slice_" + i + "_item_" + j);
                    item.addItem(initItem(values, drawables, actions, value));
                }
            }
        }
        drawables.recycle();
    }

    private BaseItem initItem(List<String> values, TypedArray drawables, String[] actions, String value) {
        if (value != null && !value.equals("none")) {
            int index = values.indexOf(value);
            if (index >= 0) {
                String action = actions[index];
                return makeItem(value, drawables.getResourceId(index, R.drawable.null_icon), mHelper.getResIdentifier(action));
            }
        }
        return makeFiller();
    }

    private BaseItem makeItem(String id, int iconRes, int action) {
        int itemSize = mXResources.getDimensionPixelSize(R.dimen.qc_item_size);
        View view;
        if (id.equals("show_tabs")) {
            view = makeTabsView(iconRes);
        } else {
            view = new ImageView(mActivity);
            ((ImageView) view).setImageDrawable(mXResources.getDrawable(iconRes));
            ((ImageView) view).setScaleType(ScaleType.CENTER);
            view.setMinimumWidth(itemSize);
            view.setMinimumHeight(itemSize);
        }
        view.setLayoutParams(new LayoutParams(itemSize, itemSize));
        try {
            return (PieItem) Class.forName(ChromePie.PACKAGE_NAME + ".Item_" + id)
                    .getConstructor(View.class, String.class, int.class).newInstance(view, id, action);
        } catch (Throwable t) {
            return new PieItem(view, id, action);
        }
    }

    private BaseItem makeFiller() {
        return new BaseItem(null, "");
    }

    private View makeTabsView(int iconRes) {
        View view = mActivity.getLayoutInflater().inflate(mXResources.getLayout(R.layout.qc_tabs_view), null);
        TextView count = (TextView) view.findViewById(R.id.count_label);
        count.setBackground(mXResources.getDrawable(R.drawable.tab_nr));
        count.setText(Integer.toString(mHelper.getTabCount()));
        ImageView icon = (ImageView) view.findViewById(R.id.count_icon);
        icon.setImageDrawable(mXResources.getDrawable(iconRes));
        icon.setScaleType(ScaleType.CENTER);
        return view;
    }

    private void hookFinishPageLoad() {
        XC_MethodHook pageLoadHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                List<PieItem> items = mPie.findItemsById("refresh");
                if (!items.isEmpty()) {
                    for (PieItem item : items) {
                        ((ImageView) item.getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_refresh_white));
                    }
                    mPie.invalidate();
                }
            }
        };

        try {
            mFinishPageLoadHook = XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(
                    mHelper.getCurrentTab().getClass(), "didFinishPageLoad"), pageLoadHook);
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }

}
