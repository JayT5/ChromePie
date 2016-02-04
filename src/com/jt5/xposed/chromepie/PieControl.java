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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.jt5.xposed.chromepie.view.BaseItem;
import com.jt5.xposed.chromepie.view.PieMenu;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Controller for Quick Controls pie menu
 */
public class PieControl implements PieMenu.PieController {

    private final Activity mChromeActivity;
    private final XModuleResources mXResources;
    private PieMenu mPie;
    private final Controller mController;
    private final int mItemSize;
    private final XSharedPreferences mXPreferences;
    private static final String TAG = "ChromePie:PieControl: ";
    public static final int MAX_SLICES = 6;
    private static List<String> mNoTabActions;
    private static List<Integer> mTriggerPositions;
    private int mThemeColor = 0;
    private Unhook mFinishPageLoadHook;

    PieControl(Activity chromeActivity, XModuleResources res, XSharedPreferences prefs, ClassLoader classLoader) {
        mChromeActivity = chromeActivity;
        mController = new Controller(mChromeActivity, classLoader);
        mXResources = res;
        mXPreferences = prefs;
        mXPreferences.reload();
        mItemSize = mXResources.getDimensionPixelSize(R.dimen.qc_item_size);
        mNoTabActions = Arrays.asList("new_tab", "new_incognito_tab", "fullscreen",
                "settings", "exit", "go_to_home", "show_tabs", "recent_apps");
        mTriggerPositions = initTriggerPositions();
        if (!mController.isDocumentMode() && mXPreferences.getBoolean("toolbar_apply_theme_color", true)) {
            initializeUIHook();
        }
        applyFullscreen();
    }

