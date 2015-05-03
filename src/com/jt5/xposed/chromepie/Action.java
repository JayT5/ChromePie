package com.jt5.xposed.chromepie;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Process;
import android.preference.PreferenceManager;
import android.widget.EditText;
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
        try {
            Object locationBar = control.getLocationBar();
            int id = control.getResIdentifier("focus_url_bar");
            if (!control.itemSelected(id)) {
                callMethod(locationBar, "requestUrlFocus");
            }
            EditText urlBar = (EditText) callMethod(locationBar, "getUrlBar");
            urlBar.selectAll();
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
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
        if (control.isDocumentMode()) {
            control.toggleRecentApps();
        } else {
            try {
                callMethod(control.getChromeActivity(), "toggleOverview");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
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
        Object contentViewCore = control.getContentViewCore();
        try {
            Integer scrollX = (Integer) callMethod(contentViewCore, "computeHorizontalScrollOffset");
            callMethod(contentViewCore, "scrollTo", scrollX, 0);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Action_scroll_to_bottom implements Action {
    @Override
    public void execute(Controller control) {
        Object contentViewCore = control.getContentViewCore();
        try {
            Integer scrollRange = (Integer) callMethod(contentViewCore, "computeVerticalScrollRange");
            Integer scrollX = (Integer) callMethod(contentViewCore, "computeHorizontalScrollOffset");
            callMethod(contentViewCore, "scrollTo", scrollX, scrollRange);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Action_fullscreen implements Action {
    @Override
    public void execute(Controller control) {
        control.setFullscreen(!control.isFullscreen());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(control.getChromeActivity());
        prefs.edit().putBoolean("chromepie_apply_fullscreen", control.isFullscreen()).apply();
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
        try {
            callMethod(control.getTabModel(), "closeAllTabs");
        } catch (NoSuchMethodError nsme) {
            int id = 0;
            if (control.isIncognito()) {
                id = control.getResIdentifier("close_all_incognito_tabs_menu_id");
            } else {
                id = control.getResIdentifier("close_all_tabs_menu_id");
            }
            control.itemSelected(id);
        }
    }
}

class Action_go_to_home implements Action {
    @Override
    public void execute(Controller control) {
        control.goToHomeScreen();
    }
}

class Action_exit implements Action {
    @Override
    public void execute(Controller control) {
        control.goToHomeScreen();
        Process.killProcess(Process.myPid());
    }
}

class Action_next_tab implements Action {
    @Override
    public void execute(Controller control) {
        Integer index = control.getCurrentTabIndex();
        if (index != -1) {
            control.showTabByIndex(index + 1);
        }
    }
}

class Action_previous_tab implements Action {
    @Override
    public void execute(Controller control) {
        Integer index = control.getCurrentTabIndex();
        if (index != -1) {
            control.showTabByIndex(index - 1);
        }
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

class Action_voice_search implements Action {
    @Override
    public void execute(Controller control) {
        try {
            XposedHelpers.callMethod(control.getLocationBar(), "startVoiceRecognition");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}
