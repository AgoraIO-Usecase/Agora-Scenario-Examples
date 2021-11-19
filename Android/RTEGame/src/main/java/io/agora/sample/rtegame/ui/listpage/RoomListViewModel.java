package io.agora.sample.rtegame.ui.listpage;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomListApi;
import io.agora.sample.rtegame.util.ViewStatus;
import io.agora.syncmanager.rtm.Scene;


/**
 * @author lq
 */
public class RoomListViewModel extends ViewModel implements RoomListApi {

    /* UI 状态管理*/
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();

    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    /* 房间列表数据集合*/
    private final MutableLiveData<List<RoomInfo>> _roomList = new MutableLiveData<>();

    public LiveData<List<RoomInfo>> roomList() {
        return _roomList;
    }

    public RoomListViewModel() {
        BaseUtil.logD("fetchRoomList");
        fetchRoomList();
    }

    @Override
    public void fetchRoomList() {
        _viewStatus.postValue(new ViewStatus.Loading(false));
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<RoomInfo> res = new ArrayList<>();
            RoomInfo roomInfo;
            if (new Random().nextBoolean())
                for (int i = 0; i < 20; i++) {
                    roomInfo = new RoomInfo("room_" + i, "" + i, "" + i);
                    res.add(roomInfo);
                }
            _roomList.postValue(res);
            _viewStatus.postValue(new ViewStatus.Done());
        }).start();
//        SyncManager.Instance().getScenes(new SyncManager.DataListCallback() {
//            @Override
//            public void onSuccess(List<IObject> result) {
//                List<RoomInfo> res = new ArrayList<>();
//                RoomInfo roomInfo;
//
//                for (IObject iObject : result) {
//                    try {
//                        roomInfo = iObject.toObject(RoomInfo.class);
//                    } catch (Exception e) {
//                        roomInfo = null;
//                        e.printStackTrace();
//                    }
//                    if (roomInfo != null)
//                        res.add(roomInfo);
//                }
//                for (int i = 0; i < 20; i++) {
//                    roomInfo = new RoomInfo("room_"+i,""+i,""+i);
//                    res.add(roomInfo);
//                }
//                _roomList.postValue(res);
//                _viewStatus.postValue(new ViewStatus.Done());
//            }
//
//            @Override
//            public void onFail(SyncManagerException exception) {
//                if (Objects.equals(exception.getMessage(), "empty scene")) {
//                    _roomList.postValue(new ArrayList<>());
//                    _viewStatus.postValue(new ViewStatus.Done());
//                }else{
////                    _roomList.postValue(null);
//                    _viewStatus.postValue(new ViewStatus.Error(exception));
//                }
//            }
//        });
    }


    private Scene getSceneFromRoomInfo(@NonNull RoomInfo roomInfo) {
        Scene scene = new Scene();
        scene.setId(roomInfo.getRoomId());
        scene.setUserId(roomInfo.getUserId());

        HashMap<String, String> map = new HashMap<>();
        map.put("backgroundId", roomInfo.getBackgroundId());
        scene.setProperty(map);
        return scene;
    }

}
