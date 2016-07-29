package com.jt5.xposed.chromepie;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.os.Handler;
import android.view.ViewGroup;

import com.jt5.xposed.chromepie.settings.PieSettings;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ChromePie implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    static final String PACKAGE_NAME = ChromePie.class.getPackage().getName();

    private String MODULE_PATH;
    private XModuleResources mModRes;
    private XSharedPreferences mXPreferences;

    private static final List<String> CHROME_ACTIVITY_CLASSES = Arrays.asList(
            "org.chromium.chrome.browser.ChromeTabbedActivity",
            "org.chromium.chrome.browser.document.DocumentActivity",
            "org.chromium.chrome.browser.document.IncognitoDocumentActivity",
            "com.google.android.apps.chrome.ChromeTabbedActivity",
            "com.google.android.apps.chrome.document.DocumentActivity",
            "com.google.android.apps.chrome.document.IncognitoDocumentActivity",
            "com.google.android.apps.chrome.Main"
    );

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        mXPreferences = new XSharedPreferences(PACKAGE_NAME);
        mXPreferences.makeWorldReadable();
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (PieSettings.CHROME_PACKAGE_NAMES.contains(resparam.packageName)) {
            mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        }
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (PieSettings.CHROME_PACKAGE_NAMES.contains(lpparam.packageName)) {
            initHooks(lpparam.classLoader);
        }
    }

    private void initHooks(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!CHROME_ACTIVITY_CLASSES.contains(activity.getClass().getName())) {
                    return;
                }
                if (XposedHelpers.getAdditionalInstanceField(activity, "pie_control") == null &&
                        areResourcesActive()) {
                    initPieControl(activity, classLoader);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!CHROME_ACTIVITY_CLASSES.contains(activity.getClass().getName())) {
                    return;
                }
                PieControl control = (PieControl) XposedHelpers.getAdditionalInstanceField(activity, "pie_control");
                if (control != null) {
                    control.destroy();
                    XposedHelpers.removeAdditionalInstanceField(activity, "pie_control");
                }
            }
        });
    }

    private void initPieControl(final Activity activity, final ClassLoader classLoader) {
        final ViewGroup container = (ViewGroup) activity.findViewById(android.R.id.content);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PieControl control = new PieControl(activity, mModRes, mXPreferences, classLoader);
                control.attachToContainer(container);
                XposedHelpers.setAdditionalInstanceField(activity, "pie_control", control);
            }
        }, 500);
    }

    private boolean areResourcesActive() {
        try {
            if (mModRes != null) {
                mModRes.getResourceEntryName(R.id.version);
            }
        } catch (Resources.NotFoundException nfe) {
            // Module has been updated but device has not
            // rebooted and so resources are not initialised
            return false;
        }
        return true;
    }

}
