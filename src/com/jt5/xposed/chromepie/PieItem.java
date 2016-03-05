package com.jt5.xposed.chromepie;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import android.view.ViewGroup;
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

    protected void onOpen(Controller control, XModuleResources mXResources) {

    }

    public void onClick(Controller control) {
        control.itemSelected(getMenuActionId());
    }

}

class Item_back extends PieItem {
    public Item_back(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.canGoBack());
    }

    @Override
    public void onClick(Controller control) {
        if (!control.itemSelected(getMenuActionId())) {
            try {
                Utils.callMethod(control.getChromeActivity(), "goBack");
                return;
            } catch (NoSuchMethodError nsme) {

            }
            try {
                Utils.callMethod(control.getCurrentTab(), "goBack");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
    }
}

class Item_forward extends PieItem {
    public Item_forward(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.canGoForward());
    }
}

class Item_refresh extends PieItem {
    public Item_refresh(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (control.isLoading()) {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_stop_white));
        } else {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_refresh_white));
        }
    }

    @Override
    public void onClick(Controller control) {
        if (!control.itemSelected(getMenuActionId())) {
            Object tab = control.getCurrentTab();
            try {
                if (control.isLoading()) {
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
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (control.isDocumentMode()) {
            setEnabled(!control.isOnNewTabPage() || control.isIncognito());
        }
    }
}

class Item_new_incognito_tab extends PieItem {
    public Item_new_incognito_tab(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (control.isDocumentMode()) {
            setEnabled(!(control.isOnNewTabPage() && control.isIncognito()));
        }
    }
}

class Item_close_tab extends PieItem {
    public Item_close_tab(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(Controller control) {
        if (control.isDocumentMode()) {
            control.closeDocumentTab();
        } else {
            control.closeCurrentTab();
        }
    }
}

class Item_close_all extends PieItem {
    public Item_close_all(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(Controller control) {
        if (!control.isIncognito() && !control.isDocumentMode()) {
            control.toggleOverview();
        }
        try {
            Utils.callMethod(control.getTabModel(), "closeAllTabs");
        } catch (NoSuchMethodError nsme) {
            int id = getMenuActionId();
            if (control.isIncognito()) {
                id = control.getResIdentifier("close_all_incognito_tabs_menu_id");
            }
            control.itemSelected(id);
        }
    }
}

class Item_bookmarks extends PieItem {
    public Item_bookmarks(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(final Controller control) {
        if (control.getTabCount() == 0) {
            String ntp = control.getChromeUrl("NTP_URL");
            control.launchUrl(ntp);
            // Allow time for the new tab animation to finish
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Item_bookmarks.super.onClick(control);
                }
            }, 150);
        } else {
            super.onClick(control);
        }
    }
}

class Item_add_bookmark extends PieItem {
    public Item_add_bookmark(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (control.bookmarkExists()) {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_add_bookmark_white));
        } else {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_added_bookmark_white));
        }
        setEnabled(control.editBookmarksSupported());
    }
}

class Item_history extends PieItem {
    public Item_history(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(Controller control) {
        if (control.getTabCount() == 0) {
            String history = control.getChromeUrl("HISTORY_URL");
            control.launchUrl(history);
        } else {
            super.onClick(control);
        }
    }
}

class Item_most_visited extends PieItem {
    public Item_most_visited(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(!control.isIncognito());
    }

    @Override
    public void onClick(Controller control) {
        String ntp = control.getChromeUrl("NTP_URL");
        if (control.getTabCount() == 0) {
            control.launchUrl(ntp);
        } else {
            control.loadUrl(ntp);
        }
    }
}

class Item_recent_tabs extends PieItem {
    public Item_recent_tabs(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.syncSupported() && !control.isIncognito());
    }

    @Override
    public void onClick(Controller control) {
        if (control.getTabCount() == 0) {
            String recents = control.getChromeUrl("RECENT_TABS_URL");
            control.launchUrl(recents);
        } else {
            super.onClick(control);
        }
    }
}

