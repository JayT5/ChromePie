package com.jt5.xposed.chromepie;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.res.XModuleResources;
import android.view.ViewGroup;

import com.jt5.xposed.chromepie.settings.PieSettings;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ChromePie implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    static final String PACKAGE_NAME = ChromePie.class.getPackage().getName();
    static String CHROME_PACKAGE;
    static Method sMenuActionMethod;

    private String MODULE_PATH;
    private XModuleResources mModRes;
    private XSharedPreferences mXPreferences;

    private static final String[] CHROME_ACTIVITY_CLASSES = {
        "org.chromium.chrome.browser.ChromeActivity",
        "com.google.android.apps.chrome.ChromeTabbedActivity",
        "com.google.android.apps.chrome.ChromeActivity",
        "com.google.android.apps.chrome.Main"
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        mXPreferences = new XSharedPreferences(PACKAGE_NAME);
        mXPreferences.makeWorldReadable();
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!PieSettings.CHROME_PACKAGE_NAMES.contains(resparam.packageName)) {
            return;
        }
        mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        CHROME_PACKAGE = resparam.packageName;
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!PieSettings.CHROME_PACKAGE_NAMES.contains(lpparam.packageName)) {
            return;
        }

        final ClassLoader classLoader = lpparam.classLoader;
        Class<?> chromeActivityClass;

        for (String clazz : CHROME_ACTIVITY_CLASSES) {
            try {
                chromeActivityClass = XposedHelpers.findClass(clazz, classLoader);
            } catch (ClassNotFoundError cnfe) {
                continue;
            }
            sMenuActionMethod = getMenuActionMethod(chromeActivityClass);
            if (sMenuActionMethod != null) {
                hookChrome(chromeActivityClass, classLoader);
                return;
            }
        }

        XposedBridge.log("Failed to initialise ChromePie, could not find hookable method");
    }

    private Method getMenuActionMethod(Class<?> activityClass) {
        try {
            XposedHelpers.findMethodExact(activityClass, "onStart");
            try {
                return XposedHelpers.findMethodExact(activityClass, "onMenuOrKeyboardAction", int.class, boolean.class);
            } catch (NoSuchMethodError nsme) {

            }
            try {
                return XposedHelpers.findMethodExact(activityClass, "onMenuOrKeyboardAction", int.class);
            } catch (NoSuchMethodError nsme) {
                return XposedHelpers.findMethodExact(activityClass, "onOptionsItemSelected", int.class);
            }
        } catch (NoSuchMethodError nsme) {
            return null;
        }
    }

    private void hookChrome(Class<?> activityClass, final ClassLoader classLoader) {
        try {

            XposedHelpers.findAndHookMethod(activityClass, "onStart", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    if (XposedHelpers.getAdditionalInstanceField(activity, "pie_control") == null) {
                        int containerId = activity.getResources().getIdentifier("content_container", "id", CHROME_PACKAGE);
                        ViewGroup container = (ViewGroup) activity.findViewById(containerId);
                        if (container == null) {
                            container = (ViewGroup) activity.findViewById(android.R.id.content);
                        }
                        initPieControl(activity, container, classLoader);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(activityClass, "onDestroy", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    PieControl control = (PieControl) XposedHelpers.getAdditionalInstanceField(activity, "pie_control");
                    if (control != null) {
                        control.destroy();
                        XposedHelpers.setAdditionalInstanceField(activity, "pie_control", null);
                    }
                }
            });

        } catch (NoSuchMethodError nsme) {
            XposedBridge.log("Could not initialise ChromePie: " + nsme);
        }
    }

    private void initPieControl(Activity activity, ViewGroup container, ClassLoader classLoader) {
        PieControl control = new PieControl(activity, mModRes, mXPreferences, classLoader);
        control.attachToContainer(container);
        XposedHelpers.setAdditionalInstanceField(activity, "pie_control", control);
    }

}
