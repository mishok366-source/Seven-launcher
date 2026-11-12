package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

public class EditionSelectFragment extends Fragment {
    public static final String TAG = "EditionSelectFragment";

    public EditionSelectFragment() {
        super(R.layout.fragment_edition_select);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button javaButton = view.findViewById(R.id.edition_java_button);
        Button bedrockButton = view.findViewById(R.id.edition_bedrock_button);

        javaButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), MainMenuFragment.class, MainMenuFragment.TAG, null));

        bedrockButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), BedrockMenuFragment.class, BedrockMenuFragment.TAG, null));
    }
}