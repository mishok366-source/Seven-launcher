package net.kdt.pojavlaunch.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

public class BedrockMenuFragment extends Fragment {
    public static final String TAG = "BedrockMenuFragment";
    private static final String BEDROCK_PACKAGE = "com.mojang.minecraftpe";

    public BedrockMenuFragment() {
        super(R.layout.fragment_bedrock_menu);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView status = view.findViewById(R.id.bedrock_status);
        Button launch = view.findViewById(R.id.bedrock_launch_button);
        Button back = view.findViewById(R.id.bedrock_back_button);

        boolean installed = isBedrockInstalled();
        status.setText(installed
                ? "Minecraft Bedrock is installed on this device."
                : "Minecraft Bedrock was not found. Install the official game first.");
        launch.setText(installed ? "OPEN MINECRAFT" : "CHECK AGAIN");

        launch.setOnClickListener(v -> {
            if (!isBedrockInstalled()) {
                Toast.makeText(requireContext(),
                        "Install the official Minecraft Bedrock app first.",
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

        back.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), EditionSelectFragment.class,
                        EditionSelectFragment.TAG, null));
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