package com.jt5.xposed.chromepie;

import static de.robv.android.xposed.XposedHelpers.callMethod;

import java.util.List;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class Controller {

    private static final String TAG = "ChromePie:Controller: ";
    private final ClassLoader mClassLoader;
    private final Activity mActivity;
    private final PieControl mPieControl;

    Controller(PieControl pieControl, Activity chromeActivity, ClassLoader classLoader) {
        mClassLoader = classLoader;
        mActivity = chromeActivity;
        mPieControl = pieControl;
    }

    public XSharedPreferences getXPreferences() {
        return mPieControl.getXPreferences();
    }

    Activity getChromeActivity() {
        return mActivity;
    }

    public List<Integer> getTriggerSide() {
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
        Class<?> loadUrlParams;
        try {
            loadUrlParams = XposedHelpers.findClass("org.chromium.content.browser.LoadUrlParams", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            loadUrlParams = XposedHelpers.findClass("org.chromium.content_public.browser.LoadUrlParams", mClassLoader);
        }
        try {
            return XposedHelpers.newInstance(loadUrlParams, url);
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
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
        Object tab = getCurrentTab();
        if (tab == null) {
            return;
        }
        try {
            callMethod(tab, "requestFocus", true);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            callMethod(tab, "requestFocus");
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
        Object tab = getCurrentTab();
        try {
            return (Boolean) callMethod(tab, "supportsFinding");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Boolean isNativePage = (Boolean) callMethod(tab, "isNativePage");
            Object webContents = callMethod(tab, "getWebContents");
            return !isNativePage && (webContents != null);
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
            int visibility = mActivity.getWindow().getDecorView().getSystemUiVisibility();
            return (visibility & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0;
        } else {
            int flags = mActivity.getWindow().getAttributes().flags;
            return (flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
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

    Boolean isOnNewTabPage() {
        String url = getUrl();
        return (url.startsWith("chrome://") || url.startsWith("chrome-native://"));
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

    Boolean isTablet() {
        try {
            return (Boolean) callMethod(mActivity, "isTablet");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> formFactor = XposedHelpers.findClass("org.chromium.ui.base.DeviceFormFactor", mClassLoader);
            Boolean isTablet = (Boolean) XposedHelpers.callStaticMethod(formFactor, "isTablet", mActivity);
            return isTablet;
        } catch (ClassNotFoundError cnfe) {

        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> deviceUtils = XposedHelpers.findClass("org.chromium.content.browser.DeviceUtils", mClassLoader);
            Boolean isTablet = (Boolean) XposedHelpers.callStaticMethod(deviceUtils, "isTablet", mActivity);
            return isTablet;
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
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

    Boolean addToHomeSupported() {
        try {
            Class<?> bookmarkUtils = XposedHelpers.findClass("org.chromium.chrome.browser.BookmarkUtils", mClassLoader);
            Boolean canAddToHome = (Boolean) XposedHelpers.callStaticMethod(bookmarkUtils, "isAddToHomeIntentSupported", mActivity);
            return canAddToHome;
        } catch (ClassNotFoundError cnfe) {

        } catch (NoSuchMethodError nsme) {

        }
        return true;
    }

    Integer getReaderModeStatus() {
        try {
            return (Integer) callMethod(callMethod(getCurrentTab(), "getReaderModeManager"), "getReaderModeStatus");
        } catch (NoSuchMethodError nsme) {

        }
        return 1;
    }

    Boolean isDistilledPage() {
        Class<?> distillerUtils = getDomDistillerUtilsClass();
        if (distillerUtils == null) {
            return false;
        }
        try {
            return (Boolean) XposedHelpers.callStaticMethod(distillerUtils, "isDistilledPage", getUrl());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    String getOriginalUrl() {
        Class<?> distillerUtils = getDomDistillerUtilsClass();
        if (distillerUtils == null) {
            return "";
        }
        try {
            return (String) XposedHelpers.callStaticMethod(distillerUtils, "getOriginalUrlFromDistillerUrl", getUrl());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return "";
    }

    private Class<?> getDomDistillerUtilsClass() {
        try {
            return XposedHelpers.findClass("org.chromium.components.dom_distiller.core.DomDistillerUrlUtils", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        }
        return null;
    }

}
