package io.agora.scene.pklivebycdn;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.util.List;

import io.agora.example.base.BaseActivity;
import io.agora.scene.pklivebycdn.databinding.SuperappHostDetailActivityBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.OnlineUserListDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class HostDetailActivity extends BaseActivity<SuperappHostDetailActivityBinding> {
    private RoomManager.RoomInfo mRoomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private final RtcManager rtcManager = new RtcManager();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        mRoomInfo = ((RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo"));

        mBinding.hostNameView.setName(mRoomInfo.roomName + "(" + mRoomInfo.roomId + ")");
        mBinding.hostNameView.setIcon(mRoomInfo.getBgResId());

        mBinding.userView.setTotalLayoutClickListener(v -> {
            // 显示在线用户列表弹窗
            roomManager.getRoomUserList(mRoomInfo.roomId, new RoomManager.DataListCallback<RoomManager.UserInfo>() {
                @Override
                public void onSuccess(List<RoomManager.UserInfo> dataList) {
                    showOnlineUserListDialog(dataList);
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(HostDetailActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        mBinding.bottomView.setFun1Visible(false)
                .setFun2Visible(false)
                .setupInputText(false, null)
                .setupCloseBtn(true, v -> onBackPressed())
                .setupMoreBtn(true, v -> {
                    // 显示工具弹窗
                    new LiveToolsDialog(HostDetailActivity.this)
                            .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> rtcManager.switchCamera())
                            .show();
                });

        mBinding.remoteVideoControl.setOnCloseClickListener(v -> roomManager.stopLink(mRoomInfo.roomId));

        initRoomManager();
        initRtcManager();
    }

    private void showOnlineUserListDialog(List<RoomManager.UserInfo> dataList) {
        OnlineUserListDialog dialog = new OnlineUserListDialog(HostDetailActivity.this);
        dialog.setListAdapter(
                new OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo>() {
                    @Override
                    protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position, RoomManager.UserInfo item) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(),
                                BitmapFactory.decodeResource(getResources(), item.getUserIcon()));
                        drawable.setCircular(true);
                        holder.binding.ivIcon.setImageDrawable(drawable);
                        holder.binding.tvName.setText(item.userName);
                        holder.binding.tvStatus.setOnClickListener(v1 -> {
                            // 开始连麦
                            roomManager.startLinkWith(mRoomInfo.roomId, item.userId);
                            dialog.dismiss();
                        });
                    }
                }.resetAll(dataList)
        ).show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(mRoomInfo.roomId, true);
        roomManager.subscriptPKInfoEvent(mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.PKInfo>() {
            @Override
            public void onSuccess(RoomManager.PKInfo pkInfo) {
                if (!pkInfo.isPKing()) {
                    // 结束连麦
                    if (mRoomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN) {
                        rtcManager.stopRtcStreaming(mRoomInfo.roomId, () -> rtcManager.startDirectCDNStreaming(mRoomInfo.roomId));
                    }
                    // 隐藏远程视图
                    mBinding.remoteVideoControl.setVisibility(View.GONE);

                } else {
                    // 开始连麦
                    if (mRoomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN) {
                        rtcManager.stopDirectCDNStreaming(() -> {
                            rtcManager.startRtcStreaming(mRoomInfo.roomId, getString(R.string.rtc_app_token), RoomManager.getCacheUserId(), true);
                        });
                    }
                    // 显示远程视图
                    mBinding.remoteVideoControl.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(HostDetailActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initRtcManager() {
        rtcManager.init(this, getString(R.string.superapp_agora_app_id), new RtcManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {

            }

            @Override
            public void onSuccess() {

            }

            @Override
            public void onUserJoined(int uid) {
                rtcManager.renderRemoteVideo(mBinding.remoteVideoControl.getVideoContainer(), uid);
            }

            @Override
            public void onUserLeaved(int uid, int reason) {

            }
        });
        rtcManager.renderLocalVideo(mBinding.fullVideoContainer);

        if (mRoomInfo.liveMode == RoomManager.PUSH_MODE_RTC) {
            rtcManager.startRtcStreaming(mRoomInfo.roomId, getString(R.string.rtc_app_token), RoomManager.getCacheUserId(), true);
        } else {
            rtcManager.startDirectCDNStreaming(mRoomInfo.roomId);
        }
    }


    @Override
    public void onBackPressed() {
        // 判断是否正在连麦，连麦中的话弹出结束连麦弹窗
        roomManager.getRoomPKInfo(mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.PKInfo>() {
            @Override
            public void onSuccess(RoomManager.PKInfo pkInfo) {
                if (pkInfo.isPKing()) {
                    roomManager.stopLink(mRoomInfo.roomId);
                } else {
                    finish();
                }

            }

            @Override
            public void onFailed(Exception e) {
                finish();
            }
        });
    }

    @Override
    public void finish() {
        roomManager.leaveRoom(mRoomInfo.roomId, true);
        rtcManager.release();
        super.finish();
    }
}
