package com.jt5.xposed.chromepie;

import android.app.Activity;

import de.robv.android.xposed.XposedBridge;

class ChromeDocumentHelper extends ChromeHelper {

    ChromeDocumentHelper(Activity activity) {
        super(activity);
    }

    @Override
    boolean isDocumentMode() {
        return true;
    }

    @Override
    Object getTabModel() {
        try {
            return Utils.getObjectField(mActivity, "mTabModel");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            return Utils.getObjectField(mActivity, "mTabList");
        } catch (NoSuchFieldError nsfe) {

        }
        return super.getTabModel();
    }

    @Override
    Object getLayoutManager() {
        try {
            return Utils.callMethod(Utils.callMethod(mActivity, "getCompositorViewHolder"), "getLayoutManager");
        } catch (NoSuchMethodError nsme) {

        }
        return super.getLayoutManager();
    }

    @Override
    Object getToolbarManager() {
        try {
            Object helper = Utils.getObjectField(mActivity, "mDocumentToolbarHelper");
            return Utils.getObjectField(helper, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {

        }
        return super.getToolbarManager();
    }

    @Override
    Integer getThemeColor() {
        try {
            return (Integer) Utils.callMethod(mActivity, "getThemeColor");
        } catch (NoSuchMethodError nsme) {

        }
        return super.getThemeColor();
    }

    @Override
    void showOverview() {
        toggleRecentApps();
    }

    @Override
    void closeCurrentTab() {
        Object nextTab = getNextTabIfClosed();
        super.closeCurrentTab();
        if (nextTab != null) {
            showNextTab(nextTab);
        }
    }

    private Object getNextTabIfClosed() {
        Object tabToClose = getCurrentTab();
        try {
            int closingTabIndex = getTabIndex(getTabModel(), tabToClose);
            Object adjacentTab = getTabAt((closingTabIndex == 0) ? 1 : closingTabIndex - 1);
            Object parentTab = getTabById((Integer) Utils.callMethod(tabToClose, "getParentId"));

            // Determine which tab to select next according to these rules:
            // * Select the parent tab if it exists.
            // * Otherwise, select an adjacent tab if one exists.
            // * Otherwise, if closing the last incognito tab, select the current normal tab.
            // * Otherwise, select nothing.

            Object nextTab = null;
            if (parentTab != null) {
                nextTab = parentTab;
            } else if (adjacentTab != null) {
                nextTab = adjacentTab;
            } else if (isIncognito()) {
                nextTab = Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "getCurrentTab", getDocumentModel(false));
            }
            return nextTab;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    private void showNextTab(Object tab) {
        try {
            Object model = getDocumentModel((Boolean) Utils.callMethod(tab, "isIncognito"));
            int index = getTabIndex(model, tab);
            Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "setIndex", model, index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private Object getDocumentModel(boolean incognito) {
        try {
            return Utils.callMethod(Utils.callStaticMethod(Utils.CLASS_CHROME_APPLICATION, "getDocumentTabModelSelector"), "getModel", incognito);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return new Object();
    }

    private Object getTabById(int id) {
        try {
            return Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "getTabById", getTabModel(), id);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    private Object getTabAt(int index) {
        try {
            return Utils.callMethod(getTabModel(), "getTabAt", index);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return Utils.callMethod(getTabModel(), "getTab", index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    private Integer getTabIndex(Object model, Object tab) {
        try {
            return (Integer) Utils.callMethod(model, "indexOf", tab);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return -1;
    }

}
