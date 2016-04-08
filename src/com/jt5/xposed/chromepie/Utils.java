package com.jt5.xposed.chromepie;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

public class Utils {

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    static Class<?> CLASS_TAB_MODEL_UTILS;
    static Class<?> CLASS_LOAD_URL_PARAMS;
    static Class<?> CLASS_URL_CONSTANTS;
    static Class<?> CLASS_DEVICE_UTILS;
    static Class<?> CLASS_FEATURE_UTILS;
    static Class<?> CLASS_DISTILLER_URL_UTILS;
    static Class<?> CLASS_DISTILLER_TAB_UTILS;
    static Class<?> CLASS_COLOR_UTILS;
    static Class<?> CLASS_SERVICE_BRIDGE;
    static Class<?> CLASS_CHROME_APPLICATION;
    static Class<?> CLASS_SHORTCUT_HELPER;
    static Class<?> CLASS_TAB_LAUNCH_TYPE;

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
        String[] distillerUrlUtils = {
                "org.chromium.components.dom_distiller.core.DomDistillerUrlUtils"
        };
        String[] distillerTabUtils = {
                "org.chromium.chrome.browser.dom_distiller.DomDistillerTabUtils"
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
        String[] tabLaunchType = {
                "org.chromium.chrome.browser.tabmodel.TabModel$TabLaunchType",
                "com.google.android.apps.chrome.tabmodel.TabModel$TabLaunchType"
        };

        CLASS_TAB_MODEL_UTILS = getClass(classLoader, tabModelUtils);
        CLASS_LOAD_URL_PARAMS = getClass(classLoader, loadUrlParams);
        CLASS_URL_CONSTANTS = getClass(classLoader, urlConstants);
        CLASS_DEVICE_UTILS = getClass(classLoader, deviceUtils);
        CLASS_FEATURE_UTILS = getClass(classLoader, featureUtilities);
        CLASS_DISTILLER_URL_UTILS = getClass(classLoader, distillerUrlUtils);
        CLASS_DISTILLER_TAB_UTILS = getClass(classLoader, distillerTabUtils);
        CLASS_COLOR_UTILS = getClass(classLoader, colorUtils);
        CLASS_SERVICE_BRIDGE = getClass(classLoader, serviceBridge);
        CLASS_CHROME_APPLICATION = getClass(classLoader, chromeApplication);
        CLASS_SHORTCUT_HELPER = getClass(classLoader, shortcutHelper);
        CLASS_TAB_LAUNCH_TYPE = getClass(classLoader, tabLaunchType);
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
        if (obj == null) {
            throw new NoSuchMethodError("NullPointerException for method: " + methodName);
        }
        try {
            return XposedHelpers.callMethod(obj, methodName, args);
        } catch (NoSuchMethodError e) {
            throw e;
        } catch (NoClassDefFoundError e) {
            try {
                return manualMethodFind(obj.getClass(), methodName, args).invoke(obj, args);
            } catch (Throwable t) {
                throw new NoSuchMethodError(t.getMessage());
            }
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
            try {
                return manualMethodFind(clazz, methodName, args).invoke(null, args);
            } catch (Throwable t) {
                throw new NoSuchMethodError(t.getMessage());
            }
        }
    }

    private static Method manualMethodFind(Class<?> clazz, String methodName, Object... args) {
        Class<?> clz = clazz;
        do {
            try {
                return XposedHelpers.findMethodExact(clz, methodName, getParameterTypes(args));
            } catch (NoSuchMethodError nsme) {

            }
        } while ((clz = clz.getSuperclass()) != null);
        throw new NoSuchMethodError(clazz + "#" + methodName);
    }

    /**
     * Return an array with the classes of the given objects. If an object
     * is a wrapper for a primitive type, then the primitive type is used
     */
    private static Class<?>[] getParameterTypes(Object... args) {
        Class<?>[] clazzes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (isWrapperType(args[i].getClass())) {
                    try {
                        clazzes[i] = (Class<?>) args[i].getClass().getField("TYPE").get(null);
                    } catch (Throwable t) {
                        clazzes[i] = args[i].getClass();
                    }
                } else {
                    clazzes[i] = args[i].getClass();
                }
            }
        }
        return clazzes;
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    private static Set<Class<?>> getWrapperTypes() {
        Set<Class<?>> types = new HashSet<Class<?>>();
        types.add(Boolean.class);
        types.add(Character.class);
        types.add(Byte.class);
        types.add(Short.class);
        types.add(Integer.class);
        types.add(Long.class);
        types.add(Float.class);
        types.add(Double.class);
        types.add(Void.class);
        return types;
    }

}
