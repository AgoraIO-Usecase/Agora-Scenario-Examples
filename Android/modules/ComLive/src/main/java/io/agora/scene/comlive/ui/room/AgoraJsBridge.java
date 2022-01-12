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
        roomViewModel.enableMic(option == 1);
    }

//    @JavascriptInterface
//    public void agoraJSBridge_leave(int option){
//        roomViewModel.enableMic(option == 1);
//    }
}
