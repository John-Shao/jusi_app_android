package com.jusi.jusiai.feature;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.utils.WindowUtils;
import com.jusi.jusiai.core.SolutionDataManager;
import com.jusi.jusiai.core.eventbus.SolutionDemoEventManager;
import com.jusi.jusiai.core.eventbus.TokenExpiredEvent;
import com.jusi.jusiai.login.ILoginImpl;
import com.jusi.jusiai.meeting.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends BaseActivity {

    private static final String TAG_SCENES = "fragment_tag_scenes";
    private static final String TAG_CAMERA = "fragment_tag_camera";
    private static final String TAG_PROFILE = "fragment_tag_profile";

    private View mTabScenes;
    private View mTabCamera;
    private View mTabProfile;
    private Fragment mFragmentScenes;
    private Fragment mFragmentCamera;
    private Fragment mFragmentProfile;

    private final ILoginImpl mLogin = new ILoginImpl();

    private final ActivityResultLauncher mLauncher = new ActivityResultLauncher<Intent>() {
        @Override
        public void launch(Intent input, @Nullable ActivityOptionsCompat options) {
            startActivity(input);
        }

        @Override
        public void unregister() {
        }

        @NonNull
        @Override
        public ActivityResultContract<Intent, ?> getContract() {
            return null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String token = SolutionDataManager.ins().getToken();
        if (TextUtils.isEmpty(token)) {
            mLogin.showLoginView(mLauncher);
        }
    }

    @Override
    protected void setupStatusBar() {
        WindowUtils.setLayoutFullScreen(getWindow());
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        mTabScenes = findViewById(R.id.tab_scenes);
        mTabScenes.setOnClickListener(v -> switchMainLayout(0));
        mTabCamera = findViewById(R.id.tab_camera);
        mTabCamera.setOnClickListener(v -> switchMainLayout(1));
        mTabProfile = findViewById(R.id.tab_profile);
        mTabProfile.setOnClickListener(v -> switchMainLayout(2));

        final FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment tabScene = fragmentManager.findFragmentByTag(TAG_SCENES);
        if (tabScene == null) {
            tabScene = new SceneEntryFragment();
            fragmentManager
                    .beginTransaction()
                    .add(R.id.tab_content, tabScene, TAG_SCENES)
                    .commit();
        }
        mFragmentScenes = tabScene;

        Fragment tabCamera = fragmentManager.findFragmentByTag(TAG_CAMERA);
        if (tabCamera == null) {
            try {
                tabCamera = (Fragment) Class.forName("com.drift.camcontroldemo.CameraHomeFragment").newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (tabCamera != null) {
                fragmentManager
                        .beginTransaction()
                        .add(R.id.tab_content, tabCamera, TAG_CAMERA)
                        .commit();
            }
        }
        mFragmentCamera = tabCamera;

        Fragment tabProfile = fragmentManager.findFragmentByTag(TAG_PROFILE);
        if (tabProfile == null) {
            tabProfile = new ProfileFragment();
            fragmentManager
                    .beginTransaction()
                    .add(R.id.tab_content, tabProfile, TAG_PROFILE)
                    .commit();
        }
        mFragmentProfile = tabProfile;

        switchMainLayout(0);
        SolutionDemoEventManager.register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SolutionDemoEventManager.unregister(this);
    }

    private void switchMainLayout(int tabIndex) {
        mTabScenes.setSelected(tabIndex == 0);
        mTabCamera.setSelected(tabIndex == 1);
        mTabProfile.setSelected(tabIndex == 2);

        FragmentManager fm = getSupportFragmentManager();
        switch (tabIndex) {
            case 0: // Scenes
                if (mFragmentCamera != null && mFragmentProfile != null) {
                    fm.beginTransaction()
                            .hide(mFragmentCamera)
                            .hide(mFragmentProfile)
                            .show(mFragmentScenes)
                            .commit();
                }
                break;
            case 1: // Camera
                if (mFragmentCamera != null && mFragmentProfile != null) {
                    fm.beginTransaction()
                            .hide(mFragmentScenes)
                            .hide(mFragmentProfile)
                            .show(mFragmentCamera)
                            .commit();
                }
                break;
            case 2: // Profile
                if (mFragmentCamera != null && mFragmentProfile != null) {
                    fm.beginTransaction()
                            .hide(mFragmentScenes)
                            .hide(mFragmentCamera)
                            .show(mFragmentProfile)
                            .commit();
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(TokenExpiredEvent event) {
        mLogin.showLoginView(mLauncher);
    }
}
