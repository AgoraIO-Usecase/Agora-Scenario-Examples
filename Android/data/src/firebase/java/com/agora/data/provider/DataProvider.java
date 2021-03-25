package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class DataProvider implements IDataProvider {

    public static final String TAG_TABLE_USER = "USER";
    public static final String TAG_TABLE_ROOM = "ROOM";
    public static final String TAG_TABLE_MEMBER = "MEMBER";
    public static final String TAG_TABLE_ACTION = "ACTION";

    private final IStoreSource mIStoreSource;
    private final IMessageSource mIMessageSource;

    public DataProvider(@NonNull Context mContext, @NonNull IRoomProxy iRoomProxy) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

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
