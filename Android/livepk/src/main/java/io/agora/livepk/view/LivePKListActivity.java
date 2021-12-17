package io.agora.livepk.view;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.livepk.R;
import io.agora.livepk.databinding.ActivityListBinding;
import io.agora.livepk.model.RoomInfo;
import io.agora.livepk.util.DataListCallback;
import io.agora.livepk.util.PreferenceUtil;
import io.agora.livepk.widget.SpaceItemDecoration;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Scene;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;
import pub.devrel.easypermissions.EasyPermissions;

import static io.agora.livepk.Constants.SYNC_COLLECTION_ROOM_INFO;
import static io.agora.livepk.Constants.SYNC_SCENE_ID;

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
        mDataBinding.ivHead.setOnClickListener(v -> {
            startActivity(new Intent(LivePKListActivity.this, UserProfileActivity.class));
        });
    }

    @Override
    protected void iniData() {
        PreferenceUtil.init(getApplicationContext());
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
            startActivity(new Intent(this, PreviewActivity.class));
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.error_leak_permission),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
        }
    }

    // ====== business method ======

    private String syncUserId;


    private void initSyncManager(){
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", getString(R.string.agora_app_id));
        SyncManager.Instance().init(this, params);

        syncUserId = UUID.randomUUID().toString();

        Scene room = new Scene();
        room.setId(SYNC_SCENE_ID);
        room.setUserId(syncUserId);
        SyncManager.Instance().joinScene(room, new SyncManager.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    private void loadRoomList(DataListCallback<RoomInfo> callback){
        SyncManager.Instance().getScene(SYNC_SCENE_ID).collection(SYNC_COLLECTION_ROOM_INFO).get(new SyncManager.DataListCallback() {
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





}
