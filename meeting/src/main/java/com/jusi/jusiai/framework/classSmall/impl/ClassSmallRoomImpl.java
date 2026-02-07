package com.jusi.jusiai.framework.classSmall.impl;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jusi.jusiai.common.AbsBroadcast;
import com.jusi.jusiai.core.UIRtcCore;
import com.jusi.jusiai.core.net.IRequestCallback;
import com.jusi.jusiai.core.net.rtm.RTMBizResponse;
import com.jusi.jusiai.core.net.rtm.RtmInfo;
import com.jusi.jusiai.framework.classSmall.UIClassSmallRoom;
import com.jusi.jusiai.framework.meeting.IUIMeetingDef;
import com.jusi.jusiai.framework.meeting.bean.MeetingTokenInfo;
import com.jusi.jusiai.framework.meeting.impl.MeetingRoomImpl;
import com.jusi.jusiai.framework.meeting.internal.IMeetingRtmDef;

public class ClassSmallRoomImpl extends MeetingRoomImpl implements UIClassSmallRoom {

    public ClassSmallRoomImpl(@NonNull Context context, @NonNull RtmInfo rtmInfo, @NonNull String roomId, @NonNull UIRtcCore uiRtcCore, IUIMeetingDef.IRoleDesc roleDesc) {
        super(context, rtmInfo, roomId, uiRtcCore, roleDesc);
    }

    @Override
    protected <T extends IMeetingRtmDef.Notify> void putEventListener(String event, Class<T> clz, AbsBroadcast.On<T> on) {
        super.putEventListener(event.replaceFirst("vc", "edus"), clz, on);
    }

    @Override
    protected <T extends RTMBizResponse> void sendMessage(String eventName, Object content, Class<T> resultClass, IRequestCallback<T> callback) {
        super.sendMessage(eventName.replaceFirst("vc", "edus"), content, resultClass, callback);
    }

    @Override
    public void joinRoom(String userName, boolean isHost, IRequestCallback<MeetingTokenInfo> callback) {
        IMeetingRtmDef.JoinClassRoomReq req = new IMeetingRtmDef.JoinClassRoomReq();
        req.userName = userName;
        req.isHost = isHost;
        req.openMic = getUIRtcCore().getRtcDataProvider().isMicOpen();
        req.openCam = getUIRtcCore().getRtcDataProvider().isCamOpen();
        sendMessage(IMeetingRtmDef.CMD_JOIN_ROOM, req, MeetingTokenInfo.class, new IRequestCallback<MeetingTokenInfo>() {
            @Override
            public void onSuccess(MeetingTokenInfo data) {
                joinRtcRoom(data.rtcToken, data.user.userId);
                configWhiteBoardRoom(data.roomInfo.appId, data.wbRoomId, data.wbUserId, data.wbToken);
                notifyHandler(callback -> callback.onJoinRoom(data));
                callback.onSuccess(data);
            }

            @Override
            public void onError(int errorCode, String message) {
                callback.onError(errorCode, message);
            }
        });
    }

    @Override
    public void joinRoom(String userName, IRequestCallback<MeetingTokenInfo> callback) {
        throw new IllegalAccessError("do not supported");
    }
}
