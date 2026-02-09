// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.jusi.jusiai.login;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.jusi.jusiai.common.SolutionToast;
import com.jusi.jusiai.core.SolutionDataManager;
import com.jusi.jusiai.core.eventbus.RefreshUserNameEvent;
import com.jusi.jusiai.core.eventbus.SolutionDemoEventManager;
import com.jusi.jusiai.core.net.ErrorTool;
import com.jusi.jusiai.core.net.IRequestCallback;
import com.jusi.jusiai.core.net.ServerResponse;
import com.jusi.jusiai.customview.CommonTitleLayout;
import com.jusi.jusiai.customview.WarningEditText;
import com.jusi.jusiai.login.databinding.ActivityEditProfileBinding;

public class EditProfileActivity extends BaseActivity {
    public static final String INPUT_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9@_-]+$";

    public static final int USER_NAME_MAX_LENGTH = 18;
    
    private String mUserName;

    private boolean mConfirmEnable = false;
    
    private ActivityEditProfileBinding mViewBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置透明状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        mViewBinding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        CommonTitleLayout titleLayout = findViewById(R.id.title_bar_layout);
        titleLayout.setLeftBack(v -> onBackPressed());
        titleLayout.setTitle(R.string.change_user_name);

        mViewBinding.profileConfirmBtn.setOnClickListener(v -> onClickConfirm());

        mViewBinding.inputNameEt.setRegex(INPUT_REGEX, USER_NAME_MAX_LENGTH, new WarningEditText.Callback() {
            @Override
            public void checkResult(boolean valid) {
                mUserName = mViewBinding.inputNameEt.getInputText();
                mConfirmEnable = valid;
                updateConfirmButtonState();
            }
        });
        mViewBinding.inputNameEt.setHintText(R.string.please_enter_user_nickname);
        mViewBinding.inputNameEt.setWarningText(getString(R.string.content_limit, String.valueOf(USER_NAME_MAX_LENGTH)));
        mUserName = SolutionDataManager.ins().getUserName();
        mViewBinding.inputNameEt.setInputText(mUserName);

        mViewBinding.profileUserNameClear.setOnClickListener(v -> mViewBinding.inputNameEt.setInputText(""));

        updateConfirmButtonState();
    }

    private void updateConfirmButtonState() {
        if (mConfirmEnable) {
            mViewBinding.profileConfirmBtn.setEnabled(true);
            mViewBinding.profileConfirmBtn.setAlpha(1F);
        } else {
            mViewBinding.profileConfirmBtn.setEnabled(false);
            mViewBinding.profileConfirmBtn.setAlpha(0.3F);
        }
    }

    private void onClickConfirm() {
        if (!mConfirmEnable) {
            return;
        }
        String userName = mViewBinding.inputNameEt.getInputText();
        if (!TextUtils.isEmpty(userName)) {
            LoginApi.changeUserName(userName,
                    new IRequestCallback<ServerResponse<Void>>() {
                        @Override
                        public void onSuccess(ServerResponse<Void> response) {
                            SolutionDataManager.ins().setUserName(userName);
                            RefreshUserNameEvent event = new RefreshUserNameEvent(userName, true);
                            SolutionDemoEventManager.post(event);
                            finish();
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            SolutionToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
                        }
                    });
        }
    }
}
