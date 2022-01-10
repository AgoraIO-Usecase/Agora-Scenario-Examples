package io.agora.scene.comlive.repo;

import android.webkit.WebView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;

import io.agora.example.base.BaseUtil;
import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.bean.AgoraGame;
import io.agora.scene.comlive.bean.GiftInfo;
import io.agora.scene.comlive.bean.LocalUser;
import io.agora.scene.comlive.util.ComLiveUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepo {

    public static final AgoraGame[] gameList =  {
                  new AgoraGame(1, "10020", "你画我猜",
            "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess_More/index.html",
            "https://testgame.yuanqihuyu.com/guess/leave",
            "https://testgame.yuanqihuyu.com/guess/gift",
            "https://testgame.yuanqihuyu.com/guess/barrage"),

            new AgoraGame(2, "10020", "谁是卧底",
            "https://imgsecond.yuanqiyouxi.com/test/spy/index.html",
            "https://testgame.yuanqihuyu.com/spy/leave",
            "https://testgame.yuanqihuyu.com/spy/gift",
            "https://testgame.yuanqihuyu.com/spy/barrage"),

            new AgoraGame(3, "10020", "大话骰",
            "https://imgsecond.yuanqiyouxi.com/test/Dice_ShengWang/index.html",
            "https://testgame.yuanqihuyu.com/dice/leave",
            "https://testgame.yuanqihuyu.com/dice/gift",
            "https://testgame.yuanqihuyu.com/dice/barrage")
    };

    @Nullable
    public static AgoraGame getGameDetail(int gameId){
        return gameList[gameId - 1];
    }

    public static void endThisGame(int targetRoomId, int userId){
        AgoraGame currentGame = ComLiveUtil.currentGame;
        if (currentGame != null) {
            BaseUtil.logD("endThisGame:" + targetRoomId);
            int appId = Integer.parseInt(currentGame.getAppId());
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

            String endUrl = currentGame.getGameEndUrl();

            YuanQiHttp.getAPI().gameEnd(endUrl, userId, appId, 1, targetRoomId, "123456789", timestamp, "123").enqueue(new EmptyRetrofitCallBack<>());
            BaseUtil.logD("end complete");
        }
    }

    @MainThread
    public static void gameStart(@NonNull WebView webView, @NonNull LocalUser user, boolean isOrganizer, int targetRoomId){
        if (ComLiveUtil.currentGame == null) return;
        int userId = Integer.parseInt(user.getUserId());
        int appId = Integer.parseInt(ComLiveUtil.currentGame.getAppId());

        String url = YuanQiHttp.getAPI().gameStart(
                ComLiveUtil.currentGame.getGameStartUrl(),
                userId, appId, targetRoomId, isOrganizer ? 1 : 2,123456789,user.getName(), user.getAvatar()
        ).request().url().toString();

        BaseUtil.logD("url:"+url);
        webView.loadUrl(url);
    }

    public static void sendGift(@NonNull LocalUser user, int roomId, int playerId, @NonNull GiftInfo gift){
        if (ComLiveUtil.currentGame == null) return;
        int userId = Integer.parseInt(user.getUserId());
        int appId = Integer.parseInt(ComLiveUtil.currentGame.getAppId());
        String timestamp = String.valueOf(System.currentTimeMillis()/1000);

        String giftUrl = ComLiveUtil.currentGame.getGameGiftUrl();

        YuanQiHttp.getAPI().gameGift(giftUrl, userId, appId,roomId, user.getName(), "token123", timestamp, YuanQiHttp.nonStr(), gift.getGiftType(), 1, playerId).enqueue(new EmptyRetrofitCallBack<>());
    }

    static class EmptyRetrofitCallBack<T> implements Callback<T>{

        @Override
        public void onResponse(@NonNull Call<T> call, Response<T> response) {
            ResponseBody body = (ResponseBody) response.body();
            if (body != null) {
                try {
                    BaseUtil.logD(body.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
            BaseUtil.logD(t.getMessage());
        }
    }
}
