package io.agora.scene.rtegame.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

public class YuanQiHttp {
    private static final Retrofit retrofit = init();

    private static Retrofit init() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    if (request.method().equalsIgnoreCase("get")){
                        HttpUrl url = request.url();
                        Map<String, Object> paramMap = new HashMap<>();
                        for (String name : url.queryParameterNames()) {
                            paramMap.put(name, url.queryParameter(name));
                        }
                        String sign = md5(getMapValueByDictionarySort(paramMap));
                        if (sign != null) {
                            HttpUrl.Builder urlBuilder = url.newBuilder();
                            urlBuilder.addQueryParameter("sign", sign);
                            return chain.proceed(request.newBuilder().url(urlBuilder.build()).build());
                        }
                    }else if(request.method().equalsIgnoreCase("post")){
                        if (request.body() != null && request.body() instanceof FormBody) {
                            FormBody body = (FormBody) request.body();
                            MediaType mediaType = body.contentType();
                            if (mediaType != null && mediaType.toString().equalsIgnoreCase("application/x-www-form-urlencoded")) {

                                FormBody.Builder formBuilder = new FormBody.Builder();

                                Map<String, Object> paramMap = new HashMap<>();
                                for (int i = 0; i < body.size(); i++) {
                                    paramMap.put(body.encodedName(i), body.encodedValue(i));
                                    formBuilder.addEncoded(body.encodedName(i), body.encodedValue(i));
                                }
                                String sign = md5(getMapValueByDictionarySort(paramMap));
                                if (sign != null) {
                                    formBuilder.addEncoded("sign", sign);
                                    return chain.proceed(request.newBuilder().post(formBuilder.build()).build());
                                }
                            }
                        }
                    }
                    return chain.proceed(chain.request());
                })
                .build();
        return new Retrofit.Builder().client(client)
                .baseUrl("https://www.baidu.com")
                .build();
    }

    @NonNull
    public static Game1API getAPI(){
        return retrofit.create(Game1API.class);
    }


    /**
     * YuanQi 要求 sign 加上 app_secret
     */
    private static String getMapValueByDictionarySort(Map<String,Object> map){
        List<String> list = new ArrayList<>(map.keySet());
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String key : list) {
            sb.append(map.get(key));
        }
        sb.append("rXYVXqcj28uG3AiPL3t4zfbe8TZ20muf");
        return sb.toString();
    }

    @Nullable
    private static String md5(String str)  {
        MessageDigest md;
        try {
            StringBuilder sb = new StringBuilder();
            md = MessageDigest.getInstance("MD5");
            byte[] re = md.digest(str.getBytes(StandardCharsets.UTF_8));
            for (byte b : re) {
                String temp = Integer.toHexString(0xFF & b);
                while (temp.length() < 2)
                    temp = "0" + temp;
                sb.append(temp);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @NonNull
    public static String nonStr(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(new Random().nextInt(10));
        }
        return sb.toString();
    }
}
