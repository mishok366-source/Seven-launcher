#!/usr/bin/env python3
"""Apply the maintained SevenX launch bridge to the pinned Winlator source tree."""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "seven-gamehub/winlator-source")
manifest = root / "app/src/main/AndroidManifest.xml"
display = root / "app/src/main/java/com/winlator/XServerDisplayActivity.java"
launcher = root / "app/src/main/java/com/winlator/xenvironment/components/GuestProgramLauncherComponent.java"
bridge = root / "app/src/main/java/com/winlator/SevenXBridgeActivity.java"

def replace_once(path: Path, old: str, new: str):
    content = path.read_text(encoding="utf-8")
    count = content.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one patch anchor in {path}: {old!r}; found {count}")
    path.write_text(content.replace(old, new, 1), encoding="utf-8")

manifest_anchor = "    </application>"
manifest_addition = """        <activity
            android:name="com.winlator.SevenXBridgeActivity"
            android:exported="true"
            android:theme="@style/AppThemeDark"
            android:screenOrientation="sensor">
            <intent-filter>
                <action android:name="com.seven.gamehub.action.RUN_EXE" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
"""
replace_once(manifest, manifest_anchor, manifest_addition + manifest_anchor)

replace_once(
    launcher,
    "    private Callback<Integer> terminationCallback;\n",
    "    private Callback<Integer> terminationCallback;\n    private Callback<Integer> startCallback;\n"
)
replace_once(
    launcher,
    "    public Callback<Integer> getTerminationCallback() {\n",
    "    public void setStartCallback(Callback<Integer> startCallback) {\n"
    "        this.startCallback = startCallback;\n"
    "    }\n\n"
    "    public Callback<Integer> getTerminationCallback() {\n"
)
replace_once(
    launcher,
    "            pid = execGuestProgram();\n",
    "            pid = execGuestProgram();\n"
    "            if (startCallback != null) startCallback.call(pid);\n"
)

replace_once(
    display,
    "import android.app.PictureInPictureParams;\n",
    "import android.app.PictureInPictureParams;\nimport android.app.PendingIntent;\n"
)
replace_once(
    display,
    "        GuestProgramLauncherComponent guestProgramLauncherComponent = new GuestProgramLauncherComponent();\n",
    "        GuestProgramLauncherComponent guestProgramLauncherComponent = new GuestProgramLauncherComponent();\n"
    "        guestProgramLauncherComponent.setStartCallback((pid) -> {\n"
    "            PendingIntent callback = (PendingIntent)getIntent().getParcelableExtra(\"sevenx_callback\");\n"
    "            if (callback != null) {\n"
    "                Intent result = new Intent();\n"
    "                result.putExtra(\"sevenx_started\", pid > 0);\n"
    "                result.putExtra(\"sevenx_pid\", pid);\n"
    "                try {\n"
    "                    callback.send(this, 0, result);\n"
    "                }\n"
    "                catch (PendingIntent.CanceledException ignored) {}\n"
    "            }\n"
    "        });\n"
)

bridge.parent.mkdir(parents=True, exist_ok=True)
bridge.write_text("""package com.winlator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class SevenXBridgeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent sourceIntent = getIntent();
        Uri source = sourceIntent.getData();
        if (!"com.seven.gamehub.action.RUN_EXE".equals(sourceIntent.getAction()) ||
            source == null || !"content".equals(source.getScheme())) {
            finishWithError("Некорректный запрос SevenX");
            return;
        }

        ContainerManager manager = new ContainerManager(this);
        ArrayList<Container> containers = manager.getContainers();
        if (containers.isEmpty()) {
            Toast.makeText(this, "Сначала создай контейнер в Winlator, затем повтори запуск из SevenX", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String[] labels = new String[containers.size()];
        for (int i = 0; i < containers.size(); i++) {
            Container container = containers.get(i);
            labels[i] = container.getName() + "  (#" + container.id + ")";
        }

        new AlertDialog.Builder(this)
            .setTitle("Запустить через контейнер")
            .setItems(labels, (dialog, which) -> launchInContainer(source, containers.get(which)))
            .setOnCancelListener(dialog -> finish())
            .show();
    }

    private void launchInContainer(Uri source, Container container) {
        try {
            String displayName = queryDisplayName(source);
            if (displayName == null || !displayName.toLowerCase(Locale.ROOT).endsWith(".exe")) {
                throw new IllegalArgumentException("SevenX передал не EXE-файл");
            }

            String safeName = displayName.replaceAll("[^A-Za-z0-9._-]", "_");
            File exeDirectory = new File(container.getRootDir(), ".wine/drive_c/SevenX");
            if (!exeDirectory.isDirectory() && !exeDirectory.mkdirs()) {
                throw new IllegalStateException("Не удалось создать папку для EXE");
            }

            File destination = new File(exeDirectory, System.currentTimeMillis() + "_" + safeName);
            try (InputStream input = getContentResolver().openInputStream(source);
                 OutputStream output = new java.io.FileOutputStream(destination)) {
                if (input == null) throw new IllegalStateException("Не удалось прочитать EXE из SevenX");
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }

            Intent displayIntent = new Intent(this, XServerDisplayActivity.class);
            displayIntent.putExtra("container_id", container.id);
            displayIntent.putExtra("exec_path", destination.getAbsolutePath());
            displayIntent.putExtra("sevenx_callback", getIntent().getParcelableExtra("sevenx_callback"));
            startActivity(displayIntent);
            finish();
        }
        catch (Exception error) {
            finishWithError(error.getMessage() != null ? error.getMessage() : "Не удалось передать EXE в Winlator");
        }
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        }
        catch (Exception ignored) {}
        return uri.getLastPathSegment();
    }

    private void finishWithError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
""", encoding="utf-8")

print("SevenX bridge patch applied.")
