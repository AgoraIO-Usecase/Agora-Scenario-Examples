package io.agora.scene.largeclass;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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

    /// 房间内用户列表
    private static final String SYNC_SCENE_ROOM_USER_COLLECTION = "agoraVoiceUsers";

    private static volatile RoomManager INSTANCE;
    private static volatile boolean isInitialized = false;

    private final Map<String, SceneReference> sceneMap = new HashMap<>();
    private final List<WrapEventListener> eventListeners = new ArrayList<>();
    private DataCallback<Exception> errorHandler;
    private final Map<String, UserInfo> roomLocalUserMap = new HashMap<>();

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

    private void notifyErrorHandler(Exception error) {
        Log.e(TAG, error.toString());
        if (errorHandler != null) {
            errorHandler.onObtained(error);
        }
    }

    public void init(Context context, String appId, DataCallback<Exception> error) {
        if (isInitialized) {
            return;
        }
        PreferenceUtil.init(context);
        isInitialized = true;
        errorHandler = error;
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("defaultChannel", "LargeClass");
        Sync.Instance().init(context, params, new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                isInitialized = false;
                notifyErrorHandler(new SyncManagerException(exception.getCode(), "init fail >> " + exception.getMessage()));
            }
        });
    }

    public void createRoom(String roomName, DataCallback<RoomInfo> success) {
        checkInitialized();
        RoomInfo roomInfo = new RoomInfo(roomName);
        roomInfo.userId = getCacheUserId();
        Scene room = new Scene();
        room.setId(roomInfo.roomId);
        room.setUserId(roomInfo.userId);
        room.setProperty(roomInfo.toMap());
        Sync.Instance().createScene(room, new Sync.Callback() {
            @Override
            public void onSuccess() {
                if (success != null) {
                    success.onObtained(roomInfo);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void getAllRooms(DataListCallback<RoomInfo> success) {
        checkInitialized();
        Sync.Instance().getScenes(new Sync.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                if (success != null) {
                    List<RoomInfo> ret = new ArrayList<>();

                    try {
                        for (IObject iObject : result) {
                            RoomInfo item = iObject.toObject(RoomInfo.class);
                            ret.add(item);
                        }
                        success.onObtained(ret);
                    } catch (Exception exception) {
                        notifyErrorHandler(exception);
                    }
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Log.e(TAG, "getAllRooms >> " + exception.toString());
                if (success != null) {
                    success.onObtained(new ArrayList<>());
                }
            }
        });
    }

    public void joinRoom(String roomId, Runnable successRun) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            Log.d(TAG, "The room of " + roomId + " has joined.");
            if (successRun != null) {
                successRun.run();
            }
            return;
        }
        Sync.Instance().joinScene(roomId, new Sync.JoinSceneCallback() {
            @Override
            public void onSuccess(SceneReference sceneReference) {
                sceneMap.put(roomId, sceneReference);
                if (successRun != null) {
                    successRun.run();
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                sceneMap.remove(roomId);
                notifyErrorHandler(exception);
            }
        });
    }

    public UserInfo getLocalUser(String roomId){
        return roomLocalUserMap.get(roomId);
    }


    public void loginLocalUser(String roomId, boolean isTeacher, DataCallback<UserInfo> success) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        UserInfo localUserInfo = new UserInfo();
        localUserInfo.status = isTeacher ? UserStatus.accept : UserStatus.none;
        localUserInfo.isEnableVideo = isTeacher;
        localUserInfo.isEnableAudio = isTeacher;
        roomLocalUserMap.put(roomId, localUserInfo);
        getRoomAllUsers(roomId, dataList -> {
            boolean containLocalUser = false;
            for (UserInfo userInfo : dataList) {
                if (userInfo.userId.equals(localUserInfo.userId)) {
                    localUserInfo.isEnableAudio = userInfo.isEnableAudio;
                    localUserInfo.isEnableVideo = userInfo.isEnableVideo;
                    localUserInfo.objectId = userInfo.objectId;
                    containLocalUser = true;
                    break;
                }
            }
            if (!containLocalUser) {
                sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                        .add(localUserInfo, new Sync.DataItemCallback() {
                            @Override
                            public void onSuccess(IObject result) {
                                localUserInfo.objectId = result.getId();
                                if (success != null) {
                                    success.onObtained(localUserInfo);
                                }
                            }

                            @Override
                            public void onFail(SyncManagerException exception) {
                                notifyErrorHandler(exception);
                            }
                        });
            } else {
                if (success != null) {
                    success.onObtained(localUserInfo);
                }
            }
        });
    }

    public void logoutLocalUser(String roomId) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        UserInfo localUserInfo = roomLocalUserMap.remove(roomId);
        if (localUserInfo == null || TextUtils.isEmpty(localUserInfo.objectId)) {
            return;
        }
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .delete(localUserInfo.objectId, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public boolean isLocalAudioEnable(String roomId) {
        UserInfo localUserInfo = roomLocalUserMap.get(roomId);
        if (localUserInfo == null) {
            return false;
        }
        return localUserInfo.isEnableAudio;
    }

    public boolean isLocalVideoEnable(String roomId) {
        UserInfo localUserInfo = roomLocalUserMap.get(roomId);
        if (localUserInfo == null) {
            return false;
        }
        return localUserInfo.isEnableVideo;
    }

    public void enableLocalAudio(String roomId, boolean enable) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        UserInfo localUserInfo = roomLocalUserMap.get(roomId);
        if (localUserInfo == null || TextUtils.isEmpty(localUserInfo.objectId)) {
            return;
        }
        localUserInfo.isEnableAudio = enable;
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .update(localUserInfo.objectId, localUserInfo, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void enableLocalVideo(String roomId, boolean enable) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        UserInfo localUserInfo = roomLocalUserMap.get(roomId);
        if (localUserInfo == null || TextUtils.isEmpty(localUserInfo.objectId)) {
            return;
        }
        localUserInfo.isEnableVideo = enable;
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .update(localUserInfo.objectId, localUserInfo, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void raiseHand(String roomId, boolean end) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        UserInfo localUserInfo = roomLocalUserMap.get(roomId);
        if (localUserInfo == null || TextUtils.isEmpty(localUserInfo.objectId)) {
            return;
        }
        localUserInfo.status = end ? UserStatus.end: UserStatus.request;
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .update(localUserInfo.objectId, localUserInfo, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void refuseRaiseHand(String roomId, UserInfo userInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        userInfo.status = UserStatus.refuse;
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .update(userInfo.objectId, userInfo, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void acceptRaiseHand(String roomId, UserInfo userInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        userInfo.status = UserStatus.accept;
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .update(userInfo.objectId, userInfo, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void getRoomAllUsers(String roomId, DataListCallback<UserInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION)
                .get(new Sync.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        if (callback != null) {
                            List<UserInfo> userInfos = new ArrayList<>();
                            for (IObject iObject : result) {
                                UserInfo e = iObject.toObject(UserInfo.class);
                                e.objectId = iObject.getId();
                                userInfos.add(e);
                            }
                            callback.onObtained(userInfos);
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                        if (callback != null) {
                            callback.onObtained(new ArrayList<>());
                        }
                    }
                });
    }

    public void subscribeUserChangeEvent(String roomId, DataCallback<UserInfo> addOrUpdate, DataCallback<UserInfo> delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(UserInfo.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (addOrUpdate != null) {
                    UserInfo data = item.toObject(UserInfo.class);
                    data.objectId = item.getId();
                    if(data.userId.equals(getCacheUserId())){
                        roomLocalUserMap.put(roomId, data);
                    }
                    addOrUpdate.onObtained(data);
                }
            }

            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                if (delete != null) {
                    delete.onObtained(item.toObject(UserInfo.class));
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.collection(SYNC_SCENE_ROOM_USER_COLLECTION).subscribe(listener);
    }


    public void subscribeRoomDeleteEvent(String roomId, DataCallback<String> delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                if (delete != null && item.getId().equals(roomId)) {
                    delete.onObtained(item.getId());
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe(listener);
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

    public void leaveRoom(String roomId, boolean delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            Iterator<WrapEventListener> iterator = eventListeners.iterator();
            while (iterator.hasNext()) {
                WrapEventListener next = iterator.next();
                if (next.roomId.equals(roomId)) {
                    sceneReference.unsubscribe(next);
                }
            }
            if (delete) {
                sceneReference.delete(new Sync.Callback() {
                    @Override
                    public void onSuccess() {
                        sceneMap.remove(roomId);
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
            } else {
                sceneReference.unsubscribe(null);
                sceneMap.remove(roomId);
            }
        }
    }

    public void destroy() {
        if (isInitialized) {
            Sync.Instance().destroy();
            errorHandler = null;
            isInitialized = false;
        }
    }

    private void checkInitialized() {
        if (!isInitialized) {
            throw new RuntimeException("The roomManager must be initialized firstly.");
        }
    }

    private static final Map<String, Integer> RoomBgResMap;

    static {
        RoomBgResMap = new HashMap<>();
        RoomBgResMap.put("portrait01", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait02", R.drawable.random_icon2_02);
        RoomBgResMap.put("portrait03", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait04", R.drawable.random_icon2_02);
        RoomBgResMap.put("portrait05", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait06", R.drawable.random_icon2_02);
        RoomBgResMap.put("portrait07", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait08", R.drawable.random_icon2_02);
        RoomBgResMap.put("portrait09", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait10", R.drawable.random_icon2_02);
        RoomBgResMap.put("portrait11", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait12", R.drawable.random_icon2_02);
        RoomBgResMap.put("portrait13", R.drawable.random_icon2_01);
        RoomBgResMap.put("portrait14", R.drawable.random_icon2_02);
    }

    public static class RoomInfo implements Serializable {
        public String roomName;
        public String roomId = getRandomRoomId();
        public String userId;
        public String backgroundId = String.format(Locale.US, "portrait%02d", RandomUtil.randomId(1, 14));

        public RoomInfo(String roomName) {
            this.roomName = roomName;
        }

        public int getAndroidBgId() {
            if (TextUtils.isEmpty(backgroundId)) {
                return 0;
            }
            int bgResId = R.drawable.user_profile_image_1;
            Integer id = RoomBgResMap.get(backgroundId);
            if (id != null) {
                bgResId = id;
            }
            return bgResId;
        }

        public Map<String, String> toMap() {
            HashMap<String, String> map = new HashMap<>();
            map.put("roomName", roomName);
            map.put("roomId", roomId);
            map.put("userId", userId);
            map.put("backgroundId", backgroundId);
            return map;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UserStatus.none, UserStatus.request, UserStatus.accept, UserStatus.refuse, UserStatus.end})
    public @interface UserStatus {
        int none = 0;
        // 请求上麦
        int request = 1;
        // 已接受
        int accept = 2;
        // 已拒绝
        int refuse = 3;
        // 已结束
        int end = 4;
    }

    public static class UserInfo {
        private String objectId;
        public String avatar = String.format(Locale.US, "portrait%02d", RandomUtil.randomId(1, 14));
        public String userId = getCacheUserId();
        public String userName = "User-" + userId;
        public @UserStatus
        int status = UserStatus.none;
        public String timestamp = System.currentTimeMillis() + "";
        public boolean isEnableVideo = true;
        public boolean isEnableAudio = true;

        public int getAvatarResId() {
            return RandomUtil.getIconById(userId);
        }
    }

    public interface DataListCallback<T> {
        void onObtained(List<T> dataList);
    }

    public interface DataCallback<T> {
        void onObtained(T data);
    }

    private class WrapEventListener implements Sync.EventListener {

        protected final String roomId;

        public WrapEventListener(String roomId) {
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
