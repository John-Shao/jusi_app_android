package com.jusi.jusiai.feature.createroom;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ss.bytertc.engine.RTCEngine;
import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.ss.video.rtc.demo.basic_module.utils.IMEUtils;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.ss.video.rtc.demo.basic_module.utils.WindowUtils;
import com.jusi.jusiai.common.MLog;
import com.jusi.jusiai.core.IUIRtcDef;
import com.jusi.jusiai.core.SolutionDataManager;
import com.jusi.jusiai.core.UIRtcCore;
import com.jusi.jusiai.core.eventbus.RefreshUserNameEvent;
import com.jusi.jusiai.core.eventbus.SolutionDemoEventManager;
import com.jusi.jusiai.core.eventbus.TokenExpiredEvent;
import com.jusi.jusiai.core.net.ErrorTool;
import com.jusi.jusiai.core.net.IRequestCallback;
import com.jusi.jusiai.core.net.ServerResponse;
import com.jusi.jusiai.core.net.rtm.RTMBaseClient;
import com.jusi.jusiai.core.net.rtm.RtmInfo;
import com.jusi.jusiai.feature.roommain.MeetingRoomActivity;
import com.jusi.jusiai.framework.UIRoomMgr;
import com.jusi.jusiai.framework.meeting.UIMeetingRoom;
import com.jusi.jusiai.framework.meeting.bean.MeetingTokenInfo;
import com.jusi.jusiai.login.LoginApi;
import com.jusi.jusiai.meeting.R;
import com.jusi.jusiai.utils.TextWatcherHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import pub.devrel.easypermissions.AppSettingsDialog;

public class CreateMeetingActivity extends BaseActivity {

