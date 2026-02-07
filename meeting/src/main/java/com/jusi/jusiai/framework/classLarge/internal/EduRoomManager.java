package com.jusi.jusiai.framework.classLarge.internal;

import androidx.annotation.NonNull;

import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.jusi.jusiai.common.MLog;
import com.jusi.jusiai.core.UIRtcCore;
import com.jusi.jusiai.framework.classLarge.IUIEduDef;
import com.jusi.jusiai.framework.classLarge.bean.EduRoomInfo;
import com.jusi.jusiai.framework.classLarge.bean.EduRoomTokenInfo;
import com.jusi.jusiai.framework.classLarge.impl.EduDataProviderImpl;
import com.jusi.jusiai.framework.classLarge.impl.EduRoomImpl;
import com.jusi.jusiai.framework.meeting.internal.IMeetingRtmEventHandler;
import com.jusi.jusiai.meeting.R;

public class EduRoomManager extends AbsEduManager implements IEduRtmEventHandler {

    private static final String TAG = "EduRoomManager";

    private volatile EduRoomInfo mEduRoomInfo;
    private volatile boolean mTiming;
    private volatile long mTimeLimit, mTimeEnter, mDurationServer;

    private final Runnable mDurationCounting = new Runnable() {
        @Override
        public void run() {
            if (!mTiming) {
                return;
            }
            long durationLocal = System.currentTimeMillis() - mTimeEnter;
            long tick = Math.max(0, mTimeLimit - (mDurationServer + durationLocal));
            getDataProvider().setTick(tick);
            getUIHandler().postDelayed(mDurationCounting, 1000);
        }
    };

    public EduRoomManager(@NonNull UIRtcCore uiRtcCore, @NonNull EduRoomImpl eduRoom, @NonNull EduDataProviderImpl dataProvider) {
        super(uiRtcCore, eduRoom, dataProvider);
        eduRoom.addHandler(this);
    }

    @Override
    public void dispose() {
        stopTiming();
    }

    private void startTiming() {
        MLog.d(TAG, "startTiming");
        mTiming = true;
        mTimeEnter = System.currentTimeMillis();
        getUIHandler().removeCallbacks(mDurationCounting);
        getUIHandler().post(mDurationCounting);
    }

    private void stopTiming() {
        MLog.d(TAG, "stopTiming");
        mTiming = false;
        getUIHandler().removeCallbacks(mDurationCounting);
    }

    @Override
    public void onJoinRoom(EduRoomTokenInfo info) {
        mTimeLimit = info.roomInfo.experienceTimeLimit * 1000;
        mDurationServer = Math.max(0, info.roomInfo.baseTime - info.roomInfo.startTime);

        IUIEduDef.RoomInfo roomInfo = new IUIEduDef.RoomInfo();
        roomInfo.mRoomId = info.roomInfo.roomId;
        getDataProvider().setRoomInfo(roomInfo);

        mEduRoomInfo = (EduRoomInfo) info.roomInfo.clone();
        getDataProvider().setRoomState(IUIEduDef.RoomState.CONNECTED);
        startTiming();

        getDataProvider().setWhiteBoardStream(info.user.isPullWhiteBoardStream, mEduRoomInfo.roomId, info.wbStreamUserId);

        if (info.roomInfo.shareType == SHARE_SCREEN) {
            // TODO(haiyanwu): need this check ?
            getDataProvider().setScreenStream(info.roomInfo.shareStatus, info.roomInfo.roomId, info.roomInfo.shareUserId);
        }
    }

    @Override
    public void onRoomReleased(int reason) {
        getDataProvider().setRoomState(IUIEduDef.RoomState.RELEASED);
        if (reason == IMeetingRtmEventHandler.ROOM_RELEASED_TIME_LIMIT) {
            runOnUIThread(() -> SafeToast.show(R.string.tips_time_limit));
        }
    }

    @Override
    public void onShareStarted(@NonNull String roomId, @NonNull String userId, String userName) {
        getDataProvider().setScreenStream(true, roomId, userId);
    }

    @Override
    public void onShareStopped(@NonNull String roomId, @NonNull String userId) {
        getDataProvider().setScreenStream(false, roomId, userId);
    }
}
