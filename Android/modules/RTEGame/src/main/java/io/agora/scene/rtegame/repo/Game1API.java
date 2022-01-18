package io.agora.scene.rtegame.repo;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import io.agora.scene.rtegame.bean.AgoraGame;
import io.agora.scene.rtegame.bean.AppServerResult;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

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

    @NonNull
    @POST("gift")
    Call<AppServerResult<Map<String,String>>> sendGift(@Body @NonNull Map<String, Object> params);

    @NonNull
    @POST("barrage")
    Call<AppServerResult<Map<String,String>>> sendBarrage(@Body @NonNull Map<String, Object> params);
}
