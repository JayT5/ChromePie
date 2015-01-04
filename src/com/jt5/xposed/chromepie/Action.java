package com.jt5.xposed.chromepie;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public interface Action {

    static final String TAG = "ChromePie:Action: ";

    void execute(Controller control);

}

class Action_main implements Action {
    @Override
    public void execute(Controller control) {}

    public void executeMain(Controller control, String action) {
        int id = control.getResIdentifier(action);
        control.itemSelected(id);
    }
}

class Action_back implements Action {
    @Override
    public void execute(Controller control) {
        int id = control.getResIdentifier("back_menu_id");
        if (!control.itemSelected(id)) {
            Activity activity = control.getChromeActivity();
            try {
                callMethod(activity, "goBack");
                return;
            } catch (NoSuchMethodError nsme) {

            }
            Object tab = control.getCurrentTab();
            try {
                callMethod(tab, "goBack");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
    }
}

class Action_refresh implements Action {
    @Override
    public void execute(Controller control) {
        int id = control.getResIdentifier("reload_menu_id");
        if (!control.itemSelected(id)) {
            Object tab = control.getCurrentTab();
            try {
                if (control.isLoading()) {
                    callMethod(tab, "stopLoading");
                } else {
                    callMethod(tab, "reload");
                }
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
    }
}

class Action_edit_url implements Action {
    @Override
    public void execute(Controller control) {
        int id = control.getResIdentifier("focus_url_bar");
        if (!control.itemSelected(id)) {
            Activity activity = control.getChromeActivity();
            try {
                Object locationBar = callMethod(activity, "getLocationBar");
                callMethod(locationBar, "requestUrlFocus");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
    }
}

class Action_close_tab implements Action {
    @Override
    public void execute(Controller control) {
        control.closeCurrentTab();
    }
}

class Action_show_tabs implements Action {
    @Override
    public void execute(Controller control) {
        Activity activity = control.getChromeActivity();
        try {
            callMethod(activity, "toggleOverview");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Action_most_visited implements Action {
    @Override
    public void execute(Controller control) {
        String ntpUrl = control.getMostVisitedUrl();
        control.loadUrl(ntpUrl);
    }
}

class Action_scroll_to_top implements Action {
    @Override
    public void execute(Controller control) {
        Object tab = control.getCurrentTab();
        try {
            Object contentViewCore = callMethod(tab, "getContentViewCore");
            if (contentViewCore != null) {
                Integer scrollY = (Integer) callMethod(contentViewCore, "computeVerticalScrollOffset");
                callMethod(contentViewCore, "scrollBy", 0, -scrollY);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Action_scroll_to_bottom implements Action {
    @Override
    public void execute(Controller control) {
        Object tab = control.getCurrentTab();
        try {
            Object contentViewCore = callMethod(tab, "getContentViewCore");
            if (contentViewCore != null) {
                Integer scrollRange = (Integer) callMethod(contentViewCore, "computeVerticalScrollRange");
                Integer scrollX = (Integer) callMethod(contentViewCore, "computeHorizontalScrollOffset");
                callMethod(contentViewCore, "scrollTo", scrollX, scrollRange);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Action_fullscreen implements Action {
    @Override
    @SuppressLint("InlinedApi")
    public void execute(final Controller control) {
        final Window window = control.getChromeActivity().getWindow();
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            // Immersive mode supported
            final View decorView = window.getDecorView();
            if (control.isFullscreen()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                decorView.setOnSystemUiVisibilityChangeListener(null);
                decorView.setSystemUiVisibility(0);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

                // Hook re-enables immersive mode when returning to Chrome after leaving
                XposedHelpers.findAndHookMethod(decorView.getClass(), "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (control.isFullscreen() && (Boolean) param.args[0]) {
                            decorView.setSystemUiVisibility(fullscreenVisibility);
                        }
                    }
                });
            }
        } else {
            if (control.isFullscreen()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }
}

class Action_share implements Action {
    @Override
    public void execute(Controller control) {
        int id = control.getResIdentifier("share_page_id");
        if (id == 0) {
            id = control.getResIdentifier("share_menu_id");
        }
        control.itemSelected(id);
    }
}

class Action_close_all implements Action {
    @Override
    public void execute(Controller control) {
        int id = 0;
        if (control.isIncognito()) {
            id = control.getResIdentifier("close_all_incognito_tabs_menu_id");
        } else {
            id = control.getResIdentifier("close_all_tabs_menu_id");
        }
        control.itemSelected(id);
    }
}

class Action_exit implements Action {
    @Override
    public void execute(Controller control) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        control.getChromeActivity().startActivity(homeIntent);
    }
}

class Action_next_tab implements Action {
    @Override
    public void execute(Controller control) {
        Integer index = control.getCurrentTabIndex();
        if (index == -1) {
            return;
        }
        control.showTabByIndex(index + 1);
    }
}

class Action_previous_tab implements Action {
    @Override
    public void execute(Controller control) {
        Integer index = control.getCurrentTabIndex();
        if (index == -1) {
            return;
        }
        control.showTabByIndex(index - 1);
    }
}

class Action_reader_mode implements Action {
    @Override
    public void execute(Controller control) {
        if (control.isDistilledPage()) {
            String originalUrl = control.getOriginalUrl();
            control.loadUrl(originalUrl);
        } else {
            int id = control.getResIdentifier("reader_mode_id");
            control.itemSelected(id);
        }
    }
}
