package io.agora.marriageinterview.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.provider.BaseDataProvider;
import com.agora.data.provider.IRoomProxy;

public class DataProvider extends BaseDataProvider {

    public DataProvider(@NonNull Context mContext, @NonNull IRoomProxy iRoomProxy) {
        super(mContext, iRoomProxy);
    }

    @Override
    public void initConfigSource() {
        mIConfigSource = new ConfigSource();
    }
}
