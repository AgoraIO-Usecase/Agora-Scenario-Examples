package io.agora.scene.rtegame.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.scene.rtegame.bean.AgoraGame;
import io.agora.scene.rtegame.bean.AppServerResult;
import io.agora.scene.rtegame.bean.LocalUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepo {

    @NonNull
    public static String X_LC_ID = "";
    @NonNull
    public static String X_LC_KEY = "";
    @NonNull
    public static String X_LC_SESSION = "";

    /**
     * @param type 1: 1v1, 2: 同玩, 3: pk
     */
    public static void getGameList(@NonNull String type, @NonNull MutableLiveData<List<AgoraGame>> gameObserver) {
        Map<String, String> map = new HashMap<>();
        map.put("type", type);
        YuanQiHttp.getAPI().getGameList(map).enqueue(new Callback<AppServerResult<List<AgoraGame>>>() {
            @Override
            public void onResponse(Call<AppServerResult<List<AgoraGame>>> res, Response<AppServerResult<List<AgoraGame>>> response) {
                AppServerResult<List<AgoraGame>> body = response.body();
                if (body == null) gameObserver.postValue(new ArrayList<>());
                else gameObserver.postValue(body.getResult());
            }

            @Override
            public void onFailure(Call<AppServerResult<List<AgoraGame>>> call, Throwable t) {
                gameObserver.postValue(new ArrayList<>());
            }
        });
    }

    /**
     * TODO NOT TESTED
     */
    public static void getGameById(@NonNull String gameId, @NonNull Callback<AppServerResult<AgoraGame>> callback) {
        Map<String, String> map = new HashMap<>();
        map.put("id", gameId);
        YuanQiHttp.getAPI().getGameById(map).enqueue(callback);
    }

    /**
     * 获取加入游戏 URL
     */
    public static void getJoinUrl(@NonNull String gameId, @NonNull LocalUser localUser, @NonNull String roomId, @NonNull String identification, @NonNull Callback<AppServerResult<String>> callback) {
        Map<String, String> map = new HashMap<>();
        map.put("user_id", localUser.getUserId());
        map.put("name", localUser.getName());
        map.put("avatar", localUser.getAvatar());
        map.put("token", "aa"); // TEST
        map.put("app_id", gameId);
        map.put("room_id", roomId);
        map.put("identity", identification);

        YuanQiHttp.getAPI().getJoinUrl(map).enqueue(callback);
    }

    /**
     * 获取加入游戏 URL
     */
    public static void leaveGame(@NonNull String gameId, @NonNull LocalUser localUser, @NonNull String roomId, @NonNull String identification) {
        Map<String, String> map = new HashMap<>();
        map.put("user_id", localUser.getUserId());
        map.put("app_id", gameId);
        map.put("identity", identification);
        map.put("room_id", roomId);

        YuanQiHttp.getAPI().leaveGame(map).enqueue(new Callback<AppServerResult<AppServerResult<Map<String, String>>>>() {
            @Override
            public void onResponse(Call<AppServerResult<AppServerResult<Map<String, String>>>> call, Response<AppServerResult<AppServerResult<Map<String, String>>>> response) {

            }

            @Override
            public void onFailure(Call<AppServerResult<AppServerResult<Map<String, String>>>> call, Throwable t) {

            }
        });
    }


}
