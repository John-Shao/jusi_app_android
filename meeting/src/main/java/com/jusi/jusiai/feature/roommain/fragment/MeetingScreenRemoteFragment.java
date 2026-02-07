package com.jusi.jusiai.feature.roommain.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jusi.jusiai.feature.roommain.AbsMeetingFragment;
import com.jusi.jusiai.framework.meeting.IUIMeetingDef;

public class MeetingScreenRemoteFragment extends AbsMeetingFragment {

    private FrameLayout mRemoteRenderContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRemoteRenderContainer = new FrameLayout(getContext());
        return mRemoteRenderContainer;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        IUIMeetingDef.RoomShareState roomShareState = getDataProvider().getRoomShareState().getValue();
        if (roomShareState == null || !roomShareState.isShareScreen()) {
            throw new IllegalStateException("unexpected share state");
        }
        getUIRoom().getUIRtcCore().setupRemoteScreenRenderer(mRemoteRenderContainer, roomShareState.mRoomId, roomShareState.mShareUserId);
    }
}