class Item_show_tabs extends PieItem {
    public Item_show_tabs(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        int tabCount = control.getTabCount();
        setEnabled(!control.isTablet() && tabCount != 0);
        TextView tv = (TextView) ((ViewGroup) getView()).getChildAt(1);
        tv.setText(Integer.toString(tabCount));
    }

    @Override
    public void onClick(Controller control) {
        if (control.isDocumentMode()) {
            control.toggleRecentApps();
        } else {
            control.toggleOverview();
        }
    }

    @Override
    public void setAlpha(float alpha) {
        final int alphaInt = Math.round(alpha * (isEnabled() ? 255 : 77));
        final ImageView icon = (ImageView) ((ViewGroup) getView()).getChildAt(0);
        final TextView count = (TextView) ((ViewGroup) getView()).getChildAt(1);
        icon.setAlpha((float) alphaInt / 255);
        count.setTextColor(Color.argb(alphaInt, 255, 255, 255));
        count.getBackground().setAlpha(alphaInt);
    }
}

class Item_add_to_home extends PieItem {
    public Item_add_to_home(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.addToHomeSupported() && !control.isIncognito() && !control.isOnNewTabPage());
    }
}

class Item_find_in_page extends PieItem {
    public Item_find_in_page(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.tabSupportsFinding());
    }
}

class Item_edit_url extends PieItem {
    public Item_edit_url(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    public void onClick(Controller control) {
        try {
            if (!control.itemSelected(getMenuActionId())) {
                Utils.callMethod(control.getLocationBar(), "requestUrlFocus");
            }
            EditText urlBar = control.getUrlBar();
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
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (control.isDesktopUserAgent()) {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_mobile_site_white));
        } else {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_desktop_site_white));
        }
    }
}

class Item_fullscreen extends PieItem {
    public Item_fullscreen(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (control.isFullscreen()) {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_fullscreen_exit_white));
        } else {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_fullscreen_white));
        }
    }

    @Override
    public void onClick(Controller control) {
        control.setFullscreen(!control.isFullscreen());
    }
}

class Item_scroll_to_top extends PieItem {
    public Item_scroll_to_top(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.getContentViewCore() != null);
    }

    @Override
    public void onClick(Controller control) {
        try {
            Object contentViewCore = control.getContentViewCore();
            Integer scrollOffset = (Integer) Utils.callMethod(contentViewCore, "computeVerticalScrollOffset");
            Integer scrollExtent = (Integer) Utils.callMethod(contentViewCore, "computeVerticalScrollExtent");
            control.scroll(contentViewCore, scrollOffset + scrollExtent, -control.getTopControlsDimen());
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
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.getContentViewCore() != null);
    }

    @Override
    public void onClick(Controller control) {
        try {
            Object contentViewCore = control.getContentViewCore();
            Integer scrollRange = (Integer) Utils.callMethod(contentViewCore, "computeVerticalScrollRange");
            Integer scrollOffset = (Integer) Utils.callMethod(contentViewCore, "computeVerticalScrollOffset");
            control.scroll(contentViewCore, -scrollRange + scrollOffset, scrollRange);
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
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(!control.isOnNewTabPage());
    }
}

class Item_direct_share extends PieItem {
    private ComponentName mDirectShareComponentName;

    public Item_direct_share(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        ComponentName compName = control.getShareComponentName();
        setEnabled(compName != null && !control.isOnNewTabPage());
        if (compName == null) {
            ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_direct_share_white));
        } else {
            if (!compName.equals(mDirectShareComponentName)) {
                mDirectShareComponentName = compName;
                try {
                    int shareSize = mXResources.getDimensionPixelSize(R.dimen.qc_direct_share_icon_size);
                    Drawable shareIcon = control.getChromeActivity().getPackageManager().getActivityIcon(mDirectShareComponentName);
                    Bitmap bitmap = ((BitmapDrawable) shareIcon).getBitmap();
                    shareIcon = new BitmapDrawable(mXResources, Bitmap.createScaledBitmap(bitmap, shareSize, shareSize, true));
                    ((ImageView) getView()).setImageDrawable(shareIcon);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_direct_share_white));
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
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.printingEnabled() && !control.isOnNewTabPage() && Build.VERSION.SDK_INT >= 19);
    }
}

