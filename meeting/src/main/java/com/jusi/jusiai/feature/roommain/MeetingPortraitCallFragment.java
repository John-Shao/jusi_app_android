package com.jusi.jusiai.feature.roommain;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jusi.jusiai.common.MLog;
import com.jusi.jusiai.feature.roommain.fragment.MeetingUserPageFragment;
import com.jusi.jusiai.feature.roommain.fragment.MeetingUserPipFragment;
import com.jusi.jusiai.meeting.R;

public class MeetingPortraitCallFragment extends AbsMeetingFragment {

    private static final String TAG = "PortraitCallFragment";

    private final MeetingUserPipFragment mPipFragment = new MeetingUserPipFragment();
    private final MeetingUserPageFragment mPagerUserFragment = new MeetingUserPageFragment();

    private boolean mCurrentIsPipMode = false;
    private boolean mFragmentInitialized = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meeting_portrait_call, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDataProvider().getUserCount().observe(getViewLifecycleOwner(), userCount -> {
            boolean shouldUsePipMode = userCount <= 2;

            // 只在需要切换模式或首次初始化时才操作Fragment
            if (!mFragmentInitialized || mCurrentIsPipMode != shouldUsePipMode) {
                if (shouldUsePipMode) {
                    MLog.d(TAG, "switch to pip mode, userCount: " + userCount);
                    getChildFragmentManager().beginTransaction()
                            .remove(mPagerUserFragment)
                            .replace(R.id.room_root, mPipFragment)
                            .commitNow();
                } else {
                    MLog.d(TAG, "switch to page mode, userCount: " + userCount);
                    getChildFragmentManager().beginTransaction()
                            .remove(mPipFragment)
                            .replace(R.id.room_root, mPagerUserFragment)
                            .commitNow();
                }
                mCurrentIsPipMode = shouldUsePipMode;
                mFragmentInitialized = true;
            }
        });
    }
}
