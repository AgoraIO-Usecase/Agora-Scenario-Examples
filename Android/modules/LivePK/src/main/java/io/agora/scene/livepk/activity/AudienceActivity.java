package io.agora.scene.livepk.activity;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import io.agora.example.base.BaseActivity;
import io.agora.scene.livepk.databinding.PkActivityVideoBinding;
import io.agora.scene.livepk.model.RoomInfo;

public class AudienceActivity extends BaseActivity<PkActivityVideoBinding> {
    private static final String TAG = "AudienceActivity";

    private static final String EXTRA_ROOM_INFO = "roomInfo";

//    private AgoraMediaPlayerKit agoraMediaPlayerKitA, agoraMediaPlayerKitB;
//    private final RtmManager rtmManager = new RtmManager();
//    private RoomInfo mRoomInfo;

@NonNull
public static Intent launch(@NonNull Context context, @NonNull RoomInfo roomInfo) {
        Intent intent = new Intent(context, AudienceActivity.class);
        intent.putExtra(EXTRA_ROOM_INFO, roomInfo);
        return intent;
    }
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        mRoomInfo = (RoomInfo)getIntent().getSerializableExtra(EXTRA_ROOM_INFO);
//
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setTitle(mRoomInfo.getRoomName());
//
//        int userProfileIcon = UserUtil.getUserProfileIcon(mRoomInfo.roomId);
//
//        mBinding.ivLoadingBg.setVisibility(View.VISIBLE);
//        mBinding.ivLoadingBg.setImageResource(userProfileIcon);
//
//        Glide.with(this)
//                .load(userProfileIcon)
//                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
//                .into(mBinding.ivRoomAvatar);
//
//        mBinding.tvRoomName.setText(String.format(Locale.US, "%s(%s)", mRoomInfo.roomName, mRoomInfo.roomId));
//
//        mBinding.startPkButton.setVisibility(View.GONE);
//
//        mBinding.ivClose.setOnClickListener(v -> onBackPressed());
//
//        initRtmManager(mRoomInfo.getRoomId());
//    }
//
//    private void initRtmManager(String roomId) {
//        rtmManager.init(this, getAgoraAppId(), new RtmManager.OnInitializeListener() {
//            @Override
//            public void onError(int code, String message) {
//
//            }
//
//            @Override
//            public void onSuccess() {
//                rtmManager.joinChannel(roomId, new RtmManager.OnChannelListener() {
//                    @Override
//                    public void onError(int code, String message) {
//
//                    }
//
//                    @Override
//                    public void onJoinSuccess() {
//                        rtmManager.getChannelAttributes(roomId, new ResultCallback<List<RtmChannelAttribute>>() {
//                            @Override
//                            public void onSuccess(List<RtmChannelAttribute> attributes) {
//                                String pkRoomId = getPkNameFromChannelAttr(attributes);
//                                runOnUiThread(() -> {
//                                    if (!TextUtils.isEmpty(pkRoomId)) {
//                                        setupLocalVideo(roomId);
//                                        setupRemoteVideo(pkRoomId);
//                                    } else {
//                                        setupLocalFullVideo(roomId);
//                                    }
//                                });
//                            }
//
//                            @Override
//                            public void onFailure(ErrorInfo errorInfo) {
//
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onChannelAttributesUpdated(List<RtmChannelAttribute> list) {
//                        String pkName = getPkNameFromChannelAttr(list);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (!TextUtils.isEmpty(pkName)) {
//                                    setupLocalVideo(roomId);
//                                    setupRemoteVideo(pkName);
//                                } else {
//                                    removeRemoteVideo();
//                                    setupLocalFullVideo(roomId);
//                                }
//                            }
//                        });
//
//                    }
//                });
//            }
//        });
//        rtmManager.login(UUID.randomUUID().toString(), null);
//    }
//
//    private void setupLocalFullVideo(String roomName){
//
//        mBinding.flLocalFullContainer.setVisibility(View.VISIBLE);
//        mBinding.llPkLayout.setVisibility(View.GONE);
//
//        SurfaceView surfaceView = null;
//        if(mBinding.flLocalContainer.getChildCount() > 0){
//            surfaceView = (SurfaceView) mBinding.flLocalContainer.getChildAt(0);
//        }
//
//        // remove old video view
//        mBinding.flLocalFullContainer.removeAllViews();
//        mBinding.flLocalContainer.removeAllViews();
//
//        if(agoraMediaPlayerKitA == null){
//            agoraMediaPlayerKitA = playUrl(mBinding.flLocalFullContainer, getVideoPullUrl(roomName), () -> {
//                mBinding.ivLoadingBg.setVisibility(View.GONE);
//            });
//        }else if(surfaceView != null){
//            mBinding.flLocalFullContainer.addView(surfaceView);
//        }else{
//            throw new RuntimeException("setupLocalFullVideo failed");
//        }
//    }
//
//    private void setupLocalVideo(String roomId) {
//
//        mBinding.flLocalFullContainer.setVisibility(View.GONE);
//        mBinding.llPkLayout.setVisibility(View.VISIBLE);
//
//        SurfaceView surfaceView = null;
//        if(mBinding.flLocalFullContainer.getChildCount() > 0){
//            surfaceView = (SurfaceView) mBinding.flLocalFullContainer.getChildAt(0);
//        }
//
//        // remove old video view
//        mBinding.flLocalFullContainer.removeAllViews();
//        mBinding.flLocalContainer.removeAllViews();
//
//        if(agoraMediaPlayerKitA == null){
//            mBinding.ivLoadingBg.setVisibility(View.VISIBLE);
//            agoraMediaPlayerKitA = playUrl(mBinding.flLocalContainer, getVideoPullUrl(roomId), () -> {
//                mBinding.ivLoadingBg.setVisibility(View.GONE);
//            });
//        }else if(surfaceView != null){
//            mBinding.flLocalContainer.addView(surfaceView);
//        }else{
//            throw new RuntimeException("setupLocalFullVideo failed");
//        }
//    }
//
//    private void setupRemoteVideo(String pkRoomId) {
//        removeRemoteVideo();
//        mBinding.ivRemoteCover.setVisibility(View.VISIBLE);
//        mBinding.ivRemoteCover.setImageResource(UserUtil.getUserProfileIcon(pkRoomId));
//        agoraMediaPlayerKitB = playUrl(mBinding.flRemoteContainer, getVideoPullUrl(pkRoomId), () -> {
//            mBinding.ivRemoteCover.setVisibility(View.GONE);
//        });
//    }
//
//    private void removeRemoteVideo(){
//        if(agoraMediaPlayerKitB != null){
//            agoraMediaPlayerKitB.destroy();
//            agoraMediaPlayerKitB = null;
//            mBinding.flRemoteContainer.removeAllViews();
//        }
//    }
//
//
//    private AgoraMediaPlayerKit playUrl(FrameLayout container, String url, Runnable complete) {
//        AgoraMediaPlayerKit mediaPlayer = new AgoraMediaPlayerKit(this);
//        SurfaceView videoView = new SurfaceView(this);
//        container.addView(videoView);
//        mediaPlayer.setView(videoView);
//        mediaPlayer.setRenderMode(PLAYER_RENDER_MODE_HIDDEN);
//        mediaPlayer.open(url, 0);
//
//        mediaPlayer.registerPlayerObserver(new MediaPlayerObserver() {
//            @Override
//            public void onPlayerStateChanged(Constants.MediaPlayerState state, Constants.MediaPlayerError error) {
//                BaseUtil.logD("agoraMediaPlayerKit1 onPlayerStateChanged:" + state + " " + error);
//                if (state == Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
//                    mediaPlayer.play();
//                    if(complete != null){
//                        complete.run();
//                    }
//                }
//            }
//
//            @Override
//            public void onPositionChanged(long l) {
//
//            }
//
//            @Override
//            public void onPlayerEvent(Constants.MediaPlayerEvent mediaPlayerEvent) {
//
//            }
//
//            @Override
//            public void onMetaData(Constants.MediaPlayerMetadataType mediaPlayerMetadataType, byte[] bytes) {
//
//            }
//
//            @Override
//            public void onPlayBufferUpdated(long l) {
//
//            }
//
//            @Override
//            public void onPreloadEvent(String s, Constants.MediaPlayerPreloadEvent mediaPlayerPreloadEvent) {
//
//            }
//
//        });
//        mediaPlayer.registerVideoFrameObserver(new VideoFrameObserver() {
//            private volatile boolean firstFrame = false;
//            @Override
//            public void onFrame(VideoFrame videoFrame) {
//                if(!firstFrame){
//                    firstFrame = true;
//
//                    mediaPlayer.unregisterVideoFrameObserver(this);
//                }
//            }
//        });
//
//        return mediaPlayer;
//    }
//
//    private String getPkNameFromChannelAttr(List<RtmChannelAttribute> attributes) {
//        if (attributes.size() > 0) {
//            RtmChannelAttribute pkAttribute = attributes.get(0);
//            if (pkAttribute.getKey().equals("PK") && !TextUtils.isEmpty(pkAttribute.getValue())) {
//                return pkAttribute.getValue();
//
//            }
//        }
//        return "";
//    }
//
//    private String getAgoraAppId() {
//        String appId = getString(R.string.pk_agora_app_id);
//        if (TextUtils.isEmpty(appId)) {
//            throw new RuntimeException("the app id is empty");
//        }
//        return appId;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (agoraMediaPlayerKitA != null) {
//            agoraMediaPlayerKitA.destroy();
//        }
//        if (agoraMediaPlayerKitB != null) {
//            agoraMediaPlayerKitB.destroy();
//        }
//        rtmManager.release();
//    }
//
//    private String getVideoPullUrl(String roomName) {
//        //return String.format(Locale.US, "rtmp://mdetest.pull.agoramde.agoraio.cn/live/%s", roomName);
//        //return String.format(Locale.US, "http://webdemo-pull.agora.io/lbhd/%s.flv", roomName);
//        return String.format(Locale.US, "rtmp://pull.webdemo.agoraio.cn/lbhd/%s", roomName);
//    }
}
