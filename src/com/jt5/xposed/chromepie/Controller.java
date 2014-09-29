package com.jt5.xposed.chromepie;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class Controller {

    private static final String TAG = "ChromePie:Controller: ";
    private final ClassLoader mClassLoader;
    private final Activity mActivity;
    private final PieControl mPieControl;

    Controller(PieControl pieControl, Object mainObj, ClassLoader classLoader) {
        mClassLoader = classLoader;
        mActivity = (Activity) mainObj;
        mPieControl = pieControl;
    }

    Activity getChromeActivity() {
        return mActivity;
    }

    public String getTriggerSide() {
        return mPieControl.getTriggerSide();
    }

    int getResIdentifier(String id) {
        return mActivity.getResources().getIdentifier(id, "id", ChromePie.CHROME_PACKAGE);
    }

    Boolean itemSelected(int id) {
        Boolean success = false;
        if (id != 0) {
            try {
                success = (Boolean) callMethod(mActivity, ChromePie.ITEM_SELECTED_METHOD, id);
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
        return success;
    }

    Object getCurrentTab() {
        Object model = getTabModel();
        try {
            return callMethod(model, "getCurrentTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(mActivity, "getCurrentTab");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return new Object();
        }
    }

    private Object getTabModel() {
        try {
            return callMethod(mActivity, "getCurrentTabModel");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return new Object();
        }
    }

    Integer getCurrentTabIndex() {
        try {
            return (Integer) callMethod(getTabModel(), "indexOf", getCurrentTab());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return -1;
        }
    }

    Boolean tabExistsAtIndex(Integer i) {
        try {
            return callMethod(getTabModel(), "getTabAt", getCurrentTabIndex() + i) != null;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(getTabModel(), "getTab", getCurrentTabIndex() + i) != null;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    void showTabByIndex(int index) {
        try {
            callMethod(getTabModel(), "setIndex", index);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        Class<?> modelUtils;
        try {
            modelUtils = XposedHelpers.findClass("org.chromium.chrome.browser.tabmodel.TabModelUtils", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            modelUtils = XposedHelpers.findClass("com.google.android.apps.chrome.tabmodel.TabModelUtils", mClassLoader);
        }
        try {
            XposedHelpers.callStaticMethod(modelUtils, "setIndex", getTabModel(), index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void loadUrl(String url) {
        Object tab = getCurrentTab();
        try {
            callMethod(tab, "loadUrl", url, null, null, 2);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object urlParams = getLoadUrlParams(url);
            if (urlParams != null) {
                callMethod(tab, "loadUrl", urlParams);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private Object getLoadUrlParams(String url) {
        try {
            Class<?> loadUrlParams = XposedHelpers.findClass("org.chromium.content.browser.LoadUrlParams", mClassLoader);
            return XposedHelpers.newInstance(loadUrlParams, url, 2);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    String getMostVisitedUrl() {
        if (isNativeNTPEnabled()) {
            return "chrome-native://newtab/";
        } else {
            return "chrome://newtab/#most_visited";
        }
    }

    Object getNTPSection(String getSection) {
        try {
            Class<?> ntpSection = XposedHelpers.findClass("com.google.android.apps.chrome.NewTabPageUtil.NTPSection", mClassLoader);
            Object section = XposedHelpers.getStaticObjectField(ntpSection, getSection);
            return section;
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
            return new Object();
        }
    }

    Object findToolbar(Object contentView) {
        try {
            Class<?> toolbar = XposedHelpers.findClass("com.google.android.apps.chrome.tab.NewTabPageToolbar", mClassLoader);
            return XposedHelpers.callStaticMethod(toolbar, "findToolbar", contentView);
        } catch (ClassNotFoundError cnfe) {
            //XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            //XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    private Object getVideoView() {
        try {
            Class<?> contentVideoView = XposedHelpers.findClass("org.chromium.content.browser.ContentVideoView", mClassLoader);
            return XposedHelpers.callStaticMethod(contentVideoView, "getContentVideoView");
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    void closeCurrentTab() {
        Object model = getTabModel();
        try {
            callMethod(model, "closeTab", getCurrentTab(), true, false, true);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            callMethod(model, "closeTab", getCurrentTab());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    public int getTabCount() {
        try {
            return (Integer) callMethod(getTabModel(), "getCount");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return 0;
        }
    }

    public String getUrl() {
        try {
            return (String) callMethod(getCurrentTab(), "getUrl");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return "";
        }
    }

    public void requestTabFocus() {
        try {
            callMethod(getCurrentTab(), "requestFocus", true);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    public Boolean isInOverview() {
        try {
            Object ovLayout = callMethod(mActivity, "getAndSetupOverviewLayout");
            if (ovLayout != null) {
                return (Boolean) callMethod(ovLayout, "overviewVisible");
            } else {
                return false;
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean isDesktopUserAgent() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "getUseDesktopUserAgent");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean isLoading() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "isLoading");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean tabSupportsFinding() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "supportsFinding");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    Boolean bookmarkExists() {
        try {
            return (Long) callMethod(getCurrentTab(), "getBookmarkId") == -1L;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean canGoBack() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "canGoBack");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    Boolean canGoForward() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "canGoForward");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    public Boolean isInFullscreenVideo() {
        try {
            return (Boolean) callMethod(mActivity, "isFullscreenVideoPlaying");
        } catch (NoSuchMethodError nsme) {
            return getVideoView() != null;
        }
    }

    Boolean isFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            View decorView = mActivity.getWindow().getDecorView();
            return decorView.getSystemUiVisibility() == 2054;
        } else {
            try {
                Object fullscreenManager = callMethod(mActivity, "getFullscreenManager");
                return (Boolean) callMethod(fullscreenManager, "getPersistentFullscreenMode");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
                return false;
            }
        }
    }

    Boolean isIncognito() {
        try {
            return (Boolean) callMethod(getTabModel(), "isIncognito");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    private Boolean isNativeNTPEnabled() {
        try {
            Class<?> newTabPage = XposedHelpers.findClass("com.google.android.apps.chrome.ntp.NewTabPage", mClassLoader);
            Boolean isEnabled = (Boolean) XposedHelpers.callStaticMethod(newTabPage, "isNativeNTPEnabled", mActivity);
            return isEnabled;
        } catch (ClassNotFoundError cnfe) {
            //XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            //XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean syncSupported() {
        Class<?> featureUtils = null;
        try {
            featureUtils = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilities", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            featureUtils = XposedHelpers.findClass("org.chromium.chrome.browser.util.FeatureUtilities", mClassLoader);
        }
        try {
            Boolean allowSync = (Boolean) XposedHelpers.callStaticMethod(featureUtils, "canAllowSync", mActivity);
            return allowSync;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean printingSupported() {
        try {
            Class<?> compatUtils = XposedHelpers.findClass("org.chromium.base.ApiCompatibilityUtils", mClassLoader);
            Boolean canPrint = (Boolean) XposedHelpers.callStaticMethod(compatUtils, "isPrintingSupported");
            return canPrint;
        } catch (ClassNotFoundError cnfe) {

        } catch (NoSuchMethodError nsme) {

        }
        return Build.VERSION.SDK_INT >= 19;
    }

    Boolean editBookmarksSupported() {
        try {
            Class<?> bookmarksBridge = XposedHelpers.findClass("org.chromium.chrome.browser.BookmarksBridge", mClassLoader);
            Boolean editBookmarks = (Boolean) XposedHelpers.callStaticMethod(bookmarksBridge, "isEditBookmarksEnabled");
            return editBookmarks;
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }
}
