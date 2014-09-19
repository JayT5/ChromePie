package com.jt5.xposed.chromepie;

import android.content.res.XModuleResources;
import android.widget.FrameLayout;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ChromePie implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    static String CHROME_PACKAGE;
    static String ITEM_SELECTED_METHOD = "onOptionsItemSelected";

    private String MODULE_PATH;
    private XModuleResources mModRes;
    private FrameLayout mContentContainer;
    private Object mMainObj;
    private PieControl mPieControl;

    private static final String CHROME_MAIN_CLASS = "com.google.android.apps.chrome.Main";
    private static final String CHROME_TABBED_ACTIVITY_CLASS = "com.google.android.apps.chrome.ChromeTabbedActivity";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!(resparam.packageName.equals("com.android.chrome") || resparam.packageName.equals("com.chrome.beta"))) {
            return;
        }
        mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        CHROME_PACKAGE = resparam.packageName;

        resparam.res.hookLayout(CHROME_PACKAGE, "layout", "main", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                mContentContainer = (FrameLayout) liparam.view.findViewById(
                        liparam.res.getIdentifier("content_container", "id", CHROME_PACKAGE));
            }
        });
    }

    private void createPie(ClassLoader classLoader) {
        if (mContentContainer != null) {
            mPieControl = new PieControl(mMainObj, mModRes, classLoader);
            mPieControl.attachToContainer(mContentContainer);
        } else {
            XposedBridge.log("Failed to initialise ChromePie, could not find Chrome content container");
        }
    }

    private void removePie() {
        if (mContentContainer != null) {
            mPieControl.removeFromContainer(mContentContainer);
        }
        mPieControl = null;
        mMainObj = null;
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!(lpparam.packageName.equals("com.android.chrome") || lpparam.packageName.equals("com.chrome.beta"))) {
            return;
        }

        final ClassLoader classLoader = lpparam.classLoader;
        final Class<?> chromeMainClass = XposedHelpers.findClass(CHROME_MAIN_CLASS, classLoader);

        try {
            // see if onOptionsItemSelected method exists in Main class
            XposedHelpers.findMethodExact(chromeMainClass, "onOptionsItemSelected", int.class);
            XposedHelpers.findAndHookMethod(chromeMainClass, "onStart", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mMainObj == null) {
                        mMainObj = param.thisObject;
                        createPie(classLoader);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(chromeMainClass, "onDestroy", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    removePie();
                }
            });

        } catch (NoSuchMethodError nsme1) {

            try {

                final Class<?> chromeActivityClass = XposedHelpers.findClass(CHROME_TABBED_ACTIVITY_CLASS, classLoader);

                // Newest Chrome Beta now using different item selected method
                try {
                    XposedHelpers.findMethodExact(chromeActivityClass, "onOptionsItemSelected", int.class);
                } catch (NoSuchMethodError nsme) {
                    XposedHelpers.findMethodExact(chromeActivityClass, "onMenuOrKeyboardAction", int.class);
                    ITEM_SELECTED_METHOD = "onMenuOrKeyboardAction";
                }

                XposedHelpers.findAndHookMethod(chromeActivityClass, "onStart", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mMainObj == null) {
                            mMainObj = param.thisObject;
                            createPie(classLoader);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(chromeActivityClass, "onDestroy", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        removePie();
                    }
                });

            } catch (NoSuchMethodError nsme2) {
                XposedBridge.log("Could not initialise ChromePie: " + nsme1);
                XposedBridge.log("" + nsme2);
            } catch (ClassNotFoundError cnfe) {
                XposedBridge.log("Could not initialise ChromePie: " + cnfe);
            }

        }
    }
}
