package io.agora.scene.voice;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final String SYNC_MANAGER_USER_INFO = "agoraVoiceUsers";
    private static final String SYNC_MANAGER_MESSAGE_INFO = "agoraVoiceMessages";

    private static volatile RoomManager INSTANCE;
    private static volatile boolean isInitialized = false;

    private final Map<String, SceneReference> sceneMap = new HashMap<>();
    private final List<WrapEventListener> eventListeners = new ArrayList<>();
    private DataCallback<Exception> errorHandler;
    private UserInfo localUserInfo;

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

    public UserInfo getLocalUserInfo() {
        return localUserInfo;
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
        params.put("defaultChannel", "agoraVoice");
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

    public void createRoom(String roomName, int bgImgRes, DataCallback<RoomInfo> callback) {
        checkInitialized();
        RoomInfo roomInfo = new RoomInfo(roomName);
        roomInfo.userId = getCacheUserId();
        roomInfo.setBackgroundId(bgImgRes);
        Scene room = new Scene();
        room.setId(roomInfo.roomId);
        room.setUserId(roomInfo.userId);
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

    public void updateRoom(RoomInfo roomInfo) {
        checkInitialized();
        String roomId = roomInfo.roomId;
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("roomName", roomInfo.roomName);
        map.put("roomId", roomInfo.roomId);
        map.put("userId", roomInfo.userId);
        map.put("backgroundId", roomInfo.backgroundId);
        sceneReference.update(map, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {
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
                    callback.onObtained(new ArrayList<>());
                }
                notifyErrorHandler(exception);
            }
        });
    }

    public void joinRoom(String roomId, DataListCallback<UserInfo> successRun) {
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
                getUserInfoList(roomId, dataList -> {
                    for (UserInfo userInfo : dataList) {
                        if (userInfo.userId.equals(getCacheUserId())) {
                            localUserInfo = userInfo;
                            break;
                        }
                    }
                    if (localUserInfo != null) {
                        if (successRun != null) {
                            successRun.onObtained(dataList);
                        }
                        return;
                    }

                    localUserInfo = new UserInfo();
                    dataList.add(localUserInfo);
                    sceneReference.collection(SYNC_MANAGER_USER_INFO).add(
                            localUserInfo, new Sync.DataItemCallback() {
                                @Override
                                public void onSuccess(IObject result) {
                                    localUserInfo.objectId = result.getId();
                                    if (successRun != null) {
                                        successRun.onObtained(dataList);
                                    }
                                }

                                @Override
                                public void onFail(SyncManagerException exception) {
                                    notifyErrorHandler(exception);
                                }
                            }
                    );
                });

            }

            @Override
            public void onFail(SyncManagerException exception) {
                sceneMap.remove(roomId);
                notifyErrorHandler(exception);
            }
        });
    }

    public void getUserInfoList(String roomId, DataListCallback<UserInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO).get(new Sync.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                if (callback != null) {
                    List<UserInfo> list = new ArrayList<>();
                    for (IObject iObject : result) {
                        UserInfo item = iObject.toObject(UserInfo.class);
                        item.objectId = iObject.getId();
                        list.add(item);
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

    public void inviteUser(String roomId, UserInfo userInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        if (localUserInfo != null && localUserInfo.userId.equals(userInfo.userId)) {
            localUserInfo.status = UserStatus.invite;
            localUserInfo.timestamp = System.currentTimeMillis() + "";
            userInfo = localUserInfo;
        } else {
            userInfo.status = UserStatus.invite;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO)
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

    public void acceptInvite(String roomId, UserInfo userInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        if (localUserInfo != null && localUserInfo.userId.equals(userInfo.userId)) {
            localUserInfo.status = UserStatus.accept;
            userInfo = localUserInfo;
        } else {
            userInfo.status = UserStatus.accept;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO)
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

    public void refuseInvite(String roomId, UserInfo userInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        if (localUserInfo != null && localUserInfo.userId.equals(userInfo.userId)) {
            localUserInfo.status = UserStatus.refuse;
            userInfo = localUserInfo;
        } else {
            userInfo.status = UserStatus.refuse;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO)
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

    public void endUser(String roomId, UserInfo userInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        if (localUserInfo != null && localUserInfo.userId.equals(userInfo.userId)) {
            localUserInfo.status = UserStatus.end;
            userInfo = localUserInfo;
        } else {
            userInfo.status = UserStatus.end;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO)
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

    public void enableLocalAudio(String roomId, boolean enable) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        localUserInfo.isEnableAudio = enable;
        sceneReference.collection(SYNC_MANAGER_USER_INFO).update(localUserInfo.objectId, localUserInfo, new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void sendMessage(String roomId, MessageInfo messageInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.update(SYNC_MANAGER_MESSAGE_INFO, messageInfo, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void subscribeMessageEvent(String roomId, DataCallback<MessageInfo> addOrUpdate) {
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
                    addOrUpdate.onObtained(item.toObject(MessageInfo.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(MessageInfo.class));
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.collection(SYNC_MANAGER_MESSAGE_INFO).subscribe(listener);
    }

    public void subscribeUserInfoEvent(String roomId, DataCallback<UserInfo> addOrUpdate, DataCallback<String> delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = null;
        if (addOrUpdate != null) {
            listener = new WrapEventListener(roomId) {
                @Override
                public void onCreated(IObject item) {
                    super.onCreated(item);
                    UserInfo userInfo = item.toObject(UserInfo.class);
                    userInfo.objectId = item.getId();
                    addOrUpdate.onObtained(userInfo);
                }

                @Override
                public void onUpdated(IObject item) {
                    UserInfo userInfo = item.toObject(UserInfo.class);
                    userInfo.objectId = item.getId();
                    addOrUpdate.onObtained(userInfo);
                }

                @Override
                public void onDeleted(IObject item) {
                    super.onDeleted(item);
                    if (delete != null) {
                        delete.onObtained(item.getId());
                    }
                }
            };
            eventListeners.add(listener);
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO).subscribe(listener);
    }

    public void subscribeRoomEvent(String roomId, DataCallback<RoomInfo> update, DataCallback<String> delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = null;
        if (delete != null) {
            listener = new WrapEventListener(roomId) {
                @Override
                public void onUpdated(IObject item) {
                    super.onUpdated(item);
                    RoomInfo roomInfo = item.toObject(RoomInfo.class);
                    if (update != null) {
                        update.onObtained(roomInfo);
                    }
                }

                @Override
                public void onDeleted(IObject item) {
                    super.onDeleted(item);
                    if (roomId.equals(item.getId())) {
                        delete.onObtained(item.getId());
                    }
                }

                @Override
                public void onSubscribeError(SyncManagerException ex) {
                    Log.d(TAG, "getAllRooms onFail error=" + ex.toString());
                }
            };
            eventListeners.add(listener);
        }
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
                if (localUserInfo != null) {
                    sceneReference.collection(SYNC_MANAGER_USER_INFO).delete(localUserInfo.objectId, new Sync.Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFail(SyncManagerException exception) {
                            notifyErrorHandler(exception);
                        }
                    });
                    localUserInfo = null;
                }
                sceneReference.unsubscribe(null);
                sceneMap.remove(roomId);
            }
        }
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

    private static final Map<String, Integer> RoomBgResMap;
    private static final Map<String, Integer> UserAvatarResMap;

    static {

        RoomBgResMap = new HashMap<>();
        RoomBgResMap.put("BG01", R.drawable.voice_room_bg_big_1);
        RoomBgResMap.put("BG02", R.drawable.voice_room_bg_big_2);
        RoomBgResMap.put("BG03", R.drawable.voice_room_bg_big_3);
        RoomBgResMap.put("BG04", R.drawable.voice_room_bg_big_4);
        RoomBgResMap.put("BG05", R.drawable.voice_room_bg_big_5);
        RoomBgResMap.put("BG06", R.drawable.voice_room_bg_big_6);
        RoomBgResMap.put("BG07", R.drawable.voice_room_bg_big_7);
        RoomBgResMap.put("BG08", R.drawable.voice_room_bg_big_8);
        RoomBgResMap.put("BG09", R.drawable.voice_room_bg_big_9);

        UserAvatarResMap = new HashMap<>();
        UserAvatarResMap.put("portrait01", R.drawable.user_profile_image_1);
        UserAvatarResMap.put("portrait02", R.drawable.user_profile_image_2);
        UserAvatarResMap.put("portrait03", R.drawable.user_profile_image_3);
        UserAvatarResMap.put("portrait04", R.drawable.user_profile_image_4);
        UserAvatarResMap.put("portrait05", R.drawable.user_profile_image_5);
        UserAvatarResMap.put("portrait06", R.drawable.user_profile_image_6);
        UserAvatarResMap.put("portrait07", R.drawable.user_profile_image_7);
        UserAvatarResMap.put("portrait08", R.drawable.user_profile_image_8);
        UserAvatarResMap.put("portrait09", R.drawable.user_profile_image_9);
        UserAvatarResMap.put("portrait10", R.drawable.user_profile_image_10);
        UserAvatarResMap.put("portrait11", R.drawable.user_profile_image_11);
        UserAvatarResMap.put("portrait12", R.drawable.user_profile_image_12);
        UserAvatarResMap.put("portrait13", R.drawable.user_profile_image_13);
        UserAvatarResMap.put("portrait14", R.drawable.user_profile_image_14);
    }


    public static class RoomInfo implements Serializable {
        public String roomName;
        public String roomId = getRandomRoomId();
        public String userId;
        public String backgroundId = String.format(Locale.US, "BG%02d", RandomUtil.randomId(1, 14));

        public RoomInfo(String roomName) {
            this.roomName = roomName;
        }

        public void setBackgroundId(int imgRes) {
            Set<Map.Entry<String, Integer>> entries = RoomBgResMap.entrySet();
            for (Map.Entry<String, Integer> entry : entries) {
                if (entry.getValue() == imgRes) {
                    backgroundId = entry.getKey();
                    break;
                }
            }
        }

        public int getAndroidBgId() {
            int bgResId = R.drawable.voice_room_bg_big_1;
            if (TextUtils.isEmpty(backgroundId)) {
                return bgResId;
            }
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

    public static class MessageInfo implements Serializable {
        public String userName;
        public String content;
        public @DrawableRes
        int giftIcon = View.NO_ID;

        public MessageInfo(String userName, String content) {
            this.userName = userName;
            this.content = content;
        }

        public MessageInfo(String userName, String content, int giftIcon) {
            this.userName = userName;
            this.content = content;
            this.giftIcon = giftIcon;
        }

    }

    public static final class UserInfo {
        public String userName = "User-" + getCacheUserId();
        public String avatar = String.format(Locale.US, "portrait%02d", RandomUtil.randomId(1, 14));
        public String userId = getCacheUserId();
        public @UserStatus
        int status = UserStatus.end;
        public String timestamp = System.currentTimeMillis() + "";
        public boolean isEnableVideo = false;
        public boolean isEnableAudio = true;
        public String objectId = "";

        public int getAvatarImgResId() {
            int bgResId = R.drawable.user_profile_image_1;
            if (TextUtils.isEmpty(avatar)) {
                return bgResId;
            }
            Integer id = UserAvatarResMap.get(avatar);
            if (id != null) {
                bgResId = id;
            }
            return bgResId;
        }
    }

    public @interface UserStatus {
        int invite = 1;
        int accept = 2;
        int refuse = 3;
        int end = 4;
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
