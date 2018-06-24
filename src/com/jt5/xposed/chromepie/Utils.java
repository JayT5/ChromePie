package com.jt5.xposed.chromepie;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.StrictMode;

import org.apache.commons.lang3.ClassUtils;
import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Utils {

    private static final String TAG = "ChromePie:Utils ";

    private static final Map<String, Field> fieldCache = new HashMap<>();
    private static final Map<String, Method> methodCache = new HashMap<>();

    static Class<?> CLASS_TAB_MODEL_UTILS;
    static Class<?> CLASS_LOAD_URL_PARAMS;
    static Class<?> CLASS_DEVICE_UTILS;
    static Class<?> CLASS_FEATURE_UTILS;
    static Class<?> CLASS_DISTILLER_URL_UTILS;
    static Class<?> CLASS_DISTILLER_TAB_UTILS;
    static Class<?> CLASS_SERVICE_BRIDGE;
    static Class<?> CLASS_CHROME_APPLICATION;
    static Class<?> CLASS_SHORTCUT_HELPER;
    static Class<?> CLASS_TAB_LAUNCH_TYPE;
    static Class<?> CLASS_SHARE_HELPER;

    static void initialise(ClassLoader classLoader) {
        String[] tabModelUtils = {
                "org.chromium.chrome.browser.tabmodel.TabModelUtils",
                "com.google.android.apps.chrome.tabmodel.TabModelUtils"
        };
        String[] loadUrlParams = {
                "org.chromium.content_public.browser.LoadUrlParams",
                "org.chromium.content.browser.LoadUrlParams"
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
        String[] shareHelper = {
                "org.chromium.chrome.browser.share.ShareHelper"
        };

        CLASS_TAB_MODEL_UTILS = getClass(classLoader, tabModelUtils);
        CLASS_LOAD_URL_PARAMS = getClass(classLoader, loadUrlParams);
        CLASS_DEVICE_UTILS = getClass(classLoader, deviceUtils);
        CLASS_FEATURE_UTILS = getClass(classLoader, featureUtilities);
        CLASS_DISTILLER_URL_UTILS = getClass(classLoader, distillerUrlUtils);
        CLASS_DISTILLER_TAB_UTILS = getClass(classLoader, distillerTabUtils);
        CLASS_SERVICE_BRIDGE = getClass(classLoader, serviceBridge);
        CLASS_CHROME_APPLICATION = getClass(classLoader, chromeApplication);
        CLASS_SHORTCUT_HELPER = getClass(classLoader, shortcutHelper);
        CLASS_TAB_LAUNCH_TYPE = getClass(classLoader, tabLaunchType);
        CLASS_SHARE_HELPER = getClass(classLoader, shareHelper);
    }

    static void reloadPreferences(XSharedPreferences prefs) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            prefs.reload();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    static Boolean isDocumentModeEnabled(Context context, ClassLoader classLoader) {
        try {
            return (Boolean) callStaticMethod(CLASS_FEATURE_UTILS, "isDocumentMode", context);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> featureUtilsInternal = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilitiesInternal", classLoader);
            return (Boolean) callStaticMethod(featureUtilsInternal, "isDocumentMode", context);
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {

        }
        return false;
    }

    public static boolean isObfuscated() {
        return CLASS_CHROME_APPLICATION == null;
    }

    public static int getDarkenedColor(int color, float factor) {
        float[] statusColor = new float[3];
        Color.colorToHSV(color, statusColor);
        statusColor[2] *= factor;
        return Color.HSVToColor(statusColor);
    }

    public static int applyColorTint(int color, float factor) {
        int r1 = Color.red(color);
        int g1 = Color.green(color);
        int b1 = Color.blue(color);
        int r2 = (int) (r1 + (factor * (255 - r1)));
        int g2 = (int) (g1 + (factor * (255 - g1)));
        int b2 = (int) (b1 + (factor * (255 - b1)));
        return Color.rgb(r2, g2, b2);
    }

    public static int applyColorAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static void log(String text) {
        if (isObfuscated()) return;
        XposedBridge.log(text);
    }

    static Map<String, ?> createDefaultsMap(Resources resources) {
        Map<String, Object> map = new HashMap<>();
        XmlResourceParser parser = resources.getXml(R.xml.aosp_preferences);
        String namespace = "http://schemas.android.com/apk/res/android";
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("SwitchPreference")) {
                        String key = parser.getAttributeValue(namespace, "key");
                        boolean value = parser.getAttributeBooleanValue(namespace, "defaultValue", true);
                        map.put(key, value);
                    } else if (parser.getName().equals("ListPreference")) {
                        String key = parser.getAttributeValue(namespace, "key");
                        String value = parser.getAttributeValue(namespace, "defaultValue");
                        map.put(key, value);
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            XposedBridge.log(TAG + e);
            parser.close();
            return Collections.emptyMap();
        }
        parser.close();
        return map;
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
            try {
                return XposedHelpers.callMethod(obj, methodName, args);
            } catch (NoSuchMethodError e) {
                return findMethodBestMatch(obj.getClass(), methodName, XposedHelpers.getParameterTypes(args)).invoke(obj, args);
            } catch (NoClassDefFoundError e) {
                return findMethodExact(obj.getClass(), methodName, getPrimitiveParameterTypes(args)).invoke(obj, args);
            }
        } catch (Throwable t) {
            throw new NoSuchMethodError(t.getMessage());
        }
    }

    static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) {
            throw new NoSuchMethodError("ClassNotFoundError for static method: " + methodName);
        }
        try {
            try {
                return XposedHelpers.callStaticMethod(clazz, methodName, args);
            } catch (NoSuchMethodError e) {
                return findMethodBestMatch(clazz, methodName, XposedHelpers.getParameterTypes(args)).invoke(null, args);
            } catch (NoClassDefFoundError e) {
                return findMethodExact(clazz, methodName, getPrimitiveParameterTypes(args)).invoke(null, args);
            }
        } catch (Throwable t) {
            throw new NoSuchMethodError(t.getMessage());
        }
    }

    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        String fullMethodName = clazz.getName() + '#' + methodName + getParametersString(parameterTypes) + "#bestmatch";

        if (methodCache.containsKey(fullMethodName)) {
            Method method = methodCache.get(fullMethodName);
            if (method == null)
                throw new NoSuchMethodError(fullMethodName);
            return method;
        }

        Method bestMatch = null;
        Class<?> clz = clazz;
        boolean considerPrivateMethods = true;
        do {
            for (Method method : clz.getDeclaredMethods()) {
                // don't consider private methods of superclasses
                if (!considerPrivateMethods && Modifier.isPrivate(method.getModifiers()))
                    continue;

                // compare name and parameters
                if ((method.getName().equals(methodName) || method.getName().startsWith(methodName + "$"))
                        && ClassUtils.isAssignable(parameterTypes, method.getParameterTypes(), true)) {
                    // get accessible version of method
                    bestMatch = method;
                    break;
                }
            }
            considerPrivateMethods = false;
        } while ((clz = clz.getSuperclass()) != null);

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            methodCache.put(fullMethodName, bestMatch);
            return bestMatch;
        } else {
            NoSuchMethodError e = new NoSuchMethodError(fullMethodName);
            methodCache.put(fullMethodName, null);
            throw e;
        }
    }

    private static Method findMethodExact(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        Class<?> clz = clazz;
        do {
            try {
                return XposedHelpers.findMethodExact(clz, methodName, parameterTypes);
            } catch (NoSuchMethodError nsme) {

            }
        } while ((clz = clz.getSuperclass()) != null);
        throw new NoSuchMethodError(clazz.getName() + '#' + methodName + getParametersString(parameterTypes));
    }

    /**
     * Return an array with the classes of the given objects. If an object
     * is a wrapper for a primitive type, then the primitive type is used
     */
    private static Class<?>[] getPrimitiveParameterTypes(Object... args) {
        Class<?>[] clazzes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (ClassUtils.isPrimitiveWrapper(args[i].getClass())) {
                    clazzes[i] = ClassUtils.wrapperToPrimitive(args[i].getClass());
                } else {
                    clazzes[i] = args[i].getClass();
                }
            }
        }
        return clazzes;
    }

    private static String getParametersString(Class<?>... clazzes) {
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Class<?> clazz : clazzes) {
            if (first)
                first = false;
            else
                sb.append(",");

            if (clazz != null)
                sb.append(clazz.getCanonicalName());
            else
                sb.append("null");
        }
        sb.append(")");
        return sb.toString();
    }

    static Object getObjectField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getObjectField(obj, fieldName);
        } catch (NoSuchFieldError e) {

        }
        try {
            return findField(obj.getClass(), fieldName).get(obj);
        } catch (IllegalAccessException e) {
            // should not happen
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getBooleanField(obj, fieldName);
        } catch (NoSuchFieldError e) {

        }
        try {
            return findField(obj.getClass(), fieldName).getBoolean(obj);
        } catch (IllegalAccessException e) {
            // should not happen
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (NoSuchFieldError e) {

        }
        try {
            return findField(clazz, fieldName).get(null);
        } catch (IllegalAccessException e) {
            // should not happen
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        String fullFieldName = clazz.getName() + '#' + fieldName;

        if (fieldCache.containsKey(fullFieldName)) {
            Field field = fieldCache.get(fullFieldName);
            if (field == null)
                throw new NoSuchFieldError(fullFieldName);
            return field;
        }

        try {
            Field field = findFieldRecursiveImpl(clazz, fieldName);
            field.setAccessible(true);
            fieldCache.put(fullFieldName, field);
            return field;
        } catch (NoSuchFieldException e) {
            fieldCache.put(fullFieldName, null);
            throw new NoSuchFieldError(fullFieldName);
        }
    }

    private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> clz = clazz;
            boolean considerPrivateFields = true;
            do {
                for (Field field : clz.getDeclaredFields()) {
                    // don't consider private fields of superclasses
                    if (!considerPrivateFields && Modifier.isPrivate(field.getModifiers()))
                        continue;

                    if (field.getName().equals(fieldName) || field.getName().startsWith(fieldName + "$"))
                        return field;
                }
                considerPrivateFields = false;
            } while ((clz = clz.getSuperclass()) != null);
            throw e;
        }
    }

}
