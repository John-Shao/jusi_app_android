package com.volcengine.vertcdemo.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.ss.video.rtc.demo.basic_module.utils.AppExecutors;
import com.volcengine.vertcdemo.bean.GetMyMeetingsResponse;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.http.HttpRequestHelper;
import com.volcengine.vertcdemo.meeting.BuildConfig;

import org.json.JSONObject;

/**
 * 会议管理 API
 */
public class MeetingApi {
    private static final String TAG = "MeetingApi";
    private static final String MEET_SERVER_URL = BuildConfig.MEET_SERVER_URL;

    /**
     * 获取我的会议列表
     *
     * @param userId   用户ID
     * @param callBack 回调
     */
    public static void getMyMeetings(String userId, @NonNull IRequestCallback<GetMyMeetingsResponse> callBack) {
        try {
            JSONObject content = new JSONObject();
            content.put("user_id", userId);

            JSONObject params = new JSONObject();
            params.put("user_id", userId);

            Log.d(TAG, "getMyMeetings params: " + params);
            sendPost("/meeting/get-my", params, GetMyMeetingsResponse.class, callBack);
        } catch (Exception e) {
            Log.e(TAG, "getMyMeetings failed", e);
            callBack.onError(-1, "Request Error: " + e.getMessage());
        }
    }

    /**
     * 发送 POST 请求
     */
    private static <T> void sendPost(String endpoint, JSONObject params, Class<T> resultClass,
                                     @NonNull IRequestCallback<T> callBack) {
        String url = MEET_SERVER_URL + endpoint;
        Log.d(TAG, "sendPost url: " + url);
        AppExecutors.networkIO().execute(() -> {
            try {
                HttpRequestHelper.sendPost(url, params, resultClass, new IRequestCallback<com.volcengine.vertcdemo.core.net.ServerResponse<T>>() {
                    @Override
                    public void onSuccess(com.volcengine.vertcdemo.core.net.ServerResponse<T> response) {
                        if (response != null && response.getData() != null) {
                            callBack.onSuccess(response.getData());
                        } else {
                            callBack.onError(-1, "Response data is null");
                        }
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        callBack.onError(errorCode, message);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "sendPost exception", e);
                AppExecutors.mainThread().execute(() -> callBack.onError(-1, e.getMessage()));
            }
        });
    }
}
