package io.agora.scene.pklivebycdn;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Scene;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;
import io.agora.uiwidget.utils.PreferenceUtil;
import io.agora.uiwidget.utils.RandomUtil;


public class RoomManager {
    private static final String TAG = "RoomManager";
    private static final String PREFERENCE_KEY_USER_ID = RoomManager.class.getName() + "_userId";
    public static final String COLLECTION_MEMBER = "member";
    public static final int PUSH_MODE_DIRECT_CDN = 1;
    public static final int PUSH_MODE_RTC = 2;

    private static volatile RoomManager INSTANCE;
    private static volatile boolean isInitialized = false;

    private final Map<String, SceneReference> sceneMap = new HashMap<>();
    private final List<WrapEventListener> eventListeners = new ArrayList<>();

    private DataCallback<Exception> errorHandler;
    private String localUserSyncId;

    public static RoomManager getInstance() {
        if (INSTANCE == null) {
            synchronized (RoomManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RoomManager();
                }
            }
        }
        return INSTANCE;
    }

    private RoomManager() {
    }

    private void notifyErrorHandler(@NonNull Exception e) {
        Log.e(TAG, e.toString());
        if (errorHandler != null) {
            errorHandler.onObtained(e);
        }
    }

    public void init(Context context, String appId, DataCallback<Exception> errorHandler) {
        if (isInitialized) {
            return;
        }
        PreferenceUtil.init(context);
        isInitialized = true;
        this.errorHandler = errorHandler;
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("defaultChannel", "PKByCDN");
        Sync.Instance().init(context, params, new Sync.Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFail(SyncManagerException exception) {
                isInitialized = false;
                notifyErrorHandler(exception);
            }
        });
    }

    public void getAllRooms(DataListCallback<RoomInfo> callback) {
        checkInitialized();
        Sync.Instance().getScenes(new Sync.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                if (callback != null) {
                    List<RoomInfo> ret = new ArrayList<>();

                    try {
                        for (IObject iObject : result) {
                            RoomInfo item = iObject.toObject(RoomInfo.class);
                            if (TextUtils.isEmpty(item.roomId)) {
                                item.roomId = iObject.getId();
                            }
                            ret.add(item);
                        }
                        callback.onObtained(ret);
                    } catch (Exception exception) {
                        notifyErrorHandler(exception);
                    }
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (callback != null) {
                    List<RoomInfo> ret = new ArrayList<>();
                    callback.onObtained(ret);
                }
                notifyErrorHandler(exception);
            }
        });
    }

    public void createRoom(String roomName, int pushMode, DataCallback<RoomInfo> callback) {
        checkInitialized();
        RoomInfo roomInfo = new RoomInfo(getRandomRoomId(), roomName, pushMode);
        Scene room = new Scene();
        room.setId(roomInfo.roomId);
        room.setUserId(getCacheUserId());
        room.setProperty(roomInfo.toMap());
        Sync.Instance().createScene(room, new Sync.Callback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onObtained(roomInfo);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void joinRoom(String roomId, boolean isHost, Runnable success) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            Log.d(TAG, "The room of " + roomId + " has joined.");
            return;
        }
        Sync.Instance().joinScene(roomId, new Sync.JoinSceneCallback() {
            @Override
            public void onSuccess(SceneReference sceneReference) {
                sceneMap.put(roomId, sceneReference);
                if (isHost) {
                    updatePKInfo(roomId, "", success);
                } else {
                    success.run();
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                sceneMap.remove(roomId);
                notifyErrorHandler(exception);
            }
        });
    }

    private void updatePKInfo(String roomId, String userIdPK, Runnable successRun) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        PKInfo pkInfo = new PKInfo();
        pkInfo.userIdPK = userIdPK;
        sceneReference.update("", pkInfo.toObjectMap(), new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                if (successRun != null) {
                    successRun.run();
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void leaveRoom(String roomId, boolean destroy) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }

        Iterator<WrapEventListener> iterator = eventListeners.iterator();
        while (iterator.hasNext()) {
            WrapEventListener next = iterator.next();
            if (next.roomId.equals(roomId)) {
                sceneReference.unsubscribe(next);
                iterator.remove();
            }
        }
        sceneMap.remove(roomId);

        if (destroy) {
            sceneReference.delete(new Sync.Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFail(SyncManagerException exception) {

                }
            });
        }
    }


    public void login(Context context, String roomId, DataCallback<UserInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.userId = getCacheUserId();
        userInfo.userName = RandomUtil.randomUserName(context);
        sceneReference.collection(COLLECTION_MEMBER)
                .add(userInfo.toMap(), new Sync.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {
                        localUserSyncId = result.getId();
                        if (callback != null) {
                            callback.onObtained(userInfo);
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void logout(String roomId) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        if (TextUtils.isEmpty(localUserSyncId)) {
            return;
        }
        sceneReference.collection(COLLECTION_MEMBER)
                .delete(localUserSyncId, new Sync.Callback() {
                    @Override
                    public void onSuccess() {
                        localUserSyncId = "";
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void getRoomUserList(String roomId, DataListCallback<UserInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        sceneReference.collection(COLLECTION_MEMBER)
                .get(new Sync.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        if (callback != null) {
                            List<UserInfo> list = new ArrayList<>();
                            for (IObject iObject : result) {
                                UserInfo userInfo = iObject.toObject(UserInfo.class);
                                list.add(userInfo);
                            }
                            callback.onObtained(list);
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        if (callback != null) {
                            callback.onObtained(new ArrayList<>());
                        }
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void startLinkWith(String roomId, String userId) {
        updatePKInfo(roomId, userId, null);
    }

    public void stopLink(String roomId) {
        updatePKInfo(roomId, "", null);
    }

    public void getRoomPKInfo(String roomId, DataCallback<PKInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        sceneReference.get("", new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                PKInfo pkInfo = result.toObject(PKInfo.class);
                if (callback != null) {
                    callback.onObtained(pkInfo);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void subscriptUserChangeEvent(String roomId, DataListCallback<UserInfo> change) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                getRoomUserList(roomId, change);
            }

            @Override
            public void onUpdated(IObject item) {
                getRoomUserList(roomId, change);
            }

            @Override
            public void onDeleted(IObject item) {
                getRoomUserList(roomId, change);
            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        };
        eventListeners.add(listener);
        sceneReference.collection(COLLECTION_MEMBER).subscribe(listener);
    }

    public void subscriptPKInfoEvent(String roomId, DataCallback<PKInfo> addOrUpdate, DataCallback<Boolean> destroy) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                PKInfo roomInfo = item.toObject(PKInfo.class);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(roomInfo);
                }
            }

            @Override
            public void onUpdated(IObject item) {
                PKInfo roomInfo = item.toObject(PKInfo.class);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(roomInfo);
                }
            }

            @Override
            public void onDeleted(IObject item) {
                String _roomId = item.getId();
                if (roomId.equals(_roomId) && destroy != null) {
                    destroy.onObtained(true);
                }
            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe("", listener);
    }


    public void destroy() {
        if (isInitialized) {
            Sync.Instance().destroy();
            isInitialized = false;
        }
    }

    private void checkInitialized() {
        if (!isInitialized) {
            throw new RuntimeException("The roomManager must be initialized firstly.");
        }
    }

    public static String getCacheUserId() {
        String userId = PreferenceUtil.get(PREFERENCE_KEY_USER_ID, "");
        if (TextUtils.isEmpty(userId)) {
            userId = RandomUtil.randomId() + 10000 + "";
            PreferenceUtil.put(PREFERENCE_KEY_USER_ID, userId);
        }
        return userId;
    }

    public static String getRandomRoomId() {
        return RandomUtil.randomId() + 10000 + "";
    }

    private static final List<Integer> ImageResIds = Arrays.asList(
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02,
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02,
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02,
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02,
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02,
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02,
            R.drawable.random_icon2_01,
            R.drawable.random_icon2_02
    );

    public static class RoomInfo implements Serializable {

        public String roomId;
        public String roomName;
        public int liveMode;

        public RoomInfo(String roomId, String roomName, int liveMode) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.liveMode = liveMode;
        }

        public HashMap<String, String> toMap() {
            HashMap<String, String> data = new HashMap<>();
            data.put("roomId", roomId);
            data.put("roomName", roomName);
            data.put("liveMode", liveMode + "");
            return data;
        }


        public int getBgResId() {
            if (TextUtils.isEmpty(roomId)) {
                return ImageResIds.get(0);
            }
            long id = 0;
            try {
                id = Long.parseLong(roomId);
            } catch (NumberFormatException e) {
                // do nothing
            }
            int index = (int) (id % ImageResIds.size());
            return ImageResIds.get(index);
        }

    }

    public static class PKInfo {
        public String userIdPK;

        public boolean isPKing() {
            return !TextUtils.isEmpty(userIdPK);
        }

        public HashMap<String, Object> toObjectMap() {
            HashMap<String, Object> ret = new HashMap<>();
            ret.put("userIdPK", userIdPK);
            return ret;
        }
    }

    public static class UserInfo {
        public String userId;
        public String userName;

        public HashMap<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("userName", userName);
            return map;
        }

        public int getUserIcon() {
            if (TextUtils.isEmpty(userId)) {
                return ImageResIds.get(0);
            }
            long id = 0;
            try {
                id = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                // do nothing
            }
            int index = (int) (id % ImageResIds.size());
            return ImageResIds.get(index);
        }
    }

    public interface DataListCallback<T> {
        void onObtained(List<T> dataList);
    }

    public interface DataCallback<T> {
        void onObtained(T data);
    }

    private class WrapEventListener implements Sync.EventListener {
        private final String roomId;

        WrapEventListener(String roomId) {
            this.roomId = roomId;
        }

        @Override
        public void onCreated(IObject item) {

        }

        @Override
        public void onUpdated(IObject item) {

        }

        @Override
        public void onDeleted(IObject item) {

        }

        @Override
        public void onSubscribeError(SyncManagerException ex) {
            notifyErrorHandler(ex);
        }
    }

}
