package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.R;

import cn.leancloud.AVLogger;
import cn.leancloud.AVOSCloud;
import cn.leancloud.push.PushService;
import io.agora.baselibrary.BuildConfig;

public class DataProvider implements IDataProvider {

    private IStoreSource mIStoreSource;
    private IMessageSource mIMessageSource;

    public DataProvider(@NonNull Context mContext, @NonNull IRoomProxy iRoomProxy) {
        if (BuildConfig.DEBUG) {
            AVOSCloud.setLogLevel(AVLogger.Level.DEBUG);
        } else {
            AVOSCloud.setLogLevel(AVLogger.Level.ERROR);
        }
        AVOSCloud.initialize(mContext, mContext.getString(R.string.leancloud_app_id),
                mContext.getString(R.string.leancloud_app_key),
                mContext.getString(R.string.leancloud_server_url));

        PushService.startIfRequired(mContext);

        IConfigSource mIConfigSource = new DefaultConfigSource();
        mIStoreSource = new StoreSource(mIConfigSource);
        mIMessageSource = new MessageSource(mContext, iRoomProxy, mIConfigSource);
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
