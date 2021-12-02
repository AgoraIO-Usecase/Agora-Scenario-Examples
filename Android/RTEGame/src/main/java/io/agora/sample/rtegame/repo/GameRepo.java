package io.agora.sample.rtegame.repo;

import android.webkit.WebView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.bean.AgoraGame;
import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.util.GameUtil;

public class GameRepo {

    @Nullable
    public static AgoraGame getGameDetail(int gameId){
        if (gameId == 1){
            return new AgoraGame(gameId, "10020", "你画我猜",
                    "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html",
                    "https://testgame.yuanqihuyu.com/guess/leave",
                    "https://testgame.yuanqihuyu.com/guess/gift",
                    "https://testgame.yuanqihuyu.com/guess/barrage");
        } else return null;
    }

    public static void endThisGame(int targetRoomId){
        if (GameUtil.currentGame == null) return;
        int userId = Integer.parseInt(GameApplication.getInstance().user.getUserId());
        int appId = Integer.parseInt(GameUtil.currentGame.getAppId());
        String timestamp = String.valueOf(System.currentTimeMillis()/1000);

        YuanQiHttp.getAPI().gameEnd(userId,appId, 1, targetRoomId, "123456789", timestamp, "1234567890123456");
    }

    @MainThread
    public static void gameStart(@NonNull WebView webView, @NonNull LocalUser user,boolean isOrganizer, int targetRoomId){
        if (GameUtil.currentGame == null) return;
        int userId = Integer.parseInt(user.getUserId());
        int appId = Integer.parseInt(GameUtil.currentGame.getAppId());

        String url = YuanQiHttp.getAPI().gameStart(
                GameUtil.currentGame.getGameStartUrl(),
                userId, appId, targetRoomId, isOrganizer ? 1 : 2,123456789,user.getName(), user.getAvatar()
        ).request().url().toString();

        BaseUtil.logD("url:"+url);
        webView.loadUrl(url);
    }
}
