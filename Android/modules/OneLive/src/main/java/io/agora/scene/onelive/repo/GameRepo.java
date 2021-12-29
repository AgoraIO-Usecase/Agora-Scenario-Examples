package io.agora.scene.onelive.repo;

import android.webkit.WebView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import io.agora.example.base.BaseUtil;
import io.agora.scene.onelive.bean.AgoraGame;
import io.agora.scene.onelive.bean.LocalUser;
import io.agora.scene.onelive.bean.RoomInfo;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    public static void endThisGame(@NonNull RoomInfo roomInfo, @NonNull AgoraGame agoraGame){
        BaseUtil.logD("endThisGame");
        int userId = Integer.parseInt(roomInfo.getUserId());
        int appId = Integer.parseInt(agoraGame.getAppId());
        int roomId = Integer.parseInt(roomInfo.getId());
        String timestamp = String.valueOf(System.currentTimeMillis()/1000);

        String endUrl = agoraGame.getGameEndUrl();

        YuanQiHttp.getAPI().gameEnd(endUrl, userId,appId, 1, roomId, "123456789", timestamp, "123").enqueue(new EmptyRetrofitCallBack<>());
    }

    @MainThread
    public static void gameStart(@NonNull WebView webView, @NonNull AgoraGame agoraGame, @NonNull LocalUser user, boolean isOrganizer, int targetRoomId){
        int userId = Integer.parseInt(user.getUserId());
        int appId = Integer.parseInt(agoraGame.getAppId());

        String url = YuanQiHttp.getAPI().gameStart(
                agoraGame.getGameStartUrl(),
                userId, appId, targetRoomId, isOrganizer ? 1 : 2,123456789,user.getName(), user.getAvatar()
        ).request().url().toString();

        BaseUtil.logD("gameStart:"+url);

        webView.loadUrl(url);
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
