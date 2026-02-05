package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * 会议信息
 */
public class MeetingInfo implements Serializable {
    @SerializedName("room_id")
    public String roomId;  // 会议号

    @SerializedName("room_name")
    public String roomName;  // 会议名称

    @SerializedName("host_user_id")
    public String hostUserId;  // 主持人用户ID

    @SerializedName("host_user_name")
    public String hostUserName;  // 主持人用户名

    @SerializedName("start_time")
    public long startTime;  // 开始时间（秒）

    @SerializedName("user_count")
    public int userCount;  // 当前房间人数

    @Override
    public String toString() {
        return "MeetingInfo{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", hostUserId='" + hostUserId + '\'' +
                ", hostUserName='" + hostUserName + '\'' +
                ", startTime=" + startTime +
                ", userCount=" + userCount +
                '}';
    }
}
