#!/usr/bin/env python3
"""Apply the maintained SevenX launch bridge to the pinned Winlator source tree."""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "seven-gamehub/winlator-source")
manifest = root / "app/src/main/AndroidManifest.xml"
menu = root / "app/src/main/res/menu/main_menu.xml"
main_activity = root / "app/src/main/java/com/winlator/MainActivity.java"
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
                <action android:name="com.seven.gamehub.action.OPEN_DESKTOP" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
"""
replace_once(manifest, manifest_anchor, manifest_addition + manifest_anchor)

replace_once(
    menu,
    '<item android:icon="@drawable/icon_container" android:id="@+id/menu_item_containers" android:title="@string/containers" />',
    '<item android:icon="@drawable/icon_run" android:id="@+id/menu_item_desktop" android:title="Рабочий стол" />\n        <item android:icon="@drawable/icon_container" android:id="@+id/menu_item_containers" android:title="@string/containers" />'
)
replace_once(
    main_activity,
    '            case R.id.menu_item_containers:\n',
    '            case R.id.menu_item_desktop:\n                Intent desktopIntent = new Intent("com.seven.gamehub.action.OPEN_DESKTOP");\n                desktopIntent.setClass(this, SevenXBridgeActivity.class);\n                startActivity(desktopIntent);\n                break;\n            case R.id.menu_item_containers:\n'
)

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

replace_once(
    display,
    """            String desktopName = shortcut != null || getIntent().hasExtra("exec_path") ? "nogui" : "shell";
            String guestExecutable = "wine explorer /desktop="+desktopName+","+xServer.screenInfo+" "+getWineStartCommand();""",
    """            boolean sevenxDesktopMode = getIntent().getBooleanExtra("sevenx_desktop_mode", false);
            String desktopName = shortcut != null || getIntent().hasExtra("exec_path") ? "nogui" : "shell";
            String desktopCommand = "wine explorer /desktop="+desktopName+","+xServer.screenInfo;
            String guestExecutable = sevenxDesktopMode ? desktopCommand : desktopCommand+" "+getWineStartCommand();"""
)

bridge.parent.mkdir(parents=True, exist_ok=True)
bridge.write_text("""package com.winlator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
        boolean desktopRequest = "com.seven.gamehub.action.OPEN_DESKTOP".equals(sourceIntent.getAction());
        boolean exeRequest = "com.seven.gamehub.action.RUN_EXE".equals(sourceIntent.getAction());
        if ((!desktopRequest && !exeRequest) || (!desktopRequest && (source == null || !"content".equals(source.getScheme())))) {
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
            .setItems(labels, (dialog, which) -> {
                if (desktopRequest) startDesktop(containers.get(which));
                else launchInContainer(source, containers.get(which));
            })
            .setOnCancelListener(dialog -> finish())
            .show();
    }

    private void startDesktop(Container container) {
        Intent displayIntent = new Intent(this, XServerDisplayActivity.class);
        displayIntent.putExtra("container_id", container.id);
        displayIntent.putExtra("sevenx_desktop_mode", true);
        startActivity(displayIntent);
        finish();
    }

    private void launchInContainer(Uri source, Container container) {
        try {
            String displayName = queryDisplayName(source);
            if (displayName == null) throw new IllegalArgumentException("Не удалось определить имя файла");
            String lowerName = displayName.toLowerCase(Locale.ROOT);
            boolean exeFile = lowerName.endsWith(".exe");
            boolean msiFile = lowerName.endsWith(".msi");
            if (!exeFile && !msiFile) {
                throw new IllegalArgumentException("Поддерживаются Windows EXE и MSI");
            }

            String safeName = displayName.replaceAll("[^A-Za-z0-9._-]", "_");
            File exeDirectory = new File(container.getRootDir(), ".wine/drive_c/SevenX");
            if (!exeDirectory.isDirectory() && !exeDirectory.mkdirs()) {
                throw new IllegalStateException("Не удалось создать папку для Windows-приложения");
            }

            File destination = new File(exeDirectory, System.currentTimeMillis() + "_" + safeName);
            try (InputStream input = getContentResolver().openInputStream(source);
                 OutputStream output = new java.io.FileOutputStream(destination)) {
                if (input == null) throw new IllegalStateException("Не удалось прочитать установщик из SevenX");
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }

            Intent displayIntent = new Intent(this, XServerDisplayActivity.class);
            displayIntent.putExtra("container_id", container.id);
            displayIntent.putExtra("exec_path", destination.getAbsolutePath());
            displayIntent.putExtra("sevenx_callback", (PendingIntent)getIntent().getParcelableExtra("sevenx_callback"));
            displayIntent.putExtra("sevenx_profile", getIntent().getStringExtra("sevenx_profile"));
            startActivity(displayIntent);
            finish();
        }
        catch (Exception error) {
            finishWithError(error.getMessage() != null ? error.getMessage() : "Не удалось передать Windows-приложение в Winlator");
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


# Enable the bundled Visual C++ 2005 runtime by default for newly created
# containers. It remains independently configurable in the native container UI.
container_java = root / "app/src/main/java/com/winlator/container/Container.java"
replace_once(
    container_java,
    'vcrun2005=0,vcrun2010=1',
    'vcrun2005=1,vcrun2010=1'
)

# Apply the per-game compatibility profile without changing the container defaults.
replace_once(
    display,
    '            if (!envVars.has("WINEESYNC")) envVars.put("WINEESYNC", "1");',
    '            if (!envVars.has("WINEESYNC")) envVars.put("WINEESYNC", "1");\n'
    '            if ("compatibility".equals(getIntent().getStringExtra("sevenx_profile"))) {\n'
    '                envVars.put("WINEESYNC", "0");\n'
    '                envVars.put("WINEFSYNC", "0");\n'
    '            }'
)

# Route MSI databases through Windows Installer in the selected Wine container.
replace_once(
    display,
    """        if (execPath != null) {
            String execDir = FileUtils.getDirname(execPath);""",
    """        if (execPath != null && execPath.toLowerCase().endsWith(".msi")) {
            String execDir = FileUtils.getDirname(execPath);
            String filename = FileUtils.getName(execPath);
            cmdArgs = "/dir "+StringUtils.escapeDOSPath(execDir)+" "+(char)34+"msiexec.exe"+(char)34+" /i "+(char)34+filename+(char)34;
            execPath = null;
        }

        if (execPath != null) {
            String execDir = FileUtils.getDirname(execPath);"""
)


# Apply SevenX Desktop branding only: keep the original Winlator UI and runtime.
runtime_gradle = root / "app_pojavlauncher/build.gradle"
gradle_text = runtime_gradle.read_text(encoding="utf-8")
if "Amethyst" not in gradle_text:
    raise SystemExit(f"Expected original app label in {runtime_gradle}")
runtime_gradle.write_text(gradle_text.replace("Amethyst", "SevenX Desktop"), encoding="utf-8")

runtime_strings = root / "app_pojavlauncher/src/main/res/values/strings.xml"
strings_text = runtime_strings.read_text(encoding="utf-8")
if "Amethyst" in strings_text:
    runtime_strings.write_text(strings_text.replace("Amethyst", "SevenX Desktop"), encoding="utf-8")

# A self-contained vector launcher mark avoids depending on external icon assets.
icon_dir = root / "app_pojavlauncher/src/main/res/drawable"
icon_dir.mkdir(parents=True, exist_ok=True)
(icon_dir / "sevenx_launcher.xml").write_text("""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#7557E8" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#FFFFFF" android:pathData="M23,25h62v13L51,83H34L67,41H23z" />
    <path android:fillColor="#FFFFFF" android:pathData="M62,57l8,-10 18,27 -8,10z" />
</vector>
""", encoding="utf-8")

manifest_text = manifest.read_text(encoding="utf-8")
manifest_text, icon_count = __import__("re").subn(
    r'android:(?:roundIcon|icon)="@mipmap/ic_launcher(?:_round)?"',
    'android:icon="@drawable/sevenx_launcher"',
    manifest_text
)
if icon_count < 1:
    raise SystemExit("Could not find Winlator launcher icon declarations in manifest")
manifest.write_text(manifest_text, encoding="utf-8")

print("SevenX bridge patch applied.")
