package io.agora.scene.onelive.repo;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import io.agora.scene.onelive.bean.AgoraGame;
import io.agora.scene.onelive.bean.AppServerResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 均在拦截器会添加 sign 字段
 */
public interface Game1API {

    @NonNull
    @POST("getGames")
    Call<AppServerResult<List<AgoraGame>>> getGameList(@Body @NonNull Map<String, String> type);

    @NonNull
    @POST("getGameById")
    Call<AppServerResult<AgoraGame>> getGameById(@Body @NonNull Map<String, String> gameId);

    @NonNull
    @POST("getJoinUrl")
    Call<AppServerResult<String>> getJoinUrl(@Body @NonNull Map<String, String> params);

    @NonNull
    @POST("leaveGame")
    Call<AppServerResult<Map<String,String>>> leaveGame(@Body @NonNull Map<String, String> params);

    @NonNull
    @POST("changeRole")
    Call<AppServerResult<Map<String,String>>> changeRole(@Body @NonNull Map<String, Object> params);
}