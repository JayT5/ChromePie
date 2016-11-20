package com.jt5.xposed.chromepie.settings;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jt5.xposed.chromepie.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PieSettings extends PreferenceActivity {

    private Fragment mCurrentFragment;

    public static final List<String> CHROME_PACKAGE_NAMES = Arrays.asList(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.metalasfook.nochromo",
        "org.chromium.chrome",
        "tugapower.codeaurora.browser",
        "tugapower.codeaurora.browser.beta",
        "org.notphenom.swe.browser",
        "com.rsbrowser.browser",
        "com.mokee.yubrowser",
        "com.hsv.freeadblockerbrowser"
    );

    public static final List<String> CHROME_ACTIVITY_CLASSES = Arrays.asList(
            "org.chromium.chrome.browser.ChromeTabbedActivity",
            "org.chromium.chrome.browser.document.DocumentActivity",
            "org.chromium.chrome.browser.document.IncognitoDocumentActivity",
            "com.google.android.apps.chrome.ChromeTabbedActivity",
            "com.google.android.apps.chrome.document.DocumentActivity",
            "com.google.android.apps.chrome.document.IncognitoDocumentActivity",
            "com.google.android.apps.chrome.Main"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() == null || getIntent().getExtras().getString(PreferenceActivity.EXTRA_SHOW_FRAGMENT) == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PiePreferenceFragment()).commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFragment != null && mCurrentFragment instanceof SubPreferenceFragment) {
            ((SubPreferenceFragment) mCurrentFragment).finishFragment();
        } else {
            super.onBackPressed();
        }
        mCurrentFragment = null;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        mCurrentFragment = fragment;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SubPreferenceFragment.class.getName().equals(fragmentName) ||
                MenuPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help:
                showHelpDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showHelpDialog() {
        View helpView = getLayoutInflater().inflate(R.layout.help_dialog, null);

        ((TextView) helpView.findViewById(R.id.about_thread)).setMovementMethod(LinkMovementMethod.getInstance());

        // Display the correct version
        try {
            ((TextView) helpView.findViewById(R.id.version)).setText(getString(R.string.app_version,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (NameNotFoundException e) {

        }

        // Prepare and show the dialog
        Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setTitle(R.string.app_name);
        dlgBuilder.setCancelable(true);
        dlgBuilder.setIcon(R.drawable.ic_launcher);
        dlgBuilder.setPositiveButton(android.R.string.ok, null);
        dlgBuilder.setView(helpView);
        dlgBuilder.show();
    }

    void killChrome(Set<String> extraPkgs, boolean launch) {
        List<Intent> chromeApps = getInstalledApps(extraPkgs);
        if (chromeApps.isEmpty()) {
            Toast.makeText(this, getResources().getString(R.string.chrome_not_found), Toast.LENGTH_SHORT).show();
        } else {
            if (launch) {
                if (chromeApps.size() == 1) {
                    startActivity(chromeApps.get(0));
                } else {
                    Intent chooserIntent = Intent.createChooser(chromeApps.remove(chromeApps.size() - 1),
                            getResources().getString(R.string.chrome_app_chooser));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, chromeApps.toArray(new Parcelable[chromeApps.size()]));
                    startActivity(chooserIntent);
                }
            } else {
                Toast.makeText(this, getResources().getString(R.string.chrome_killed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<Intent> getInstalledApps(Set<String> extraPkgs) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = getPackageManager();
        List<Intent> apps = new ArrayList<Intent>();
        extraPkgs.addAll(CHROME_PACKAGE_NAMES);
        for (String packageName : extraPkgs) {
            Intent launch = pm.getLaunchIntentForPackage(packageName);
            if (launch != null) {
                am.killBackgroundProcesses(packageName);
                apps.add(launch);
            }
        }
        return apps;
    }

}
