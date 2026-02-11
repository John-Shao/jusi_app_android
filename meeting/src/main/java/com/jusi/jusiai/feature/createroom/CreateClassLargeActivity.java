package com.jusi.jusiai.feature.createroom;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.jusi.jusiai.core.IUIRtcDef;
import com.jusi.jusiai.core.net.ErrorTool;
import com.jusi.jusiai.core.net.IRequestCallback;
import com.jusi.jusiai.feature.roommain.ClassLargeRoomActivity;
import com.jusi.jusiai.framework.UIRoomMgr;
import com.jusi.jusiai.framework.classLarge.UIEduRoom;
import com.jusi.jusiai.framework.classLarge.bean.EduRoomTokenInfo;
import com.jusi.jusiai.meeting.R;

public class CreateClassLargeActivity extends CreateMeetingActivity {

    private static final String TAG = "CreateClassLargeActivity";

    @Override
    protected void onGlobalLayoutCompleted() {
        super.onGlobalLayoutCompleted();
        TextView titleTv = (findViewById(R.id.title_bar_title_tv));
        titleTv.setText(R.string.class_large);
        titleTv.setTextColor(ContextCompat.getColor(this,R.color.white));
        RadioGroup clientRole = findViewById(R.id.create_room_role);
        RadioButton roleHost = findViewById(R.id.create_room_role_host);
        RadioButton roleAttendee = findViewById(R.id.create_room_role_attendee);
        roleAttendee.setText(R.string.create_choose_role_student);
        clientRole.setVisibility(View.VISIBLE);
        roleHost.setVisibility(View.GONE);
        roleAttendee.setChecked(true);
    }

    @Override
    protected int getResLayoutId(){
        return R.layout.activity_create_class_small;
    }

    @Override
    public void joinRoom(String roomId, String userName, boolean isHost) {
        if (!mUIRtcCore.getRtcDataProvider().isNetworkConnected()) {
            SafeToast.show(R.string.network_lost_tips);
            // 恢复按钮状态，允许用户重试
            restoreButtonState();
            return;
        }
        UIEduRoom eduRoom = UIRoomMgr.createEduRoom(this, mRtmInfo, roomId);
        if (eduRoom == null) {
            // 恢复按钮状态，允许用户重试
            restoreButtonState();
            return;
        }
        eduRoom.joinRoom(userName, isHost, new IRequestCallback<EduRoomTokenInfo>() {
            @Override
            public void onSuccess(EduRoomTokenInfo data) {
                if (isFinishing()) {
                    return;
                }
                jumpToRoomActivity();
            }

            @Override
            public void onError(int errorCode, String message) {
                if (errorCode == IUIRtcDef.ERROR_CODE_DEFAULT) {
                    SafeToast.show(R.string.network_lost_tips);
                } else {
                    SafeToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
                }
                // 恢复按钮状态，允许用户重试
                restoreButtonState();
            }
        });
    }

    protected void jumpToRoomActivity() {
        Intent intent = new Intent(this, ClassLargeRoomActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
