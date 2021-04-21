package io.agora.marriageinterview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.ObjectsCompat;
import androidx.databinding.DataBindingUtil;

import com.agora.data.model.Member;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;

import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.LayoutRoomSpeakerBinding;
import io.agora.rtc.RtcEngine;

/**
 * 房间中说话人View
 *
 * @author chenhengfei@agora.io
 */
public class RoomSpeakerView extends ConstraintLayout {

    protected LayoutRoomSpeakerBinding mDataBinding;

    private Member mMember;
    private SurfaceView svVideo;

    public RoomSpeakerView(@NonNull Context context) {
        super(context);
        init(context, null, 0);
    }

    public RoomSpeakerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RoomSpeakerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        mDataBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.layout_room_speaker, this, true);

        mDataBinding.ivUser.setVisibility(GONE);
        mDataBinding.ivJoin.setVisibility(VISIBLE);
        mDataBinding.ivMaster.setVisibility(View.GONE);
        mDataBinding.ivAudio.setVisibility(View.GONE);
    }

    public SurfaceView getVideoSurfaceView() {
        if (svVideo == null) {
            svVideo = RtcEngine.CreateRendererView(getContext());
            addView(svVideo, 0);
        }
        return svVideo;
    }

    public void onMemberJoin(@NonNull Member member) {
        this.mMember = member;
        mDataBinding.ivJoin.setVisibility(GONE);
        svVideo.setVisibility(GONE);
        mDataBinding.ivUser.setVisibility(VISIBLE);
        setUserInfo(member);
        onMemberAudioChanged(member);
    }

    public void onMemberLeave(@NonNull Member member) {
        mMember = null;
        svVideo.setVisibility(GONE);
        mDataBinding.ivUser.setVisibility(GONE);
        mDataBinding.ivJoin.setVisibility(VISIBLE);
    }

    public void leaveMember(@NonNull Member member) {
        if (ObjectsCompat.equals(member, mMember)) {
            onMemberLeave(mMember);
        }
    }

    public void onMemberVideoChanged(@NonNull Member member) {
        if (ObjectsCompat.equals(member, mMember)) {
            if (member.getIsSDKVideoMuted() == 0) {
                mDataBinding.ivUser.setVisibility(View.GONE);
                svVideo.setVisibility(VISIBLE);
            } else {
                mDataBinding.ivUser.setVisibility(View.VISIBLE);
                svVideo.setVisibility(GONE);
            }
        }
    }

    public void onMemberAudioChanged(@NonNull Member member) {
        if (ObjectsCompat.equals(member, mMember)) {
            if (member.getIsSpeaker() == 0) {
                mDataBinding.ivAudio.setVisibility(View.GONE);
            } else {
                mDataBinding.ivAudio.setVisibility(View.VISIBLE);
                if (member.getIsMuted() == 1) {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_audio_close);
                } else if (member.getIsSelfMuted() == 1) {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_audio_close);
                } else {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_audio_open);
                }
            }
        }
    }

    public void setUserInfo(@NonNull Member member) {
        User user = member.getUserId();

        if (ObjectsCompat.equals(user, member.getRoomId().getAnchorId())) {
            mDataBinding.ivMaster.setVisibility(View.VISIBLE);
        } else {
            mDataBinding.ivMaster.setVisibility(View.GONE);
        }

        Glide.with(this)
                .load(user.getAvatarRes())
                .into(mDataBinding.ivUser);
        mDataBinding.tvName.setText(user.getName());
    }
}
