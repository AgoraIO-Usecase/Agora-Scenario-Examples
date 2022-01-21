package io.agora.scene.rtegame.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.agora.example.base.BaseUtil;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class YuanQiHttp {
    private static final Retrofit retrofit = init();

    private static Retrofit init() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(BaseUtil::logD);
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("X-LC-Id", GameRepo.X_LC_ID)
                            .addHeader("X-LC-Key", GameRepo.X_LC_KEY)
                            .addHeader("X-LC-Session", GameRepo.X_LC_SESSION).build();
                    return chain.proceed(request);
                })
                .addNetworkInterceptor(loggingInterceptor)
                .build();
        return new Retrofit.Builder().client(client)
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .baseUrl("https://agoraktv.xyz/1.1/functions/")
                .build();
    }

    @NonNull
    public static Game1API getAPI() {
        return retrofit.create(Game1API.class);
    }
}

