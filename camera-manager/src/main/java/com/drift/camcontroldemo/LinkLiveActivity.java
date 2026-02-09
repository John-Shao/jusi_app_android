package com.drift.camcontroldemo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.drift.foreamlib.local.ctrl.LocalController;
import com.drift.foreamlib.local.ctrl.LocalListener;

public class LinkLiveActivity extends AppCompatActivity {
    private static String TAG = "LinkLiveActivity";

    private RelativeLayout rlNav;
    private ImageView ivBack;
    private TextView rlConfirm;

    private String liveUrl;
    private LocalController localController;
    private String camIP;
    private int streamRes;
    private int streamBitrate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置透明状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(0x00000000);
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

        setContentView(R.layout.activity_link_live);

        Intent i = getIntent();

        camIP = i.getStringExtra("camIP");
        streamRes = i.getIntExtra("streamRes",0);
        streamBitrate = i.getIntExtra("streamBitrate",0);
        localController = new LocalController();

        rlNav = (RelativeLayout) findViewById(R.id.rl_nav);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        rlConfirm = (TextView) findViewById(R.id.rl_confirm);
        rlConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) LinkLiveActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getEtLiveUrlValue().getWindowToken( ), 0);
                liveUrl = getEtLiveUrlValue().getText().toString();
                localController.startPushStreamWithURL(camIP, liveUrl, "" + streamRes, "" + (streamBitrate/8), new LocalListener.OnCommonResListener() {
                    @Override
                    public void onCommonRes(boolean success) {
                        Log.e(TAG, "kc test: bSuccess is" + success);
                        Toast.makeText(LinkLiveActivity.this, R.string.link_send_successfully, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        InputMethodManager imm = (InputMethodManager) LinkLiveActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getEtLiveUrlValue().getWindowToken( ), 0);
        super.onPause();
    }

    private EditText getEtLiveUrlValue(){
        return (EditText) findViewById(R.id.et_live_url_value);
    }
}