    private static final String TAG = "CreateMeetingActivity";
    private static final String ROOM_ID_REGEX = "^[A-Za-z0-9@_-]+$";
    private static final int ROOM_ID_MAX_LENGTH = 6;
    private static final String USER_NAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9@_-]+$";
    private static final int USER_NAME_MAX_LENGTH = 18;

    private FrameLayout mCameraPreviewContainer;
    private EditText mInputRoomId;
    private TextWatcherHelper mRoomIdWatcher;
    private EditText mInputUserName;
    private TextWatcherHelper mUserNameWatcher;
    private ImageView mCameraSwitch;
    private ImageView mMicSwitch;
    protected RtmInfo mRtmInfo;
    protected UIRtcCore mUIRtcCore;
    private boolean isJoining = false; // 标记是否正在加入会议，防止重复点击
    private TextView joinRoomBtn; // 保存按钮引用，方便禁用和启用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResLayoutId());
        mCameraPreviewContainer = findViewById(R.id.create_room_camera_preview);
        initRtsInfo();
        SolutionDemoEventManager.register(this);
    }

    protected int getResLayoutId() {
        return R.layout.activity_create_room;
    }

    private void initRtsInfo() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        mRtmInfo = intent.getParcelableExtra(RtmInfo.KEY_RTM);
        if (mRtmInfo == null || !mRtmInfo.isValid()) {
            finish();
        }
        UIRtcCore.init(this, mRtmInfo);
    }

    @Override
    protected void setupStatusBar() {
        WindowUtils.setLayoutFullScreen(getWindow());
    }

    @Override
    protected void onResume() {
        super.onResume();
        MLog.d(TAG, "onResume");
        configLocalRenderer();
    }

    protected void onGlobalLayoutCompleted() {
        MLog.d(TAG, "onGlobalLayoutCompleted");
        View rootView = findViewById(R.id.create_room_root);
        rootView.setOnClickListener(IMEUtils::closeIME);

        TextView titleTv = findViewById(R.id.title_bar_title_tv);
        titleTv.setText(R.string.scene_meeting);
        titleTv.setTextColor(ContextCompat.getColor(this, R.color.white));

        RadioGroup clientRole = findViewById(R.id.create_room_role);
        RadioButton roleHost = findViewById(R.id.create_room_role_host);
        RadioButton roleAttendee = findViewById(R.id.create_room_role_attendee);
        clientRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.create_room_role_host) {
                roleHost.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.txt_H1_18sp));
                roleAttendee.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.txt_H2_16sp));
            } else if (checkedId == R.id.create_room_role_attendee) {
                roleAttendee.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.txt_H1_18sp));
                roleHost.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.txt_H2_16sp));
            }
        });
        clientRole.setVisibility(View.GONE);

        ImageView leftIv = findViewById(R.id.title_bar_left_iv);
        leftIv.setImageResource(R.drawable.ic_back_white);
        leftIv.setOnClickListener(v -> finish());

        mInputRoomId = findViewById(R.id.create_room_id);
        TextView inputRoomIdError = findViewById(R.id.create_room_id_waring);
        mRoomIdWatcher = new TextWatcherHelper(mInputRoomId, inputRoomIdError, ROOM_ID_REGEX, R.string.create_input_room_id_content_warn, ROOM_ID_MAX_LENGTH, R.string.create_input_room_id_length_warn);

        mInputUserName = findViewById(R.id.create_room_user_name);
        TextView inputUserNameError = findViewById(R.id.create_room_user_name_waring);
        mUserNameWatcher = new TextWatcherHelper(mInputUserName, inputUserNameError, USER_NAME_REGEX, R.string.create_input_user_name_content_warn, USER_NAME_MAX_LENGTH, R.string.create_input_user_name_length_warn);
        mInputUserName.setText(SolutionDataManager.ins().getUserName());

        joinRoomBtn = findViewById(R.id.create_room_join);
        joinRoomBtn.setOnClickListener(v -> {
            // 防止重复点击
            if (isJoining) {
                MLog.d(TAG, "Already joining room, ignore click");
                return;
            }

            String roomId = mInputRoomId.getText().toString().trim();
            if (TextUtils.isEmpty(roomId)) {
                SafeToast.show(R.string.create_input_room_id_hint);
                return;
            }
            if (mRoomIdWatcher.isContentWarn()) {
                mRoomIdWatcher.showContentError();
                return;
            }
            String userName = mInputUserName.getText().toString().trim();
            if (TextUtils.isEmpty(userName)) {
                SafeToast.show(R.string.create_input_user_name_hint);
                return;
            }
            if (mUserNameWatcher.isContentWarn()) {
                mUserNameWatcher.showContentError();
                return;
            }

            // 设置标志位并禁用按钮,防止重复点击
            isJoining = true;
            joinRoomBtn.setEnabled(false);
            joinRoomBtn.setAlpha(0.5f);

            changeUserName(userName);
            joinRoom(roomId, userName, roleHost.isChecked());
            IMEUtils.closeIME(v);
        });

        mCameraSwitch = findViewById(R.id.create_room_camera);
        mCameraSwitch.setOnClickListener(v -> mUIRtcCore.openCam(!mCameraSwitch.isSelected()));

        mMicSwitch = findViewById(R.id.create_room_mic);
        mMicSwitch.setOnClickListener(v -> mUIRtcCore.openMic(!mMicSwitch.isSelected()));

        initRTC();
    }

    private void initRTC() {
        MLog.d(TAG, "init RTC");
        mUIRtcCore = UIRtcCore.ins();
        mUIRtcCore.requestPermissions();
        mUIRtcCore.getRtcDataProvider().micState().observe(this, openMic -> {
            MLog.d(TAG, "microphone opened: " + openMic);
            mMicSwitch.setSelected(openMic);
        });
        mUIRtcCore.getRtcDataProvider().camState().observe(this, openCam -> {
            MLog.d(TAG, "camera opened: " + openCam);
            mCameraSwitch.setSelected(openCam);
            if (openCam) {
                mUIRtcCore.switchCamera(true);
                mUIRtcCore.setupLocalVideoRenderer(mCameraPreviewContainer);
            } else {
                mUIRtcCore.removeVideoRenderer(mCameraPreviewContainer);
            }
        });
        mUIRtcCore.getRtcDataProvider().getRtmKickOutReason().observe(this, kickOutReason -> {
            if (IUIRtcDef.KickOutReason.LOGIN_IN_OTHER_DEVICE.equals(kickOutReason)) {
                MLog.w(TAG, "kicked out, login in other device");
                showKickedOffDialog();
            }
        });
        mUIRtcCore.loginRtm((resultCode, message) -> {
            if (resultCode != RTMBaseClient.LoginCallBack.SUCCESS) {
                SafeToast.show("Login Rtm Fail Error:" + resultCode + ",Message:" + message);
                finish();
            }
        });
    }

    private void configLocalRenderer() {
        if (mUIRtcCore == null) {
            return;
        }
        if (mUIRtcCore.getRtcDataProvider().isCamOpen()) {
            mUIRtcCore.switchCamera(true);
            mUIRtcCore.setupLocalVideoRenderer(mCameraPreviewContainer);
        } else {
            mUIRtcCore.removeVideoRenderer(mCameraPreviewContainer);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UIRtcCore.REQUEST_CODE_OF_DEVICE_PERMISSION) {
            mUIRtcCore.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            mUIRtcCore.reCheckPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SolutionDemoEventManager.unregister(this);
    }

    @Override
    public void finish() {
        super.finish();
        if (mUIRtcCore != null) {
            mUIRtcCore.logoutRtm();
        }
        UIRtcCore.release();
    }

    public void joinRoom(String roomId, String userName, boolean isHost) {
        if (!mUIRtcCore.getRtcDataProvider().isNetworkConnected()) {
            SafeToast.show(R.string.network_lost_tips);
            // 恢复按钮状态，允许用户重试
            restoreButtonState();
            return;
        }
        UIMeetingRoom roomControlService = UIRoomMgr.createMeetingRoom(this, mRtmInfo, roomId);
        if (roomControlService == null) {
            return;
        }

        roomControlService.joinRoom(userName, new IRequestCallback<MeetingTokenInfo>() {
            @Override
            public void onSuccess(MeetingTokenInfo data) {
                if (isFinishing()) {
                    return;
                }
                jumpToRoomActivity();
            }

            @Override
            public void onError(int errorCode, String message) {
                if (errorCode == IUIRtcDef.ERROR_CODE_DEFAULT) {
                    SafeToast.show(R.string.network_lost_tips);
                } else if (errorCode == IUIRtcDef.ERROR_CODE_ROOM_FULL) {
                    SafeToast.show(R.string.error_room_full);
                } else if (errorCode == IUIRtcDef.ERROR_HOST_EXIST) {
                    SafeToast.show(R.string.error_host_exist);
                } else {
                    SafeToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
                }
                // 恢复按钮状态，允许用户重试
                restoreButtonState();
            }
        });
    }

    /**
     * 恢复按钮状态，允许再次点击
     */
    protected void restoreButtonState() {
        runOnUiThread(() -> {
            isJoining = false;
            if (joinRoomBtn != null) {
                joinRoomBtn.setEnabled(true);
                joinRoomBtn.setAlpha(1.0f);
            }
        });
    }

    protected void jumpToRoomActivity() {
        Intent intent = new Intent(CreateMeetingActivity.this, MeetingRoomActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void showKickedOffDialog() {
        CommonDialog kickOutDialog = new CommonDialog(this);
        kickOutDialog.setMessage(getString(R.string.login_duplicate_login));
        kickOutDialog.setPositiveListener(v -> {
            kickOutDialog.dismiss();
            finish();
        });
        kickOutDialog.setCancelable(false);
        kickOutDialog.show();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(TokenExpiredEvent event) {
        finish();
    }

    protected void changeUserName(String newUname) {
        if (TextUtils.isEmpty(newUname)
                || TextUtils.equals(newUname, SolutionDataManager.ins().getUserName())) {
            return;
        }
        LoginApi.changeUserName(newUname, new IRequestCallback<ServerResponse<Void>>() {
            @Override
            public void onSuccess(ServerResponse<Void> response) {
                SolutionDataManager.ins().setUserName(newUname);
                RefreshUserNameEvent event = new RefreshUserNameEvent(newUname, true);
                SolutionDemoEventManager.post(event);
            }

            @Override
            public void onError(int errorCode, String message) {
                SafeToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
            }
        });
    }
}