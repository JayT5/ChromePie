package com.jt5.xposed.chromepie;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.res.XModuleResources;
import android.os.Handler;
import android.view.ViewGroup;

import com.jt5.xposed.chromepie.settings.PieSettings;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
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
        if (!PieSettings.CHROME_PACKAGE_NAMES.contains(resparam.packageName)) {
            return;
        }
        mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!PieSettings.CHROME_PACKAGE_NAMES.contains(lpparam.packageName)) {
            return;
        }
        initHooks(lpparam.classLoader);
    }

    private void initHooks(final ClassLoader classLoader) {
        try {

            XposedHelpers.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity activity = (Activity) param.thisObject;
                    if (!CHROME_ACTIVITY_CLASSES.contains(activity.getClass().getName())) {
                        return;
                    }
                    if (XposedHelpers.getAdditionalInstanceField(activity, "pie_control") == null) {
                        final ViewGroup container = findContainer(activity);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                initPieControl(activity, container, classLoader);
                            }
                        }, 500);
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

        } catch (NoSuchMethodError nsme) {
            XposedBridge.log("Could not initialise ChromePie: " + nsme);
        }
    }

    private ViewGroup findContainer(Activity activity) {
        int containerId = activity.getResources().getIdentifier("content_container", "id", activity.getPackageName());
        ViewGroup container;
        if ((container = (ViewGroup) activity.findViewById(containerId)) != null) {
            return container;
        } else {
            return (ViewGroup) activity.findViewById(android.R.id.content);
        }
    }

    private void initPieControl(Activity activity, ViewGroup container, ClassLoader classLoader) {
        PieControl control = new PieControl(activity, mModRes, mXPreferences, classLoader);
        control.attachToContainer(container);
        XposedHelpers.setAdditionalInstanceField(activity, "pie_control", control);
    }

}
