package com.jt5.xposed.chromepie;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PieSettings extends PreferenceActivity {

    private Fragment mCurrentFragment;

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
      if (SubPreferenceFragment.class.getName().equals(fragmentName)) {
          return true;
      }
      return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionbar_kill:
                killChrome(false);
                return true;
            case R.id.menu_help:
                showHelpDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelpDialog() {
        View helpView;
        helpView = getLayoutInflater().inflate(R.layout.help_dialog, null);

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

    void killChrome(boolean launch) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = getPackageManager();
        List<Intent> chromeApps = getInstalledApps(am, pm);
        if (launch) {
            if (chromeApps.size() == 0) {
                Toast.makeText(this, "Cannot launch Chrome", Toast.LENGTH_SHORT).show();
            } else if (chromeApps.size() == 1) {
                startActivity(chromeApps.get(0));
            } else if (chromeApps.size() == 2) {
                String packageName = getMostRecentApp(am);
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                startActivity(launchIntent);
            }
        } else {
            Toast.makeText(this, "Chrome killed", Toast.LENGTH_SHORT).show();
        }
    }

    private List<Intent> getInstalledApps(ActivityManager am, PackageManager pm) {
        List<Intent> apps = new ArrayList<Intent>();
        Intent launch = pm.getLaunchIntentForPackage("com.android.chrome");
        if (launch != null) {
            apps.add(launch);
            am.killBackgroundProcesses("com.android.chrome");
        }
        launch = pm.getLaunchIntentForPackage("com.chrome.beta");
        if (launch != null) {
            apps.add(launch);
            am.killBackgroundProcesses("com.chrome.beta");
        }
        return apps;
    }

    private String getMostRecentApp(ActivityManager am) {
        String packageName = null;
        for (RunningTaskInfo task: am.getRunningTasks(10)){
            String pkg = task.topActivity.getPackageName();
            if (pkg.equals("com.android.chrome") || pkg.equals("com.chrome.beta")) {
                packageName = pkg;
                break;
            }
        }
        if (packageName == null) {
            packageName = "com.android.chrome";
        }
        return packageName;
    }

}
