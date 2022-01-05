package io.agora.scene.livepk.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.agora.example.base.BaseActivity;
import io.agora.example.base.BaseUtil;
import io.agora.scene.livepk.R;
import io.agora.scene.livepk.adapter.RoomListAdapter;
import io.agora.scene.livepk.databinding.PkActivityListBinding;
import io.agora.scene.livepk.model.RoomInfo;
import io.agora.scene.livepk.widget.CreateRoomDialog;
import io.agora.scene.livepk.widget.SpaceItemDecoration;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Scene;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

public class LivePKListActivity extends BaseActivity<PkActivityListBinding> {
    private static final String TAG = LivePKListActivity.class.getSimpleName();

    private final String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final int RECYCLER_VIEW_SPAN_COUNT = 2;
    private static final int TAG_PERMISSION_REQUEST_CODE = 1000;

    private RoomListAdapter mAdapter;

    private final BaseUtil.PermissionResultCallback<String[]> resultCallback = () -> new CreateRoomDialog()
            .show(getSupportFragmentManager(), roomName -> {
                long currTime = System.currentTimeMillis();
                createRoom(new RoomInfo(currTime, currTime + "", roomName));
            });

    private final ActivityResultLauncher<String[]> launcher = BaseUtil.registerForActivityResult(this, resultCallback);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        showLoading();
        initSyncManager();
    }

    private void initView() {
        // RecyclerView
        mAdapter = new RoomListAdapter();
        mAdapter.setItemClickListener(new RoomListAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(RoomInfo item) {
                startActivity(AudienceActivity.launch(LivePKListActivity.this, item));
            }

            @Override
            public void onItemDeleteClicked(RoomInfo item, int index) {
                new AlertDialog.Builder(LivePKListActivity.this)
                        .setTitle("Tip")
                        .setMessage("Sure to delete the room?")
                        .setPositiveButton(R.string.pk_cmm_ok, (dialog, which) -> deleteRoom(item, () -> {
                            mAdapter.remoteItem(item.getRoomId());
                        }))
                        .setNegativeButton(R.string.pk_cmm_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        mBinding.hostInRoomListRecycler.setAdapter(mAdapter);
        mBinding.hostInRoomListRecycler.setVisibility(View.VISIBLE);
        mBinding.hostInRoomListRecycler.setLayoutManager(new GridLayoutManager(this, RECYCLER_VIEW_SPAN_COUNT));
        mBinding.hostInRoomListRecycler.addItemDecoration(new SpaceItemDecoration(getResources()
                .getDimensionPixelSize(R.dimen.pk_activity_horizontal_margin), RECYCLER_VIEW_SPAN_COUNT));

        // SwipeRefreshLayout
        mBinding.swipeRefreshLayoutAttList.setNestedScrollingEnabled(false);
        mBinding.swipeRefreshLayoutAttList.setOnRefreshListener(this::loadRoomList);
        // Button
        mBinding.liveRoomStartBroadcast.setOnClickListener(v -> alertCreateDialog());
    }


    @MainThread
    private void onFetchRoomListSucceed(List<RoomInfo> data) {
        BaseUtil.logD("initData loadRoomList data=" + data);
        mAdapter.appendList(data, true);
        checkNoData();
        mBinding.swipeRefreshLayoutAttList.setRefreshing(false);
    }

    @MainThread
    private void onFetchRoomListFailed() {
        mBinding.swipeRefreshLayoutAttList.setRefreshing(false);
    }

    private void checkNoData() {
        boolean hasData = mAdapter.getItemCount() > 0;
        mBinding.noDataBg.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    private void alertCreateDialog() {
        BaseUtil.checkPermissionBeforeNextOP(this, launcher, permissions, resultCallback);
    }


    private void showSyncManagerInitErrorDialog() {
        new AlertDialog.Builder(LivePKListActivity.this).setMessage("SyncManager init error, try again?")
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> LivePKListActivity.this.finish())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> initSyncManager())
                .setCancelable(false).show();
    }

    // ====== business method ======

    public final static String SYNC_SCENE_ID = "LivePK";
    public final static String SYNC_COLLECTION_ROOM_INFO = "RoomInfo";


    // TODO extract this function to something like ViewModel
    private void initSyncManager() {
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", getString(R.string.rtm_app_id));
        params.put("token", getString(R.string.rtm_app_token));
        params.put("defaultChannel", SYNC_SCENE_ID);
        Sync.Instance().init(this, params, new Sync.Callback() {
            @Override
            public void onSuccess() {
                loadRoomList();
            }

            @Override
            public void onFail(SyncManagerException exception) {
                showSyncManagerInitErrorDialog();
            }
        });
    }

    /**
     * fetch room list
     */
    private void loadRoomList() {
        Sync.Instance().getScenes(new Sync.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                List<RoomInfo> list = new ArrayList<>();
                for (IObject item : result) {
                    list.add(item.toObject(RoomInfo.class));
                }
                Collections.sort(list, (o1, o2) -> (int) (o2.getCreateTime() - o1.getCreateTime()));
                mBinding.getRoot().post(() -> onFetchRoomListSucceed(list));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Log.e(this.getClass().getSimpleName(), "loadRoomList error: " + exception.toString());
                mBinding.getRoot().post(() -> onFetchRoomListFailed());
            }
        });
    }

    /**
     * Just create
     */
    private void createRoom(RoomInfo roomInfo) {
        // TODO using a Util function to simplify this
        Scene scene = new Scene();
        scene.setProperty(roomInfo.toMap());

        Sync.Instance().createScene(scene, new Sync.Callback() {
            @Override
            public void onSuccess() {
                startActivity(HostPKActivity.launch(LivePKListActivity.this, roomInfo));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                runOnUiThread(() -> Toast.makeText(LivePKListActivity.this, "Room create failed -- " + exception.toString(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteRoom(RoomInfo roomInfo, Runnable successRun) {
        // TODO
//        Sync.Instance()
//                .getScenes(LivePKListActivity.SYNC_SCENE_ID)
//                .collection(LivePKListActivity.SYNC_COLLECTION_ROOM_INFO)
//                .document(roomInfo.roomId)
//                .delete(new Sync.Callback() {
//                    @Override
//                    public void onSuccess() {
//                        runOnUiThread(successRun);
//                    }
//
//                    @Override
//                    public void onFail(SyncManagerException exception) {
//                        runOnUiThread(() -> Toast.makeText(LivePKListActivity.this, "deleteRoomInfo failed exception: " + exception.toString(), Toast.LENGTH_LONG).show());
//                    }
//                });
    }


}
