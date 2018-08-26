package com.jt5.xposed.chromepie;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jt5.xposed.chromepie.broadcastreceiver.PieReceiver;
import com.jt5.xposed.chromepie.view.BaseItem;

import de.robv.android.xposed.XposedBridge;

public class PieItem extends BaseItem {

    static final String TAG = "ChromePie:PieItem: ";

    PieItem(View view, String id) {
        super(view, id, 0);
    }

    PieItem(View view, String id, int action) {
        super(view, id, action);
    }

    void onOpen(ChromeHelper helper, Resources resources) {

    }

    public void onClick(ChromeHelper helper) {
        helper.itemSelected(getMenuActionId());
    }

}

class Item_back extends PieItem {
    public Item_back(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.canGoBack());
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (!helper.itemSelected(getMenuActionId())) {
            helper.dispatchKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_ALT_ON);
        }
    }
}

class Item_forward extends PieItem {
    public Item_forward(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.canGoForward());
    }
}

class Item_refresh extends PieItem {
    public Item_refresh(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        int drawable = helper.isLoading() ? R.drawable.ic_stop_white : R.drawable.ic_refresh_white;
        ((ImageView) getView()).setImageDrawable(resources.getDrawable(drawable));
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (!helper.itemSelected(getMenuActionId())) {
            Object tab = helper.getCurrentTab();
            try {
                if (helper.isLoading()) {
                    Utils.callMethod(tab, "stopLoading");
                } else {
                    Utils.callMethod(tab, "reload");
                }
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
    }
}

class Item_new_tab extends PieItem {
    public Item_new_tab(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        if (helper.isDocumentMode()) {
            setEnabled(!helper.isOnNewTabPage() || helper.isIncognito());
        }
    }
}

class Item_new_incognito_tab extends PieItem {
    public Item_new_incognito_tab(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        if (helper.isDocumentMode()) {
            setEnabled(!(helper.isOnNewTabPage() && helper.isIncognito()));
        }
    }
}

class Item_close_tab extends PieItem {
    public Item_close_tab(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.closeCurrentTab();
    }
}

class Item_close_all extends PieItem {
    public Item_close_all(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (!helper.isIncognito() && !helper.isDocumentMode()) {
            helper.showOverview();
        }
        try {
            Utils.callMethod(helper.getTabModel(), "closeAllTabs");
        } catch (NoSuchMethodError nsme) {
            int id = getMenuActionId();
            if (helper.isIncognito()) {
                id = helper.getResIdentifier("close_all_incognito_tabs_menu_id");
            }
            helper.itemSelected(id);
        }
    }
}

class Item_bookmarks extends PieItem {
    public Item_bookmarks(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(final ChromeHelper helper) {
        if (helper.getTabCount() == 0) {
            helper.createNewTab();
        }
        super.onClick(helper);
    }
}

class Item_add_bookmark extends PieItem {
    public Item_add_bookmark(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        int drawable = helper.bookmarkExists() ? R.drawable.ic_add_bookmark_white : R.drawable.ic_added_bookmark_white;
        ((ImageView) getView()).setImageDrawable(resources.getDrawable(drawable));
        setEnabled(helper.editBookmarksSupported());
    }
}

class Item_history extends PieItem {
    public Item_history(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (helper.getTabCount() == 0) {
            helper.createNewTab();
        }
        super.onClick(helper);
    }
}

class Item_most_visited extends PieItem {
    public Item_most_visited(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(!helper.isIncognito() && !Utils.isObfuscated());
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (helper.getTabCount() == 0) {
            helper.createNewTab();
        } else {
            helper.loadUrl(ChromeHelper.NTP_URL);
        }
    }
}

class Item_recent_tabs extends PieItem {
    public Item_recent_tabs(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.syncSupported() && !helper.isIncognito());
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (helper.getTabCount() == 0) {
            helper.createNewTab();
        }
        super.onClick(helper);
    }
}

class Item_show_tabs extends PieItem {
    public Item_show_tabs(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.getTabCount() != 0 && !helper.isTablet() && !helper.isCustomTabs());
        TextView count = getView().findViewById(R.id.count_label);
        if (count == null) return;
        count.setText(Integer.toString(helper.getTabCount()));
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.showOverview();
    }

    @Override
    public void setAlpha(float alpha) {
        ImageView icon = getView().findViewById(R.id.count_icon);
        TextView count = getView().findViewById(R.id.count_label);
        if (icon == null || count == null) {
            super.setAlpha(alpha);
        } else {
            int alphaInt = Math.round(alpha * (isEnabled() ? 255 : 77));
            icon.setAlpha((float) alphaInt / 255);
            count.setTextColor(Color.argb(alphaInt, 255, 255, 255));
            count.getBackground().setAlpha(alphaInt);
        }
    }
}

class Item_add_to_home extends PieItem {
    public Item_add_to_home(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.addToHomeSupported() && !helper.isIncognito() && !helper.isOnNewTabPage());
    }
}

