package io.agora.sample.rtegame.ui.roompage.hostdialog;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.bean.PKApplyInfo;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.ui.roompage.RoomViewModel;
import io.agora.sample.rtegame.util.Event;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

public class HostListViewModel extends ViewModel {

    private final MutableLiveData<Event<Boolean>> _pkResult = new MutableLiveData<>();

    public LiveData<Event<Boolean>> pkResult() {
        return _pkResult;
    }

    public void sendPKInvite(RoomViewModel roomViewModel, RoomInfo targetRoom, int gameId) {
        SyncManager.Instance().joinScene(GameUtil.getSceneFromRoomInfo(targetRoom), new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                roomViewModel.subscribeApplyPKInfo(targetRoom);

                PKApplyInfo pkApplyInfo = new PKApplyInfo(roomViewModel.currentRoom.getUserId(), targetRoom.getUserId(), GameApplication.getInstance().user.getName(), PKApplyInfo.APPLYING, gameId,
                        roomViewModel.currentRoom.getId(), targetRoom.getId());

                SyncManager.Instance().getScene(targetRoom.getId()).update(GameConstants.PK_APPLY_INFO, pkApplyInfo, new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {
                        _pkResult.postValue(new Event<>(true));
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        _pkResult.postValue(new Event<>(false));
                    }
                });
            }

            @Override
            public void onFail(SyncManagerException exception) {
                _pkResult.postValue(new Event<>(false));
            }
        });
    }


}