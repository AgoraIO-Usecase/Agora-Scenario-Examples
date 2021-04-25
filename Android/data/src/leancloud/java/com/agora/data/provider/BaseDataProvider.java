package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.R;

import cn.leancloud.AVLogger;
import cn.leancloud.AVOSCloud;
import cn.leancloud.push.PushService;
import io.agora.baselibrary.BuildConfig;

public class BaseDataProvider implements IDataProvider {

    protected IConfigSource mIConfigSource;
    protected IStoreSource mIStoreSource;
    protected IMessageSource mIMessageSource;

    private Context mContext;
    private IRoomProxy iRoomProxy;

    public BaseDataProvider(@NonNull Context mContext, @NonNull IRoomProxy iRoomProxy) {
        this.mContext = mContext;
        this.iRoomProxy = iRoomProxy;

        if (BuildConfig.DEBUG) {
            AVOSCloud.setLogLevel(AVLogger.Level.DEBUG);
        } else {
            AVOSCloud.setLogLevel(AVLogger.Level.ERROR);
        }
        AVOSCloud.initialize(mContext, mContext.getString(R.string.leancloud_app_id),
                mContext.getString(R.string.leancloud_app_key),
                mContext.getString(R.string.leancloud_server_url));

        PushService.startIfRequired(mContext);

        initConfigSource();
        initStoreSource();
        initMessageSource();
    }

    @Override
    public void initConfigSource() {
        mIConfigSource = new DefaultConfigSource();
    }

    @Override
    public IConfigSource getConfigSource() {
        return mIConfigSource;
    }

    @Override
    public void initStoreSource() {
        mIStoreSource = new StoreSource(mIConfigSource);
    }

    @Override
    public IStoreSource getStoreSource() {
        return mIStoreSource;
    }

    @Override
    public void initMessageSource() {
        mIMessageSource = new MessageSource(mContext, iRoomProxy, mIConfigSource);
    }

    @Override
    public IMessageSource getMessageSource() {
        return mIMessageSource;
    }
}