class Item_find_in_page extends PieItem {
    public Item_find_in_page(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.tabSupportsFinding());
    }
}

class Item_edit_url extends PieItem {
    public Item_edit_url(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        try {
            if (!helper.itemSelected(getMenuActionId())) {
                Utils.callMethod(helper.getLocationBar(), "requestUrlFocus");
            }
            EditText urlBar = helper.getUrlBar();
            if (urlBar != null) {
                urlBar.selectAll();
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Item_desktop_site extends PieItem {
    public Item_desktop_site(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        int drawable = helper.isDesktopUserAgent() ? R.drawable.ic_mobile_site_white :
                R.drawable.ic_desktop_site_white;
        ((ImageView) getView()).setImageDrawable(resources.getDrawable(drawable));
    }
}

class Item_fullscreen extends PieItem {
    public Item_fullscreen(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        int drawable = helper.isFullscreen() ? R.drawable.ic_fullscreen_exit_white :
                R.drawable.ic_fullscreen_white;
        ((ImageView) getView()).setImageDrawable(resources.getDrawable(drawable));
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.setFullscreen(!helper.isFullscreen());
    }
}

class Item_scroll_to_top extends PieItem {
    public Item_scroll_to_top(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.getContentViewCore() != null);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        try {
            Object contentView = helper.getContentView();
            Integer scrollOffset = (Integer) Utils.callMethod(contentView, "computeVerticalScrollOffset");
            Integer scrollExtent = (Integer) Utils.callMethod(contentView, "computeVerticalScrollExtent");
            helper.scroll(contentView, scrollOffset + scrollExtent, -helper.getControlContainerHeightDimen("control_container_height"));
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Item_scroll_to_bottom extends PieItem {
    public Item_scroll_to_bottom(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.getContentViewCore() != null);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        try {
            Object contentView = helper.getContentView();
            Integer scrollRange = (Integer) Utils.callMethod(contentView, "computeVerticalScrollRange");
            Integer scrollOffset = (Integer) Utils.callMethod(contentView, "computeVerticalScrollOffset");
            helper.scroll(contentView, -scrollRange + scrollOffset, scrollRange);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Item_share extends PieItem {
    public Item_share(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(!helper.isOnNewTabPage());
    }
}

class Item_direct_share extends PieItem {
    private ComponentName mDirectShareComponentName;

    public Item_direct_share(View view, String id, int action) {
        super(view, id, action);
        ((ImageView) view).setAdjustViewBounds(true);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        ComponentName compName = helper.getShareComponentName();
        setEnabled((compName != null && !helper.isOnNewTabPage()) || Utils.isObfuscated());
        ImageView iv = (ImageView) getView();
        if (compName == null) {
            iv.setImageDrawable(resources.getDrawable(R.drawable.ic_direct_share_white));
        } else {
            if (!compName.equals(mDirectShareComponentName)) {
                mDirectShareComponentName = compName;
                try {
                    int size = resources.getDimensionPixelSize(R.dimen.qc_direct_share_icon_size);
                    iv.setMaxWidth(size);
                    iv.setMaxHeight(size);
                    Drawable icon = helper.getActivity().getPackageManager().getActivityIcon(mDirectShareComponentName);
                    iv.setImageDrawable(icon);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    iv.setImageDrawable(resources.getDrawable(R.drawable.ic_direct_share_white));
                    setEnabled(false);
                }
            }
        }
    }
}

class Item_print extends PieItem {
    public Item_print(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.printingEnabled() && !helper.isOnNewTabPage() && Build.VERSION.SDK_INT >= 19);
    }
}

class Item_go_to_home extends PieItem {
    public Item_go_to_home(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.goToHomeScreen();
    }
}

class Item_exit extends PieItem {
    public Item_exit(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.goToHomeScreen();
        Process.killProcess(Process.myPid());
    }
}

class Item_next_tab extends PieItem {
    public Item_next_tab(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.currentTabIndex() < helper.getTabCount() - 1 || Utils.isObfuscated());
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.dispatchKeyEvent(KeyEvent.KEYCODE_TAB, KeyEvent.META_CTRL_ON);
    }
}

class Item_previous_tab extends PieItem {
    public Item_previous_tab(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.currentTabIndex() > 0 || Utils.isObfuscated());
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.dispatchKeyEvent(KeyEvent.KEYCODE_TAB, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON);
    }
}

class Item_reader_mode extends PieItem {
    public Item_reader_mode(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(!Utils.isObfuscated() && (helper.isDistilledPage() ||
                (helper.getWebContents() != null && helper.nativeIsUrlDistillable())));
    }

    @Override
    public void onClick(ChromeHelper helper) {
        if (helper.isDistilledPage()) {
            String originalUrl = helper.getOriginalUrl();
            helper.loadUrl(originalUrl);
        } else {
            if (!helper.itemSelected(getMenuActionId())) {
                helper.distillCurrentPage();
            }
        }
    }
}

class Item_voice_search extends PieItem {
    public Item_voice_search(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(helper.isVoiceSearchEnabled() && !Utils.isObfuscated());
    }

    @Override
    public void onClick(ChromeHelper helper) {
        try {
            Utils.callMethod(helper.getLocationBar(), "startVoiceRecognition");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Item_recent_apps extends PieItem {
    public Item_recent_apps(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(Build.VERSION.SDK_INT < Build.VERSION_CODES.N);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        helper.toggleRecentApps();
    }
}

class Item_toggle_data_saver extends PieItem {
    private Resources mResources;

    public Item_toggle_data_saver(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        if (mResources == null) mResources = resources;
        setEnabled(!Utils.isObfuscated());
        ImageView view = (ImageView) getView();
        if (helper.isDataReductionEnabled(helper.getDataReductionSettings())) {
            view.setColorFilter(0x7C000000);
            view.setImageDrawable(resources.getDrawable(R.drawable.ic_data_saver_off_white));
        } else {
            view.setColorFilter(null);
            view.setImageDrawable(resources.getDrawable(R.drawable.ic_data_saver_white));
        }
    }

    @Override
    public void onClick(ChromeHelper helper) {
        Object dataSettings = helper.getDataReductionSettings();
        boolean enabled = helper.isDataReductionEnabled(dataSettings);
        if (helper.setDataReductionEnabled(dataSettings, !enabled)) {
            Toast.makeText(helper.getActivity(), mResources.getString(enabled ?
                    R.string.data_saver_disabled : R.string.data_saver_enabled), Toast.LENGTH_SHORT).show();
        }
    }
}

class Item_expand_notifications extends PieItem {
    public Item_expand_notifications(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(ChromeHelper helper) {
        Intent intent = new Intent(PieReceiver.EXPAND_NOTIFICATIONS_INTENT);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setComponent(new ComponentName(ChromePie.PACKAGE_NAME, PieReceiver.class.getName()));
        helper.getActivity().sendBroadcast(intent);
    }
}

class Item_open_recently_closed extends PieItem {
    public Item_open_recently_closed(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(ChromeHelper helper, Resources resources) {
        setEnabled(!helper.isIncognito() && helper.hasRecentlyClosedTabs());
    }
}
