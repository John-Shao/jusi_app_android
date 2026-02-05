package com.drift.camcontroldemo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ss.video.rtc.demo.basic_module.utils.AppExecutors;
import com.ss.video.rtc.demo.basic_module.utils.IMEUtils;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.drift.util.TextWatcherHelper;
import com.volcengine.vertcdemo.core.SolutionDataManager;

import com.drift.foreamlib.local.ctrl.LocalController;
import com.drift.foreamlib.local.ctrl.LocalListener;
import com.drift.manager.CameraManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JoinMeetingActivity extends AppCompatActivity {

    private static final String TAG = "JoinMeetingActivity";
    private static final String ROOM_ID_REGEX = "^[A-Za-z0-9@_-]+$";
    private static final int ROOM_ID_MAX_LENGTH = 18;
    private static final String USER_NAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9@_-]+$";
    private static final int USER_NAME_MAX_LENGTH = 18;

    private String mCamIP;
    private String mSerialNumber;
    private String mStreamRes;
    private String mStreamBitrate;
    private RadioGroup mMeetingTypeRadioGroup;
    private EditText mInputRoomId;
    private TextWatcherHelper mRoomIdWatcher;
    private EditText mDeviceSn;
    private TextWatcherHelper mUserNameWatcher;
    private boolean isCreateMeeting = true; // 默认为发起会议
    private String generatedRoomId = null; // 存储生成的房间ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_meeting);

        // 从 Intent 中获取 camIP 和 serialNumber
        Intent intent = getIntent();
        if (intent != null) {
            mCamIP = intent.getStringExtra("camIP");
            mSerialNumber = intent.getStringExtra("serialNumber");
            mStreamRes = "" + intent.getIntExtra("streamRes", 0);
            mStreamBitrate = "" + (intent.getIntExtra("streamBitrate", 0) / 8);
        }

        initViews();
    }

    private void initViews() {
        View rootView = findViewById(R.id.join_meeting_root);
        rootView.setOnClickListener(IMEUtils::closeIME);

        ImageView leftIv = findViewById(R.id.title_bar_left_iv);
        leftIv.setImageResource(R.drawable.ic_back_white);
        leftIv.setOnClickListener(v -> finish());

        // 初始化场景切换按钮
        mMeetingTypeRadioGroup = findViewById(R.id.meeting_type_radio_group);
        mMeetingTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_create_meeting) {
                // 发起会议模式
                isCreateMeeting = true;
                mInputRoomId.setEnabled(false);
                mInputRoomId.setFocusable(false);
                mInputRoomId.setFocusableInTouchMode(false);
                mInputRoomId.setClickable(false);
                // 自动生成房间ID
                generateRoomId();
            } else {
                // 加入会议模式
                isCreateMeeting = false;
                mInputRoomId.setEnabled(true);
                mInputRoomId.setFocusable(true);
                mInputRoomId.setFocusableInTouchMode(true);
                mInputRoomId.setClickable(true);
                mInputRoomId.setText("");
                generatedRoomId = null;
            }
        });

        mInputRoomId = findViewById(R.id.join_meeting_room_id);
        TextView inputRoomIdError = findViewById(R.id.join_meeting_room_id_waring);
        mRoomIdWatcher = new TextWatcherHelper(mInputRoomId, inputRoomIdError, ROOM_ID_REGEX,
            R.string.create_input_room_id_content_warn, ROOM_ID_MAX_LENGTH, R.string.create_input_room_id_length_warn);

        mDeviceSn = findViewById(R.id.join_meeting_device_sn);
        TextView inputUserNameError = findViewById(R.id.join_meeting_device_sn_waring);
        mUserNameWatcher = new TextWatcherHelper(mDeviceSn, inputUserNameError, USER_NAME_REGEX,
            R.string.create_input_user_name_content_warn, USER_NAME_MAX_LENGTH, R.string.create_input_user_name_length_warn);

        // 自动填充设备序列号到用户名输入框
        if (!TextUtils.isEmpty(mSerialNumber)) {
            mDeviceSn.setText(mSerialNumber);
        }

        // 设置用户名输入框为只读
        mDeviceSn.setFocusable(false);
        mDeviceSn.setFocusableInTouchMode(false);
        mDeviceSn.setClickable(false);

        TextView joinMeetingBtn = findViewById(R.id.join_meeting_button);
        joinMeetingBtn.setOnClickListener(v -> {
            String roomId = mInputRoomId.getText().toString().trim();
            if (TextUtils.isEmpty(roomId)) {
                SafeToast.show(R.string.create_input_room_id_hint);
                return;
            }
            if (mRoomIdWatcher.isContentWarn()) {
                mRoomIdWatcher.showContentError();
                return;
            }
            String userName = mDeviceSn.getText().toString().trim();
            if (TextUtils.isEmpty(userName)) {
                SafeToast.show(R.string.create_input_user_name_hint);
                return;
            }
            if (mUserNameWatcher.isContentWarn()) {
                mUserNameWatcher.showContentError();
                return;
            }
            // 检查用户是否已登录
            String userId = SolutionDataManager.ins().getUserId();
            if (TextUtils.isEmpty(userId)) {
                // 跳转到登录页面
                Intent loginIntent = new Intent();
                loginIntent.setClassName(getApplicationContext(), "com.volcengine.vertcdemo.login.LoginActivity");
                startActivity(loginIntent);
                return;
            }

            // 直接进入会议，由服务端检查房间状态
            handleJoinMeeting(roomId);
            IMEUtils.closeIME(v);
        });

        // 默认触发发起会议模式
        mMeetingTypeRadioGroup.check(R.id.radio_create_meeting);

        // 手动触发初始化逻辑，因为 check() 方法可能不会触发 listener
        isCreateMeeting = true;
        mInputRoomId.setEnabled(false);
        mInputRoomId.setFocusable(false);
        mInputRoomId.setFocusableInTouchMode(false);
        mInputRoomId.setClickable(false);
        generateRoomId();
    }

    /**
     * 生成房间ID
     */
    private void generateRoomId() {
        AppExecutors.diskIO().execute(() -> {
            try {
                // 创建 OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // 创建空请求体
                RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    ""
                );

                // 构建请求
                Request request = new Request.Builder()
                    .url(BuildConfig.MEET_SERVER_URL + "/meeting/generate-room-id")
                    .post(requestBody)
                    .build();

                // 发送请求
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    Log.d(TAG, "Generate room id response: " + responseStr);

                    JSONObject jsonResponse = new JSONObject(responseStr);
                    int code = jsonResponse.optInt("code");

                    if (code == 200) {
                        String roomId = jsonResponse.optString("room_id");
                        generatedRoomId = roomId;

                        // 在主线程中更新UI
                        AppExecutors.mainThread().execute(() -> {
                            mInputRoomId.setText(roomId);
                        });
                    } else {
                        String message = jsonResponse.optString("message", "Unknown error");
                        showGenerateRoomIdError("Error code: " + code + ", message: " + message);
                    }
                } else {
                    showGenerateRoomIdError("HTTP error: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Generate room id failed", e);
                showGenerateRoomIdError(e.getMessage());
            }
        });
    }

    /**
     * 显示生成房间ID错误信息
     */
    private void showGenerateRoomIdError(String message) {
        AppExecutors.mainThread().execute(() -> {
            SafeToast.show(R.string.create_meeting_generate_room_id_failed);
            Log.e(TAG, "Generate room id error: " + message);
        });
    }

    /**
     * 处理进入会议的逻辑
     * @param roomId 房间ID
     */
    private void handleJoinMeeting(String roomId) {
        // 检查 camIP 是否为空
        if (TextUtils.isEmpty(mCamIP)) {
            SafeToast.show(R.string.join_meeting_cam_ip_empty);
            return;
        }

        // 调用后端接口获取推流和拉流地址
        requestCameraJoinRoom(roomId);
    }

    /**
     * 请求相机加入房间接口（支持发起会议和加入会议两种场景）
     */
    private void requestCameraJoinRoom(String roomId) {
        AppExecutors.diskIO().execute(() -> {
            try {
                // 构建请求参数
                JSONObject params = new JSONObject();
                params.put("room_id", roomId);
                params.put("room_name", "R" + roomId);
                params.put("device_sn", mSerialNumber); // 使用设备序列号
                params.put("action_type", isCreateMeeting ? 0 : 1); // 0:发起会议, 1:加入会议

                // 如果是发起会议，添加持有者信息
                if (isCreateMeeting) {
                    String userId = SolutionDataManager.ins().getUserId();
                    String userName = SolutionDataManager.ins().getUserName();
                    if (!TextUtils.isEmpty(userId)) {
                        params.put("holder_user_id", userId);
                    }
                    if (!TextUtils.isEmpty(userName)) {
                        params.put("holder_user_name", userName);
                    }
                }

                // 创建 OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // 创建请求体
                RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    params.toString()
                );

                // 构建请求
                Request request = new Request.Builder()
                    .url(BuildConfig.MEET_SERVER_URL + "/meeting/camera-join")
                    .post(requestBody)
                    .build();

                // 发送请求
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    Log.d(TAG, "Camera join response: " + responseStr);

                    JSONObject jsonResponse = new JSONObject(responseStr);
                    int code = jsonResponse.optInt("code");

                    if (code == 200) {
                        String rtmpUrl = jsonResponse.optString("rtmp_url");
                        String rtspUrl = jsonResponse.optString("rtsp_url");

                        // 在主线程中调用推流和拉流
                        AppExecutors.mainThread().execute(() -> {
                            // 调用推流方法
                            startPushStream(rtmpUrl);

                            // 调用拉流方法，传递roomId
                            // startPullStream(rtspUrl, roomId);

                            // 加入会议后，关闭当前界面
                            finish();
                        });
                    } else {
                        String message = jsonResponse.optString("message", "Unknown error");
                        showError("Error code: " + code + ", message: " + message);
                    }
                } else {
                    showError("HTTP error: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Request failed", e);
                showError(e.getMessage());
            }
        });
    }

    /**
     * 开始推流
     */
    private void startPushStream(String rtmpUrl) {
        LocalController localController = new LocalController();
        // localController.startPushStreamWithURL(mCamIP, rtmpUrl, mStreamRes, mStreamBitrate,
        localController.startPushStreamWithURL(mCamIP, rtmpUrl, "720P", "2000000",
            new LocalListener.OnCommonResListener() {
                @Override
                public void onCommonRes(boolean success) {
                    Log.d(TAG, "Push stream result: " + success);
                    if (!success) {
                        SafeToast.show("Failed to start push stream");
                    }
                }
            }
        );
    }

    /**
     * 开始拉流
     * @param rtspUrl 拉流地址
     * @param roomId 房间ID
     */
    private void startPullStream(String rtspUrl, String roomId) {
        LocalController localController = new LocalController();
        localController.startPullStreamWithURL(mCamIP, rtspUrl,
            new LocalListener.OnCommonResListener() {
                @Override
                public void onCommonRes(boolean success) {
                    Log.d(TAG, "Pull stream result: " + success);
                    if (success) {
                        // 设置相机会议状态为 true，并保存房间ID
                        CameraManager.getInstance().setCameraInMeeting(mCamIP, true);
                        CameraManager.getInstance().setCameraRoomId(mCamIP, roomId);
                        SafeToast.show("Join meeting successfully");
                    } else {
                        SafeToast.show("Failed to start pull stream");
                    }
                }
            }
        );
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        AppExecutors.mainThread().execute(() -> {
            SafeToast.show(R.string.join_meeting_request_failed + ": " + message);
            Log.e(TAG, "Error: " + message);
        });
    }
}
