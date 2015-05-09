package com.jt5.xposed.chromepie;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class Controller {

    private static final String TAG = "ChromePie:Controller: ";
    private final ClassLoader mClassLoader;
    private Activity mActivity;
    private Unhook mFullscreenWindowFocusHook;
    private int mBrandColor;

    Controller(Activity chromeActivity, ClassLoader classLoader) {
        mClassLoader = classLoader;
        mActivity = chromeActivity;
    }

    Activity getChromeActivity() {
        return mActivity;
    }

    void setChromeActivity(Activity activity) {
        mActivity = activity;
    }

    int getResIdentifier(String id) {
        return getResIdentifier(id, "id");
    }

    private int getResIdentifier(String id, String type) {
        return mActivity.getResources().getIdentifier(id, type, ChromePie.CHROME_PACKAGE);
    }

    Boolean itemSelected(int id) {
        if (id != 0) {
            try {
                if (ChromePie.sMenuActionMethod.getParameterTypes().length == 1) {
                    return (Boolean) ChromePie.sMenuActionMethod.invoke(mActivity, id);
                } else {
                    return (Boolean) ChromePie.sMenuActionMethod.invoke(mActivity, id, false);
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + t);
            }
        }
        return false;
    }

    Object getCurrentTab() {
        try {
            return callMethod(mActivity, "getActivityTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(getTabModel(), "getCurrentTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(mActivity, "getCurrentTab");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return new Object();
        }
    }

    Object getTabModel() {
        if (isDocumentMode()) {
            try {
                return XposedHelpers.getObjectField(mActivity, "mTabModel");
            } catch (NoSuchFieldError nsfe) {

            }
            try {
                return XposedHelpers.getObjectField(mActivity, "mTabList");
            } catch (NoSuchFieldError nsfe) {
                XposedBridge.log(TAG + nsfe);
            }
        } else {
            try {
                return callMethod(mActivity, "getCurrentTabModel");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
        return new Object();
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
        if (isTablet() || isDocumentMode()) {
            return getTabCount() == 0;
        }
        try {
            Object ovLayout = callMethod(mActivity, "getAndSetupOverviewLayout");
            if (ovLayout != null) {
                return (Boolean) callMethod(ovLayout, "overviewVisible");
            }
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object layoutMan = callMethod(mActivity, "getLayoutManager");
            if (layoutMan != null) {
                return (Boolean) callMethod(layoutMan, "overviewVisible");
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
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

    @SuppressLint("InlinedApi")
    void setFullscreen(boolean fullscreen) {
        final Window window = mActivity.getWindow();
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            // Immersive mode supported
            final View decorView = window.getDecorView();
            final int windowFlags = WindowManager.LayoutParams.FLAG_FULLSCREEN | (android.os.Build.VERSION.SDK_INT >= 21
                                  ? WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS : 0);
            if (fullscreen) {
                window.addFlags(windowFlags);
                final int fullscreenVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                decorView.setSystemUiVisibility(fullscreenVisibility);

                // Listener re-enables immersive mode after closing the soft keyboard
                decorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        decorView.setSystemUiVisibility(fullscreenVisibility);
                    }
                });

                if (mFullscreenWindowFocusHook == null) {
                    // Hook re-enables immersive mode when returning to Chrome after leaving
                    mFullscreenWindowFocusHook = XposedHelpers.findAndHookMethod(decorView.getClass(),
                            "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            if (isFullscreen() && (Boolean) param.args[0]) {
                                decorView.setSystemUiVisibility(fullscreenVisibility);
                            }
                        }
                    });
                }
            } else {
                window.clearFlags(windowFlags);
                decorView.setSystemUiVisibility(0);
                decorView.setOnSystemUiVisibilityChangeListener(null);
                if (mFullscreenWindowFocusHook != null) {
                    mFullscreenWindowFocusHook.unhook();
                    mFullscreenWindowFocusHook = null;
                }
            }
        } else {
            if (fullscreen) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    @SuppressLint("InlinedApi")
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

    String getMostVisitedUrl() {
        Class<?> urlConstants = null;
        try {
            urlConstants = XposedHelpers.findClass("com.google.android.apps.chrome.UrlConstants", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            urlConstants = XposedHelpers.findClass("org.chromium.chrome.browser.UrlConstants", mClassLoader);
        }
        try {
            return (String) XposedHelpers.getStaticObjectField(urlConstants, "MOST_VISITED_URL");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            return (String) XposedHelpers.getStaticObjectField(urlConstants, "NTP_URL");
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
        }
        return "chrome-native://newtab/";
    }

    Boolean isTablet() {
        try {
            return (Boolean) callMethod(mActivity, "isTablet");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> formFactor = XposedHelpers.findClass("org.chromium.ui.base.DeviceFormFactor", mClassLoader);
            return (Boolean) XposedHelpers.callStaticMethod(formFactor, "isTablet", mActivity);
        } catch (ClassNotFoundError cnfe) {

        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> deviceUtils = XposedHelpers.findClass("org.chromium.content.browser.DeviceUtils", mClassLoader);
            return (Boolean) XposedHelpers.callStaticMethod(deviceUtils, "isTablet", mActivity);
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
            return (Boolean) XposedHelpers.callStaticMethod(featureUtils, "canAllowSync", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean printingSupported() {
        try {
            Class<?> compatUtils = XposedHelpers.findClass("org.chromium.base.ApiCompatibilityUtils", mClassLoader);
            return (Boolean) XposedHelpers.callStaticMethod(compatUtils, "isPrintingSupported");
        } catch (ClassNotFoundError cnfe) {

        } catch (NoSuchMethodError nsme) {

        }
        return Build.VERSION.SDK_INT >= 19;
    }

    Boolean editBookmarksSupported() {
        Class<?> bookmarksBridge;
        try {
            bookmarksBridge = XposedHelpers.findClass("org.chromium.chrome.browser.BookmarksBridge", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
            return true;
        }
        try {
            Object profile = callMethod(callMethod(getCurrentTab(), "getProfile"), "getOriginalProfile");
            return (Boolean) XposedHelpers.callStaticMethod(bookmarksBridge, "isEditBookmarksEnabled", profile);
        } catch (NoSuchMethodError nsme) {
            return (Boolean) XposedHelpers.callStaticMethod(bookmarksBridge, "isEditBookmarksEnabled");
        }
    }

    Boolean addToHomeSupported() {
        try {
            Class<?> bookmarkUtils = XposedHelpers.findClass("org.chromium.chrome.browser.BookmarkUtils", mClassLoader);
            return (Boolean) XposedHelpers.callStaticMethod(bookmarkUtils, "isAddToHomeIntentSupported", mActivity);
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

    ComponentName getShareComponentName() {
        try {
            Class<?> shareHelper = XposedHelpers.findClass("org.chromium.chrome.browser.share.ShareHelper", mClassLoader);
            return (ComponentName) XposedHelpers.callStaticMethod(shareHelper, "getLastShareComponentName", mActivity);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    Object getContentViewCore() {
        Object tab = getCurrentTab();
        if (tab == null) {
            return null;
        }
        try {
            return callMethod(tab, "getContentViewCore");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    public Integer getTopControlsHeight() {
        Object contentViewCore = getContentViewCore();
        if (contentViewCore == null) {
            Resources res = mActivity.getResources();
            int controlHeightId = res.getIdentifier("control_container_height", "dimen", ChromePie.CHROME_PACKAGE);
            if (controlHeightId != 0) {
                return (int) res.getDimension(controlHeightId);
            } else {
                return 0;
            }
        }
        try {
            return (Integer) callMethod(contentViewCore, "getTopControlsLayoutHeightPix");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Integer) callMethod(contentViewCore, "getViewportSizeOffsetHeightPix");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return 0;
    }

    Boolean isDocumentMode() {
        Class<?> featureUtils;
        try {
            featureUtils = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilitiesInternal", mClassLoader);
        } catch (ClassNotFoundError cnfe) {

        }
        try {
            featureUtils = XposedHelpers.findClass("org.chromium.chrome.browser.util.FeatureUtilities", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            return false;
        }
        try {
            return (Boolean) XposedHelpers.callStaticMethod(featureUtils, "isDocumentMode", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    Object getLocationBar() {
        try {
            return callMethod(mActivity, "getLocationBar");
        } catch (NoSuchMethodError nsme) {

        }
        Object locationBar = mActivity.findViewById(getResIdentifier("location_bar"));
        return (locationBar == null) ? new Object() : locationBar;
    }

    EditText getUrlBar() {
        Object locationBar = getLocationBar();
        try {
            return (EditText) callMethod(locationBar, "getUrlBar");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (EditText) XposedHelpers.getObjectField(locationBar, "mUrlBar");
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
        }
        return null;
    }

    Boolean isVoiceSearchEnabled() {
        try {
            return (Boolean) callMethod(getLocationBar(), "isVoiceSearchEnabled");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    void toggleRecentApps() {
        try {
            Class<?> serviceClass = XposedHelpers.findClass("android.os.ServiceManager", mClassLoader);
            IBinder statusBarBinder = (IBinder) XposedHelpers.callStaticMethod(serviceClass, "getService", "statusbar");
            Class<?> statusBarClass = XposedHelpers.findClass(statusBarBinder.getInterfaceDescriptor(), mClassLoader).getClasses()[0];
            Object statusBar = XposedHelpers.callStaticMethod(statusBarClass, "asInterface", statusBarBinder);
            callMethod(statusBar, "toggleRecentApps");
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }

    void goToHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.startActivity(intent);
    }

    Boolean shouldUseThemeColor(int themeColor) {
        try {
            if (isDocumentMode()) {
                return !(Boolean) callMethod(mActivity, "shouldUseDefaultStatusBarColor") && !isDefaultPrimaryColor(themeColor);
            } else {
                return !isDefaultPrimaryColor(themeColor);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    private int getDefaultPrimaryColor() {
        int color;
        if (isIncognito()) {
            color = getResIdentifier("incognito_primary_color", "color");
        } else {
            color = getResIdentifier("default_primary_color", "color");
        }
        return color == 0 ? 0 : mActivity.getResources().getColor(color);
    }

    private boolean isDefaultPrimaryColor(int color) {
        return color == getDefaultPrimaryColor();
    }

    Integer getThemeColor() {
        try {
            if (isDocumentMode()) {
                return (Integer) callMethod(mActivity, "getThemeColor");
            } else {
                Object tab = getCurrentTab();
                if (tab == null || isIncognito()) {
                    return getDefaultPrimaryColor();
                }
                Object webContents = callMethod(tab, "getWebContents");
                return (Integer) callMethod(webContents, "getThemeColor", getDefaultPrimaryColor());
            }
        } catch (NoSuchMethodError nsme) {

        }
        return 0;
    }

    public Integer getStatusBarColor(int color) {
        if (isDefaultPrimaryColor(color)) {
            return Color.BLACK;
        }
        Class<?> colorUtils;
        try {
            colorUtils = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.DocumentUtilities", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            colorUtils = XposedHelpers.findClass("org.chromium.chrome.browser.document.BrandColorUtils", mClassLoader);
        }
        try {
            return (Integer) XposedHelpers.callStaticMethod(colorUtils, "computeStatusBarColor", color);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return color;
    }

    void applyThemeColors(Object toolbarManager) {
        try {
            int themeColor = getThemeColor();
            if (mBrandColor != themeColor) {
                mBrandColor = themeColor;
                callMethod(toolbarManager, "updatePrimaryColor", themeColor);
                setStatusBarColor(themeColor);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private void setStatusBarColor(int themeColor) {
        int statusColor = getStatusBarColor(themeColor);
        try {
            Class<?> apiCompatUtils = XposedHelpers.findClass("org.chromium.base.ApiCompatibilityUtils", mClassLoader);
            XposedHelpers.callStaticMethod(apiCompatUtils, "setStatusBarColor", mActivity, statusColor);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

}
