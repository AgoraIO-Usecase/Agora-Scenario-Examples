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

import com.agora.data.manager.RtcManager;
import com.agora.data.model.Member;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;

import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryLayoutRoomSpeakerBinding;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

/**
 * 房间中说话人View
 *
 * @author chenhengfei@agora.io
 */
public class RoomSpeakerView extends ConstraintLayout {

    protected MerryLayoutRoomSpeakerBinding mDataBinding;

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
        mDataBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.merry_layout_room_speaker, this, true);

        mDataBinding.ivUser.setVisibility(GONE);
        mDataBinding.ivJoin.setVisibility(VISIBLE);
        mDataBinding.ivMaster.setVisibility(View.GONE);
        mDataBinding.ivAudio.setVisibility(View.GONE);
    }

    public boolean hasMember() {
        return mMember != null;
    }

    public Member getMember() {
        return mMember;
    }

    private SurfaceView getVideoSurfaceView() {
        if (svVideo == null) {
            svVideo = RtcEngine.CreateRendererView(getContext());
            svVideo.setZOrderMediaOverlay(true);
            addView(svVideo, 0);
        }
        return svVideo;
    }

    public void onMemberJoin(boolean isLocal, @NonNull Member member) {
        this.mMember = member;
        mDataBinding.ivJoin.setVisibility(GONE);

        mDataBinding.ivAudio.setVisibility(VISIBLE);

        SurfaceView svVideo = getVideoSurfaceView();
        if (member.getIsSDKVideoMuted() == 0) {
            svVideo.setVisibility(VISIBLE);
            mDataBinding.ivUser.setVisibility(GONE);
        } else {
            svVideo.setVisibility(GONE);
            mDataBinding.ivUser.setVisibility(VISIBLE);
        }

        if (isLocal) {
            VideoCanvas mVideoCanvasOwner = new VideoCanvas(svVideo);
            mVideoCanvasOwner.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
            mVideoCanvasOwner.uid = member.getStreamIntId();
            RtcManager.Instance(getContext()).getRtcEngine().setupLocalVideo(mVideoCanvasOwner);
        } else {
            VideoCanvas mVideoCanvasOwner = new VideoCanvas(svVideo);
            mVideoCanvasOwner.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
            mVideoCanvasOwner.uid = member.getStreamIntId();
            RtcManager.Instance(getContext()).getRtcEngine().setupRemoteVideo(mVideoCanvasOwner);
        }

        setUserInfo(member);
        onMemberAudioChanged(member);
    }

    public void onMemberLeave(@NonNull Member member) {
        if (mMember == null) {
            return;
        }

        if (ObjectsCompat.equals(member, mMember) == false) {
            return;
        }

        mMember = null;
        if (svVideo != null) {
            svVideo.setVisibility(GONE);
        }
        mDataBinding.ivUser.setVisibility(GONE);
        mDataBinding.ivJoin.setVisibility(VISIBLE);
        mDataBinding.tvName.setText("");
        mDataBinding.ivAudio.setVisibility(GONE);
    }

    public void onMemberVideoChanged(@NonNull Member member) {
        if (ObjectsCompat.equals(member, mMember)) {
            if (member.getIsSDKVideoMuted() == 0) {
                mDataBinding.ivUser.setVisibility(View.GONE);
                if (svVideo != null) {
                    svVideo.setVisibility(VISIBLE);
                }
            } else {
                mDataBinding.ivUser.setVisibility(View.VISIBLE);
                if (svVideo != null) {
                    svVideo.setVisibility(GONE);
                }
            }
        }
    }

    public void onMemberAudioChanged(@NonNull Member member) {
        if (mMember == null) {
            return;
        }

        if (ObjectsCompat.equals(member, mMember)) {
            if (member.getIsMuted() == 1) {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_audio_close);
            } else if (member.getIsSelfMuted() == 1) {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_audio_close);
            } else {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_audio_open);
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
