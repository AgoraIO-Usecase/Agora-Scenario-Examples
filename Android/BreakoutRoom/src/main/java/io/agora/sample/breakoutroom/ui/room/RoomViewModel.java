package io.agora.sample.breakoutroom.ui.room;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.agora.sample.breakoutroom.RoomConstant;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.bean.SubRoomInfo;
import io.agora.sample.breakoutroom.repo.RoomApi;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 *
 */
public class RoomViewModel extends ViewModel implements RoomApi {

    public RoomInfo currentRoomInfo;

    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();
    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    // 子房间列表
    private final MutableLiveData<List<SubRoomInfo>> _subRoomList = new MutableLiveData<>();
    public LiveData<List<SubRoomInfo>> subRoomList() {
        return _subRoomList;
    }

    // 准备创建的子房间
    private final MutableLiveData<SubRoomInfo> _pendingSubRoom = new MutableLiveData<>();
    public LiveData<SubRoomInfo> pendingSubRoom() {
        return _pendingSubRoom;
    }

    public void clearPendingSubRoom(){
        _pendingSubRoom.setValue(null);
    }

    @Override
    public void createSubRoom(@NonNull String roomName) {
        _viewStatus.postValue(new ViewStatus.Loading());
        SubRoomInfo pendingSubRoom = new SubRoomInfo(roomName);
        HashMap<String, Object> map = RoomUtil.convertObjToHashMap(pendingSubRoom, RoomConstant.gson);
        SyncManager.Instance().getScene(currentRoomInfo.getId()).collection(RoomConstant.globalSubRoom).add(map, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                List<SubRoomInfo> list = _subRoomList.getValue();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(pendingSubRoom);

                _pendingSubRoom.postValue(pendingSubRoom);

                // 不会干扰正在进行的子房间
                if (_subRoomList.getValue() == null)
                    _subRoomList.postValue(list);
                _viewStatus.postValue(new ViewStatus.Done());
            }

            @Override
            public void onFail(SyncManagerException e) {
                _viewStatus.postValue(new ViewStatus.Error(e));
            }
        });
    }

    @Override
    public void fetchAllSubRooms() {
        _viewStatus.postValue(new ViewStatus.Loading());
        SyncManager.Instance().getScene(currentRoomInfo.getId()).collection(RoomConstant.globalSubRoom).get(new SyncManager.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {

                List<SubRoomInfo> res = new ArrayList<>();
                SubRoomInfo subRoomInfo;

                for (IObject iObject : result) {
                    try {
                        subRoomInfo = iObject.toObject(SubRoomInfo.class);
                    } catch (Exception e) {
                        subRoomInfo = null;
                        e.printStackTrace();
                    }
                    if (subRoomInfo != null)
                        res.add(subRoomInfo);
                }
                _subRoomList.postValue(res);
                _viewStatus.postValue(new ViewStatus.Done());
            }

            @Override
            public void onFail(SyncManagerException e) {
                if (Objects.equals(e.getMessage(), "empty attributes")) {
                    _subRoomList.postValue(new ArrayList<>());
                    _viewStatus.postValue(new ViewStatus.Done());
                }else{
                    _viewStatus.postValue(new ViewStatus.Error(e));
                }
            }
        });
    }
}
