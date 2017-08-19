package com.jt5.xposed.chromepie;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.view.ViewGroup;

import com.jt5.xposed.chromepie.settings.PieSettings;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ChromePie implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final String TAG = "ChromePie: ";
    static final String PACKAGE_NAME = ChromePie.class.getPackage().getName();

    private XSharedPreferences mXPreferences;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mXPreferences = new XSharedPreferences(PACKAGE_NAME);
        mXPreferences.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        Utils.reloadPreferences(mXPreferences);
        Set<String> extraPkgs = mXPreferences.getStringSet("extra_packages", new HashSet<String>());
        if (PieSettings.CHROME_PACKAGE_NAMES.contains(lpparam.packageName) ||
                extraPkgs.contains(lpparam.packageName)) {
            initHooks(lpparam.classLoader);
        }
    }

    private void initHooks(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!PieSettings.CHROME_ACTIVITY_CLASSES.contains(activity.getClass().getName())) {
                    return;
                }
                if (XposedHelpers.getAdditionalInstanceField(activity, "pie_control") == null) {
                    initPieControl(activity, classLoader);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!PieSettings.CHROME_ACTIVITY_CLASSES.contains(activity.getClass().getName())) {
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
        final Resources resources;
        try {
            resources = activity.getPackageManager().getResourcesForApplication(PACKAGE_NAME);
        } catch (PackageManager.NameNotFoundException e) {
            XposedBridge.log(TAG + "Failed to initialise resources");
            return;
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PieControl control = new PieControl(activity, resources, mXPreferences, classLoader);
                control.attachToContainer(container);
                XposedHelpers.setAdditionalInstanceField(activity, "pie_control", control);
            }
        }, 1000L);
    }

}
