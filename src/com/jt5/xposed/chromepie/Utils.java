package com.jt5.xposed.chromepie;

import de.robv.android.xposed.XposedHelpers;

public class Utils {

    static Class<?> CLASS_TAB_MODEL_UTILS;
    static Class<?> CLASS_LOAD_URL_PARAMS;
    static Class<?> CLASS_URL_CONSTANTS;
    static Class<?> CLASS_DEVICE_UTILS;
    static Class<?> CLASS_FEATURE_UTILS;
    static Class<?> CLASS_DOM_DISTILLER_UTILS;
    static Class<?> CLASS_COLOR_UTILS;
    static Class<?> CLASS_SERVICE_BRIDGE;
    static Class<?> CLASS_CHROME_APPLICATION;
    static Class<?> CLASS_SHORTCUT_HELPER;

    static void initialise(ClassLoader classLoader) {
        String[] tabModelUtils = {
                "org.chromium.chrome.browser.tabmodel.TabModelUtils",
                "com.google.android.apps.chrome.tabmodel.TabModelUtils"
        };
        String[] loadUrlParams = {
                "org.chromium.content_public.browser.LoadUrlParams",
                "org.chromium.content.browser.LoadUrlParams"
        };
        String[] urlConstants = {
                "org.chromium.chrome.browser.UrlConstants",
                "com.google.android.apps.chrome.UrlConstants"
        };
        String[] deviceUtils = {
                "org.chromium.ui.base.DeviceFormFactor",
                "org.chromium.content.browser.DeviceUtils"
        };
        String[] featureUtilities = {
                "org.chromium.chrome.browser.util.FeatureUtilities",
                "com.google.android.apps.chrome.utilities.FeatureUtilities"
        };
        String[] domDistillerUtils = {
                "org.chromium.components.dom_distiller.core.DomDistillerUrlUtils"
        };
        String[] colorUtils = {
                "org.chromium.chrome.browser.util.ColorUtils",
                "org.chromium.chrome.browser.document.BrandColorUtils",
                "com.google.android.apps.chrome.utilities.DocumentUtilities"
        };
        String[] serviceBridge = {
                "org.chromium.chrome.browser.preferences.PrefServiceBridge",
                "com.google.android.apps.chrome.preferences.ChromeNativePreferences"
        };
        String[] chromeApplication = {
                "org.chromium.chrome.browser.ChromeApplication",
                "org.chromium.chrome.browser.ChromeMobileApplication",
                "com.google.android.apps.chrome.ChromeMobileApplication"
        };
        String[] shortcutHelper = {
                "org.chromium.chrome.browser.BookmarkUtils",
                "org.chromium.chrome.browser.ShortcutHelper"
        };

        CLASS_TAB_MODEL_UTILS = getClass(classLoader, tabModelUtils);
        CLASS_LOAD_URL_PARAMS = getClass(classLoader, loadUrlParams);
        CLASS_URL_CONSTANTS = getClass(classLoader, urlConstants);
        CLASS_DEVICE_UTILS = getClass(classLoader, deviceUtils);
        CLASS_FEATURE_UTILS = getClass(classLoader, featureUtilities);
        CLASS_DOM_DISTILLER_UTILS = getClass(classLoader, domDistillerUtils);
        CLASS_COLOR_UTILS = getClass(classLoader, colorUtils);
        CLASS_SERVICE_BRIDGE = getClass(classLoader, serviceBridge);
        CLASS_CHROME_APPLICATION = getClass(classLoader, chromeApplication);
        CLASS_SHORTCUT_HELPER = getClass(classLoader, shortcutHelper);
    }

    private static Class<?> getClass(ClassLoader classLoader, String[] classes) {
        for (String clazz : classes) {
            try {
                return XposedHelpers.findClass(clazz, classLoader);
            } catch (XposedHelpers.ClassNotFoundError e) {

            }
        }
        return null;
    }

    static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            return XposedHelpers.callMethod(obj, methodName, args);
        } catch (NoSuchMethodError e) {
            throw e;
        } catch (NoClassDefFoundError e) {
            throw new NoSuchMethodError("NoClassDefFoundError: " + obj.getClass() + "#" + methodName);
        }
    }

    static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) {
            throw new NoSuchMethodError("ClassNotFoundError for static method: " + methodName);
        }
        try {
            return XposedHelpers.callStaticMethod(clazz, methodName, args);
        } catch (NoSuchMethodError e) {
            throw e;
        } catch (NoClassDefFoundError e) {
            throw new NoSuchMethodError("NoClassDefFoundError: " + clazz + "#" + methodName);
        }
    }

}
