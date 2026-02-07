package com.jusi.jusiai.framework;

import android.content.Context;

import com.jusi.jusiai.common.MLog;
import com.jusi.jusiai.core.UIRtcCore;
import com.jusi.jusiai.core.net.rtm.RtmInfo;
import com.jusi.jusiai.framework.classLarge.UIEduRoom;
import com.jusi.jusiai.framework.classLarge.impl.EduRoomImpl;
import com.jusi.jusiai.framework.classSmall.UIClassSmallRoom;
import com.jusi.jusiai.framework.classSmall.impl.ClassSmallRoomImpl;
import com.jusi.jusiai.framework.meeting.IUIMeetingDef;
import com.jusi.jusiai.framework.meeting.UIMeetingRoom;
import com.jusi.jusiai.framework.meeting.impl.MeetingRoomImpl;
import com.jusi.jusiai.meeting.R;

public class UIRoomMgr {

    private static final String TAG = "UIRoomMgr";

    private static UIMeetingRoom sMeetingRoomIns = null;
    private static UIEduRoom sUIEduRoomIns = null;

    public static synchronized UIMeetingRoom createMeetingRoom(Context context, RtmInfo rtmInfo, String roomId) {
        UIRtcCore uiRtcCore = UIRtcCore.ins();
        if (uiRtcCore == null) {
            MLog.w(TAG, "");
            return null;
        }

        sMeetingRoomIns = new MeetingRoomImpl(context, rtmInfo, roomId, uiRtcCore, new IUIMeetingDef.IRoleDesc() {
            @Override
            public String hostDesc() {
                return context.getString(R.string.role_host_desc_meeting);
            }

            @Override
            public String participantDesc() {
                return context.getString(R.string.role_participant_desc_meeting);
            }
        });

        return sMeetingRoomIns;
    }

    public static synchronized UIClassSmallRoom createClassSmallRoom(Context context, RtmInfo rtmInfo, String roomId) {
        UIRtcCore uiRtcCore = UIRtcCore.ins();
        if (uiRtcCore == null) {
            MLog.w(TAG, "");
            return null;
        }

        ClassSmallRoomImpl classSmallCore = new ClassSmallRoomImpl(context, rtmInfo, roomId, uiRtcCore, new IUIMeetingDef.IRoleDesc() {
            @Override
            public String hostDesc() {
                return context.getString(R.string.role_host_desc_class);
            }

            @Override
            public String participantDesc() {
                return context.getString(R.string.role_participant_desc_class);
            }
        });
        sMeetingRoomIns = classSmallCore;
        return classSmallCore;
    }

    public static synchronized UIEduRoom createEduRoom(Context context, RtmInfo rtmInfo, String roomId) {
        UIRtcCore uiRtcCore = UIRtcCore.ins();
        if (uiRtcCore == null) {
            MLog.w(TAG, "");
            return null;
        }
        sUIEduRoomIns = new EduRoomImpl(context, rtmInfo, roomId, uiRtcCore);
        return sUIEduRoomIns;
    }

    public static synchronized UIMeetingRoom meetingRoom() {
        return sMeetingRoomIns;
    }

    public static synchronized UIEduRoom eduRoom() {
        return sUIEduRoomIns;
    }

    public static synchronized void releaseMeetingRoom() {
        if (sMeetingRoomIns != null) {
            sMeetingRoomIns.dispose();
            sMeetingRoomIns = null;
        }
    }

    public static synchronized void releaseEduRoom() {
        if (sUIEduRoomIns != null) {
            sUIEduRoomIns.dispose();
            sUIEduRoomIns = null;
        }
    }
}
