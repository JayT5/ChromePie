package com.jt5.xposed.chromepie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;

import com.jt5.xposed.chromepie.broadcastreceiver.PieReceiver;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class Controller {

    private static final String TAG = "ChromePie:Controller: ";
    private final ClassLoader mClassLoader;
    private final Activity mActivity;
    private Unhook mFullscreenWindowFocusHook;
    private final Boolean mIsDocumentMode;
    private final Menu mMenu;

    Controller(Activity chromeActivity, ClassLoader classLoader) {
        mClassLoader = classLoader;
        mActivity = chromeActivity;
        Utils.initialise(mClassLoader);
        mIsDocumentMode = isDocumentMode();
        PopupMenu popup = new PopupMenu(mActivity, null);
        mMenu = popup.getMenu();
    }

    Activity getChromeActivity() {
        return mActivity;
    }

    int getResIdentifier(String id) {
        return getResIdentifier(id, "id");
    }

    private int getResIdentifier(String id, String type) {
        return mActivity.getResources().getIdentifier(id, type, mActivity.getPackageName());
    }

    Boolean itemSelected(int id) {
        if (id != 0) {
            MenuItem item = mMenu.add(Menu.NONE, id, Menu.NONE, "");
            mMenu.removeItem(id);
            return mActivity.onOptionsItemSelected(item);
        }
        return false;
    }

    Object getCurrentTab() {
        try {
            return Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "getCurrentTab", getTabModel());
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return Utils.callMethod(mActivity, "getActivityTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return Utils.callMethod(getTabModel(), "getCurrentTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return Utils.callMethod(mActivity, "getCurrentTab");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return new Object();
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
                Object modelSelector = XposedHelpers.getObjectField(mActivity, "mTabModelSelector");
                if (modelSelector != null) {
                    return Utils.callMethod(modelSelector, "getCurrentModel");
                }
            } catch (NoSuchFieldError | NoSuchMethodError e) {

            }
            try {
                return Utils.callMethod(mActivity, "getCurrentTabModel");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
        return new Object();
    }

    Integer getTabIndex(Object tab) {
        try {
            return (Integer) Utils.callMethod(getTabModel(), "indexOf", tab);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return -1;
        }
    }

    Boolean tabExistsAtIndex(int i) {
        return getTabAt(getTabIndex(getCurrentTab()) + i) != null;
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

    private Object getTabById(int id) {
        try {
            return Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "getTabById", getTabModel(), id);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    void showTabByIndex(int index) {
        try {
            Utils.callMethod(getTabModel(), "setIndex", index);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "setIndex", getTabModel(), index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private void showNextTab(Object tab) {
        try {
            Object model = getDocumentModel((Boolean) Utils.callMethod(tab, "isIncognito"));
            int index = (Integer) Utils.callMethod(model, "indexOf", tab);
            Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "setIndex", model, index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void launchUrl(String url) {
        try {
            Object tabCreator = Utils.callMethod(mActivity, "getTabCreator", false);
            Utils.callMethod(tabCreator, "launchUrl", url, getTabLaunchType());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void loadUrl(String url) {
        Object tab = getCurrentTab();
        try {
            Utils.callMethod(tab, "loadUrl", url, null, null, 2);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object urlParams = getLoadUrlParams(url);
            if (urlParams != null) {
                Utils.callMethod(tab, "loadUrl", urlParams);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private Object getLoadUrlParams(String url) {
        if (Utils.CLASS_LOAD_URL_PARAMS == null) return null;
        try {
            return XposedHelpers.newInstance(Utils.CLASS_LOAD_URL_PARAMS, url);
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
        return null;
    }

    private Object getTabLaunchType() {
        if (Utils.CLASS_TAB_LAUNCH_TYPE == null) return new Object();
        try {
            return XposedHelpers.getStaticObjectField(Utils.CLASS_TAB_LAUNCH_TYPE, "FROM_CHROME_UI");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            return XposedHelpers.getStaticObjectField(Utils.CLASS_TAB_LAUNCH_TYPE, "FROM_MENU_OR_OVERVIEW");
        } catch (NoSuchFieldError nsfe) {

        }
        return new Object();
    }

    private Object getVideoView() {
        try {
            Class<?> contentVideoView = XposedHelpers.findClass("org.chromium.content.browser.ContentVideoView", mClassLoader);
            return Utils.callStaticMethod(contentVideoView, "getContentVideoView");
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return null;
    }

    void closeCurrentTab() {
        Object model = getTabModel();
        Object tabToClose = getCurrentTab();
        try {
            Utils.callMethod(model, "closeTab", tabToClose, true, false, true);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Utils.callMethod(model, "closeTab", tabToClose);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void closeDocumentTab() {
        Object nextTab = getNextTabIfClosed();
        closeCurrentTab();
        if (nextTab != null) {
            showNextTab(nextTab);
        }
    }

    private Object getNextTabIfClosed() {
        Object tabToClose = getCurrentTab();
        try {
            int closingTabIndex = getTabIndex(tabToClose);
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

    public int getTabCount() {
        try {
            return (Integer) Utils.callMethod(getTabModel(), "getCount");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return 1;
        }
    }

    private String getUrl() {
        try {
            return (String) Utils.callMethod(getCurrentTab(), "getUrl");
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
            Utils.callMethod(tab, "requestFocus", true);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Utils.callMethod(tab, "requestFocus");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void toggleOverview() {
        try {
            Utils.callMethod(mActivity, "toggleOverview");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void documentModeToggleOverview() {
        try {
            Object layout = getLayoutManager();
            if (layout != null) {
                Utils.callMethod(layout, "toggleOverview");
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    Boolean isTabSwitchingEnabledInDocumentMode() {
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_FEATURE_UTILS, "isTabSwitchingEnabledInDocumentModeInternal");
        } catch (NoSuchMethodError nsme) {

        }
        return false;
    }

    public Boolean isInOverview() {
        if (isTablet() || (isDocumentMode() && !isTabSwitchingEnabledInDocumentMode())) {
            return getTabCount() == 0;
        }
        try {
            Object layout = getLayoutManager();
            if (layout != null) {
                return (Boolean) Utils.callMethod(layout, "overviewVisible");
            }
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Boolean) Utils.callMethod(mActivity, "isInOverviewMode");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    private Object getLayoutManager() {
        if (isDocumentMode()) {
            try {
                return Utils.callMethod(Utils.callMethod(mActivity, "getCompositorViewHolder"), "getLayoutManager");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        } else {
            try {
                return XposedHelpers.getObjectField(mActivity, "mLayoutManager");
            } catch (NoSuchFieldError nsfe) {

            }
            try {
                return Utils.callMethod(mActivity, "getLayoutManager");
            } catch (NoSuchMethodError nsme) {

            }
            try {
                return Utils.callMethod(mActivity, "getAndSetupOverviewLayout");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
        return null;
    }

    Boolean isDesktopUserAgent() {
        try {
            return (Boolean) Utils.callMethod(getCurrentTab(), "getUseDesktopUserAgent");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean isLoading() {
        try {
            return (Boolean) Utils.callMethod(getCurrentTab(), "isLoading");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean tabSupportsFinding() {
        Object tab = getCurrentTab();
        try {
            return (Boolean) Utils.callMethod(tab, "supportsFinding");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Boolean isNativePage = (Boolean) Utils.callMethod(tab, "isNativePage");
            return !isNativePage && getWebContents() != null;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    Boolean bookmarkExists() {
        try {
            return (Long) Utils.callMethod(getCurrentTab(), "getBookmarkId") == -1L;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean canGoBack() {
        try {
            return (Boolean) Utils.callMethod(getCurrentTab(), "canGoBack");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    Boolean canGoForward() {
        try {
            return (Boolean) Utils.callMethod(getCurrentTab(), "canGoForward");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    public Boolean isInFullscreenVideo() {
        try {
            Object fullscreenManager = Utils.callMethod(mActivity, "getFullscreenManager");
            if (fullscreenManager != null) {
                return (Boolean) Utils.callMethod(fullscreenManager, "isOverlayVideoMode") ||
                        (Boolean) Utils.callMethod(fullscreenManager, "getPersistentFullscreenMode");
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return getVideoView() != null;
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
        broadcastFullscreenIntent(fullscreen);
    }

    private void broadcastFullscreenIntent(boolean fullscreen) {
        Intent intent = new Intent(PieReceiver.FULLSCREEN_UPDATED_INTENT);
        intent.putExtra("IS_FULLSCREEN", fullscreen);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setComponent(new ComponentName(ChromePie.PACKAGE_NAME, PieReceiver.class.getName()));
        mActivity.sendBroadcast(intent);
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
            return (Boolean) Utils.callMethod(getTabModel(), "isIncognito");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean isOnNewTabPage() {
        String url = getUrl();
        return (url.startsWith("chrome://") || url.startsWith("chrome-native://"));
    }

    String getChromeUrl(String page) {
        if (Utils.CLASS_URL_CONSTANTS == null) return "chrome-native://newtab/";
        try {
            return (String) XposedHelpers.getStaticObjectField(Utils.CLASS_URL_CONSTANTS, page);
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
        }
        return "chrome-native://newtab/";
    }

    Boolean isTablet() {
        try {
            return (Boolean) Utils.callMethod(mActivity, "isTablet");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_DEVICE_UTILS, "isTablet", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    Boolean syncSupported() {
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_FEATURE_UTILS, "canAllowSync", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean printingEnabled() {
        try {
            return (Boolean) Utils.callMethod(Utils.callStaticMethod(Utils.CLASS_SERVICE_BRIDGE, "getInstance"), "isPrintingEnabled");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean editBookmarksSupported() {
        try {
            Object bookmarkBridge = getBookmarkBridge();
            if (bookmarkBridge != null) {
                return (Boolean) Utils.callMethod(bookmarkBridge, "isEditBookmarksEnabled");
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    private Object getBookmarkBridge() {
        try {
            return Utils.callMethod(getToolbarManager(), "getBookmarkBridge");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return Utils.callMethod(getToolbarManager(), "getBookmarksBridge");
        } catch (NoSuchMethodError nsme) {

        }
        return null;
    }

    Boolean addToHomeSupported() {
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_SHORTCUT_HELPER, "isAddToHomeIntentSupported", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    void distillCurrentPage() {
        try {
            Utils.callStaticMethod(Utils.CLASS_DISTILLER_TAB_UTILS, "nativeDistillCurrentPageAndView", getWebContents());
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Utils.callStaticMethod(Utils.CLASS_DISTILLER_TAB_UTILS, "distillCurrentPageAndView", getWebContents());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    Boolean isDistilledPage() {
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_DISTILLER_URL_UTILS, "isDistilledPage", getUrl());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    Boolean nativeIsUrlDistillable() {
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_DISTILLER_URL_UTILS, "nativeIsUrlDistillable", getUrl());
        } catch (NoSuchMethodError nsme) {

        }
        return true;
    }

    String getOriginalUrl() {
        try {
            return (String) Utils.callStaticMethod(Utils.CLASS_DISTILLER_URL_UTILS, "getOriginalUrlFromDistillerUrl", getUrl());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return "";
    }

    ComponentName getShareComponentName() {
        try {
            Class<?> shareHelper = XposedHelpers.findClass("org.chromium.chrome.browser.share.ShareHelper", mClassLoader);
            return (ComponentName) Utils.callStaticMethod(shareHelper, "getLastShareComponentName", mActivity);
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return null;
    }

    Object getContentViewCore() {
        Object tab = getCurrentTab();
        if (tab == null) return null;
        try {
            return Utils.callStaticMethod(Utils.CLASS_TAB_MODEL_UTILS, "getCurrentContentViewCore", getTabModel());
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return Utils.callMethod(tab, "getContentViewCore");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    public Boolean touchScrollInProgress() {
        try {
            Object contentViewCore = getContentViewCore();
            if (contentViewCore != null) {
                return XposedHelpers.getBooleanField(contentViewCore, "mTouchScrollInProgress");
            }
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
        }
        return false;
    }

    void scroll(Object contentViewCore, int yVel, int y) {
        try {
            float density = mActivity.getResources().getDisplayMetrics().density + 1;
            Utils.callMethod(contentViewCore, "flingViewport", SystemClock.uptimeMillis(), 0, (int) (yVel * density));
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Integer x = (Integer) Utils.callMethod(contentViewCore, "computeHorizontalScrollOffset");
            ViewGroup containerView = (ViewGroup) Utils.callMethod(contentViewCore, "getContainerView");
            Utils.callMethod(containerView, "scrollTo", x, y);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    Object getWebContents() {
        try {
            return Utils.callMethod(getCurrentTab(), "getWebContents");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return new Object();
    }

    public Integer getTopControlsHeight() {
        Object contentViewCore = getContentViewCore();
        if (contentViewCore == null) {
            return getTopControlsDimen();
        }
        try {
            return (Integer) Utils.callMethod(contentViewCore, "getViewportSizeOffsetHeightPix");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Boolean) Utils.callMethod(contentViewCore, "doTopControlsShrinkBlinkSize") ?
                    (Integer) Utils.callMethod(contentViewCore, "getTopControlsHeightPix") : 0;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return getTopControlsDimen();
    }

    int getTopControlsDimen() {
        int controlHeightId = getResIdentifier("control_container_height", "dimen");
        return controlHeightId == 0 ? 0 : mActivity.getResources().getDimensionPixelSize(controlHeightId);
    }

    Boolean isDocumentMode() {
        if (mIsDocumentMode != null) {
            return mIsDocumentMode;
        }
        try {
            return (Boolean) Utils.callStaticMethod(Utils.CLASS_FEATURE_UTILS, "isDocumentMode", mActivity);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> featureUtilsInternal = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilitiesInternal", mClassLoader);
            return (Boolean) Utils.callStaticMethod(featureUtilsInternal, "isDocumentMode", mActivity);
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return false;
    }

    Object getLocationBar() {
        try {
            return Utils.callMethod(mActivity, "getLocationBar");
        } catch (NoSuchMethodError nsme) {

        }
        Object locationBar = mActivity.findViewById(getResIdentifier("location_bar"));
        return (locationBar == null) ? new Object() : locationBar;
    }

    public EditText getUrlBar() {
        Object locationBar = getLocationBar();
        try {
            return (EditText) Utils.callMethod(locationBar, "getUrlBar");
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
            return (Boolean) Utils.callMethod(getLocationBar(), "isVoiceSearchEnabled");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    void toggleRecentApps() {
        try {
            Class<?> serviceClass = XposedHelpers.findClass("android.os.ServiceManager", mClassLoader);
            IBinder statusBarBinder = (IBinder) Utils.callStaticMethod(serviceClass, "getService", "statusbar");
            Class<?> statusBarClass = XposedHelpers.findClass(statusBarBinder.getInterfaceDescriptor(), mClassLoader).getClasses()[0];
            Object statusBar = Utils.callStaticMethod(statusBarClass, "asInterface", statusBarBinder);
            Utils.callMethod(statusBar, "toggleRecentApps");
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
        return !isDefaultPrimaryColor(themeColor) && !isIncognito();
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
        return color == getDefaultPrimaryColor() || getDefaultPrimaryColor() == 0 || color == 0;
    }

    Integer getThemeColor() {
        Object tab = getCurrentTab();
        if (tab == null || getWebContents() == null || isIncognito()) {
            return getDefaultPrimaryColor();
        }
        try {
            return (Integer) Utils.callMethod(getWebContents(), "getThemeColor", getDefaultPrimaryColor());
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Integer) Utils.callMethod(tab, "getThemeColor");
        } catch (NoSuchMethodError nsme) {

        }
        if (isDocumentMode()) {
            try {
                return (Integer) Utils.callMethod(mActivity, "getThemeColor");
            } catch (NoSuchMethodError nsme) {

            }
        }
        return 0;
    }

    public Integer getStatusBarColor(int color) {
        if (isDefaultPrimaryColor(color)) {
            return Color.BLACK;
        }
        try {
            return (Integer) Utils.callStaticMethod(Utils.CLASS_COLOR_UTILS, "getDarkenedColorForStatusBar", color);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Integer) Utils.callStaticMethod(Utils.CLASS_COLOR_UTILS, "computeStatusBarColor", color);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return Color.BLACK;
    }

    private Object getToolbarManager() {
        try {
            return XposedHelpers.getObjectField(mActivity, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            Object helper = XposedHelpers.getObjectField(mActivity, "mToolbarHelper");
            return XposedHelpers.getObjectField(helper, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            Object helper = XposedHelpers.getObjectField(mActivity, "mDocumentToolbarHelper");
            return XposedHelpers.getObjectField(helper, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {
            return new Object();
        }
    }

    Object getDataReductionSettings() {
        try {
            Class<?> dataReduction = XposedHelpers.findClass("org.chromium.chrome.browser.net.spdyproxy.DataReductionProxySettings", mClassLoader);
            return Utils.callStaticMethod(dataReduction, "getInstance");
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return new Object();
    }

}
