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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.jt5.xposed.chromepie.view.PieItem;
import com.jt5.xposed.chromepie.view.PieMenu;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Controller for Quick Controls pie menu
 */
public class PieControl implements PieMenu.PieController, OnClickListener {

    private final Activity mChromeActivity;
    private final XModuleResources mXResources;
    private PieMenu mPie;
    private final Controller mController;
    private final Map<String,Action> mActionMap = new HashMap<String,Action>();
    private final int mItemSize;
    private final XSharedPreferences mXPreferences;
    private XC_MethodHook mOnPageLoad;
    private static final String PACKAGE_NAME = PieControl.class.getPackage().getName();
    private static final String TAG = "ChromePie:PieControl: ";
    public static final int MAX_SLICES = 6;
    private static List<String> actionNoTab;

    PieControl(Activity chromeActivity, XModuleResources mModRes, ClassLoader classLoader) {
        mChromeActivity = chromeActivity;
        mController = new Controller(this, mChromeActivity, classLoader);
        mXResources = mModRes;
        mXPreferences = new XSharedPreferences(PACKAGE_NAME);
        mXPreferences.makeWorldReadable();
        mItemSize = (int) mXResources.getDimension(R.dimen.qc_item_size);
        actionNoTab = Arrays.asList("new_tab", "new_incognito_tab", "fullscreen", "settings", "exit");
    }

    protected void attachToContainer(FrameLayout container) {
        if (mPie == null) {
            mPie = new PieMenu(mChromeActivity, mXResources, mController);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            mPie.setLayoutParams(lp);
            populateMenu();
            mPie.setController(this);
        }
        container.addView(mPie);
    }

    protected void removeFromContainer(FrameLayout container) {
        container.removeView(mPie);
    }

    protected void forceToTop(FrameLayout container) {
        if (mPie.getParent() != null) {
            container.removeView(mPie);
            container.addView(mPie);
        }
    }

    XSharedPreferences getXPreferences() {
        return mXPreferences;
    }

    List<Integer> getTriggerSide() {
        Set<String> triggerSet = mXPreferences.getStringSet("trigger_side_set",
                new HashSet<String>(Arrays.asList("0", "1", "2")));
        List<Integer> triggerInt = new ArrayList<Integer>();
        for (String trigger : triggerSet) {
            triggerInt.add(Integer.valueOf(trigger));
        }
        return triggerInt;
    }

