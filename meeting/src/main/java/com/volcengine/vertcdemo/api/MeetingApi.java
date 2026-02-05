package com.volcengine.vertcdemo.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.ss.video.rtc.demo.basic_module.utils.AppExecutors;
import com.volcengine.vertcdemo.bean.GetMyMeetingsResponse;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.meeting.BuildConfig;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 会议管理 API
 */
public class MeetingApi {
    private static final String TAG = "MeetingApi";
    private static final String MEET_SERVER_URL = BuildConfig.MEET_SERVER_URL;
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();

    /**
     * 获取我的会议列表
     *
     * @param userId   用户ID
     * @param callBack 回调
     */
    public static void getMyMeetings(String userId, @NonNull IRequestCallback<GetMyMeetingsResponse> callBack) {
        try {
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
     * 发送 POST 请求（直接解析后端返回的格式）
     */
    private static <T> void sendPost(String endpoint, JSONObject params, Class<T> resultClass,
                                     @NonNull IRequestCallback<T> callBack) {
        String url = MEET_SERVER_URL + endpoint;
        Log.d(TAG, "sendPost url: " + url);

        AppExecutors.networkIO().execute(() -> {
            try {
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    params.toString()
                );

                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

                Call call = OK_HTTP_CLIENT.newCall(request);
                Response response = call.execute();

                if (!response.isSuccessful()) {
                    throw new IOException("HTTP error code: " + response.code());
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IOException("Response body is null");
                }

                String responseString = responseBody.string();
                Log.d(TAG, "Response: " + responseString);

                // 直接解析后端返回的 JSON（不使用 ServerResponse 包装）
                T result = GSON.fromJson(responseString, resultClass);

                AppExecutors.mainThread().execute(() -> {
                    if (result != null) {
                        callBack.onSuccess(result);
                    } else {
                        callBack.onError(-1, "Failed to parse response");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "sendPost exception", e);
                AppExecutors.mainThread().execute(() ->
                    callBack.onError(-1, "Network error: " + e.getMessage())
                );
            }
        });
    }
}
