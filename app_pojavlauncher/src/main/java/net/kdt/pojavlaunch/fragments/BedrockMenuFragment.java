package net.kdt.pojavlaunch.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
        TextView status = view.findViewById(R.id.bedrock_status);
        Button launch = view.findViewById(R.id.bedrock_launch_button);
        Button importApk = view.findViewById(R.id.bedrock_import_apk_button);
        Button importAddon = view.findViewById(R.id.bedrock_import_addon_button);
        Button importWorld = view.findViewById(R.id.bedrock_import_world_button);
        Button back = view.findViewById(R.id.bedrock_back_button);

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
        back.setOnClickListener(v ->
                Tools.backToMainMenu(requireActivity()));
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