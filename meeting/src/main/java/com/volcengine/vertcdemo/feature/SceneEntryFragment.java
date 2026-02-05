package com.volcengine.vertcdemo.feature;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.volcengine.vertcdemo.api.MeetingApi;
import com.volcengine.vertcdemo.bean.GetMyMeetingsResponse;
import com.volcengine.vertcdemo.bean.MeetingInfo;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.ServerResponse;
import com.volcengine.vertcdemo.core.net.http.NetworkException;
import com.volcengine.vertcdemo.core.net.rtm.RtmInfo;
import com.volcengine.vertcdemo.feature.adapter.MeetingListAdapter;
import com.volcengine.vertcdemo.feature.createroom.CreateClassLargeActivity;
import com.volcengine.vertcdemo.feature.createroom.CreateClassSmallActivity;
import com.volcengine.vertcdemo.feature.createroom.CreateMeetingActivity;
import com.volcengine.vertcdemo.framework.RoomType;
import com.volcengine.vertcdemo.login.ILoginImpl;
import com.volcengine.vertcdemo.login.LoginApi;
import com.volcengine.vertcdemo.meeting.R;

public class SceneEntryFragment extends Fragment {

    private static final String TAG = "SceneEntryFragment";
    private RecyclerView rvMyMeetings;
    private MeetingListAdapter meetingListAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scene_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.scene_meeting).setOnClickListener(v -> startScene(CreateMeetingActivity.class, RoomType.MEETING));
        // 隐藏小班课场景入口
        // view.findViewById(R.id.scene_class_small).setOnClickListener(v -> startScene(CreateClassSmallActivity.class, RoomType.CLASS_SMALL));
        // view.findViewById(R.id.scene_class_large).setOnClickListener(v -> startScene(CreateClassLargeActivity.class, RoomType.CLASS_LARGE));

        // 初始化我的会议列表
        initMyMeetingsList(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次进入/回到页面时刷新会议列表
        loadMyMeetings();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Fragment 从隐藏变为可见时刷新会议列表
            Log.d(TAG, "Fragment visible, refreshing meetings list");
            loadMyMeetings();
        }
    }

    private void initMyMeetingsList(View view) {
        rvMyMeetings = view.findViewById(R.id.rv_my_meetings);
        rvMyMeetings.setLayoutManager(new LinearLayoutManager(getContext()));
        meetingListAdapter = new MeetingListAdapter();
        meetingListAdapter.setOnMeetingClickListener(this::onMeetingClick);
        rvMyMeetings.setAdapter(meetingListAdapter);
    }

    private void loadMyMeetings() {
        String userId = SolutionDataManager.ins().getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "User ID is empty, skip loading meetings");
            return;
        }

        MeetingApi.getMyMeetings(userId, new IRequestCallback<GetMyMeetingsResponse>() {
            @Override
            public void onSuccess(GetMyMeetingsResponse response) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (response != null && response.code == 200) {
                    Log.d(TAG, "Load meetings success, total: " + response.total);
                    meetingListAdapter.setMeetingList(response.meetings);
                } else {
                    Log.e(TAG, "Load meetings failed: " + (response != null ? response.message : "null response"));
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                Log.e(TAG, "Load meetings error: " + errorCode + ", " + message);
            }
        });
    }

    private void onMeetingClick(MeetingInfo meetingInfo) {
        // 点击会议项，进入会议
        Log.d(TAG, "Meeting clicked: " + meetingInfo.roomId);
        // 这里可以实现直接加入会议的逻辑
        SafeToast.show(getString(R.string.join_meeting_toast, meetingInfo.roomId));
    }

    private void startScene(Class<? extends Activity> targetActivity, RoomType roomType) {
        LoginApi.getRTMAuthentication(SolutionDataManager.ins().getToken(), roomType.getSense(),
                new IRequestCallback<ServerResponse<RtmInfo>>() {
                    @Override
                    public void onSuccess(ServerResponse<RtmInfo> response) {
                        Activity activity = getActivity();
                        if (activity == null || activity.isFinishing()) {
                            return;
                        }
                        RtmInfo data = response == null ? null : response.getData();
                        if (data == null || !data.isValid()) {
                            onError(-1, "");
                        } else {
                            Intent intent = new Intent(activity, targetActivity);
                            intent.putExtra(RtmInfo.KEY_RTM, data);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        Activity activity = getActivity();
                        if (activity == null || activity.isFinishing()) {
                            return;
                        }
                        if (errorCode == NetworkException.CODE_TOKEN_EXPIRED) {
                            SolutionDataManager.ins().clear();
                            requestLogin();
                        } else {
                            if (errorCode == NetworkException.CODE_ERROR) {
                                SafeToast.show(R.string.network_lost_tips);
                            } else {
                                SafeToast.show(R.string.request_rtm_fail);
                            }
                        }
                    }
                });
    }

    private void requestLogin() {
        new ILoginImpl().showLoginView(new ActivityResultLauncher<Intent>() {
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
        });
    }
}
