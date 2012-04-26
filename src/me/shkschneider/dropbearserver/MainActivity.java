package me.shkschneider.dropbearserver;

import com.astuetz.viewpagertabs.ViewPagerTabs;
import com.markupartist.android.widget.ActionBar;

import me.shkschneider.dropbearserver.R;
import me.shkschneider.dropbearserver.Tasks.Checker;
import me.shkschneider.dropbearserver.Tasks.CheckerCallback;
import me.shkschneider.dropbearserver.Utils.RootUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements CheckerCallback<Boolean> {

	private static final String TAG = "DropBearServer";
	
	public static Boolean needToCheckDependencies = true;
	public static Boolean needToCheckDropbear = true;

	private static String appVersion = "1.0";

	private ActionBar mActionBar;
	private ViewPager mPager;
	private ViewPagerTabs mPagerTabs;
	private MainAdapter mAdapter;
	
	public static String getAppVersion() {
		return appVersion;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Introduction
		String appName = getResources().getString(R.string.app_name);
		String packageName = getApplication().getPackageName();
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
			appVersion = packageInfo.versionName.toString();
		}
		catch (Exception e) {
			appVersion = "1.0";
			Log.e(TAG, "MainActivity: onCreate(): " + e.getMessage());
		}
		Log.i(TAG, appName + " v" + appVersion + " (" + packageName + ") Android " + Build.VERSION.RELEASE + " (API-" + Build.VERSION.SDK + ")");
		
		// Header
		mActionBar = (ActionBar) findViewById(R.id.actionbar);
		mActionBar.setTitle(getResources().getString(R.string.app_name));
		mActionBar.setHomeAction(new HomeAction(this));
		mActionBar.addAction(new CheckAction(this));

		// ViewPagerTabs
		mAdapter = new MainAdapter(this);
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		mPagerTabs = (ViewPagerTabs) findViewById(R.id.tabs);
		mPagerTabs.setViewPager(mPager);
		goToDefaultPage();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (SettingsHelper.getInstance(getBaseContext()).getAssumeRootAccess() == true) {
			RootUtils.hasRootAccess = true;
			RootUtils.hasBusybox = true;
			if (needToCheckDropbear == true) {
				RootUtils.checkDropbear(this);
				updateAll();
			}
			else
				needToCheckDropbear = true;
		}
		else if (needToCheckDependencies == true) {
			Toast.makeText(this, "You can get rid of those checks in Settings", Toast.LENGTH_LONG).show();
			// Root dependencies
			check();
			needToCheckDependencies = false;
		}
		
		if (SettingsHelper.getInstance(getBaseContext()).getKeepScreenOn() == true) {
			this.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		else {
			this.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	public static Intent createIntent(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public void goToDefaultPage() {
		mPager.setCurrentItem(MainAdapter.DEFAULT_PAGE);
	}
	
	public void check() {
		// Checker
		Checker checker = new Checker(this, this);
		checker.execute();
	}

	public void onCheckerComplete(Boolean result) {
		updateAll();
	}
	
	public void updateAll() {
		mAdapter.updateAll();
	}
	
	public void updatePublicKeys() {
		mAdapter.updatePublicKeys();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		updatePublicKeys();
	}
}