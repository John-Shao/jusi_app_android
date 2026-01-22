package com.volcengine.vertcdemo;

import android.app.Application;

import com.ss.video.rtc.demo.basic_module.utils.SPUtils;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.core.startup.StartupManager;
import com.drift.manager.CameraManager;
import com.drift.util.SPUtil;
import com.tencent.bugly.crashreport.CrashReport;

import cn.finalteam.okhttpfinal.OkHttpFinal;
import cn.finalteam.okhttpfinal.OkHttpFinalConfiguration;

public class VeRTCApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SPUtils.initSP(this, "meeting_sp");
        Utilities.initApp(this);
        StartupManager.invoke(this);

        // Initialize camera manager components
        initializeCameraManager();
    }

    private void initializeCameraManager() {
        // Initialize camera manager
        SPUtil.setswCodec(this, true);
        CameraManager.getInstance().init(this);

        // Initialize OkHttp
        OkHttpFinalConfiguration.Builder builder = new OkHttpFinalConfiguration.Builder();
        OkHttpFinal.getInstance().init(builder.build());

        // Initialize Bugly crash report
        CrashReport.initCrashReport(getApplicationContext(), "f678368361", false);
    }
}
