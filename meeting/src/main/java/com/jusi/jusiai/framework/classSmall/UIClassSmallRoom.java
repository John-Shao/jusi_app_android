package com.jusi.jusiai.framework.classSmall;

import com.jusi.jusiai.core.net.IRequestCallback;
import com.jusi.jusiai.framework.meeting.UIMeetingRoom;
import com.jusi.jusiai.framework.meeting.bean.MeetingTokenInfo;

public interface UIClassSmallRoom extends UIMeetingRoom {

    void joinRoom(String userName, boolean isHost, IRequestCallback<MeetingTokenInfo> callback);
}
