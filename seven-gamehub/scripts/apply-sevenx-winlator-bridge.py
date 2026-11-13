#!/usr/bin/env python3
"""Apply the maintained SevenX launch bridge to the pinned Winlator source tree."""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "seven-gamehub/winlator-source")
manifest = root / "app/src/main/AndroidManifest.xml"
styles = root / "app/src/main/res/values/styles.xml"
strings = root / "app/src/main/res/values/strings.xml"
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

# Rebrand the companion UI and give it a distinct SevenX desktop visual identity.
dark_theme_old = """    <style name="AppThemeDark" parent="@style/AppThemeBase">
        <item name="colorAccent">#0288d1</item>
        <item name="colorPrimary">#505963</item>
        <item name="colorPrimaryDark">#3f474f</item>
        <item name="colorControlNormal">#616161</item>
        <item name="colorPrimaryText">#fafafa</item>
        <item name="colorSecondaryText">#9e9e9e</item>
        <item name="colorPrimarySurface">#1b1e21</item>
        <item name="colorSecondarySurface">#2d3238</item>
        <item name="colorPrimaryVariant">#56677a</item>
        <item name="colorSecondaryVariant">#37424f</item>"""
dark_theme_new = """    <style name="AppThemeDark" parent="@style/AppThemeBase">
        <item name="colorAccent">#9B7BFF</item>
        <item name="colorPrimary">#25213A</item>
        <item name="colorPrimaryDark">#171426</item>
        <item name="colorControlNormal">#77718F</item>
        <item name="colorPrimaryText">#F6F3FF</item>
        <item name="colorSecondaryText">#AAA4C2</item>
        <item name="colorPrimarySurface">#11101A</item>
        <item name="colorSecondarySurface">#201D30</item>
        <item name="colorPrimaryVariant">#9B7BFF</item>
        <item name="colorSecondaryVariant">#39324F</item>"""
replace_once(styles, dark_theme_old, dark_theme_new)
replace_once(strings, '<string name="app_name">Winlator</string>', '<string name="app_name">SevenX Desktop</string>')

replace_once(
    menu,
    '<item android:icon="@drawable/icon_shortcut" android:id="@+id/menu_item_shortcuts" android:title="@string/shortcuts" />',
    '<item android:icon="@drawable/icon_shortcut" android:id="@+id/menu_item_shortcuts" android:title="Приложения" />'
)
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
            displayIntent.putExtra("sevenx_callback", (PendingIntent)getIntent().getParcelableExtra("sevenx_callback"));
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


build_gradle = root / "app/build.gradle"
replace_once(
    build_gradle,
    '        versionCode 33\n        versionName "11.2"',
    '        versionCode 34\n        versionName "0.9.0"'
)

# Add a dedicated SevenX component manager to the Winlator navigation drawer.
replace_once(
    menu,
    '<item android:icon="@drawable/icon_container" android:id="@+id/menu_item_containers" android:title="@string/containers" />',
    '<item android:icon="@drawable/icon_install" android:id="@+id/menu_item_components" android:title="Компоненты" />\n        <item android:icon="@drawable/icon_container" android:id="@+id/menu_item_containers" android:title="@string/containers" />'
)
replace_once(
    main_activity,
    '            case R.id.menu_item_containers:\n',
    '            case R.id.menu_item_components:\n                showFragment(new SevenXComponentsFragment());\n                break;\n            case R.id.menu_item_containers:\n'
)

components_fragment = root / "app/src/main/java/com/winlator/SevenXComponentsFragment.java"
components_fragment.write_text("""package com.winlator;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.core.DefaultVersion;
import com.winlator.core.GeneralComponents;

import java.util.ArrayList;

public class SevenXComponentsFragment extends Fragment {
    private int dp(Context context, int value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(Context context, String value, int size, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(0xFFF3F0FF);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private void addComponent(Context context, LinearLayout parent, String title,
                              GeneralComponents.Type type, String defaultVersion) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        card.setBackgroundColor(0xFF201D30);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(context, 12);
        parent.addView(card, cardParams);

        TextView heading = text(context, title, 16, true);
        card.addView(heading);

        ArrayList<String> installed = GeneralComponents.getInstalledComponentNames(type, context);
        TextView description = text(context,
            installed.size() + " дополнительных версий установлено · встроенная версия доступна",
            12, false);
        description.setTextColor(0xFFAAA4C2);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptionParams.topMargin = dp(context, 4);
        card.addView(description, descriptionParams);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.topMargin = dp(context, 8);
        card.addView(controls, controlsParams);

        Spinner versions = new Spinner(context);
        controls.addView(versions, new LinearLayout.LayoutParams(0, dp(context, 48), 1f));

        LinearLayout toolbox = new LinearLayout(context);
        toolbox.setOrientation(LinearLayout.HORIZONTAL);
        controls.addView(toolbox, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 48)));

        ImageButton install = new ImageButton(context);
        install.setImageResource(R.drawable.icon_install);
        install.setBackgroundResource(android.R.drawable.btn_default);
        install.setContentDescription("Установить версию " + title);
        install.setTag("install");
        toolbox.addView(install, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        ImageButton remove = new ImageButton(context);
        remove.setImageResource(R.drawable.icon_remove);
        remove.setBackgroundResource(android.R.drawable.btn_default);
        remove.setContentDescription("Удалить версию " + title);
        remove.setTag("remove");
        toolbox.addView(remove, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        GeneralComponents.initViews(type, toolbox, versions, null, defaultVersion);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        ScrollView scroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 24));
        scroll.addView(content);

        content.addView(text(context, "Менеджер компонентов", 22, true));
        TextView intro = text(context,
            "Устанавливай дополнительные версии движка и графических компонентов. Встроенные версии защищены от удаления.",
            13, false);
        intro.setTextColor(0xFFAAA4C2);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introParams.topMargin = dp(context, 6);
        introParams.bottomMargin = dp(context, 16);
        content.addView(intro, introParams);

        addComponent(context, content, "Box64", GeneralComponents.Type.BOX64, DefaultVersion.BOX64);
        addComponent(context, content, "Turnip GPU Driver", GeneralComponents.Type.TURNIP, DefaultVersion.TURNIP);
        addComponent(context, content, "DXVK", GeneralComponents.Type.DXVK, DefaultVersion.DXVK());
        addComponent(context, content, "VKD3D-Proton", GeneralComponents.Type.VKD3D, DefaultVersion.VKD3D);
        addComponent(context, content, "WineD3D", GeneralComponents.Type.WINED3D, DefaultVersion.WINED3D);
        addComponent(context, content, "SoundFont", GeneralComponents.Type.SOUNDFONT, DefaultVersion.SOUNDFONT);

        TextView note = text(context,
            "Важно: установка графических драйверов зависит от GPU и версии Android. Наличие компонента не гарантирует совместимость с каждой игрой.",
            12, false);
        note.setTextColor(0xFFFFD28A);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(context, 4);
        content.addView(note, noteParams);
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Компоненты");
        }
    }
}
""", encoding="utf-8")

print("SevenX Component Manager patch added.")

print("SevenX bridge patch applied.")
