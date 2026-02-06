package com.volcengine.vertcdemo.feature.roommain.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.feature.roommain.AbsMeetingFragment;
import com.volcengine.vertcdemo.feature.roommain.MeetingUserWindowView;
import com.volcengine.vertcdemo.framework.meeting.IUIMeetingDef;
import com.volcengine.vertcdemo.framework.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeetingUserPipFragment extends AbsMeetingFragment {

    private static final String TAG = "UserPipFragment";

    private MeetingUserWindowView mLargeWindowLayout;
    private MeetingUserWindowView mSmallWindowLayout;

    private MeetingUserInfo mLargeUserInfo;
    private MeetingUserInfo mSmallUserInfo;

    private boolean mSwitchedWindow;

    // 拖动相关变量
    private float mLastTouchX;
    private float mLastTouchY;
    private float mSmallWindowX;
    private float mSmallWindowY;
    private boolean mIsDragging = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meeting_pip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MLog.d(TAG, "onViewCreated");
        mLargeWindowLayout = view.findViewById(R.id.user_large);
        mSmallWindowLayout = view.findViewById(R.id.user_small);

        // 设置小窗口的触摸监听，实现拖动功能
        setupSmallWindowDraggable();

        getDataProvider().addHandler(mDataObserver);
        bindPipUser();
    }

    /**
     * 设置小窗口可拖动
     */
    private void setupSmallWindowDraggable() {
        mSmallWindowLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 记录初始触摸位置
                    mLastTouchX = event.getRawX();
                    mLastTouchY = event.getRawY();
                    mIsDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // 计算移动距离
                    float deltaX = event.getRawX() - mLastTouchX;
                    float deltaY = event.getRawY() - mLastTouchY;

                    // 如果移动距离超过阈值，则认为是拖动而不是点击
                    if (!mIsDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                        mIsDragging = true;
                    }

                    if (mIsDragging) {
                        // 计算新位置
                        float newX = mSmallWindowLayout.getX() + deltaX;
                        float newY = mSmallWindowLayout.getY() + deltaY;

                        // 边界检查，确保小窗口不会拖出屏幕
                        View parent = (View) mSmallWindowLayout.getParent();
                        if (parent != null) {
                            float maxX = parent.getWidth() - mSmallWindowLayout.getWidth();
                            float maxY = parent.getHeight() - mSmallWindowLayout.getHeight();

                            newX = Math.max(0, Math.min(newX, maxX));
                            newY = Math.max(0, Math.min(newY, maxY));
                        }

                        // 更新位置
                        mSmallWindowLayout.setX(newX);
                        mSmallWindowLayout.setY(newY);

                        // 更新记录的触摸位置
                        mLastTouchX = event.getRawX();
                        mLastTouchY = event.getRawY();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 如果没有拖动，则执行点击操作（交换大小窗口）
                    if (!mIsDragging) {
                        if (mSmallUserInfo != null) {
                            mSwitchedWindow = true;
                        }
                        bindPipUser(mSmallUserInfo, mLargeUserInfo);
                    }
                    mIsDragging = false;
                    return true;

                default:
                    return false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getDataProvider().removeHandler(mDataObserver);
    }

    private void bindPipUser() {
        List<MeetingUserInfo> userList = getDataProvider().getUsers();
        if (userList.size() > 2) {
            mLargeUserInfo = null;
            mSmallUserInfo = null;
            mLargeWindowLayout.bind(getUIRoom(), null);
            mSmallWindowLayout.bind(getUIRoom(), null);
        } else {
            MeetingUserInfo user1 = userList.size() >= 1 ? userList.get(0) : null;
            MeetingUserInfo user2 = userList.size() >= 2 ? userList.get(1) : null;
            if (!mSwitchedWindow
                    && userList.size() == 2
                    && TextUtils.equals(user1 == null ? null : user1.userId,
                    SolutionDataManager.ins().getUserId())) {
                bindPipUser(user2, user1);
                return;
            }
            bindPipUser(user1, user2);
        }
    }

    private void bindPipUser(MeetingUserInfo largeUserInfo, MeetingUserInfo smallUserInfo) {
        mLargeUserInfo = largeUserInfo;
        mSmallUserInfo = smallUserInfo;

        Set<String> subVideoStreamUserIds = new HashSet<>();
        if (mLargeUserInfo == null) {
            mLargeWindowLayout.setVisibility(View.INVISIBLE);
        } else {
            mLargeWindowLayout.setVisibility(View.VISIBLE);
            if (!mLargeUserInfo.isMe) {
                subVideoStreamUserIds.add(mLargeUserInfo.userId);
            }
        }
        mLargeWindowLayout.bind(getUIRoom(), mLargeUserInfo);

        if (mSmallUserInfo == null) {
            mSmallWindowLayout.setVisibility(View.INVISIBLE);
        } else {
            mSmallWindowLayout.setVisibility(View.VISIBLE);
            if (!mSmallUserInfo.isMe) {
                subVideoStreamUserIds.add(mSmallUserInfo.userId);
            }
        }
        getUIRoom().subscribeVideoStream(subVideoStreamUserIds);
        mSmallWindowLayout.bind(getUIRoom(), mSmallUserInfo);
    }

    private final IUIMeetingDef.IUserDataObserver mDataObserver = new IUIMeetingDef.IUserDataObserver() {
        @Override
        public void onUserDataRenew(List<MeetingUserInfo> userList) {
            bindPipUser();
        }

        @Override
        public void onUserInserted(MeetingUserInfo user, int position) {
            bindPipUser();
        }

        @Override
        public void onUserUpdated(MeetingUserInfo user, int position) {
            if (user.equals(mLargeUserInfo)) {
                mLargeWindowLayout.bind(getUIRoom(), mLargeUserInfo);
            }
            if (user.equals(mSmallUserInfo)) {
                mSmallWindowLayout.bind(getUIRoom(), mSmallUserInfo);
            }
        }

        @Override
        public void onUserUpdated(MeetingUserInfo user, int position, Object payload) {
            if (user.equals(mLargeUserInfo)) {
                mLargeWindowLayout.updateByPayload(payload);
            }
            if (user.equals(mSmallUserInfo)) {
                mSmallWindowLayout.updateByPayload(payload);
            }
        }

        @Override
        public void onUserRemoved(MeetingUserInfo user, int position) {
            mSwitchedWindow = false;
            bindPipUser();
        }

        @Override
        public void onUserMoved(MeetingUserInfo user, int fromPosition, int toPosition) {
        }
    };
}
