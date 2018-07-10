package com.jt5.xposed.chromepie;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.view.ViewGroup;

import com.jt5.xposed.chromepie.settings.PieSettings;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ChromePie implements IXposedHookZygoteInit {

    private static final String TAG = "ChromePie: ";
    static final String PACKAGE_NAME = ChromePie.class.getPackage().getName();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        final XSharedPreferences preferences = new XSharedPreferences(PACKAGE_NAME);
        preferences.makeWorldReadable();

        XposedHelpers.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!PieSettings.CHROME_ACTIVITY_CLASSES.contains(activity.getClass().getName())) {
                    return;
                }
                if (XposedHelpers.getAdditionalInstanceField(activity, "pie_control") == null) {
                    initPieControl(activity, preferences);
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

    private void initPieControl(final Activity activity, final XSharedPreferences preferences) {
        final Resources resources = getResources(activity);
        if (resources == null) return;
        final ViewGroup container = activity.findViewById(android.R.id.content);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PieControl control = new PieControl(activity, resources, preferences);
                control.attachToContainer(container);
                XposedHelpers.setAdditionalInstanceField(activity, "pie_control", control);
            }
        }, 1000L);
    }

    private Resources getResources(final Activity activity) {
        try {
            Resources resources = activity.getPackageManager().getResourcesForApplication(PACKAGE_NAME);
            resources.getInteger(R.integer.qc_radius_increment);
            return resources;
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            XposedBridge.log(TAG + "Failed to initialise resources. Have you rebooted? " + e);
            return null;
        }
    }

}
