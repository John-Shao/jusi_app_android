package com.volcengine.vertcdemo.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drift.camcontroldemo.LinkHomeActivity;

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
        // Automatically navigate to LinkHomeActivity
        Intent intent = new Intent(getActivity(), LinkHomeActivity.class);
        startActivity(intent);
    }
}
