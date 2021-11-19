package io.agora.sample.rtegame.ui.roompage;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomApi;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.util.ViewStatus;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;


/**
 * @author lq
 */
public class RoomViewModel extends ViewModel implements RoomApi {

//    /* UI 状态管理*/
//    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();
//    public LiveData<ViewStatus> viewStatus(){
//        return _viewStatus;
//    }

    @Override
    public void joinRoom(@NonNull RoomInfo roomInfo){
//        _viewStatus.postValue(new ViewStatus.Loading(true));
//
//        SyncManager.Instance().joinScene(GameUtil.getSceneFromRoomInfo(roomInfo), new SyncManager.Callback() {
//            @Override
//            public void onSuccess() {
//                _pendingRoomInfo.postValue(roomInfo);
//                _viewStatus.postValue(new ViewStatus.Done());
//            }
//
//            @Override
//            public void onFail(SyncManagerException exception) {
//                _viewStatus.postValue(new ViewStatus.Error(exception));
//            }
//        });
    }


}
