package net.kdt.pojavlaunch.fragments;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

public class BedrockMenuFragment extends Fragment {
    public static final String TAG = "BedrockMenuFragment";
    private static final String BEDROCK_PACKAGE = "com.mojang.minecraftpe";
    private static final String PREFS = "seven_bedrock_downloads";
    private static final String PREF_DOWNLOAD_ID = "active_download_id";

    private EditText downloadUrl;
    private ProgressBar downloadProgress;
    private TextView downloadStatus;
    private Button downloadButton;
    private Button cancelDownloadButton;
    private DownloadManager downloadManager;
    private long activeDownloadId = -1L;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean receiverRegistered;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDownloadProgress();
            if (activeDownloadId != -1L && isAdded()) {
                progressHandler.postDelayed(this, 500L);
            }
        }
    };

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (completedId == activeDownloadId) {
                    handleDownloadComplete(completedId);
                }
            }
        }
    };

    private final ActivityResultLauncher<String[]> addonPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> importIntoMinecraft(uri));
    private final ActivityResultLauncher<String[]> worldPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> importIntoMinecraft(uri));
    private final ActivityResultLauncher<String[]> apkPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> installUserSelectedApk(uri));

    public BedrockMenuFragment() {
        super(R.layout.fragment_bedrock_menu);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        downloadManager = (DownloadManager) requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);

        TextView status = view.findViewById(R.id.bedrock_status);
        Button launch = view.findViewById(R.id.bedrock_launch_button);
        Button importApk = view.findViewById(R.id.bedrock_import_apk_button);
        Button importAddon = view.findViewById(R.id.bedrock_import_addon_button);
        Button importWorld = view.findViewById(R.id.bedrock_import_world_button);
        Button back = view.findViewById(R.id.bedrock_back_button);
        downloadUrl = view.findViewById(R.id.bedrock_download_url);
        downloadProgress = view.findViewById(R.id.bedrock_download_progress);
        downloadStatus = view.findViewById(R.id.bedrock_download_status);
        downloadButton = view.findViewById(R.id.bedrock_download_button);
        cancelDownloadButton = view.findViewById(R.id.bedrock_cancel_download_button);

        updateInstalledStatus(status, launch);
        boolean installed = isBedrockInstalled();
        importAddon.setEnabled(installed);
        importWorld.setEnabled(installed);

        launch.setOnClickListener(v -> {
            if (!isBedrockInstalled()) {
                updateInstalledStatus(status, launch);
                Toast.makeText(requireContext(),
                        "Minecraft Bedrock is not installed.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Intent gameIntent = requireContext().getPackageManager()
                    .getLaunchIntentForPackage(BEDROCK_PACKAGE);
            if (gameIntent == null) {
                Toast.makeText(requireContext(),
                        "Could not open Minecraft Bedrock.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(gameIntent);
        });

        importApk.setOnClickListener(v ->
                apkPicker.launch(new String[]{"application/vnd.android.package-archive"}));
        importAddon.setOnClickListener(v ->
                addonPicker.launch(new String[]{"*/*"}));
        importWorld.setOnClickListener(v ->
                worldPicker.launch(new String[]{"*/*"}));
        downloadButton.setOnClickListener(v -> startAddonDownload());
        cancelDownloadButton.setOnClickListener(v -> cancelAddonDownload());
        back.setOnClickListener(v ->
                Tools.backToMainMenu(requireActivity()));

        restoreActiveDownload();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            requireContext().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(downloadReceiver, filter);
        }
        receiverRegistered = true;
        if (activeDownloadId != -1L) {
            progressHandler.removeCallbacks(progressRunnable);
            progressHandler.post(progressRunnable);
        }
    }

    @Override
    public void onStop() {
        progressHandler.removeCallbacks(progressRunnable);
        if (receiverRegistered) {
            try {
                requireContext().unregisterReceiver(downloadReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            receiverRegistered = false;
        }
        super.onStop();
    }

    private void restoreActiveDownload() {
        activeDownloadId = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(PREF_DOWNLOAD_ID, -1L);
        if (activeDownloadId != -1L) {
            refreshDownloadProgress();
            if (receiverRegistered) progressHandler.post(progressRunnable);
        }
    }

    private void startAddonDownload() {
        String rawUrl = downloadUrl.getText().toString().trim();
        if (rawUrl.isEmpty()) {
            downloadUrl.setError("Enter a direct HTTPS download link");
            return;
        }

        Uri uri;
        try {
            uri = Uri.parse(rawUrl);
        } catch (Exception error) {
            downloadUrl.setError("Invalid link");
            return;
        }

        String scheme = uri.getScheme();
        String path = uri.getPath();
        String fileName = uri.getLastPathSegment();
        if (!"https".equalsIgnoreCase(scheme) || path == null || fileName == null) {
            downloadUrl.setError("Only direct HTTPS file links are supported");
            return;
        }

        String lowerName = fileName.toLowerCase(java.util.Locale.ROOT);
        if (!lowerName.endsWith(".mcpack") && !lowerName.endsWith(".mcworld")) {
            downloadUrl.setError("Only .mcpack and .mcworld files can be downloaded here");
            return;
        }

        if (activeDownloadId != -1L) {
            Toast.makeText(requireContext(),
                    "A download is already active. Cancel it before starting another.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("Seven Launcher — " + fileName);
            request.setDescription("Downloading Bedrock content");
            request.setMimeType("application/octet-stream");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalFilesDir(requireContext(),
                    Environment.DIRECTORY_DOWNLOADS, "Seven/" + fileName);

            activeDownloadId = downloadManager.enqueue(request);
            requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putLong(PREF_DOWNLOAD_ID, activeDownloadId).apply();
            downloadStatus.setText("Starting download…");
            downloadProgress.setProgress(0);
            downloadProgress.setVisibility(View.VISIBLE);
            cancelDownloadButton.setEnabled(true);
            downloadButton.setEnabled(false);
            progressHandler.removeCallbacks(progressRunnable);
            progressHandler.post(progressRunnable);
        } catch (Exception error) {
            activeDownloadId = -1L;
            downloadStatus.setText("Could not start download.");
            Toast.makeText(requireContext(),
                    "Download could not be started: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void refreshDownloadProgress() {
        if (downloadManager == null || activeDownloadId == -1L || !isAdded()) return;

        Cursor cursor = null;
        try {
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(activeDownloadId);
            cursor = downloadManager.query(query);
            if (cursor == null || !cursor.moveToFirst()) {
                finishDownloadUi("Download not found. You can try again.", false);
                return;
            }

            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long downloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            if (total > 0L) {
                int percent = (int) Math.min(100L, (downloaded * 100L) / total);
                downloadProgress.setIndeterminate(false);
                downloadProgress.setProgress(percent);
                downloadStatus.setText(percent + "%  •  " + formatBytes(downloaded)
                        + " / " + formatBytes(total));
            } else {
                downloadProgress.setIndeterminate(true);
                downloadStatus.setText("Downloading… " + formatBytes(downloaded));
            }

            if (status == DownloadManager.STATUS_FAILED) {
                int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                finishDownloadUi("Download failed (code " + reason + "). Check the link and retry.", false);
            } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                handleDownloadComplete(activeDownloadId);
            } else if (status == DownloadManager.STATUS_PAUSED) {
                downloadStatus.setText("Paused by Android/network. It will continue when possible.");
            }
        } catch (Exception error) {
            downloadStatus.setText("Checking download…");
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void handleDownloadComplete(long downloadId) {
        if (downloadId != activeDownloadId || !isAdded()) return;
        Uri downloadedUri = downloadManager.getUriForDownloadedFile(downloadId);
        if (downloadedUri == null) {
            finishDownloadUi("Download finished, but Android did not provide the file.", false);
            return;
        }

        String fileName = downloadUrl.getText().toString().trim();
        String path = Uri.parse(fileName).getPath();
        String lower = path == null ? "" : path.toLowerCase(java.util.Locale.ROOT);
        String mime = lower.endsWith(".mcworld")
                ? "application/octet-stream" : "application/octet-stream";

        finishDownloadUi("Download complete. Opening Minecraft…", true);
        if (!isBedrockInstalled()) {
            downloadStatus.setText("Downloaded. Install Minecraft, then import the file from Downloads/Seven.");
            return;
        }

        Intent importIntent = new Intent(Intent.ACTION_VIEW);
        importIntent.setDataAndType(downloadedUri, mime);
        importIntent.setPackage(BEDROCK_PACKAGE);
        importIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(importIntent);
        } catch (ActivityNotFoundException error) {
            downloadStatus.setText("Downloaded. Minecraft did not accept the file automatically; use IMPORT ADD-ON / IMPORT WORLD.");
        }
    }

    private void cancelAddonDownload() {
        if (activeDownloadId == -1L) return;
        downloadManager.remove(activeDownloadId);
        finishDownloadUi("Download cancelled.", false);
    }

    private void finishDownloadUi(String message, boolean completed) {
        progressHandler.removeCallbacks(progressRunnable);
        activeDownloadId = -1L;
        requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(PREF_DOWNLOAD_ID).apply();
        downloadProgress.setIndeterminate(false);
        downloadProgress.setProgress(completed ? 100 : 0);
        downloadStatus.setText(message);
        downloadButton.setEnabled(true);
        cancelDownloadButton.setEnabled(false);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void updateInstalledStatus(@NonNull TextView status, @NonNull Button launch) {
        boolean installed = isBedrockInstalled();
        status.setText(installed
                ? "Minecraft Bedrock is installed on this device."
                : "Minecraft Bedrock was not found. Select an APK you already have, or install the game from your store.");
        launch.setText(installed ? "OPEN MINECRAFT" : "CHECK AGAIN");
    }

    private void installUserSelectedApk(@Nullable Uri uri) {
        if (uri == null) return;
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(),
                    "Android could not open this APK file.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void importIntoMinecraft(@Nullable Uri uri) {
        if (uri == null) return;
        if (!isBedrockInstalled()) {
            Toast.makeText(requireContext(),
                    "Minecraft Bedrock is not installed.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent importIntent = new Intent(Intent.ACTION_VIEW);
        importIntent.setDataAndType(uri, "application/octet-stream");
        importIntent.setPackage(BEDROCK_PACKAGE);
        importIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(importIntent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(),
                    "Minecraft could not import this file. Check that it is a valid .mcpack or .mcworld file.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBedrockInstalled() {
        try {
            requireContext().getPackageManager().getPackageInfo(BEDROCK_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }
}