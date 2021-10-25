package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/11
 */
public class GsonConverter extends Converter {

    private Gson mGson = new GsonBuilder()
            .create();

    @Override
    public <T> T toObject(@NonNull String json, @NonNull Class<T> valueType) {
        return mGson.fromJson(json, valueType);
    }
}
