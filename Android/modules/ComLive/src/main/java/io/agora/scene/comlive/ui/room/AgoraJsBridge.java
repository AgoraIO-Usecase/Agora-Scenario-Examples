package io.agora.scene.comlive.ui.room;

import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;

import io.agora.example.base.BaseUtil;

public class AgoraJsBridge {
    @NonNull
    private final RoomViewModel roomViewModel;
    public AgoraJsBridge(@NonNull RoomViewModel mViewModel) {
        this.roomViewModel = mViewModel;
    }

    @JavascriptInterface
    public void enableAudio(int option){
        BaseUtil.logD("JS - enableAudio:"+option);
        roomViewModel.jsEnableMic(option == 1);
    }

    /**
     * 主动离开游戏接口
     * 该接口用于游戏流程中个性定制化需求需要主动终止游戏进程的.
     * 如游戏中的crtical错误, 关键角色退出游戏等.
     */
    @JavascriptInterface
    public void leave(int option){
        BaseUtil.logD("JS - leave:" + option);
        roomViewModel.requestExitGame();
    }

    /**
     * 适用于观众在游戏中改变角色(旁观, 玩家)
     * 在观众端游戏视图内, 点击加入游戏后需要通过jsBridge通知声网
     *
     *  2 玩家, 3 旁观 (和加入游戏的identify一致)
     */
    @JavascriptInterface
    public void setRole(int oldRole, int newRole){
        BaseUtil.logD("JS - setRole:" + oldRole + " ->" + newRole);
        roomViewModel.changeRole(oldRole, newRole);
    }
}
