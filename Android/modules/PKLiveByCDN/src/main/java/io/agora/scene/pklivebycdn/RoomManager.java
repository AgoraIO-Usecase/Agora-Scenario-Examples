package io.agora.scene.pklivebycdn;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    private final Map<String, List<Runnable>> roomJoinedRuns = new HashMap<>();
    private final Map<String, List<Sync.EventListener>> roomEventListeners = new HashMap<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String localUserSyncId = "";

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

    public void init(Context context, String appId, String token) {
        if (isInitialized) {
            return;
        }
        PreferenceUtil.init(context);
        isInitialized = true;
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("token", token);
        params.put("defaultChannel", "PKByCDN");
        Sync.Instance().init(context, params, new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                isInitialized = false;
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
                            if(TextUtils.isEmpty(item.roomId)){
                                item.roomId = iObject.getId();
                            }
                            ret.add(item);
                        }
                        mainHandler.post(() -> callback.onObtained(ret));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if(exception.getMessage().contains("empty")){
                    if (callback != null) {
                        List<RoomInfo> ret = new ArrayList<>();
                        mainHandler.post(() -> callback.onObtained(ret));
                    }
                }
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
                    mainHandler.post(() -> callback.onObtained(roomInfo));
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void joinRoom(String roomId, boolean isHost) {
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
                Runnable successRun = () -> {
                    List<Runnable> runnables = roomJoinedRuns.get(roomId);
                    if (runnables != null) {
                        for (Runnable runnable : runnables) {
                            mainHandler.post(runnable);
                        }
                        runnables.clear();
                    }
                };
                if(isHost){
                    updatePKInfo(roomId, "", successRun);
                }else{
                    successRun.run();
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                sceneMap.remove(roomId);
            }
        });
    }

    private void updatePKInfo(String roomId, String userIdPK, Runnable successRun) {
        checkInitialized();
        doOnRoomJoined(roomId, () -> {
            SceneReference sceneReference = sceneMap.get(roomId);
            if(sceneReference == null){
                return;
            }
            PKInfo pkInfo = new PKInfo();
            pkInfo.userIdPK = userIdPK;
            sceneReference.update(pkInfo.toObjectMap(), new Sync.DataItemCallback() {
                @Override
                public void onSuccess(IObject result) {
                    if(successRun != null){
                        successRun.run();
                    }
                }

                @Override
                public void onFail(SyncManagerException exception) {

                }
            });
        });
    }

    public void leaveRoom(String roomId, boolean destroy) {
        checkInitialized();
        roomJoinedRuns.remove(roomId);
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            if(destroy){
                sceneReference.delete(new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {

                    }
                });
            }
            List<Sync.EventListener> eventListeners = roomEventListeners.get(roomId);
            if (eventListeners != null) {
                for (Sync.EventListener eventListener : eventListeners) {
                    sceneReference.unsubscribe(eventListener);
                }
            }
        }
        roomEventListeners.remove(roomId);
        mainHandler.removeCallbacksAndMessages(null);
        sceneMap.remove(roomId);
    }


    public void localUserEnterRoom(Context context, String roomId, DataCallback<UserInfo> callback) {
        checkInitialized();
        doOnRoomJoined(roomId, new Runnable() {
            @Override
            public void run() {
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
                                if(callback != null){
                                    mainHandler.post(() -> callback.onObtained(userInfo));
                                }
                            }

                            @Override
                            public void onFail(SyncManagerException exception) {

                            }
                        });
            }
        });
    }

    public void localUserExitRoom(String roomId) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            Log.e(TAG, "The room has not joined. roomId=" + roomId);
            return;
        }
        sceneReference.collection(COLLECTION_MEMBER)
                .delete(localUserSyncId, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {

                    }
                });
    }

    public void getRoomUserList(String roomId, DataListCallback<UserInfo> callback) {
        checkInitialized();
        doOnRoomJoined(roomId, new Runnable() {
            @Override
            public void run() {
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
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            callback.onObtained(list);
                                        }
                                    });

                                }
                            }

                            @Override
                            public void onFail(SyncManagerException exception) {

                            }
                        });
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
        doOnRoomJoined(roomId, new Runnable() {
            @Override
            public void run() {
                SceneReference sceneReference = sceneMap.get(roomId);
                if (sceneReference == null) {
                    Log.e(TAG, "The room has not joined. roomId=" + roomId);
                    return;
                }
                sceneReference.get(new Sync.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {

                        PKInfo pkInfo = result.toObject(PKInfo.class);
                        if (callback != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onObtained(pkInfo);
                                }
                            });
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {

                    }
                });
            }
        });
    }

    public void subscriptRoomInfoEvent(String roomId, DataCallback<PKInfo> addOrUpdate, DataCallback<Boolean> destroy) {
        checkInitialized();
        doOnRoomJoined(roomId, new Runnable() {
            @Override
            public void run() {
                SceneReference sceneReference = sceneMap.get(roomId);
                if (sceneReference == null) {
                    Log.e(TAG, "The room has not joined. roomId=" + roomId);
                    return;
                }
                Sync.EventListener listener = new Sync.EventListener() {
                    @Override
                    public void onCreated(IObject item) {

                    }

                    @Override
                    public void onUpdated(IObject item) {
                        PKInfo roomInfo = item.toObject(PKInfo.class);
                        if (addOrUpdate != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    addOrUpdate.onObtained(roomInfo);
                                }
                            });
                        }
                    }

                    @Override
                    public void onDeleted(IObject item) {
                        String _roomId = item.getId();
                        if (roomId.equals(_roomId) && destroy != null) {
                            mainHandler.post(() -> destroy.onObtained(true));
                        }
                    }

                    @Override
                    public void onSubscribeError(SyncManagerException ex) {

                    }
                };
                List<Sync.EventListener> eventListeners = roomEventListeners.get(roomId);
                if (eventListeners == null) {
                    eventListeners = new ArrayList<>();
                    roomEventListeners.put(roomId, eventListeners);
                }
                eventListeners.add(listener);
                sceneReference.subscribe(listener);
            }
        });
    }


    private void doOnRoomJoined(String roomId, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        List<Runnable> runnables = roomJoinedRuns.get(roomId);
        if (runnables == null) {
            runnables = new ArrayList<>();
            roomJoinedRuns.put(roomId, runnables);
        }
        if (sceneMap.get(roomId) == null) {
            runnables.add(runnable);
        } else {
            mainHandler.post(runnable);
        }
    }

    public void destroy(){
        if(isInitialized){
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

        public boolean isPKing(){
            return !TextUtils.isEmpty(userIdPK);
        }

        public HashMap<String, Object> toObjectMap(){
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

}
