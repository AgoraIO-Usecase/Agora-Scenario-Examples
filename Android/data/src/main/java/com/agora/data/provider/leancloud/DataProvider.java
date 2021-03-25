package com.agora.data.provider.leancloud;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.provider.IDataProvider;
import com.agora.data.provider.IMessageSource;
import com.agora.data.provider.IRoomProxy;
import com.agora.data.provider.IStoreSource;

public class DataProvider implements IDataProvider {

    private final IStoreSource mIStoreSource;
    private final IMessageSource mIMessageSource;

    public DataProvider(@NonNull Context mContext, @NonNull IRoomProxy iRoomProxy) {
        mIStoreSource = new StoreSource();
        mIMessageSource = new MessageSource(mContext, iRoomProxy);
    }

    @Override
    public IStoreSource getStoreSource() {
        return mIStoreSource;
    }

    @Override
    public IMessageSource getMessageSource() {
        return mIMessageSource;
    }
}
