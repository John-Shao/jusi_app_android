package com.jusi.jusiai.framework.classLarge;

import androidx.annotation.NonNull;

import com.jusi.jusiai.core.UIRtcCore;
import com.jusi.jusiai.core.WhiteBoardService;
import com.jusi.jusiai.core.net.IRequestCallback;
import com.jusi.jusiai.framework.classLarge.bean.EduRoomTokenInfo;

import java.util.Set;

public interface UIEduRoom {

    UIRtcCore getUIRtcCore();

    IUIEduDef.IEduDataProvider getDataProvider();

    WhiteBoardService getWhiteBoardService();

    void dispose();

    void joinRoom(String userName, boolean isHost, IRequestCallback<EduRoomTokenInfo> callback);

    void openMic(boolean open);

    void openCam(boolean open);

    void subscribeVideoStream(@NonNull Set<String> userIds);

    void subscribeWhiteBoardStream();

    void unsubscribeWhiteBoardStream();

    void linkMicApply();
    void linkMicApplyCancel();

    void linkMicLeave();

    void leaveRoom();
}
