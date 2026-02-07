package com.jusi.jusiai.feature.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jusi.jusiai.bean.MeetingInfo;
import com.jusi.jusiai.meeting.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的会议列表适配器
 */
public class MeetingListAdapter extends RecyclerView.Adapter<MeetingListAdapter.MeetingViewHolder> {

    private List<MeetingInfo> meetingList = new ArrayList<>();
    private OnMeetingClickListener onMeetingClickListener;

    public interface OnMeetingClickListener {
        void onMeetingClick(MeetingInfo meetingInfo);
    }

    public void setOnMeetingClickListener(OnMeetingClickListener listener) {
        this.onMeetingClickListener = listener;
    }

    public void setMeetingList(List<MeetingInfo> meetings) {
        this.meetingList = meetings != null ? meetings : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_meeting, parent, false);
        return new MeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingViewHolder holder, int position) {
        MeetingInfo meeting = meetingList.get(position);
        holder.bind(meeting);
    }

    @Override
    public int getItemCount() {
        return meetingList.size();
    }

    class MeetingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRoomId;
        private final TextView tvUserCount;

        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomId = itemView.findViewById(R.id.tv_room_id);
            tvUserCount = itemView.findViewById(R.id.tv_user_count);
        }

        public void bind(MeetingInfo meeting) {
            tvRoomId.setText(meeting.roomId);

            // 使用字符串资源显示参会者数量
            int stringResId = meeting.userCount == 1
                ? R.string.participant_count
                : R.string.participant_count_plural;
            String userCountText = itemView.getContext().getString(stringResId, meeting.userCount);
            tvUserCount.setText(userCountText);

            itemView.setOnClickListener(v -> {
                if (onMeetingClickListener != null) {
                    onMeetingClickListener.onMeetingClick(meeting);
                }
            });
        }
    }
}