class Item_go_to_home extends PieItem {
    public Item_go_to_home(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(Controller control) {
        control.goToHomeScreen();
    }
}

class Item_exit extends PieItem {
    public Item_exit(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(Controller control) {
        control.goToHomeScreen();
        Process.killProcess(Process.myPid());
    }
}

class Item_next_tab extends PieItem {
    public Item_next_tab(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.tabExistsAtIndex(1));
    }

    @Override
    public void onClick(Controller control) {
        int index = control.getTabIndex(control.getCurrentTab());
        if (index != -1) {
            control.showTabByIndex(index + 1);
        }
    }
}

class Item_previous_tab extends PieItem {
    public Item_previous_tab(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.tabExistsAtIndex(-1));
    }

    @Override
    public void onClick(Controller control) {
        int index = control.getTabIndex(control.getCurrentTab());
        if (index != -1) {
            control.showTabByIndex(index - 1);
        }
    }
}

class Item_reader_mode extends PieItem {
    public Item_reader_mode(View view, String id, int action) {
        super(view, id, action);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled((control.getWebContents() != null && control.nativeIsUrlDistillable()) || control.isDistilledPage());
    }

    @Override
    public void onClick(Controller control) {
        if (control.isDistilledPage()) {
            String originalUrl = control.getOriginalUrl();
            control.loadUrl(originalUrl);
        } else {
            if (!control.itemSelected(getMenuActionId())) {
                control.distillCurrentPage();
            }
        }
    }
}

class Item_voice_search extends PieItem {
    public Item_voice_search(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        setEnabled(control.isVoiceSearchEnabled());
    }

    @Override
    public void onClick(Controller control) {
        try {
            Utils.callMethod(control.getLocationBar(), "startVoiceRecognition");
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
    public void onClick(Controller control) {
        control.toggleRecentApps();
    }
}

class Item_toggle_data_saver extends PieItem {
    private XModuleResources mResources;

    public Item_toggle_data_saver(View view, String id, int action) {
        super(view, id);
    }

    @Override
    protected void onOpen(Controller control, XModuleResources mXResources) {
        if (mResources == null) mResources = mXResources;
        try {
            boolean enabled = (Boolean) Utils.callMethod(control.getDataReductionSettings(), "isDataReductionProxyEnabled");
            if (enabled) {
                ((ImageView) getView()).setColorFilter(0xFF676f73);
                ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_data_saver_off_white));
            } else {
                ((ImageView) getView()).setColorFilter(null);
                ((ImageView) getView()).setImageDrawable(mXResources.getDrawable(R.drawable.ic_data_saver_white));
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    @Override
    public void onClick(Controller control) {
        try {
            Object dataSettings = control.getDataReductionSettings();
            boolean enabled = (Boolean) Utils.callMethod(dataSettings, "isDataReductionProxyEnabled");
            Utils.callMethod(dataSettings, "setDataReductionProxyEnabled", control.getChromeActivity(), !enabled);
            Toast.makeText(control.getChromeActivity(), mResources.getString(enabled ?
                    R.string.data_saver_disabled : R.string.data_saver_enabled), Toast.LENGTH_SHORT).show();
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }
}

class Item_expand_notifications extends PieItem {
    public Item_expand_notifications(View view, String id, int action) {
        super(view, id);
    }

    @Override
    public void onClick(Controller control) {
        Intent intent = new Intent(PieReceiver.EXPAND_NOTIFICATIONS_INTENT);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setComponent(new ComponentName(ChromePie.PACKAGE_NAME, PieReceiver.class.getName()));
        control.getChromeActivity().sendBroadcast(intent);
    }
}
