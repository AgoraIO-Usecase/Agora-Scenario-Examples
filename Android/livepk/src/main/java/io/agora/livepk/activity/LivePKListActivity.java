package io.agora.livepk.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.livepk.R;
import io.agora.livepk.adapter.RoomListAdapter;
import io.agora.livepk.databinding.ActivityListBinding;
import io.agora.livepk.model.RoomInfo;
import io.agora.livepk.util.DataListCallback;
import io.agora.livepk.widget.CreateRoomDialog;
import io.agora.livepk.widget.SpaceItemDecoration;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Scene;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;
import pub.devrel.easypermissions.EasyPermissions;

public class LivePKListActivity extends DataBindBaseActivity<ActivityListBinding> {
    private static final String TAG = LivePKListActivity.class.getSimpleName();
    private static final int RECYCLER_VIEW_SPAN_COUNT = 2;
    private static final int TAG_PERMISSTION_REQUESTCODE = 1000;
    private static final String[] PERMISSTION = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    private RoomListAdapter mAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_list;
    }

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {
        // do nothing
    }

    @Override
    protected void iniView() {
        mDataBinding.hostInSwipe.setNestedScrollingEnabled(false);
        mDataBinding.hostInRoomListRecycler.setVisibility(View.VISIBLE);
        mDataBinding.hostInRoomListRecycler.setLayoutManager(new GridLayoutManager(this, RECYCLER_VIEW_SPAN_COUNT));
        mAdapter = new RoomListAdapter();
        mDataBinding.hostInRoomListRecycler.setAdapter(mAdapter);
        mDataBinding.hostInRoomListRecycler.addItemDecoration(new SpaceItemDecoration(getResources()
                .getDimensionPixelSize(R.dimen.activity_horizontal_margin), RECYCLER_VIEW_SPAN_COUNT));
    }

    @Override
    protected void iniListener() {
        mDataBinding.hostInSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadRoomList(data -> {
                    runOnUiThread(() -> {
                        mAdapter.appendList(data, true);
                        checkNoData();
                        mDataBinding.hostInSwipe.setRefreshing(false);
                    });
                });
            }
        });
        mDataBinding.liveRoomStartBroadcast.setOnClickListener(v -> {
            alertCreateDialog();
        });
        mAdapter.setItemClickListener(item -> startActivity(AudienceActivity.launch(LivePKListActivity.this, item)));
    }

    @Override
    protected void iniData() {
        initSyncManager();
        mDataBinding.hostInSwipe.setRefreshing(true);
        loadRoomList(data -> {
            runOnUiThread(() -> {
                Log.d(TAG, "initData loadRoomList data=" + data);
                mAdapter.appendList(data, true);
                checkNoData();
                mDataBinding.hostInSwipe.setRefreshing(false);
            });
        });
    }

    private void checkNoData() {
        boolean hasData = mAdapter.getItemCount() > 0;
        mDataBinding.noDataBg.setVisibility(hasData? View.GONE: View.VISIBLE);
    }

    private void alertCreateDialog() {
        if (EasyPermissions.hasPermissions(this, PERMISSTION)) {
            new CreateRoomDialog()
                    .show(getSupportFragmentManager(), new CreateRoomDialog.ICreateCallback() {
                        @Override
                        public void onRoomCreate(@NonNull String roomName) {
                            long currTime = System.currentTimeMillis();
                            createRoom(new RoomInfo(currTime, currTime + "", roomName));
                        }
                    });
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.error_leak_permission),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
        }
    }

    // ====== business method ======

    public final static String SYNC_SCENE_ID = "LivePK";
    public final static String SYNC_COLLECTION_ROOM_INFO = "RoomInfo";
    private String syncUserId;


    private void initSyncManager(){
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", getString(R.string.agora_app_id));
        SyncManager.Instance().init(this, params);

        syncUserId = UUID.randomUUID().toString();

        Scene room = new Scene();
        room.setId(SYNC_SCENE_ID);
        room.setUserId(syncUserId);
        SyncManager.Instance().joinScene(room);
    }

    private void loadRoomList(DataListCallback<RoomInfo> callback){
        SceneReference livePK = SyncManager.Instance().getScene(SYNC_SCENE_ID);
        livePK.collection(SYNC_COLLECTION_ROOM_INFO).get(new SyncManager.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                List<RoomInfo> list = new ArrayList<>();
                for (IObject item : result) {
                    list.add(item.toObject(RoomInfo.class));
                }
                Collections.sort(list, (o1, o2) -> (int) (o2.createTime - o1.createTime));
                callback.onSuccess(list);
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Log.e(this.getClass().getSimpleName(), "loadRoomList error: " + exception.toString());
                callback.onSuccess(null);
            }
        });
    }

    private void createRoom(RoomInfo roomInfo){
        SyncManager.Instance().getScene(SYNC_SCENE_ID).collection(SYNC_COLLECTION_ROOM_INFO).add(roomInfo.toMap(), new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                startActivity(HostPKActivity.launch(LivePKListActivity.this, roomInfo));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Toast.makeText(LivePKListActivity.this, "Room create failed -- " + exception.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }



}
