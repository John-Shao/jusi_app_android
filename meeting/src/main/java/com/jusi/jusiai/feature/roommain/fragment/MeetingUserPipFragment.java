package com.jusi.jusiai.feature.roommain.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jusi.jusiai.common.MLog;
import com.jusi.jusiai.core.IUIRtcDef;
import com.jusi.jusiai.core.SolutionDataManager;
import com.jusi.jusiai.feature.roommain.AbsMeetingFragment;
import com.jusi.jusiai.feature.roommain.MeetingUserWindowView;
import com.jusi.jusiai.framework.meeting.IUIMeetingDef;
import com.jusi.jusiai.framework.meeting.bean.MeetingUserInfo;
import com.jusi.jusiai.meeting.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeetingUserPipFragment extends AbsMeetingFragment implements IUIRtcDef.IRtcListener {

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

        // 添加RTC监听器，监听远程视频流可用事件
        getUIRoom().getUIRtcCore().addHandler(this);
        getDataProvider().addHandler(mDataObserver);

        // 监听本地摄像头状态，控制小窗口的显示和隐藏
        getUIRoom().getRtcDataProvider().camState().observe(getViewLifecycleOwner(), isCameraOpen -> {
            updateSmallWindowVisibility();
        });

        bindPipUser();
    }

    /**
     * 更新小窗口的显示状态
     * 只有在两人对话场景下：
     * - 当本地用户关闭摄像头时，隐藏小窗口（不遮挡画面）
     * - 当本地用户打开摄像头时，显示小窗口
     * 如果只有一个人或超过两个人，隐藏小窗口
     */
    private void updateSmallWindowVisibility() {
        if (mSmallWindowLayout == null) {
            return;
        }

        // 获取当前用户数量
        List<MeetingUserInfo> userList = getDataProvider().getUsers();
        int userCount = userList != null ? userList.size() : 0;

        // 只有在两人对话场景下才根据摄像头状态显示小窗口
        if (userCount == 2) {
            // 获取本地摄像头状态
            boolean isCameraOpen = getUIRoom().getRtcDataProvider().isCamOpen();
            // 如果摄像头关闭，隐藏小窗口；如果摄像头打开，显示小窗口
            mSmallWindowLayout.setVisibility(isCameraOpen ? View.VISIBLE : View.GONE);
            MLog.d(TAG, "updateSmallWindowVisibility: userCount=" + userCount + ", local camera is " + (isCameraOpen ? "open" : "closed") +
                    ", small window visibility: " + (isCameraOpen ? "VISIBLE" : "GONE"));
        } else {
            // 非两人对话场景，隐藏小窗口
            mSmallWindowLayout.setVisibility(View.GONE);
            MLog.d(TAG, "updateSmallWindowVisibility: userCount=" + userCount + ", hiding small window");
        }
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
                    // 禁用画中画位置切换功能（后续可能需要恢复）
                    /*
                    if (!mIsDragging) {
                        if (mSmallUserInfo != null) {
                            mSwitchedWindow = true;
                        }
                        bindPipUser(mSmallUserInfo, mLargeUserInfo);
                    }
                    */
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
        getUIRoom().getUIRtcCore().removeHandler(this);
        getDataProvider().removeHandler(mDataObserver);
    }

    // 实现 IRtcListener 接口，监听远程视频流可用事件
    @Override
    public void onUserVideoStreamAvailable(String userId, boolean available) {
        MLog.d(TAG, "onUserVideoStreamAvailable, userId: " + userId + ", available: " + available);

        // 当远程用户视频流变为可用时，检查是否是当前显示的用户，如果是则重新绑定渲染器
        if (available) {
            if (mLargeUserInfo != null && userId.equals(mLargeUserInfo.userId)) {
                MLog.d(TAG, "rebind large window for userId: " + userId);
                mLargeWindowLayout.bind(getUIRoom(), mLargeUserInfo);
            }
            if (mSmallUserInfo != null && userId.equals(mSmallUserInfo.userId)) {
                MLog.d(TAG, "rebind small window for userId: " + userId);
                mSmallWindowLayout.bind(getUIRoom(), mSmallUserInfo);
            }
        }
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

        if (mSmallUserInfo == null) {
            mSmallWindowLayout.setVisibility(View.INVISIBLE);
        } else {
            // 不在这里设置可见性，让 updateSmallWindowVisibility() 统一处理
            // mSmallWindowLayout.setVisibility(View.VISIBLE);
            if (!mSmallUserInfo.isMe) {
                subVideoStreamUserIds.add(mSmallUserInfo.userId);
            }
        }

        // 先订阅视频流，确保视频流可用后再绑定窗口
        getUIRoom().subscribeVideoStream(subVideoStreamUserIds);

        // 然后绑定窗口，此时视频流订阅已经完成
        mLargeWindowLayout.bind(getUIRoom(), mLargeUserInfo);
        mSmallWindowLayout.bind(getUIRoom(), mSmallUserInfo);

        // 更新小窗口的显示状态
        updateSmallWindowVisibility();
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
