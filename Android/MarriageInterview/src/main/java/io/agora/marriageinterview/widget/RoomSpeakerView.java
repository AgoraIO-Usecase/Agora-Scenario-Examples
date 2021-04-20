package io.agora.marriageinterview.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.databinding.DataBindingUtil;

import com.agora.data.model.Member;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;

import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.LayoutRoomSpeakerBinding;

/**
 * 房间中说话人View
 *
 * @author chenhengfei@agora.io
 */
public class RoomSpeakerView extends ConstraintLayout {

    protected LayoutRoomSpeakerBinding mDataBinding;

    private Member mMember;

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
    }

    public void setUserInfo(@NonNull Member member) {
        User user = member.getUserId();

        if (ObjectsCompat.equals(user, member.getRoomId().getAnchorId())) {
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.mipmap.icon_master);
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                mDataBinding.tvName.setCompoundDrawables(drawable, null, null, null);
            }
        } else {
            mDataBinding.tvName.setCompoundDrawables(null, null, null, null);
        }

        if (this.mMember == null
                || this.mMember.getUserId() == null || !TextUtils.equals(this.mMember.getUserId().getAvatar(), user.getAvatar())) {
            Glide.with(this)
                    .load(user.getAvatarRes())
                    .placeholder(R.mipmap.default_head)
                    .error(R.mipmap.default_head)
                    .circleCrop()
                    .into(mDataBinding.ivUser);
        }

        if (this.mMember == null
                || this.mMember.getUserId() == null || !TextUtils.equals(this.mMember.getUserId().getName(), user.getName())) {
            mDataBinding.tvName.setText(user.getName());
        }

        if (member.getIsSpeaker() == 0) {
            mDataBinding.ivVoice.setVisibility(View.GONE);
        } else {
            mDataBinding.ivVoice.setVisibility(View.VISIBLE);
            if (member.getIsMuted() == 1) {
                mDataBinding.ivVoice.setImageResource(R.mipmap.icon_mocrophonered);
            } else if (member.getIsSelfMuted() == 1) {
                mDataBinding.ivVoice.setImageResource(R.mipmap.icon_mocrophonered);
            } else {
                mDataBinding.ivVoice.setImageResource(R.mipmap.icon_mocrophoneblue);
            }
        }

        this.mMember = member;
    }
}
