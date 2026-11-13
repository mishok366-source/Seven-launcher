#!/usr/bin/env python3
"""Harden the SevenX EXE/MSI hand-off after the base Winlator bridge patch."""
from pathlib import Path
import sys

repo = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
runtime = repo / "seven-gamehub/winlator-source"
bridge = runtime / "app/src/main/java/com/winlator/SevenXBridgeActivity.java"
display = runtime / "app/src/main/java/com/winlator/XServerDisplayActivity.java"
runtime_client = repo / "seven-gamehub/app/src/main/java/com/seven/gamehub/runtime/WindowsRuntime.kt"
receiver = repo / "seven-gamehub/app/src/main/java/com/seven/gamehub/runtime/RuntimeLaunchReceiver.kt"
main_activity = repo / "seven-gamehub/app/src/main/java/com/seven/gamehub/MainActivity.kt"

def replace_once(path: Path, old: str, new: str):
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected one patch anchor in {path}: found {count}")
    path.write_text(source.replace(old, new, 1), encoding="utf-8")

if not bridge.is_file() or not display.is_file():
    raise SystemExit("Base SevenX bridge patch must run before installer hardening")

bridge.write_text(r'''package com.winlator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.provider.OpenableColumns;
import android.widget.Toast;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONObject;

public class SevenXBridgeActivity extends Activity {
    private static final int CONTRACT_VERSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent sourceIntent = getIntent();
        Uri source = sourceIntent.getData();
        boolean desktopRequest = "com.seven.gamehub.action.OPEN_DESKTOP".equals(sourceIntent.getAction());
        boolean exeRequest = "com.seven.gamehub.action.RUN_EXE".equals(sourceIntent.getAction());

        if ((!desktopRequest && !exeRequest) ||
            (exeRequest && sourceIntent.getIntExtra("sevenx_contract_version", -1) != CONTRACT_VERSION) ||
            (exeRequest && (source == null || !"content".equals(source.getScheme())))) {
            finishWithError("Некорректный запрос SevenX или неподдерживаемая версия контракта");
            return;
        }

        ContainerManager containerManager;
        ArrayList<Container> containers;
        try {
            containerManager = new ContainerManager(this);
            containers = containerManager.getContainers();
        }
        catch (Exception error) {
            finishWithError("Не удалось прочитать список Wine-контейнеров");
            return;
        }

        if (containers == null || containers.isEmpty()) {
            // First-run path: create a default Winlator prefix automatically.
            // ContainerManager extracts the bundled Wine container pattern on
            // its worker thread and reports completion on the main thread.
            containerManager.createContainerAsync(new JSONObject(), (createdContainer) -> {
                if (createdContainer == null) {
                    finishWithError("Не удалось автоматически создать Wine-контейнер. Проверь свободное место и повтори запуск");
                    return;
                }
                try {
                    containerManager.activateContainer(createdContainer);
                    if (desktopRequest) startDesktop(createdContainer);
                    else launchInContainer(source, createdContainer);
                }
                catch (Exception error) {
                    finishWithError("Не удалось подготовить новый Wine-контейнер: " + safeMessage(error));
                }
            });
            return;
        }

        // Reuse Winlator's currently active container automatically. If no active
        // symlink can be resolved, use the first available container and activate it.
        Container selectedContainer = containers.get(0);
        File activeContainerLink = new File(selectedContainer.getRootDir().getParentFile(), "user");
        try {
            File activeContainer = activeContainerLink.getCanonicalFile();
            for (Container candidate : containers) {
                if (candidate.getRootDir().getCanonicalFile().equals(activeContainer)) {
                    selectedContainer = candidate;
                    break;
                }
            }
        }
        catch (IOException ignored) {
        }

        try {
            containerManager.activateContainer(selectedContainer);
            if (desktopRequest) startDesktop(selectedContainer);
            else launchInContainer(source, selectedContainer);
        }
        catch (Exception error) {
            finishWithError("Не удалось выбрать Wine-контейнер: " + safeMessage(error));
        }
    }

    private PendingIntent getSevenXCallback() {
        return getIntent().getParcelableExtra("sevenx_callback");
    }

    private void reportResult(boolean started, String message, int pid) {
        PendingIntent callback = getSevenXCallback();
        if (callback == null) return;

        Intent result = new Intent();
        result.putExtra("sevenx_started", started);
        result.putExtra("sevenx_pid", pid);
        result.putExtra("sevenx_error", message == null ? "" : message);
        try {
            callback.send(this, 0, result);
        }
        catch (PendingIntent.CanceledException ignored) {
        }
    }

    private void startDesktop(Container container) {
        try {
            Intent displayIntent = new Intent(this, XServerDisplayActivity.class);
            displayIntent.putExtra("container_id", container.id);
            displayIntent.putExtra("sevenx_desktop_mode", true);
            startActivity(displayIntent);
            finish();
        }
        catch (Exception error) {
            finishWithError("Не удалось открыть рабочий стол: " + safeMessage(error));
        }
    }

    private String enableWinComponent(String config, String component) {
        String source = config == null ? "" : config;
        StringBuilder updated = new StringBuilder();
        boolean found = false;
        for (String rawEntry : source.split(",")) {
            String entry = rawEntry.trim();
            int separator = entry.indexOf('=');
            if (separator > 0 && entry.substring(0, separator).trim().equals(component)) {
                entry = component + "=1";
                found = true;
            }
            if (updated.length() > 0) updated.append(",");
            if (!entry.isEmpty()) updated.append(entry);
        }
        if (!found) {
            if (updated.length() > 0) updated.append(",");
            updated.append(component).append("=1");
        }
        return updated.toString();
    }

    private void prepareBundledDependencies(Container container, String compatibilityHints) {
        String current = container.getWinComponents();
        if (current == null) current = "";

        // These two runtimes are bundled in this exact pinned Winlator source.
        String updated = enableWinComponent(current, "vcrun2005");
        updated = enableWinComponent(updated, "vcrun2010");

        // Optional components are enabled per prefix only when static PE imports request them.
        if (compatibilityHints != null && compatibilityHints.contains("[SEVENX_COMPONENTS:")) {
            if (compatibilityHints.contains("directshow")) updated = enableWinComponent(updated, "directshow");
            if (compatibilityHints.contains("directplay")) updated = enableWinComponent(updated, "directplay");
        }

        if (!updated.equals(current)) {
            container.setWinComponents(updated);
            container.saveData();
        }
    }

    private void launchInContainer(Uri source, Container container) {
        File temporary = null;
        File destination = null;
        try {
            prepareBundledDependencies(container, getIntent().getStringExtra("sevenx_compatibility_hints"));
            String displayName = queryDisplayName(source);
            if (displayName == null || displayName.trim().isEmpty()) {
                throw new IllegalArgumentException("Не удалось определить имя Windows-файла");
            }
            String lowerName = displayName.toLowerCase(Locale.ROOT);
            boolean isExe = lowerName.endsWith(".exe");
            boolean isMsi = lowerName.endsWith(".msi");
            if (!isExe && !isMsi) {
                throw new IllegalArgumentException("Поддерживаются Windows EXE и MSI");
            }

            String safeName = displayName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (safeName.length() > 120) safeName = safeName.substring(safeName.length() - 120);

            File exeDirectory = new File(container.getRootDir(), ".wine/drive_c/SevenX");
            if (!exeDirectory.isDirectory() && !exeDirectory.mkdirs()) {
                throw new IOException("Не удалось создать папку для установщика в контейнере");
            }

            String unique = UUID.randomUUID().toString();
            temporary = new File(exeDirectory, "." + unique + ".part");
            destination = new File(exeDirectory, unique + "_" + safeName);

            InputStream input = getContentResolver().openInputStream(source);
            if (input == null) throw new IOException("SevenX не предоставил доступ к файлу");

            long copied = 0;
            try (InputStream in = input; FileOutputStream out = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    long available = new StatFs(exeDirectory.getAbsolutePath()).getAvailableBytes();
                    if (available < count + 1024L * 1024L) {
                        throw new IOException("Недостаточно свободного места в контейнере");
                    }
                    out.write(buffer, 0, count);
                    copied += count;
                }
                out.flush();
                out.getFD().sync();
            }

            if (copied == 0) throw new IOException("Установщик пустой");
            if (!temporary.renameTo(destination)) {
                throw new IOException("Не удалось завершить копирование установщика");
            }

            Intent displayIntent = new Intent(this, XServerDisplayActivity.class);
            displayIntent.putExtra("container_id", container.id);
            displayIntent.putExtra("exec_path", destination.getAbsolutePath());
            displayIntent.putExtra("sevenx_callback", getSevenXCallback());
            displayIntent.putExtra("sevenx_profile", getIntent().getStringExtra("sevenx_profile"));
            displayIntent.putExtra("sevenx_source_uri", source.toString());
            displayIntent.putExtra("sevenx_contract_version", CONTRACT_VERSION);
            displayIntent.putExtra("sevenx_install_args", getIntent().getStringExtra("sevenx_install_args"));
            startActivity(displayIntent);
            finish();
        }
        catch (Exception error) {
            if (temporary != null) temporary.delete();
            if (destination != null) destination.delete();
            finishWithError("Не удалось передать установщик в Wine: " + safeMessage(error));
        }
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
            uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        }
        catch (Exception ignored) {
        }
        return uri.getLastPathSegment();
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
            ? "неизвестная ошибка"
            : message;
    }

    private void finishWithError(String message) {
        reportResult(false, message, -1);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
''', encoding="utf-8")

