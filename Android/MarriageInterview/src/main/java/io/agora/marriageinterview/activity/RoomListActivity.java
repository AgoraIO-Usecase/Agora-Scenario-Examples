package io.agora.marriageinterview.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.data.BaseError;
import com.agora.data.SimpleRoomEventCallback;
import com.agora.data.manager.RTMManager;
import com.agora.data.manager.RoomManager;
import com.agora.data.manager.UserManager;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.observer.DataMaybeObserver;
import com.agora.data.observer.DataObserver;

import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.adapter.RoomListAdapter;
import io.agora.marriageinterview.data.DataRepositroy;
import io.agora.marriageinterview.databinding.MerryActivityRoomListBinding;
import io.agora.marriageinterview.widget.CreateRoomDialog;
import io.agora.marriageinterview.widget.SpaceItemDecoration;
import io.reactivex.android.schedulers.AndroidSchedulers;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 房间列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomListActivity extends DataBindBaseActivity<MerryActivityRoomListBinding> implements View.OnClickListener,
        OnItemClickListener<Room>, EasyPermissions.PermissionCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private static final int TAG_PERMISSTION_REQUESTCODE = 1000;
    private static final String[] PERMISSTION = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    private RoomListAdapter mAdapter;

    private SimpleRoomEventCallback mSimpleRoomEventCallback = new SimpleRoomEventCallback() {
        @Override
        public void onRoomClosed(@NonNull Room room, boolean fromUser) {
            super.onRoomClosed(room, fromUser);

            mAdapter.deleteItem(room);
            mDataBinding.tvEmpty.setVisibility(mAdapter.getItemCount() <= 0 ? View.VISIBLE : View.GONE);
        }
    };

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.merry_activity_room_list;
    }

    @Override
    protected void iniView() {
        mAdapter = new RoomListAdapter(null, this);
        mDataBinding.list.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mDataBinding.list.setAdapter(mAdapter);
        mDataBinding.list.addItemDecoration(new SpaceItemDecoration(this));
    }

    @Override
    protected void iniListener() {
        RoomManager.Instance(this).addRoomEventCallback(mSimpleRoomEventCallback);
        mDataBinding.swipeRefreshLayout.setOnRefreshListener(this);
        mDataBinding.ivHead.setOnClickListener(this);
        mDataBinding.btCrateRoom.setOnClickListener(this);
    }

    @Override
    protected void iniData() {
        RTMManager.Instance(this).init();

        UserManager.Instance(this).setupDataRepositroy(DataRepositroy.Instance(this));
        RoomManager.Instance(this).setupDataRepositroy(DataRepositroy.Instance(this));

        mDataBinding.btCrateRoom.setVisibility(View.VISIBLE);
        mDataBinding.tvEmpty.setVisibility(mAdapter.getItemCount() <= 0 ? View.VISIBLE : View.GONE);

        login();
    }

    private void login() {
        UserManager.Instance(this)
                .loginIn()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataObserver<User>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {

                    }

                    @Override
                    public void handleSuccess(@NonNull User user) {
                        mDataBinding.swipeRefreshLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                mDataBinding.swipeRefreshLayout.setRefreshing(true);
                                loadRooms();
                            }
                        });
                    }
                });
    }

    private void loadRooms() {
        DataRepositroy.Instance(this)
                .getRooms()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataMaybeObserver<List<Room>>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        mDataBinding.swipeRefreshLayout.setRefreshing(false);
                        ToastUtile.toastShort(RoomListActivity.this, e.getMessage());
                    }

                    @Override
                    public void handleSuccess(@Nullable List<Room> rooms) {
                        mDataBinding.swipeRefreshLayout.setRefreshing(false);

                        if (rooms == null) {
                            mAdapter.clear();
                        } else {
                            mAdapter.setDatas(rooms);
                        }
                        mDataBinding.tvEmpty.setVisibility(mAdapter.getItemCount() <= 0 ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (!UserManager.Instance(this).isLogin()) {
            login();
            return;
        }

        if (v.getId() == R.id.btCrateRoom) {
            gotoCreateRoom();
        } else if (v.getId() == R.id.ivHead) {
            Intent intent = UserInfoActivity.newIntent(this);
            startActivity(intent);
        }
    }

    private void gotoCreateRoom() {
        if (EasyPermissions.hasPermissions(this, PERMISSTION)) {
            new CreateRoomDialog().show(getSupportFragmentManager(), new CreateRoomDialog.ICreateCallback() {
                @Override
                public void onRoomCreated(@NonNull Room room) {
                    Intent intent = ChatRoomActivity.newIntent(RoomListActivity.this, room);
                    startActivity(intent);
                }
            });
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.error_permisstion),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onItemClick(@NonNull Room data, View view, int position, long id) {
        if (!EasyPermissions.hasPermissions(this, PERMISSTION)) {
            EasyPermissions.requestPermissions(this, getString(R.string.error_permisstion),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
            return;
        }

        Room roomCur = RoomManager.Instance(this).getRoom();
        if (roomCur != null) {
            if (!ObjectsCompat.equals(roomCur, data)) {
                ToastUtile.toastShort(this, R.string.error_joined_room);
                return;
            }
        }

        Intent intent = ChatRoomActivity.newIntent(this, data);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        RoomManager.Instance(this).removeRoomEventCallback(mSimpleRoomEventCallback);
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        loadRooms();
    }
}
