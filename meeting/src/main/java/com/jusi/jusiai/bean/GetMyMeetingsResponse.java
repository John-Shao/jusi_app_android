package com.jusi.jusiai.bean;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 获取我的会议列表响应
 */
public class GetMyMeetingsResponse {
    @SerializedName("code")
    public int code;  // 状态码：200成功，500服务器错误

    @SerializedName("meetings")
    public List<MeetingInfo> meetings;  // 会议列表

    @SerializedName("total")
    public int total;  // 会议总数

    @SerializedName("message")
    public String message;  // 消息

    @Override
    public String toString() {
        return "GetMyMeetingsResponse{" +
                "code=" + code +
                ", meetings=" + meetings +
                ", total=" + total +
                ", message='" + message + '\'' +
                '}';
    }
}
