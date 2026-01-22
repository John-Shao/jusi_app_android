package com.drift.camcontroldemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Camera management fragment
 * Entry point for camera device management functionality
 */
public class CameraFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        // Navigate to LinkHomeActivity when button is clicked
        Button btnEnter = view.findViewById(R.id.btn_enter_camera_manager);
        btnEnter.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LinkHomeActivity.class);
            startActivity(intent);
        });
    }
}