    @Override
    public boolean onOpen() {
        if (mOnPageLoad == null && mController.getCurrentTab() != null) {
            hookOnPageLoad();
        }
        final int tabCount = mController.getTabCount();
        final List<PieItem> items = mPie.getItems();
        for (PieItem item : items) {
            View icon = item.getView();
            String id = item.getId();
            boolean disableNoTabs = (tabCount == 0) && !actionNoTab.contains(id);
            item.setEnabled(!disableNoTabs);
            if (id.equals("show_tabs")) {
                //icon = icon.findViewById(R.id.count_label);
                icon = ((ViewGroup) icon).getChildAt(1);
                ((TextView) icon).setText(Integer.toString(tabCount));
            }
            if (icon == null || disableNoTabs) {
                continue;
            }
            if (id.equals("desktop_site")) {
                if (mController.isDesktopUserAgent()) {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_mobile_site));
                } else {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_desktop_site));
                }
            } else if (id.equals("refresh")) {
                if (mController.isLoading()) {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_cancel));
                } else {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_reload));
                }
            } else if (id.equals("fullscreen")) {
                if (mController.isFullscreen()) {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_return_from_full_screen));
                } else {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_full_screen));
                }
            } else if (id.equals("add_bookmark")) {
                if (mController.bookmarkExists()) {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_not_important));
                } else {
                    ((ImageView) icon).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_important));
                }
                item.setEnabled(mController.editBookmarksSupported());
            } else if (id.equals("forward")) {
                item.setEnabled(mController.canGoForward());
            } else if (id.equals("back")) {
                item.setEnabled(mController.canGoBack());
            } else if (id.equals("show_tabs")) {
                item.setEnabled(!mController.isTablet());
            } else if (id.equals("find_in_page")) {
                item.setEnabled(mController.tabSupportsFinding());
            } else if (id.equals("print")) {
                item.setEnabled(mController.printingSupported() && !mController.isOnNewTabPage());
            } else if (id.equals("recent_tabs")) {
                item.setEnabled(mController.syncSupported() && !mController.isIncognito());
            } else if (id.equals("next_tab")) {
                item.setEnabled(mController.tabExistsAtIndex(1));
            } else if (id.equals("previous_tab")) {
                item.setEnabled(mController.tabExistsAtIndex(-1));
            } else if (id.equals("add_to_home")) {
                item.setEnabled(mController.addToHomeSupported() && !mController.isIncognito() && !mController.isOnNewTabPage());
            } else if (id.equals("most_visited")) {
                item.setEnabled(!mController.isIncognito());
            } else if (id.equals("share")) {
                item.setEnabled(!mController.isOnNewTabPage());
            } else if (id.equals("reader_mode")) {
                item.setEnabled(mController.getReaderModeStatus() != 1);
            }
        }
        return true;
    }

    private void populateMenu() {
        final String[] actions = mXResources.getStringArray(R.array.pie_item_actions);
        final String[] values = mXResources.getStringArray(R.array.pie_item_values);
        final TypedArray drawables = mXResources.obtainTypedArray(R.array.pie_item_dark_drawables);
        mPie.clearItems();
        mXPreferences.reload();
        final Map<String, ?> keys = mXPreferences.getAll();
        if (keys.isEmpty()) {
            XposedBridge.log(TAG + "Failed to load preferences. Can read file: " + mXPreferences.getFile().canRead());
            return;
        }
        mActionMap.put("Action_main", new Action_main());
        for (int i = 1; i <= MAX_SLICES; i++) {
            if (mXPreferences.getBoolean("screen_slice_" + i, false)) {
                String key = "slice_" + i + "_item_" + i;
                if (keys.containsKey(key)) {
                    String value = (String) keys.get(key);
                    int index = Arrays.asList(values).indexOf(value);
                    PieItem item = makeItem(value, actions[index], drawables.getResourceId(index, 0), 1);
                    mPie.addItem(item);
                    addAction(actions[index], value);
                    for (int j = 1; j <= MAX_SLICES; j++) {
                        if (i == j) {
                            continue;
                        }
                        key = "slice_" + i + "_item_" + j;
                        if (keys.containsKey(key)) {
                            value = (String) keys.get(key);
                            index = Arrays.asList(values).indexOf(value);
                            item.addItem(makeItem(value, actions[index], drawables.getResourceId(index, 0), 1));
                            addAction(actions[index], value);
                        } else {
                            item.addItem(makeFiller());
                        }
                    }
                } else {
                    mPie.addItem(makeFiller());
                }
            }
        }
        drawables.recycle();
    }

    private void addAction(String action, String id) {
        if (id.equals("none")) {
            return;
        }
        try {
            if (action.isEmpty()) {
                mActionMap.put("Action_" + id, (Action) Class.forName(PACKAGE_NAME + ".Action_" + id).newInstance());
            }
        } catch (InstantiationException ie) {
            XposedBridge.log(TAG + ie);
        } catch (IllegalAccessException iae) {
            XposedBridge.log(TAG + iae);
        } catch (ClassNotFoundException cnfe) {
            XposedBridge.log(TAG + cnfe);
        }
    }

    @Override
    public void onClick(View v) {
        ItemInfo info = (ItemInfo) v.getTag();
        if (mActionMap.containsKey("Action_" + info.id)) {
            mActionMap.get("Action_" + info.id).execute(mController);
        } else {
            if (info.action != null) {
                ((Action_main) mActionMap.get("Action_main")).executeMain(mController, info.action);
            }
        }
    }

    private PieItem makeItem(String id, String action, int iconRes, int level) {
        if (id.equals("none")) {
            return makeFiller();
        }
        ItemInfo info = new ItemInfo();
        info.action = action;
        info.id = id;
        if (id.equals("show_tabs")) {
            View tabs = makeTabsView();
            tabs.setTag(info);
            PieItem tabsItem = new PieItem(tabs, id, "", level);
            tabs.setOnClickListener(this);
            return tabsItem;
        }
        ImageView view = new ImageView(mChromeActivity);
        view.setImageDrawable(mXResources.getDrawable(iconRes));
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setTag(info);
        view.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        view.setOnClickListener(this);
        PieItem item = new PieItem(view, id, action, level);
        return item;
    }

    private PieItem makeFiller() {
        return new PieItem(null, "", null, 1);
    }

    @SuppressWarnings("deprecation")
    private View makeTabsView() {
        LayoutInflater li = mChromeActivity.getLayoutInflater();
        View view = li.inflate(mXResources.getLayout(R.layout.qc_tabs_view), null);
        // findViewById returns null on some versions of Chrome
        // (some sort of resource conflict?) - so use getChildAt
        //TextView count = (TextView) view.findViewById(R.id.count_label);
        TextView count = (TextView) ((ViewGroup) view).getChildAt(1);
        count.setBackgroundDrawable(mXResources.getDrawable(R.drawable.tab_nr));
        count.setText(Integer.toString(mController.getTabCount()));
        //ImageView icon = (ImageView) view.findViewById(R.id.count_icon);
        ImageView icon = (ImageView) ((ViewGroup) view).getChildAt(0);
        icon.setImageDrawable(mXResources.getDrawable(R.drawable.ic_windows_holo_dark));
        icon.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        return view;
    }

    private void hookOnPageLoad() {
        mOnPageLoad = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                List<PieItem> items = mPie.findItemsById("refresh");
                if (items.size() != 0) {
                    for (PieItem item : items) {
                        ((ImageView) item.getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_action_reload));
                    }
                    mPie.invalidate();
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(mController.getCurrentTab().getClass(),
                    "didFinishPageLoad", mOnPageLoad);
        } catch (NoSuchMethodError nsme) {
            XposedHelpers.findAndHookMethod(mController.getCurrentTab().getClass(),
                    "didFinishPageLoad", String.class, mOnPageLoad);
        }
    }

    static class ItemInfo {
        private String id;
        private String action;
    }

}
