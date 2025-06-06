package com.Orakatrap.rbx;

import static com.Orakatrap.rbx.Utility.INeedPath.getRBXPathDir;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.Orakatrap.rbx.Integrations.ActivityWatcher;
import com.Orakatrap.rbx.UI.Elements.CustomDialogs.AboutFragment;
import com.Orakatrap.rbx.UI.Elements.CustomDialogs.MessageboxFragment;
import com.Orakatrap.rbx.Utility.FileTool;
import com.Orakatrap.rbx.Utility.FileToolAlt;
import com.Orakatrap.rbx.Utility.aboutApp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
	private ExecutorService RBXActivityWatcher;
	private LinearLayout buttonLaunchrbx;
	private LinearLayout buttonConfiguresettings;
	private LinearLayout buttonAbout;
	private TextView textview_version;
	private LaunchHandler launchHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		setContentView(R.layout.main);

		initialize();
		initializeLogic();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (RBXActivityWatcher != null && !RBXActivityWatcher.isShutdown()) {
			RBXActivityWatcher.shutdownNow();
		}
	}

	private void initialize() {
		launchHandler = new LaunchHandler();
		launchHandler.setContext(this);
		launchHandler.setFragmentManager(getSupportFragmentManager());

		buttonLaunchrbx = findViewById(R.id.button_launchrbx);
		buttonConfiguresettings = findViewById(R.id.button_configuresettings);
		buttonAbout = findViewById(R.id.button_about);
		textview_version = findViewById(R.id.textview_version);

		if (buttonLaunchrbx != null) {
			buttonLaunchrbx.setOnClickListener(v -> {
				boolean isApplied = false;
				try {
					FFlagsSettingsManager.applyFastFlag(getApplicationContext());
					isApplied = true;
				} catch (Exception e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
				try {
					launchHandler.LaunchRoblox(isApplied);
				} catch (IOException | RuntimeException e) {
					Toast.makeText(this, "Failed to launch Roblox: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			});
		}

		if (buttonConfiguresettings != null) {
			buttonConfiguresettings.setOnClickListener(v -> {
				startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
				finish();
			});
		}

		if (buttonAbout != null) {
			buttonAbout.setOnClickListener(v -> {
				AboutFragment dialog = new AboutFragment();
				dialog.show(getSupportFragmentManager(), "aD");
			});
		}
	}

	private void initializeLogic() {
		aboutApp app = new aboutApp(getApplicationContext());
		if (textview_version != null) {
			textview_version.setText(app.getAppVersion());
		}

		AstyleButtonBlack1(buttonAbout);
		AstyleButtonBlack1(buttonConfiguresettings);
		AstyleButtonBlack1(buttonLaunchrbx);

		StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
		long blockSize = stat.getBlockSizeLong();
		long free = (stat.getAvailableBlocksLong() * blockSize) / (1024 * 1024 * 1024);

		if (free < 0.6) {
			showStorageFullDialog();
		} else {
			if (!FileToolAlt.isRootAvailable()) {
				hellohelloo13();
			}
		}
	}
	
	public void LaunchWatcher() {
		FFlagsSettingsManager manager = new FFlagsSettingsManager(getApplicationContext());

		boolean getSetting1 = Boolean.parseBoolean(manager.getSetting("EnableActivityTracking"));
		boolean getSetting2 = Boolean.parseBoolean(manager.getSetting("ShowServerDetails"));

		if (!getSetting1 || !getSetting2) return;

		if (RBXActivityWatcher != null && !RBXActivityWatcher.isShutdown()) {
			RBXActivityWatcher.shutdownNow();
		}

		RBXActivityWatcher = Executors.newSingleThreadExecutor();

		String target = FFlagsSettingsManager.getPackageTarget(getApplicationContext());

        String rbxpath = getRBXPathDir(getApplicationContext(), target);
		if (rbxpath == null) {
			Toast.makeText(this, "Roblox path is null", Toast.LENGTH_SHORT).show();
			return;
		}

		ActivityWatcher watcher = new ActivityWatcher(getApplicationContext(), rbxpath.concat("appData/logs"), RBXActivityWatcher);
		RBXActivityWatcher.submit(watcher::start);
	}


	public void hellohelloo13() {
		String target1 = FFlagsSettingsManager.getPackageTarget(getApplicationContext());
		String rbxPath1 = getRBXPathDir(getApplicationContext(), target1);

		if (!FileToolAlt.isRootAvailable()) {
			try {
				ApplicationInfo info = getApplicationContext().getPackageManager().getApplicationInfo(target1, 0);
				rbxPath1 = info.dataDir;
			} catch (PackageManager.NameNotFoundException ignored) {

			}

			if (!FileTool.isExist(rbxPath1)) {
				showNOTROOTBox();
			}
		}
	}

	public void showStorageFullDialog() {
		runOnUiThread(() -> {
			MessageboxFragment fragment = getMessageboxFragment3();
			fragment.show(getSupportFragmentManager(), "Messagebox");
		});
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment3() {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.disableCancel();
		fragment.setMessageText(getString(R.string.StorageFullMessage));
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override
			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
					finish();
				});
			}
			@Override
			public void onCancelClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
				});
			}
		});
		return fragment;
	}

	public void showRBXNotExistDialog() {
		runOnUiThread(() -> {
			MessageboxFragment fragment = getMessageboxFragment4();
			fragment.show(getSupportFragmentManager(), "Messagebox");
		});
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment4() {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.setMessageText("Roblox is either not installed or not added to the cloner app");
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override
			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
					finish();
				});
			}
			@Override
			public void onCancelClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
				});
			}
		});
		return fragment;
	}

	public void showNOTROOTBox() {
		runOnUiThread(() -> {
			MessageboxFragment fragment = getMessageboxFragment1();
			fragment.show(getSupportFragmentManager(), "Messagebox");
		});
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment1() {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.disableCancel();
		fragment.setMessageText("You don't seem to have root access, please use a cloning app but make sure it's trusted");
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override
			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
					finish();
				});
			}
			@Override
			public void onCancelClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
					finish();
				});
			}
		});
		return fragment;
	}

	public boolean isPackageInstalled(String packageName) {
		try {
			getPackageManager().getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	public void AstyleButtonBlack1(LinearLayout button) {
		if (button == null) return;
		GradientDrawable drawable = new GradientDrawable();
		drawable.setCornerRadius(5);
		drawable.setStroke(2, Color.parseColor("#0C0F19"));
		drawable.setColor(Color.parseColor("#000000"));
		button.setBackground(drawable);
	}
}