    void attachToContainer(ViewGroup container) {
        if (mPie == null) {
            mPie = new PieMenu(mChromeActivity, mController, mXResources, mXPreferences);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            mPie.setLayoutParams(lp);
            populateMenu();
            mPie.setController(this);
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
        mPie = null;
        if (mFinishPageLoadHook != null) {
            mFinishPageLoadHook.unhook();
            mFinishPageLoadHook = null;
        }
    }

    private void applyFullscreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mChromeActivity);
        mController.setFullscreen(prefs.getBoolean("chromepie_apply_fullscreen", false));
    }

    private List<Integer> initTriggerPositions() {
        Set<String> triggerSet = mXPreferences.getStringSet("trigger_side_set",
                new HashSet<String>(Arrays.asList("0", "1", "2")));
        List<Integer> triggerInt = new ArrayList<Integer>();
        for (String trigger : triggerSet) {
            triggerInt.add(Integer.valueOf(trigger));
        }
        return triggerInt;
    }

    public static List<Integer> getTriggerPositions() {
        return mTriggerPositions;
    }

    @Override
    public boolean onOpen() {
        if (mController.getCurrentTab() == null) {
            if (mController.isDocumentMode()) {
                return false;
            }
        } else if (mFinishPageLoadHook == null) {
            hookFinishPageLoad();
        }

        if ((mController.isDocumentMode() || mXPreferences.getBoolean("toolbar_apply_theme_color", false))
                && mXPreferences.getBoolean("apply_theme_color", true)) {
            int color = mController.getThemeColor();
            if (mThemeColor != color) {
                mThemeColor = color;
                boolean useThemeColor = mController.shouldUseThemeColor(mThemeColor) &&
                        !(Color.red(mThemeColor) == Color.green(mThemeColor) &&
                          Color.green(mThemeColor) == Color.blue(mThemeColor) &&
                          Color.red(mThemeColor) > 230);
                if (useThemeColor) {
                    mPie.setThemeColors(color);
                } else {
                    mPie.setDefaultColors(mXResources);
                }
            }
        }

        final int tabCount = mController.getTabCount();
        final List<BaseItem> items = mPie.getItems();
        for (BaseItem item : items) {
            boolean shouldEnable = (tabCount != 0) || mNoTabActions.contains(item.getId());
            item.setEnabled(shouldEnable);
            if (!shouldEnable || item.getView() == null) {
                continue;
            }
            ((PieItem) item).onOpen(mController, mXResources);
        }
        return true;
    }

    private void populateMenu() {
        final List<String> values = Arrays.asList(mXResources.getStringArray(R.array.pie_item_values));
        final TypedArray drawables = mXResources.obtainTypedArray(R.array.pie_item_dark_drawables);
        final String[] actions = mXResources.getStringArray(R.array.pie_item_actions);
        mPie.clearItems();
        final Map<String, ?> keyMap = mXPreferences.getAll();
        if (keyMap.isEmpty()) {
            XposedBridge.log(TAG + "Failed to load preferences. Can read file: " + mXPreferences.getFile().canRead());
            return;
        }
        for (int i = 1; i <= MAX_SLICES; i++) {
            if (mXPreferences.getBoolean("screen_slice_" + i, false)) {
                String key = "slice_" + i + "_item_" + i;
                BaseItem item = initItem(values, drawables, actions, keyMap, key);
                mPie.addItem(item);
                for (int j = 1; j <= MAX_SLICES; j++) {
                    if (i == j) {
                        continue;
                    }
                    key = "slice_" + i + "_item_" + j;
                    item.addItem(initItem(values, drawables, actions, keyMap, key));
                }
            }
        }
        drawables.recycle();
    }

    private BaseItem initItem(List<String> values, TypedArray drawables, String[] actions, Map<String, ?> keyMap, String key) {
        String value = (String) keyMap.get(key);
        if (value != null && !value.equals("none")) {
            int index = values.indexOf(value);
            String action = actions[index];
            return makeItem(value, drawables.getResourceId(index, 0), mController.getResIdentifier(action));
        } else {
            return makeFiller();
        }
    }

    private BaseItem makeItem(String id, int iconRes, int action) {
        View view;
        if (id.equals("show_tabs")) {
            view = makeTabsView(iconRes);
        } else {
            view = new ImageView(mChromeActivity);
            ((ImageView) view).setImageDrawable(mXResources.getDrawable(iconRes));
            view.setMinimumWidth(mItemSize);
            view.setMinimumHeight(mItemSize);
            ((ImageView) view).setScaleType(ScaleType.CENTER);
            view.setLayoutParams(new LayoutParams(mItemSize, mItemSize));
        }
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
        LayoutInflater li = mChromeActivity.getLayoutInflater();
        ViewGroup view = (ViewGroup) li.inflate(mXResources.getLayout(R.layout.qc_tabs_view), null);
        TextView count = (TextView) view.getChildAt(1);
        count.setBackground(mXResources.getDrawable(R.drawable.tab_nr));
        count.setText(Integer.toString(mController.getTabCount()));
        ImageView icon = (ImageView) view.getChildAt(0);
        icon.setImageDrawable(mXResources.getDrawable(iconRes));
        icon.setScaleType(ScaleType.CENTER);
        view.setLayoutParams(new LayoutParams(mItemSize, mItemSize));
        return view;
    }

    private void initializeUIHook() {
        try {
            XposedHelpers.findAndHookMethod(mChromeActivity.getClass(), "initializeUI", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    initColorHooks();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
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
                    mController.getCurrentTab().getClass(), "didFinishPageLoad"), pageLoadHook);
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }

    private void initColorHooks() {
        final Object webContentsObserver;
        final Object toolbarModel;
        final Object tabModelSelector;
        if (mController.getThemeColor() == 0) {
            return;
        }

        try {
            webContentsObserver = XposedHelpers.getObjectField(mController.getCurrentTab(), "mWebContentsObserver");
            tabModelSelector = XposedHelpers.getObjectField(mChromeActivity, "mTabModelSelectorImpl");
            toolbarModel = XposedHelpers.getObjectField(mController.getToolbarManager(), "mToolbarModel");
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
            return;
        }

        try {
            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(webContentsObserver.getClass(), "didChangeThemeColor", int.class), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mController.applyThemeColors();
                }
            });

            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(webContentsObserver.getClass(), "didNavigateMainFrame", String.class, String.class,
                    boolean.class, boolean.class, int.class), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (!(Boolean) param.args[2] || (mController.getCurrentTab() != null && mController.isDistilledPage())) {
                        mController.applyThemeColors();
                    }
                }
            });

            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(tabModelSelector.getClass(), "notifyChanged"), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mController.applyThemeColors();
                }
            });

            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(toolbarModel.getClass(), "setPrimaryColor", int.class), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (!param.args[0].equals(mController.getThemeColor())) {
                        param.setResult(null);
                    }
                }
            });

            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(toolbarModel.getClass(), "isUsingBrandColor"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    param.setResult(mController.shouldUseThemeColor(mController.getThemeColor()));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }

}
