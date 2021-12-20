package io.agora.scene.livepk.activity;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import io.agora.example.base.BaseActivity;
import io.agora.scene.livepk.databinding.PkActivityVideoBinding;
import io.agora.scene.livepk.model.RoomInfo;

public class HostPKActivity extends BaseActivity<PkActivityVideoBinding> {
    private static final String TAG = "HostPKActivity";

    private static final String EXTRA_ROOM_INFO = "roomInfo";

    @NonNull
    public static Intent launch(@NonNull Context context, @NonNull RoomInfo roomInfo) {
        Intent intent = new Intent(context, HostPKActivity.class);
        intent.putExtra(EXTRA_ROOM_INFO, roomInfo);
        return intent;
    }
//
//    private final RtmManager rtmManager = new RtmManager();
//    private final RtcManager rtcManager = new RtcManager();
//    private RoomInfo mRoomInfo;
//
//    private final Handler mHandler = new Handler(Looper.getMainLooper());
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mRoomInfo = ((RoomInfo)getIntent().getSerializableExtra(EXTRA_ROOM_INFO));
//        setTitle(mRoomInfo.getRoomName());
//
//        int userProfileIcon = UserUtil.getUserProfileIcon(getLocalRoomId());
//
//        mBinding.ivLoadingBg.setVisibility(View.VISIBLE);
//        mBinding.ivLoadingBg.setImageResource(userProfileIcon);
//
//        Glide.with(this)
//                .load(userProfileIcon)
//                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
//                .into(mBinding.ivRoomAvatar);
//
//        mBinding.tvRoomName.setText(String.format(Locale.US, "%s(%s)", mRoomInfo.getRoomName(), mRoomInfo.getRoomId()) );
//        mBinding.switchCameraButton.setVisibility(View.VISIBLE);
//        mBinding.switchCameraButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                rtcManager.switchCamera();
//            }
//        });
//
//        mBinding.ivClose.setOnClickListener(v -> onBackPressed());
//        mBinding.startPkButton.setOnClickListener(v -> loadRoomInfoList());
//
//        initManager();
//        joinLocalChannel();
//    }
//
//    @Override
//    public void onBackPressed() {
//        //super.onBackPressed();
//        checkIsPKing(getLocalRoomId(),
//                () -> deleteRoomInfo(this::finish),
//                data -> {
//                    new AlertDialog.Builder(HostPKActivity.this)
//                            .setTitle("PK")
//                            .setMessage("Currently in PK, whether to stop PK?")
//                            .setPositiveButton(R.string.pk_cmm_ok, (dialog, which) -> {
//                                dialog.dismiss();
//                                stopPK();
//                            })
//                            .setNegativeButton(R.string.pk_cmm_cancel, (dialog, which) -> dialog.dismiss())
//                            .show();
//                });
//    }
//
//    private void setupLocalFullVideo(){
//
//        mBinding.flLocalFullContainer.setVisibility(View.VISIBLE);
//        mBinding.llPkLayout.setVisibility(View.GONE);
//
//        View renderView = null;
//        if(mBinding.flLocalContainer.getChildCount() > 0){
//            renderView = mBinding.flLocalContainer.getChildAt(0);
//        }
//
//        // remove old video view
//        mBinding.flLocalFullContainer.removeAllViews();
//        mBinding.flLocalContainer.removeAllViews();
//        if(renderView == null){
//            mBinding.ivLoadingBg.setVisibility(View.VISIBLE);
//            rtcManager.renderLocalVideo(mBinding.flLocalFullContainer, () -> mHandler.post(() -> mBinding.ivLoadingBg.setVisibility(View.GONE)));
//        }else{
//            mBinding.flLocalFullContainer.addView(renderView);
//        }
//
//    }
//
//    private void setupLocalVideo() {
//
//        mBinding.ivLoadingBg.setVisibility(View.GONE);
//        mBinding.flLocalFullContainer.setVisibility(View.GONE);
//        mBinding.llPkLayout.setVisibility(View.VISIBLE);
//
//        mBinding.ivLocalCover.setImageResource(UserUtil.getUserProfileIcon(getLocalRoomId()));
//
//        View renderView = null;
//        if(mBinding.flLocalFullContainer.getChildCount() > 0){
//            renderView = mBinding.flLocalFullContainer.getChildAt(0);
//        }
//
//        mBinding.flLocalFullContainer.removeAllViews();
//        mBinding.flLocalContainer.removeAllViews();
//
//        if(renderView == null){
//            mBinding.ivLocalCover.setVisibility(View.VISIBLE);
//            rtcManager.renderLocalVideo(mBinding.flLocalContainer, () -> mHandler.post(() -> mBinding.ivLocalCover.setVisibility(View.GONE)));
//        }else{
//            mBinding.flLocalContainer.addView(renderView);
//        }
//
//    }
//
//    private void loadRoomInfoList() {
//        SyncManager.Instance().getScene(LivePKListActivity.SYNC_SCENE_ID).collection(LivePKListActivity.SYNC_COLLECTION_ROOM_INFO).get(new SyncManager.DataListCallback() {
//            @Override
//            public void onSuccess(List<IObject> result) {
//                List<RoomInfo> list = new ArrayList<>();
//                for (IObject item : result) {
//                    RoomInfo info = item.toObject(RoomInfo.class);
//                    if(!info.roomId.equals(getLocalRoomId())){
//                        list.add(info);
//                    }
//                }
//                Collections.sort(list, (o1, o2) -> (int) (o2.createTime - o1.createTime));
//
//                if(isDestroyed()){
//                    return;
//                }
//
//                mHandler.post(() -> {
//                    showRoomListDialog(list);
//                });
//            }
//
//            @Override
//            public void onFail(SyncManagerException exception) {
//
//            }
//        });
//    }
//
//    private void deleteRoomInfo(Runnable success){
//        SyncManager.Instance()
//                .getScene(LivePKListActivity.SYNC_SCENE_ID)
//                .collection(LivePKListActivity.SYNC_COLLECTION_ROOM_INFO)
//                .document(getLocalRoomId())
//                .delete(new SyncManager.Callback() {
//                    @Override
//                    public void onSuccess() {
//                        if(success != null){
//                            success.run();
//                        }
//                    }
//
//                    @Override
//                    public void onFail(SyncManagerException exception) {
//                        Toast.makeText(HostPKActivity.this, "deleteRoomInfo failed exception: " + exception.toString(), Toast.LENGTH_LONG).show();
//                    }
//                });
//    }
//
//    private void showRoomListDialog(List<RoomInfo> list) {
//        final BottomSheetDialog dialog = new BottomSheetDialog(HostPKActivity.this);
//        RecyclerView view = new RecyclerView(HostPKActivity.this);
//        view.setLayoutManager(new LinearLayoutManager(HostPKActivity.this, LinearLayoutManager.VERTICAL, false));
//        class ItemViewHolder extends RecyclerView.ViewHolder{
//            ImageView roomAvatarIv;
//            TextView roomNameTv;
//            TextView pkTv;
//
//            public ItemViewHolder(@NonNull View itemView) {
//                super(itemView);
//                roomAvatarIv = itemView.findViewById(R.id.iv_room_avatar);
//                roomNameTv = itemView.findViewById(R.id.tv_room_name);
//                pkTv = itemView.findViewById(R.id.tv_pk);
//            }
//        }
//        view.setAdapter(new RecyclerView.Adapter<ItemViewHolder>() {
//            @NonNull
//            @Override
//            public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//                return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.live_room_list_dialog_item, parent, false));
//            }
//
//            @Override
//            public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
//                RoomInfo item = list.get(position);
//                Glide.with(holder.roomAvatarIv)
//                        .load(UserUtil.getUserProfileIcon(item.roomId))
//                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
//                        .into(holder.roomAvatarIv);
//
//                holder.roomNameTv.setText(item.roomName);
//                holder.pkTv.setOnClickListener(v -> {
//                    dialog.dismiss();
//                    startPK(item.roomId);
//                });
//            }
//
//            @Override
//            public int getItemCount() {
//                return list.size();
//            }
//        });
//        dialog.setTitle(R.string.pk_room_list_title);
//        dialog.setContentView(view);
//        dialog.show();
//    }
//
//    private void setupPkVideo(String channelId, int uid) {
//        rtcManager.renderRemoteVideo(mBinding.flRemoteContainer, channelId, uid, () -> mHandler.post(() -> {
//            ImageView ivCover = mBinding.ivRemoteCover;
//            ivCover.setVisibility(View.GONE);
//        }));
//    }
//
//    private void initManager() {
//        rtcManager.init(this, getAgoraAppId(), new RtcManager.OnInitializeListener() {
//            @Override
//            public void onError(int code, String message) {
//                if(!TextUtils.isEmpty(message)){
//                    mHandler.post(() -> {
//                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
//                        finish();
//                    });
//                }
//            }
//
//            @Override
//            public void onSuccess() {
//
//            }
//        });
//        rtmManager.init(this, getAgoraAppId(), new RtmManager.OnInitializeListener() {
//            @Override
//            public void onError(int code, String message) {
//                if(!TextUtils.isEmpty(message)){
//                    mHandler.post(() -> {
//                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
//                    });
//                }
//            }
//
//            @Override
//            public void onSuccess() {
//
//            }
//        });
//    }
//
//    private void joinLocalRtm(String uid){
//        rtmManager.login(uid, new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                String channelId = getLocalRoomId();
//                rtmManager.joinChannel(channelId, new RtmManager.OnChannelListener() {
//                    @Override
//                    public void onError(int code, String message) {
//                        if(!TextUtils.isEmpty(message)){
//                            mHandler.post(() -> {
//                                Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
//                            });
//                        }
//                    }
//
//                    @Override
//                    public void onJoinSuccess() {
//                        checkIsPKing(getLocalRoomId(), HostPKActivity.this::setupLocalFullVideo, data -> {
//                            setupLocalVideo();
//                            joinPKChannel(data);
//                        });
//                    }
//
//                    @Override
//                    public void onChannelAttributesUpdated(List<RtmChannelAttribute> list) {
//                        Log.d(TAG, "Local RTM ChannelAttributesUpdated : " + list);
//
//                        String pkName = getPkNameFromChannelAttr(list);
//                        if (!TextUtils.isEmpty(pkName)) {
//                            // 正在PK
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    setupLocalVideo();
//                                }
//                            });
//                            joinPKChannel(pkName);
//                        } else {
//                            // 不在PK，只渲染自己的全屏画面
//                            leavePKChannel();
//                            mHandler.post(HostPKActivity.this::setupLocalFullVideo);
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                if(errorInfo != null && !TextUtils.isEmpty(errorInfo.getErrorDescription())){
//                    mHandler.post(() -> {
//                        Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
//                    });
//                }
//            }
//        });
//    }
//
//    private void joinPKRtm(String channelId){
//        RtmChannelAttribute pkInfo = new RtmChannelAttribute();
//        pkInfo.setKey("PK");
//        pkInfo.setValue(channelId);
//        rtmManager.updateChannelAttributes(getLocalRoomId(), Collections.singletonList(pkInfo), null);
//
//        RtmChannelAttribute pkInfo2 = new RtmChannelAttribute();
//        pkInfo2.setKey("PK");
//        pkInfo2.setValue(getLocalRoomId());
//        rtmManager.updateChannelAttributes(channelId, Collections.singletonList(pkInfo2), null);
//    }
//
//
//
//    private void joinLocalChannel() {
//        String channelId = getLocalRoomId();
//        rtcManager.joinChannel(channelId, true, new RtcManager.OnChannelListener() {
//            @Override
//            public void onError(int code, String message) {
//                if(!TextUtils.isEmpty(message)){
//                    mHandler.post(() -> {
//                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
//                        finish();
//                    });
//                }
//            }
//
//            @Override
//            public void onJoinSuccess(int uid) {
//                joinLocalRtm(uid + "");
//            }
//
//            @Override
//            public void onUserJoined(String channelId, int uid) {
//
//            }
//        });
//    }
//
//    private void checkIsPKing(String channelId, Runnable idle, DataCallback<String> pking){
//        rtmManager.getChannelAttributes(channelId, new ResultCallback<List<RtmChannelAttribute>>() {
//            @Override
//            public void onSuccess(List<RtmChannelAttribute> attributes) {
//                Log.d(TAG, "checkIsPKing attributes : " + attributes);
//                String pkName = getPkNameFromChannelAttr(attributes);
//                if (!TextUtils.isEmpty(pkName)) {
//                    if(pking != null){
//                        mHandler.post(() -> pking.onSuccess(pkName));
//                    }
//                } else {
//                    if (idle != null) {
//                        mHandler.post(idle);
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                if (errorInfo != null && TextUtils.isEmpty(errorInfo.getErrorDescription())) {
//                    mHandler.post(()->{
//                        Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
//                    });
//                }
//            }
//        });
//    }
//
//    private String getPkNameFromChannelAttr(List<RtmChannelAttribute> attributes){
//        if (attributes.size() > 0) {
//            RtmChannelAttribute pkAttribute = attributes.get(0);
//            if(pkAttribute.getKey().equals("PK") && !TextUtils.isEmpty(pkAttribute.getValue())){
//                return pkAttribute.getValue();
//
//            }
//        }
//        return "";
//    }
//
//    private void startPK(String channelId){
//        checkIsPKing(channelId, () -> joinPKRtm(channelId), data -> Toast.makeText(HostPKActivity.this, "The host "+ channelId + " is kping with " + data, Toast.LENGTH_LONG).show());
//    }
//
//    private void stopPK() {
//        String localChannelId = getLocalRoomId();
//        rtmManager.getChannelAttributes(localChannelId, new ResultCallback<List<RtmChannelAttribute>>() {
//            @Override
//            public void onSuccess(List<RtmChannelAttribute> attributes) {
//                String pkName = getPkNameFromChannelAttr(attributes);
//                if(!TextUtils.isEmpty(pkName)){
//                    rtmManager.deleteChannelAttribute(getLocalRoomId(), Collections.singletonList("PK"), null);
//
//                    rtmManager.getChannelAttributes(pkName, new ResultCallback<List<RtmChannelAttribute>>() {
//                        @Override
//                        public void onSuccess(List<RtmChannelAttribute> attributes) {
//                            String _pkName = getPkNameFromChannelAttr(attributes);
//                            if(localChannelId.equals(_pkName)){
//                                rtmManager.deleteChannelAttribute(pkName, Collections.singletonList("PK"), null);
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(ErrorInfo errorInfo) {
//                            if (errorInfo != null && TextUtils.isEmpty(errorInfo.getErrorDescription())) {
//                                mHandler.post(()->{
//                                    Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
//                                });
//                            }
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                if (errorInfo != null && TextUtils.isEmpty(errorInfo.getErrorDescription())) {
//                    mHandler.post(()->{
//                        Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
//                    });
//                }
//            }
//        });
//    }
//
//    private void joinPKChannel(String channelId) {
//        mHandler.post(() -> {
//            mBinding.ivRemoteCover.setVisibility(View.VISIBLE);
//            mBinding.ivRemoteCover.setImageResource(UserUtil.getUserProfileIcon(channelId));
//        });
//        rtcManager.joinChannel(channelId, false, new RtcManager.OnChannelListener() {
//            @Override
//            public void onError(int code, String message) {
//                if(!TextUtils.isEmpty(message)){
//                    mHandler.post(() -> {
//                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
//                    });
//                }
//            }
//
//            @Override
//            public void onJoinSuccess(int uid) {
//
//            }
//
//            @Override
//            public void onUserJoined(String channelId, int uid) {
//                mHandler.post(() -> setupPkVideo(channelId, uid));
//            }
//        });
//    }
//
//    private void leavePKChannel(){
//        rtcManager.leaveChannelExcept(getLocalRoomId());
//        mHandler.post(() -> mBinding.flRemoteContainer.removeAllViews());
//    }
//
//    private String getLocalRoomId(){
//        return mRoomInfo.roomId;
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
//        rtcManager.release();
//        rtmManager.release();
//        mHandler.removeCallbacksAndMessages(null);
//    }
}