# The runtime callback reports process creation, not installer completion.
replace_once(
    display,
    '                result.putExtra("sevenx_pid", pid);\n',
    '                result.putExtra("sevenx_pid", pid);\n'
    '                result.putExtra("sevenx_error", pid > 0 ? "" : "Wine не смог создать процесс установщика");\n'
)

# Version the cross-app intent and explicitly include the source URI.
replace_once(
    runtime_client,
    '    private const val WINLATOR_PACKAGE = "com.winlator"\n',
    '    private const val CONTRACT_VERSION = 1\n'
    '    private const val WINLATOR_PACKAGE = "com.winlator"\n'
)
replace_once(
    runtime_client,
    '            putExtra("sevenx_callback", callback)\n',
    '            putExtra("sevenx_callback", callback)\n'
    '            putExtra("sevenx_contract_version", CONTRACT_VERSION)\n'
    '            putExtra("sevenx_source_uri", executable.toString())\n'
)

# Preserve a useful error from the runtime callback for the Game Manager UI.
replace_once(
    receiver,
    '        val started = intent.getBooleanExtra(EXTRA_STARTED, false)\n',
    '        val started = intent.getBooleanExtra(EXTRA_STARTED, false)\n'
    '        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)\n'
)
replace_once(
    receiver,
    '            .putBoolean(PREFS_LAST_RESULT_STARTED, started)\n',
    '            .putBoolean(PREFS_LAST_RESULT_STARTED, started)\n'
    '            .putString(PREFS_LAST_RESULT_ERROR, errorMessage)\n'
)
replace_once(
    receiver,
    '        const val EXTRA_STARTED = "sevenx_started"\n',
    '        const val EXTRA_STARTED = "sevenx_started"\n'
    '        const val EXTRA_ERROR_MESSAGE = "sevenx_error"\n'
)
replace_once(
    receiver,
    '        const val PREFS_LAST_RESULT_STARTED = "runtime_last_result_started"\n',
    '        const val PREFS_LAST_RESULT_STARTED = "runtime_last_result_started"\n'
    '        const val PREFS_LAST_RESULT_ERROR = "runtime_last_result_error"\n'
)

