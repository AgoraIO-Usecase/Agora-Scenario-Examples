package io.agora.scene.rtegame.ui.list;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.agora.example.base.BaseUtil;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.util.ViewStatus;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;


/**
 * @author lq
 */
public class RoomListViewModel extends ViewModel {

    /* UI 状态管理*/
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();

    @NonNull
    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    /* 房间列表数据集合*/
    private final MutableLiveData<List<RoomInfo>> _roomList = new MutableLiveData<>();

    @NonNull
    public LiveData<List<RoomInfo>> roomList() {
        return _roomList;
    }

    public RoomListViewModel() {
        fetchRoomList();
    }

    public void fetchRoomList() {
        _viewStatus.postValue(new ViewStatus.Loading(true));

        try {
            Sync.Instance().getScenes(new Sync.DataListCallback() {
                @Override
                public void onSuccess(List<IObject> result) {
                    List<RoomInfo> res = new ArrayList<>();
                    RoomInfo roomInfo;

                    for (IObject iObject : result) {
                        try {
                            roomInfo = iObject.toObject(RoomInfo.class);
                        } catch (Exception e) {
                            roomInfo = null;
                            e.printStackTrace();
                        }
                        if (roomInfo != null)
                            res.add(roomInfo);
                    }
                    for (RoomInfo re : res) {
                        BaseUtil.logD(re.toString());
                    }
                    _roomList.postValue(res);
                    _viewStatus.postValue(new ViewStatus.Done());
                }

                @Override
                public void onFail(SyncManagerException exception) {
                    if (Objects.equals(exception.getMessage(), "empty scene")) {
                        _roomList.postValue(new ArrayList<>());
                        _viewStatus.postValue(new ViewStatus.Done());
                    }else{
                        _viewStatus.postValue(new ViewStatus.Error(exception));
                    }
                }
            });
        } catch (Exception e) {
            // knowing issue
//            java.lang.NullPointerException: Attempt to invoke interface method 'void io.agora.syncmanager.rtm.ISyncManager.getScenes(io.agora.syncmanager.rtm.Sync$DataListCallback)' on a null object reference
//            at io.agora.syncmanager.rtm.Sync.getScenes(Sync.java:52)
            BaseUtil.logD("Sync is down, re init it");
            e.printStackTrace();
//            Sync.Instance().init();
        }
    }

}
