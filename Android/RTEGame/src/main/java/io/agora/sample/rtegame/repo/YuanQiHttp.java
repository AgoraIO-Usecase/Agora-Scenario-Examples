package io.agora.sample.rtegame.repo;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class YuanQiHttp {
    private static final Retrofit retrofit = init();

    private static Retrofit init() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        return new Retrofit.Builder().client(client)
                .baseUrl("https://www.baidu.com")
                .build();
    }

    @NonNull
    public static Game1API getAPI(){
        return retrofit.create(Game1API.class);
    }
}
