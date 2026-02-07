package com.jusi.jusiai.framework.meeting.bean;

import com.google.gson.annotations.SerializedName;
import com.jusi.jusiai.core.net.rtm.RTMBizResponse;

import java.util.List;

public class MeetingUsersInfo implements RTMBizResponse {

    @SerializedName("user_list")
    public List<MeetingUserInfo> usersList;

    @SerializedName("user_count")
    public int userCount;
}
