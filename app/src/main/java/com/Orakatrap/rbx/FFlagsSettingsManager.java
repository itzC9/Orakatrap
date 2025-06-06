package com.Orakatrap.rbx;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.Orakatrap.rbx.Utility.FileTool;
import com.Orakatrap.rbx.Utility.FileToolAlt;
import com.Orakatrap.rbx.Utility.INeedPath;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FFlagsSettingsManager {
    private final Context context;

    public FFlagsSettingsManager(Context context) {
        this.context = context;
    }

    public static String getPackageTarget(Context context) {
        String preferredApp = getSetting1(context, "PreferredRobloxApp");

        if (Objects.equals(preferredApp, "Roblox VN")) {
            return "com.roblox.client.vnggames";
        } else if (Objects.equals(preferredApp, "Roblox")) {
            return "com.roblox.client";
        }

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.roblox.client", 0);
            return "com.roblox.client";
        } catch (PackageManager.NameNotFoundException e) {
            try {
                pm.getPackageInfo("com.roblox.client.vnggames", 0);
                return "com.roblox.client.vnggames";
            } catch (PackageManager.NameNotFoundException ignored) {
                return "com.roblox.client"; // fallback
            }
        }
    }

    public static void applyFastFlag(Context context) throws IOException {
        String rbxpathh = INeedPath.getRBXPathDir(context, getPackageTarget(context));

        if (isExistSettingKey1(context, "UseFastFlagManager")) {
            boolean bo3 = Boolean.parseBoolean(getSetting1(context, "UseFastFlagManager"));
            if (bo3) {
                throw new IllegalStateException("No permission to apply fast flags");
            }
        }

        File clientSettingsDir = new File(context.getFilesDir(), "Modifications/ClientSettings");
        File outFile1 = new File(clientSettingsDir, "ClientAppSettings.json");

        if (FileToolAlt.isRootAvailable()) {
            String targetDir = rbxpathh + "exe/ClientSettings";
            String targetFile = targetDir + "/ClientAppSettings.json";

            FileToolAlt.createDirectoryWithPermissions(targetDir);

            if (FileToolAlt.pathExists(targetDir)) {
                FileToolAlt.writeFile(targetFile, FileTool.read(outFile1));
                return;
            } else {
                throw new IOException("Directory blocked by SELinux or does not exist: " + targetDir);
            }
        }

        // Fallback
        String fallbackDirPath = rbxpathh + "exe/ClientSettings";
        File fallbackDir = new File(fallbackDirPath);
        File fallbackFile = new File(fallbackDir, "ClientAppSettings.json");

        if (!fallbackDir.exists() && !fallbackDir.mkdirs()) {
            throw new IOException("Failed to create fallback ClientSettings directory: " + fallbackDirPath);
        }

        String content = FileTool.read(outFile1);
        if (content.isEmpty()) {
            throw new IOException("Source file is empty or unreadable: " + outFile1.getAbsolutePath());
        }

        FileTool.write(fallbackFile, content);
    }

    public static boolean isExistSettingKey1(Context context, String keyName) {
        File filePath = new File(context.getFilesDir(), "AppSettings.json");

        if (!filePath.exists()) return false;

        try {
            JSONObject jsonObject = new JSONObject(FileTool.read(filePath));
            return jsonObject.has(keyName);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getSetting1(Context context, String flagName) {
        File filePath = new File(context.getFilesDir(), "LastAppSettings.json");

        if (!filePath.exists()) return null;

        try {
            String content = FileTool.read(filePath);
            JSONObject jsonObject = new JSONObject(content);
            return jsonObject.optString(flagName, null);
        } catch (Exception e) {
            return null;
        }
    }

    public String getPreset(String flagName) {
        File clientSettingsDir = new File(context.getFilesDir(), "Modifications/ClientSettings");
        File filePath = new File(clientSettingsDir, "LastClientAppSettings.json");

        if (!filePath.exists()) return null;

        try {
            String content = FileTool.read(filePath);
            JSONObject jsonObject = new JSONObject(content);
            return jsonObject.optString(flagName, null);
        } catch (Exception e) {
            return null;
        }
    }

    public String getSetting(String flagName) {
        File filePath = new File(context.getFilesDir(), "AppSettings.json");

        if (!filePath.exists()) return null;

        try {
            String content = FileTool.read(filePath);
            JSONObject jsonObject = new JSONObject(content);
            return jsonObject.optString(flagName, null);
        } catch (Exception e) {
            return null;
        }
    }
}
