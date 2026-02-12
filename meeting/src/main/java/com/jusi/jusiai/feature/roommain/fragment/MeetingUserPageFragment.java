package com.jusi.jusiai.feature.roommain.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.jusi.jusiai.common.MLog;
import com.jusi.jusiai.core.IUIRtcDef;
import com.jusi.jusiai.feature.roommain.AbsMeetingFragment;
import com.jusi.jusiai.feature.roommain.MeetingUserAdapter;
import com.jusi.jusiai.framework.meeting.MeetingUserObserverTransfer;
import com.jusi.jusiai.framework.meeting.bean.MeetingUserInfo;
import com.jusi.jusiai.meeting.R;
import com.jusi.jusiai.ui.page.CircleIndicator;
import com.jusi.jusiai.ui.page.PagerSnapHelper;
import com.jusi.jusiai.ui.page.RoomPageLayoutManager;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MeetingUserPageFragment extends AbsMeetingFragment implements IUIRtcDef.IRtcListener {

    private static final String TAG = "PagerUserFragment";

    public enum LayoutMode {
        MODE2(1, 2), MODE4(2, 2), MODE6(3, 2);

        private final int row, column;

        LayoutMode(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int totalLimit() {
            return row * column;
        }
    }

    private MeetingUserAdapter mUserAdapter;
    private MeetingUserObserverTransfer mTransfer;

    private RecyclerView mRecyclerView;
    private LayoutMode mLayoutMode;
    private RoomPageLayoutManager mPageLayoutManager;

    private int mPageNum;
    private CircleIndicator mPageIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meeting_pager_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MLog.d(TAG, "onViewCreated");
        mUserAdapter = new MeetingUserAdapter(getUIRoom(), getDataProvider().getUsers(), 1.0f);
        mTransfer = new MeetingUserObserverTransfer(mUserAdapter) {
            @Override
            public void onUserDataRenew(List<MeetingUserInfo> userList) {
                updateLayout();
                super.onUserDataRenew(userList);
            }

            @Override
            public void onUserInserted(MeetingUserInfo user, int position) {
                updateLayout();
                super.onUserInserted(user, position);
            }

            @Override
            public void onUserRemoved(MeetingUserInfo user, int position) {
                updateLayout();
                super.onUserRemoved(user, position);
            }
        };
        mRecyclerView = view.findViewById(R.id.users_page_conversation);
        mRecyclerView.setItemAnimator(null);
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.setAdapter(mUserAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                updateScrollIndicator();
            }
        });
        mPageIndicator = view.findViewById(R.id.users_page_indicator);
        updateLayout();
        getDataProvider().addHandler(mTransfer);

        // 添加RTC监听器，监听远程视频流可用事件
        getUIRoom().getUIRtcCore().addHandler(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MLog.d(TAG, "onDestroyView");
        getDataProvider().removeHandler(mTransfer);
        getUIRoom().getUIRtcCore().removeHandler(this);
        mLayoutMode = null;
        mPageNum = 0;
    }

    private void setLayoutManager() {
        RoomPageLayoutManager pageLayoutManager = new RoomPageLayoutManager(mLayoutMode.row, mLayoutMode.column, RoomPageLayoutManager.HORIZONTAL);
        pageLayoutManager.setAllowContinuousScroll(false);
        pageLayoutManager.setPageListener(new RoomPageLayoutManager.PageListener() {
            @Override
            public void onPageSizeChanged(int pageSize) {
            }

            @Override
            public void onPageSelect(int pageIndex) {
            }

            @Override
            public void onItemVisible(int fromItem, int toItem) {
                subscribeVideoStream(fromItem, toItem);
            }
        });

        mRecyclerView.setLayoutManager(pageLayoutManager);
        mPageLayoutManager = pageLayoutManager;
    }

    // 实现 IRtcListener 接口，监听远程视频流可用事件
    @Override
    public void onUserVideoStreamAvailable(String userId, boolean available) {
        MLog.d(TAG, "onUserVideoStreamAvailable, userId: " + userId + ", available: " + available);

        // 当远程用户视频流变为可用时，通知adapter刷新对应用户的视图
        if (available && mUserAdapter != null && isAdded()) {
            // 必须在主线程更新UI
            if (mRecyclerView != null) {
                mRecyclerView.post(() -> {
                    List<MeetingUserInfo> userList = getDataProvider().getUsers();
                    for (int i = 0; i < userList.size(); i++) {
                        MeetingUserInfo userInfo = userList.get(i);
                        if (userId.equals(userInfo.userId)) {
                            MLog.d(TAG, "notify adapter to rebind item at position: " + i);
                            mUserAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                });
            }
        }
    }

    private void updateScrollIndicator() {
        int position = mPageLayoutManager.findFirstVisibleItemPosition();
        if (mPageNum <= 1) {
            return;
        }
        int maxItemCount = mLayoutMode.totalLimit();
        int indicatorIndex = (position / maxItemCount) % mPageNum;
        if (position % maxItemCount > 0) {
            indicatorIndex = indicatorIndex == mPageNum ? 0 : indicatorIndex + 1;
        }
        mPageIndicator.onPageScrolled(indicatorIndex, 0.0f);
    }

    private void subscribeVideoStream(int fromItem, int toItem) {
        List<MeetingUserInfo> userList = getUIRoom().getDataProvider().getUsers();
        Set<String> subVideoStreamUserIds = new HashSet<>();
        for (int i = fromItem; i <= toItem && i < userList.size(); i++) {
            MeetingUserInfo userInfo = userList.get(i);
            if (userInfo.isMe) {
                continue;
            }
            subVideoStreamUserIds.add(userInfo.userId);
        }
        getUIRoom().subscribeVideoStream(subVideoStreamUserIds);
    }

    protected void setLayoutMode(LayoutMode layoutMode) {
        if (Objects.equals(mLayoutMode, layoutMode)) {
            return;
        }
        MLog.d(TAG, "setLayoutMode, " + layoutMode);
        mLayoutMode = layoutMode;
        setLayoutManager();
    }

    protected void updatePageNum(int userCount) {
        mPageNum = (int) Math.ceil((1.0d * userCount) / mLayoutMode.totalLimit());
        MLog.d(TAG, "updatePageNum, userCount: " + userCount + ", pageNum: " + mPageNum);
        mPageIndicator.setPageNum(mPageNum);
    }

    protected void updateLayout() {
        int userCount = getDataProvider().getUsers().size();
        setLayoutMode(userCount < 5 ? LayoutMode.MODE4 : LayoutMode.MODE6);
        updatePageNum(userCount);
    }
}
