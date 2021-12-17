package io.agora.sample.breakoutroom.ui.list;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.repo.RoomListApi;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Scene;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 * @author lq
 */
public class RoomListViewModel extends ViewModel implements RoomListApi {

    /* 暂存 Room */
    private final MutableLiveData<RoomInfo> _pendingRoomInfo = new MutableLiveData<>();
    public LiveData<RoomInfo> pendingRoomInfo(){
        return _pendingRoomInfo;
    }

    public void clearPendingRoomInfo(){
        _pendingRoomInfo.setValue(null);
    }

    /* UI 状态管理*/
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();
    public LiveData<ViewStatus> viewStatus(){
        return _viewStatus;
    }

    /* 房间列表数据集合*/
    private final MutableLiveData<List<RoomInfo>> _roomList = new MutableLiveData<>();
    public LiveData<List<RoomInfo>> roomList() {
        return _roomList;
    }

    public RoomListViewModel() {
        fetchRoomList();
    }

    @Override
    public void fetchRoomList() {
        _viewStatus.postValue(new ViewStatus.Loading(false));
        SyncManager.Instance().getScenes(new SyncManager.DataListCallback() {
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
//                for (int i = 0; i < 20; i++) {
//                    roomInfo = new RoomInfo("room_"+i,""+i,""+i);
//                    res.add(roomInfo);
//                }
                _roomList.postValue(res);
                _viewStatus.postValue(new ViewStatus.Done());
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (Objects.equals(exception.getMessage(), "empty scene")) {
                    _roomList.postValue(new ArrayList<>());
                    _viewStatus.postValue(new ViewStatus.Done());
                }else{
                    _roomList.postValue(null);
                    _viewStatus.postValue(new ViewStatus.Error(exception));
                }
            }
        });
    }

    /**
     * 成功：1. {@link this#_pendingRoomInfo} 通知UI
     * 失败：{@link this#_viewStatus} 通知 UI
     */
    @Override
    public void joinRoom(@NonNull RoomInfo roomInfo){
        _viewStatus.postValue(new ViewStatus.Loading(true));

        SyncManager.Instance().joinScene(getSceneFromRoomInfo(roomInfo), new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                _pendingRoomInfo.postValue(roomInfo);
                _viewStatus.postValue(new ViewStatus.Done());
            }

            @Override
            public void onFail(SyncManagerException exception) {
                _viewStatus.postValue(new ViewStatus.Error(exception));
            }
        });
    }

    /**
     * 设置 roomId、ownerId、backgroundId
     * 成功：1. 提示 UI {@link this#_roomList} 有新房间 2. 通知UI {@link this#_pendingRoomInfo} 改变
     * 失败：通知UI {@link this#_viewStatus}改变
     */
    @Override
    public void createRoom(@NonNull RoomInfo roomInfo) {
        _viewStatus.postValue(new ViewStatus.Loading(true));

        SyncManager.Instance().joinScene(getSceneFromRoomInfo(roomInfo), new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                List<RoomInfo> list = _roomList.getValue();
                if (list == null)
                    list = new ArrayList<>();
                list.add(roomInfo);
                _roomList.postValue(list);
                _pendingRoomInfo.postValue(roomInfo);
                _viewStatus.postValue(new ViewStatus.Done());
            }

            @Override
            public void onFail(SyncManagerException exception) {
                _viewStatus.postValue(new ViewStatus.Error(exception));
            }
        });
    }


    private Scene getSceneFromRoomInfo(@NonNull RoomInfo roomInfo){
        Scene scene = new Scene();
        scene.setId(roomInfo.getId());
        scene.setUserId(roomInfo.getUserId());

        HashMap<String, String> map = new HashMap<>();
        map.put("backgroundId", roomInfo.getBackgroundId());
        scene.setProperty(map);
        return scene;
    }

}
