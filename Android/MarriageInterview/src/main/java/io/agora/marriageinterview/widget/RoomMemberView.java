package io.agora.marriageinterview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.agora.data.model.Member;
import com.bumptech.glide.Glide;

import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryLayoutRoomMemberBinding;

/**
 * 房间中成员view
 *
 * @author chenhengfei@agora.io
 */
public class RoomMemberView extends ConstraintLayout {

    protected MerryLayoutRoomMemberBinding mDataBinding;

    private Member mMember;

    public RoomMemberView(@NonNull Context context) {
        super(context);
        init(context, null, 0);
    }

    public RoomMemberView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RoomMemberView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        mDataBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.merry_layout_room_member, this, true);
    }

    public void setUser(@NonNull Member mMember) {
        Glide.with(this).load(mMember.getUserId().getAvatarRes()).circleCrop().into(mDataBinding.ivHead);
    }
}