replace_once(
    main_activity,
    '                val started = preferences.getBoolean("runtime_last_result_started", false)\n',
    '                val started = preferences.getBoolean("runtime_last_result_started", false)\n'
    '                val runtimeError = preferences.getString("runtime_last_result_error", null)\n'
)
replace_once(
    main_activity,
    '                    } else {\n'
    '                        "Winlator не подтвердил запуск. Запись и файл сохранены."\n'
    '                    }\n',
    '                    } else {\n'
    '                        runtimeError?.takeIf { it.isNotBlank() }?.let { "Запуск не удался: $it" }\n'
    '                            ?: "Winlator не подтвердил запуск. Запись и файл сохранены."\n'
    '                    }\n'
)
replace_once(
    main_activity,
    '                preferences.edit().remove("runtime_last_result_uri").remove("runtime_last_result_started").apply()\n',
    '                preferences.edit().remove("runtime_last_result_uri")\n'
    '                    .remove("runtime_last_result_started").remove("runtime_last_result_error").apply()\n'
)


# Carry silent-install arguments through the existing Winlator launcher.
replace_once(
    display,
    '        if (cmdArgs.isEmpty()) cmdArgs = "/dir C:\\\\windows \\"wfm.exe\\"";',
    '        if (cmdArgs.isEmpty()) cmdArgs = "/dir C:\\\\windows \\"wfm.exe\\"";\n'
    '        String sevenxInstallArgs = getIntent().getStringExtra("sevenx_install_args");\n'
    '        if (sevenxInstallArgs != null && !sevenxInstallArgs.trim().isEmpty()) cmdArgs += " " + sevenxInstallArgs.trim();'
)

# Report the actual Wine process exit code, not merely that it was created.
replace_once(
    display,
    '        guestProgramLauncherComponent.setTerminationCallback((status) -> exit());',
    '        guestProgramLauncherComponent.setTerminationCallback((status) -> {\n'
    '            PendingIntent callback = (PendingIntent)getIntent().getParcelableExtra("sevenx_callback");\n'
    '            if (callback != null) {\n'
    '                Intent result = new Intent();\n'
    '                result.putExtra("sevenx_process_finished", true);\n'
    '                result.putExtra("sevenx_exit_code", status);\n'
    '                try { callback.send(this, 0, result); }\n'
    '                catch (PendingIntent.CanceledException ignored) {}\n'
    '            }\n'
    '            exit();\n'
    '        });'
)

print("SevenX 0.10.0 installer bridge hardening applied.")
