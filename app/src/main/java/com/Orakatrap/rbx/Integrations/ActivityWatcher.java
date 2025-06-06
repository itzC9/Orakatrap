package com.Orakatrap.rbx.Integrations;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;

import com.Orakatrap.rbx.Logger;

import com.Orakatrap.rbx.Models.Entities.ActivityData;
import com.Orakatrap.rbx.R;
import com.Orakatrap.rbx.Utility.FileToolAlt;

public class ActivityWatcher {
    private final String logLocation;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService serviceE;

    private String lastPlaceId;
    private String lastJobId;
    private AlertDialog currentDialogLastServer;
    public ActivityWatcher(Context context, String logFilePath, ExecutorService service) {
        this.context = context;
        this.logLocation = logFilePath;
        this.serviceE = service;
    }

    private void runOnUiThread(Runnable action) {
        mainHandler.post(action);
    }

    public void showLastServerDialog(String placeId, String instanceId) {
        if (currentDialogLastServer != null && currentDialogLastServer.isShowing()) {
            currentDialogLastServer.dismiss();
        }

        String deepLink = "https://www.roblox.com/games/start?placeId=" + placeId + "&gameInstanceId=" + instanceId;

        currentDialogLastServer = new AlertDialog.Builder(context)
                .setTitle("Message")
                .setMessage("Looks like you left the server, I caught your last server")
                .setCancelable(false)
                .setPositiveButton("Copy Deeplink", (dialog, id) -> {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Roblox Server Link", deepLink);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "Successfully copied to clipboard", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Failed to copy to clipboard", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel())
                .create();

        currentDialogLastServer.show();
    }

    public void start() {
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            //showToast(appContext, "ActivityWatcher thread started");

            final String joinGameStr = "[FLog::Output] ! Joining game";
            final String connectionStr = "[FLog::Output] Connection accepted from";

            boolean isDir;
            try {
                isDir = FileToolAlt.isDirectory(logLocation);
                System.out.println("Is directory: " + isDir);
            } catch (IOException e) {
               // showToast(appContext, "Invalid log directory");
            }

            File[] logsList;
            try {
                logsList = FileToolAlt.listFiles(logLocation);
            } catch (IOException e) {
                logsList = new File(logLocation).listFiles(); // fallback
                Logger logger = new Logger(appContext);
                logger.initializePersistent();
                logger.writeLine("ActivityWatcher", "FileToolAlt.listFiles failed, fallback used: " + e.getMessage());
            }

            if (logsList == null || logsList.length == 0) {
                showToast(appContext, "Place Information Failed, Notification disabled");
                return;
            }

            Arrays.sort(logsList, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            File latestLog = logsList[0];

            try (FileInputStream fis = new FileInputStream(latestLog);
                 FileChannel channel = fis.getChannel()) {

                long filePointer = channel.size();
                long lastModified = latestLog.lastModified();
                boolean foundJoin = false;
                Logger logger = new Logger(appContext);
                logger.initializePersistent();

                while (!Thread.currentThread().isInterrupted()) {
                    if (latestLog.lastModified() != lastModified || latestLog.length() > filePointer) {
                        lastModified = latestLog.lastModified();

                        channel.position(filePointer);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(channel)));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            if (line.contains("[FLog::Network] NetworkClient:Remove")) {
                                foundJoin = false;
                                NotificationManager nm = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                                if (nm != null) nm.cancel(1001);
                            } else if (!foundJoin && line.contains(joinGameStr)) {
                                int placeIdStart = line.indexOf("place ") + 6;
                                int placeIdEnd = line.indexOf(" at", placeIdStart);
                                int jobIdStart = line.indexOf("'") + 1;
                                int jobIdEnd = line.indexOf("'", jobIdStart);

                                if (placeIdStart > 5 && placeIdEnd > placeIdStart && jobIdStart > 0 && jobIdEnd > jobIdStart) {
                                    lastPlaceId = line.substring(placeIdStart, placeIdEnd);
                                    lastJobId = line.substring(jobIdStart, jobIdEnd);
                                    foundJoin = true;
                                }
                            } else if (foundJoin && line.contains(connectionStr)) {
                                String[] parts = line.split(" ");
                                String[] ipSplit = parts[parts.length - 1].split("\\|");

                                if (ipSplit.length == 2) {
                                    String ip = ipSplit[0];
                                    showConnectionNotification(ip, logger);
                                }

                                foundJoin = false;
                            }
                        }

                        filePointer = channel.position();
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                Logger logger = new Logger(appContext);
                logger.initializePersistent();
                logger.writeLine("ActivityWatcher", "Error reading log file: " + e.getMessage());
            }
        }).start();
    }

    private long lastToastTime = 0;

    private void throttledToast(Context context, String msg) {
        long now = System.currentTimeMillis();
        if (now - lastToastTime > 1000) {
            lastToastTime = now;
            showToast(context, msg);
        }
    }

    private void showToast(Context context, String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        );
    }

    private void showConnectionNotification(String ip, Logger logger) {
        runOnUiThread(() -> {
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(context, "rbx_connection_channel")
                    : new Notification.Builder(context);

            ActivityData data = new ActivityData(ip);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            data.queryServerLocation(new ActivityData.LocationCallback() {
                @Override
                public void onLocationResolved(String location) {
                    String message = "Server location: " + location;
                    builder.setSmallIcon(R.drawable.Orakatrap_logo)
                            .setContentTitle("Connected to server")
                            .setContentText(message)
                            .setAutoCancel(true);

                    logger.writeLine("NotifyIconWrapper::ShowAlert", message);
                    if (notificationManager != null) {
                        notificationManager.notify(1001, builder.build());
                    }
                }

                @Override
                public void onFailure() {
                    String failMsg = "Server location: Failed";
                    builder.setSmallIcon(R.drawable.Orakatrap_logo)
                            .setContentTitle("Connected to server")
                            .setContentText("Location lookup failed")
                            .setAutoCancel(true);

                    logger.writeLine("NotifyIconWrapper::ShowAlert", failMsg);
                    if (notificationManager != null) {
                        notificationManager.notify(1001, builder.build());
                    }
                }
            });
        });
    }
}
