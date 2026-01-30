package com.drift.camcontroldemo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
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

import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

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
    private static final int ROOM_ID_LENGTH = 6;
    private static final String USER_NAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9@_-]+$";
    private static final int USER_NAME_MAX_LENGTH = 18;
    private static final String ROOM_NAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9@_-\\s]+$";
    private static final int ROOM_NAME_MAX_LENGTH = 50;

    private String mCamIP;
    private String mSerialNumber;
    private String mStreamRes;
    private String mStreamBitrate;
    private RadioGroup mMeetingModeRadioGroup;
    private RadioButton mRadioCreateMeeting;
    private RadioButton mRadioJoinMeeting;
    private EditText mInputRoomName;
    private TextWatcherHelper mRoomNameWatcher;
    private View mRoomNameDivider;
    private TextView mRoomNameWarning;
    private EditText mInputRoomId;
    private TextWatcherHelper mRoomIdWatcher;
    private EditText mDeviceSn;
    private TextWatcherHelper mUserNameWatcher;
    private TextView mJoinMeetingBtn;
    private boolean isCreateMeetingMode = true;

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

        // 初始化会议模式选择RadioGroup
        mMeetingModeRadioGroup = findViewById(R.id.meeting_mode_radio_group);
        mRadioCreateMeeting = findViewById(R.id.radio_create_meeting);
        mRadioJoinMeeting = findViewById(R.id.radio_join_meeting);

        // 初始化房间名称输入框
        mInputRoomName = findViewById(R.id.create_meeting_room_name);
        mRoomNameDivider = findViewById(R.id.create_meeting_room_name_divider);
        mRoomNameWarning = findViewById(R.id.create_meeting_room_name_waring);
        mRoomNameWatcher = new TextWatcherHelper(mInputRoomName, mRoomNameWarning, ROOM_NAME_REGEX,
            R.string.create_meeting_room_name_content_warn, ROOM_NAME_MAX_LENGTH, R.string.create_meeting_room_name_length_warn);

        // 设置RadioGroup变化监听
        mMeetingModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_create_meeting) {
                switchMeetingMode(true);
            } else if (checkedId == R.id.radio_join_meeting) {
                switchMeetingMode(false);
            }
        });

        // 默认选中"发起会议"，初始化UI
        switchMeetingMode(true);

        mJoinMeetingBtn = findViewById(R.id.join_meeting_button);
        mJoinMeetingBtn.setOnClickListener(v -> {
            // 检查用户是否已登录
            String userId = SolutionDataManager.ins().getUserId();
            if (TextUtils.isEmpty(userId)) {
                // 跳转到登录页面
                Intent loginIntent = new Intent();
                loginIntent.setClassName(getApplicationContext(), "com.volcengine.vertcdemo.login.LoginActivity");
                startActivity(loginIntent);
                return;
            }

            // 检查设备序列号
            String deviceSn = mDeviceSn.getText().toString().trim();
            if (TextUtils.isEmpty(deviceSn)) {
                SafeToast.show(R.string.join_meeting_device_sn_hint);
                return;
            }

            // 检查camIP
            if (TextUtils.isEmpty(mCamIP)) {
                SafeToast.show(R.string.join_meeting_cam_ip_empty);
                return;
            }

            String roomId = mInputRoomId.getText().toString().trim();
            if (TextUtils.isEmpty(roomId)) {
                SafeToast.show(R.string.create_input_room_id_hint);
                return;
            }

            if (isCreateMeetingMode) {
                // 发起会议模式
                String roomName = mInputRoomName.getText().toString().trim();
                if (TextUtils.isEmpty(roomName)) {
                    SafeToast.show(R.string.create_meeting_room_name_empty);
                    return;
                }
                if (mRoomNameWatcher.isContentWarn()) {
                    mRoomNameWatcher.showContentError();
                    return;
                }
                // 调用创建会议
                createMeeting(roomId, roomName);
            } else {
                // 加入会议模式
                // 验证房间ID必须是6位数字
                if (roomId.length() != ROOM_ID_LENGTH || !roomId.matches("\\d{6}")) {
                    SafeToast.show(R.string.create_meeting_room_id_must_6_digits);
                    return;
                }
                // 先检查房间是否存在
                checkRoomExists(roomId);
            }

            IMEUtils.closeIME(v);
        });
    }

    /**
     * 检查房间是否存在
     * @param roomId 房间ID
     */
    private void checkRoomExists(String roomId) {
        AppExecutors.diskIO().execute(() -> {
            try {
                // 构建请求参数
                JSONObject params = new JSONObject();
                params.put("room_id", roomId);

                // 创建 OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // 创建请求体
                RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    params.toString()
                );

                // 构建请求
                Request request = new Request.Builder()
                    .url(BuildConfig.MEET_SERVER_URL + "/meeting/check-room")
                    .post(requestBody)
                    .build();

                // 发送请求
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    Log.d(TAG, "Check room response: " + responseStr);

                    JSONObject jsonResponse = new JSONObject(responseStr);
                    int code = jsonResponse.optInt("code");
                    boolean exists = jsonResponse.optBoolean("exists", false);

                    if (code == 200) {
                        if (exists) {
                            // 房间存在，继续进入会议流程
                            AppExecutors.mainThread().execute(() -> {
                                handleJoinMeeting(roomId);
                            });
                        } else {
                            // 房间不存在，显示错误
                            AppExecutors.mainThread().execute(() -> {
                                SafeToast.show(R.string.join_meeting_room_not_exist);
                            });
                        }
                    } else {
                        String message = jsonResponse.optString("message", "Unknown error");
                        showCheckRoomError("Error code: " + code + ", message: " + message);
                    }
                } else {
                    showCheckRoomError("HTTP error: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Check room failed", e);
                showCheckRoomError(e.getMessage());
            }
        });
    }

    /**
     * 显示检查房间错误信息
     */
    private void showCheckRoomError(String message) {
        AppExecutors.mainThread().execute(() -> {
            SafeToast.show(R.string.join_meeting_check_room_failed);
            Log.e(TAG, "Check room error: " + message);
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
     * 请求相机加入房间接口
     */
    private void requestCameraJoinRoom(String roomId) {
        AppExecutors.diskIO().execute(() -> {
            try {
                // 构建请求参数（扁平化结构）
                JSONObject params = new JSONObject();
                params.put("user_id", SolutionDataManager.ins().getUserId());
                params.put("room_id", roomId);
                params.put("device_sn", mSerialNumber); // 使用设备序列号

                // 创建 OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // 创建请求体
                RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    params.toString()
                );

                // 构建请求
                Request request = new Request.Builder()
                    .url(BuildConfig.MEET_SERVER_URL + "/camera/join")
                    .post(requestBody)
                    .build();

                // 发送请求
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    Log.d(TAG, "Response: " + responseStr);

                    JSONObject jsonResponse = new JSONObject(responseStr);
                    int code = jsonResponse.optInt("code");

                    if (code == 200) {
                        JSONObject responseData = jsonResponse.optJSONObject("data");
                        if (responseData != null) {
                            String rtmpUrl = responseData.optString("rtmp_url");
                            String rtspUrl = responseData.optString("rtsp_url");

                            // 在主线程中调用推流和拉流
                            AppExecutors.mainThread().execute(() -> {
                                // 调用推流方法
                                startPushStream(rtmpUrl);

                                // 调用拉流方法
                                startPullStream(rtspUrl);
                            });
                        } else {
                            showError("Response data is null");
                        }
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
        localController.startPushStreamWithURL(mCamIP, rtmpUrl, mStreamRes, mStreamBitrate,
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
     */
    private void startPullStream(String rtspUrl) {
        LocalController localController = new LocalController();
        localController.startPullStreamWithURL(mCamIP, rtspUrl,
            new LocalListener.OnCommonResListener() {
                @Override
                public void onCommonRes(boolean success) {
                    Log.d(TAG, "Pull stream result: " + success);
                    if (success) {
                        SafeToast.show("Join meeting successfully");
                        finish();
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
            SafeToast.show(R.string.join_meeting_request_failed);
            Log.e(TAG, "Error: " + message);
        });
    }

    /**
     * 切换会议模式
     * @param isCreateMode true表示发起会议模式，false表示加入会议模式
     */
    private void switchMeetingMode(boolean isCreateMode) {
        isCreateMeetingMode = isCreateMode;
        if (isCreateMode) {
            // 发起会议模式
            mInputRoomName.setVisibility(View.VISIBLE);
            mRoomNameDivider.setVisibility(View.VISIBLE);

            // 设置房间ID为只读
            mInputRoomId.setFocusable(false);
            mInputRoomId.setFocusableInTouchMode(false);
            mInputRoomId.setClickable(false);

            // 生成6位房间ID
            generateRoomId();

            // 设置默认房间名称
            String userName = SolutionDataManager.ins().getUserName();
            if (!TextUtils.isEmpty(userName)) {
                String defaultRoomName = getString(R.string.create_meeting_room_name_default, userName);
                mInputRoomName.setText(defaultRoomName);
            }

            mJoinMeetingBtn.setText(R.string.create_meeting);
        } else {
            // 加入会议模式
            mInputRoomName.setVisibility(View.GONE);
            mRoomNameDivider.setVisibility(View.GONE);

            // 设置房间ID为可编辑
            mInputRoomId.setFocusable(true);
            mInputRoomId.setFocusableInTouchMode(true);
            mInputRoomId.setClickable(true);

            // 清空自动生成的房间ID
            mInputRoomId.setText("");

            mJoinMeetingBtn.setText(R.string.join_meeting);
        }
    }

    /**
     * 生成6位随机房间ID
     */
    private void generateRoomId() {
        Random random = new Random();
        int roomId = 100000 + random.nextInt(900000); // 生成100000-999999之间的随机数
        mInputRoomId.setText(String.valueOf(roomId));
    }

    /**
     * 创建会议
     * @param roomId 房间ID
     * @param roomName 房间名称
     */
    private void createMeeting(String roomId, String roomName) {
        AppExecutors.diskIO().execute(() -> {
            try {
                // 构建请求参数
                JSONObject params = new JSONObject();
                params.put("user_id", SolutionDataManager.ins().getUserId());
                params.put("room_name", roomName);
                params.put("room_id", roomId);
                params.put("device_sn", mSerialNumber);

                // 创建 OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // 创建请求体
                RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    params.toString()
                );

                // 构建请求
                Request request = new Request.Builder()
                    .url(BuildConfig.MEET_SERVER_URL + "/meeting/book")
                    .post(requestBody)
                    .build();

                // 发送请求
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    Log.d(TAG, "Create meeting response: " + responseStr);

                    JSONObject jsonResponse = new JSONObject(responseStr);
                    int code = jsonResponse.optInt("code");

                    if (code == 200) {
                        JSONObject responseData = jsonResponse.optJSONObject("data");
                        if (responseData != null) {
                            String rtmpUrl = responseData.optString("rtmp_url");
                            String rtspUrl = responseData.optString("rtsp_url");

                            // 在主线程中调用推流和拉流
                            AppExecutors.mainThread().execute(() -> {
                                // 调用推流方法
                                startPushStream(rtmpUrl);

                                // 调用拉流方法
                                startPullStream(rtspUrl);
                            });
                        } else {
                            showCreateMeetingError("Response data is null");
                        }
                    } else {
                        String message = jsonResponse.optString("message", "Unknown error");
                        showCreateMeetingError("Error code: " + code + ", message: " + message);
                    }
                } else {
                    showCreateMeetingError("HTTP error: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Create meeting failed", e);
                showCreateMeetingError(e.getMessage());
            }
        });
    }

    /**
     * 显示创建会议错误信息
     */
    private void showCreateMeetingError(String message) {
        AppExecutors.mainThread().execute(() -> {
            SafeToast.show(R.string.create_meeting_failed);
            Log.e(TAG, "Create meeting error: " + message);
        });
    }
}
