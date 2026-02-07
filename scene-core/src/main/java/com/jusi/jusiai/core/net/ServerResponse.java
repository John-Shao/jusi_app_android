package com.jusi.jusiai.core.net;

import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.ss.video.rtc.demo.basic_module.utils.GsonUtils;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.jusi.jusiai.core.R;
import com.jusi.jusiai.core.SolutionDataManager;
import com.jusi.jusiai.core.eventbus.SolutionDemoEventManager;
import com.jusi.jusiai.core.eventbus.TokenExpiredEvent;
import com.jusi.jusiai.core.net.http.NetworkException;

import org.json.JSONObject;

import java.lang.reflect.Type;

public class ServerResponse<T> {
    @Keep
    public static final String MESSAGE_TYPE_RETURN = "return";
    @Keep
    public static final String MESSAGE_TYPE_INFORM = "inform";

    private int code = -1;
    private String msg = "";
    private long timestamp = -1;
    private T data;

    private ServerResponse(int code, String message, T data, long timestamp) {
        this.code = code;
        this.msg = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public ServerResponse(Object originResponse, Class<T> clz) {
        if (originResponse instanceof JSONObject) {
            JSONObject resp = (JSONObject) originResponse;
            code = resp.optInt("code");
            msg = resp.optString("message");
            timestamp = resp.optLong("timestamp");
            Object respObj = resp.opt("response");
            if (!JSONObject.NULL.equals(respObj)
//                    && respObj instanceof JSONObject
                    && !TextUtils.equals("{}", respObj.toString())) {
                data = GsonUtils.gson().fromJson(respObj.toString(), clz);
            }
            if (code == NetworkException.CODE_TOKEN_EXPIRED) {
                SolutionDataManager.ins().clear();
                SolutionDemoEventManager.post(new TokenExpiredEvent());
            } else if (code == 430) {
                msg = Utilities.getApplicationContext().getString(R.string.error_msg_sensitive_word_input);
            }
        }
    }

    public ServerResponse(Object originResponse, Type type) {
        if (originResponse instanceof JSONObject) {
            JSONObject resp = (JSONObject) originResponse;
            code = resp.optInt("code");
            msg = resp.optString("message");
            timestamp = resp.optLong("timestamp");
            Object respObj = resp.opt("response");
            if (!JSONObject.NULL.equals(respObj)
                    && respObj instanceof JSONObject
                    && !TextUtils.equals("{}", respObj.toString())) {
                data = GsonUtils.gson().fromJson(respObj.toString(), type);
            }
            if (code == NetworkException.CODE_TOKEN_EXPIRED) {
                SolutionDataManager.ins().clear();
                SolutionDemoEventManager.post(new TokenExpiredEvent());
            } else if (code == 430) {
                msg = Utilities.getApplicationContext().getString(R.string.error_msg_sensitive_word_input);
            }
        }
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static <T> ServerResponse<T> create(int code, String message, @Nullable T data, long timestamp) {
        return new ServerResponse<>(code, message, data, timestamp);
    }
}