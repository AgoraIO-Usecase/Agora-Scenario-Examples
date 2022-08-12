package io.agora.scene.multicall;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

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
    private static final String SYNC_MANAGER_GIFT_INFO = "giftInfo";
    private static final String SYNC_MANAGER_USER_INFO_LIST = "agoraVoiceUsers";
    private static final String SYNC_MANAGER_MESSAGE_INFO = "messageInfo";
    private static final int ROOM_MAX_USER = 4;

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

    public void init(Context context, String appId, String token, DataCallback<Exception> errorHandler) {
        if (isInitialized) {
            return;
        }
        PreferenceUtil.init(context);
        localUserInfo = new UserInfo();
        isInitialized = true;
        this.errorHandler = errorHandler;
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("token", token);
        params.put("defaultChannel", "mutli");
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

    public void createRoom(RoomInfo roomInfo, DataCallback<RoomInfo> callback) {
        checkInitialized();
        roomInfo.userId = getCacheUserId();
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
                        Log.e(TAG, "", exception);
                    }
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Log.e(TAG, "", exception);
                if (callback != null) {
                    callback.onObtained(new ArrayList<>());
                }
                notifyErrorHandler(exception);
            }
        });
    }

    public void joinRoom(String roomId, DataCallback<String> successRun, DataCallback<Exception> failure) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        localUserInfo = new UserInfo();
        if (sceneReference != null) {
            Log.d(TAG, "The room of " + roomId + " has joined.");
            if(successRun != null){
                successRun.onObtained(roomId);
            }
            return;
        }
        Sync.Instance().joinScene(roomId, new Sync.JoinSceneCallback() {
            @Override
            public void onSuccess(SceneReference sceneReference) {
                sceneMap.put(roomId, sceneReference);
                if(successRun != null){
                    successRun.onObtained(roomId);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                sceneMap.remove(roomId);
                if (failure != null) {
                    failure.onObtained(exception);
                }
            }
        });
    }

    public @NonNull UserInfo getLocalUserInfo() {
        return localUserInfo;
    }

    public void inviteUser(String roomId, UserInfo userInfo){
        updateUserStatus(roomId, userInfo, Status.INVITING);
    }

    public void acceptUser(String roomId, UserInfo userInfo){
        updateUserStatus(roomId, userInfo, Status.ACCEPT);
    }

    public void refuseUser(String roomId, UserInfo userInfo){
        updateUserStatus(roomId, userInfo, Status.REFUSE);
        deleteUserInfo(roomId, userInfo);
    }

    public void endUser(String roomId, UserInfo userInfo){
        updateUserStatus(roomId, userInfo, Status.END);
        deleteUserInfo(roomId, userInfo);
    }

    private void deleteUserInfo(String roomId, UserInfo userInfo){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO_LIST)
                .delete(userInfo.objectId, new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    private void updateUserStatus(String roomId, UserInfo userInfo, @Status int status) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        UserInfo _userInfo = new UserInfo();
        _userInfo.objectId = userInfo.objectId;
        _userInfo.userId = userInfo.userId;
        _userInfo.userName = userInfo.userName;
        _userInfo.avatar = userInfo.avatar;
        _userInfo.status = status;
        _userInfo.isEnableVideo = userInfo.isEnableVideo;
        _userInfo.isEnableAudio = userInfo.isEnableAudio;
        sceneReference.collection(SYNC_MANAGER_USER_INFO_LIST)
                .update(userInfo.objectId, _userInfo, new Sync.Callback() {
                    @Override
                    public void onSuccess() {
                        if(_userInfo.userId.equals(getCacheUserId())){
                            localUserInfo = _userInfo;
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void leaveRoom(String roomId) {
        localUserInfo = null;
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        Iterator<WrapEventListener> iterator = eventListeners.iterator();
        while (iterator.hasNext()){
            WrapEventListener next = iterator.next();
            if(next.roomId.equals(roomId)){
                iterator.remove();
                sceneReference.unsubscribe(next);
            }
        }
        sceneMap.remove(roomId);
    }

    public void destroyRoom(String roomId){
        localUserInfo = null;
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }

        Iterator<WrapEventListener> iterator = eventListeners.iterator();
        while (iterator.hasNext()){
            WrapEventListener next = iterator.next();
            if(next.roomId.equals(roomId)){
                iterator.remove();
                sceneReference.unsubscribe(next);
            }
        }

        sceneMap.remove(roomId);

        sceneReference.collection(SYNC_MANAGER_USER_INFO_LIST).delete(new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });

        sceneReference.delete(new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void destroy(){
        if(isInitialized){
            Sync.Instance().destroy();
            isInitialized = false;
        }
    }

    public void getUserList(String roomId, DataListCallback<UserInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.collection(SYNC_MANAGER_USER_INFO_LIST).get(new Sync.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                if (callback != null) {
                    List<UserInfo> userInfos = new ArrayList<>();
                    if (result != null && result.size() > 0) {
                        for (IObject iObject : result) {
                            UserInfo userInfo = iObject.toObject(UserInfo.class);
                            userInfo.objectId = iObject.getId();
                            userInfos.add(userInfo);
                        }
                    }
                    callback.onObtained(userInfos);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Log.e(TAG, "", exception);
                if (exception.getMessage().contains("empty")) {
                    if (callback != null) {
                        callback.onObtained(new ArrayList<>());
                    }
                }
            }
        });
    }

    public void subscribeUserChangeEvent(String roomId,
                                         DataCallback<UserInfo> addOrUpdateCallback,
                                         DataCallback<String> deleteCallback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId){
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                UserInfo userInfo = item.toObject(UserInfo.class);
                userInfo.objectId = item.getId();
                if(userInfo.userId.equals(getCacheUserId())){
                    localUserInfo = userInfo;
                }
                if (addOrUpdateCallback != null) {
                    addOrUpdateCallback.onObtained(userInfo);
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                UserInfo userInfo = item.toObject(UserInfo.class);
                userInfo.objectId = item.getId();
                if(userInfo.userId.equals(getCacheUserId())){
                    localUserInfo = userInfo;
                }
                if (addOrUpdateCallback != null) {
                    addOrUpdateCallback.onObtained(userInfo);
                }
            }

            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                if (deleteCallback != null) {
                    deleteCallback.onObtained(item.getId());
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.collection(SYNC_MANAGER_USER_INFO_LIST)
                .subscribe(listener);
    }

    public void sendGift(String roomId, GiftInfo giftInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.update(SYNC_MANAGER_GIFT_INFO, giftInfo, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void sendMessage(String roomId, MessageInfo messageInfo){
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

            }
        });
    }

    public void subscribeRoomDeleteEvent(String roomId, DataCallback<String> deleted){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId){
            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                if(deleted != null){
                    deleted.onObtained(item.getId());
                }
            }
        };
        sceneReference.subscribe(listener);
    }

    public void subscribeMessageReceiveEvent(String roomId, DataCallback<MessageInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId){
            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                MessageInfo messageInfo = item.toObject(MessageInfo.class);
                if (callback != null) {
                    callback.onObtained(messageInfo);
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe(SYNC_MANAGER_MESSAGE_INFO, listener);
    }

    public void subscribeGiftReceiveEvent(String roomId, DataCallback<GiftInfo> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId){
            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                GiftInfo giftInfo = item.toObject(GiftInfo.class);
                if (callback != null) {
                    callback.onObtained(giftInfo);
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe(SYNC_MANAGER_GIFT_INFO, listener);
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

    private void checkInitialized() {
        if (!isInitialized) {
            throw new RuntimeException("The roomManager must be initialized firstly.");
        }
    }

    private static final Map<String, Integer> IconNameResMap;
    private static final Map<String, Integer> GifNameResMap;
    private static final Map<String, Integer> RoomBgResMap;

    static {
        IconNameResMap = new HashMap<>();
        IconNameResMap.put("gift-dang", R.drawable.gift_01_bell);
        IconNameResMap.put("gift-icecream", R.drawable.gift_02_icecream);
        IconNameResMap.put("gift-wine", R.drawable.gift_03_wine);
        IconNameResMap.put("gift-cake", R.drawable.gift_04_cake);
        IconNameResMap.put("gift-ring", R.drawable.gift_05_ring);
        IconNameResMap.put("gift-watch", R.drawable.gift_06_watch);
        IconNameResMap.put("gift-diamond", R.drawable.gift_07_diamond);
        IconNameResMap.put("gift-rocket", R.drawable.gift_08_rocket);

        GifNameResMap = new HashMap<>();
        GifNameResMap.put("SuperBell", R.drawable.gift_anim_bell);
        GifNameResMap.put("SuperIcecream", R.drawable.gift_anim_icecream);
        GifNameResMap.put("SuperWine", R.drawable.gift_anim_wine);
        GifNameResMap.put("SuperCake", R.drawable.gift_anim_cake);
        GifNameResMap.put("SuperRing", R.drawable.gift_anim_ring);
        GifNameResMap.put("SuperWatch", R.drawable.gift_anim_watch);
        GifNameResMap.put("SuperDiamond", R.drawable.gift_anim_diamond);
        GifNameResMap.put("SuperRocket", R.drawable.gift_anim_rocket);

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
        public String videoUrl = "";

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
            map.put("videoUrl", videoUrl);
            return map;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RoomType.SINGLE_HOST, RoomType.MULTI_HOST})
    public @interface RoomType {
        int SINGLE_HOST = 1;
        int MULTI_HOST = 2;
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

    public static class GiftInfo implements Serializable {
        public String iconName;
        public String title;
        public int coin;
        public String gifName;
        public String userId;
        public String objectId = SYNC_MANAGER_GIFT_INFO;
        public int giftType = 5;

        public void setIconNameById(@DrawableRes int iconRes) {
            for (Map.Entry<String, Integer> entry : IconNameResMap.entrySet()) {
                if (entry.getValue() == iconRes) {
                    iconName = entry.getKey();
                }
            }
        }

        public void setGifNameById(@DrawableRes int gifRes) {
            for (Map.Entry<String, Integer> entry : GifNameResMap.entrySet()) {
                if (entry.getValue() == gifRes) {
                    gifName = entry.getKey();
                }
            }
        }

        public int getIconId() {
            Integer ret = IconNameResMap.get(iconName);
            if (ret == null) {
                ret = 0;
            }
            return ret;
        }

        public int getGifId() {
            Integer ret = GifNameResMap.get(gifName);
            if (ret == null) {
                ret = 0;
            }
            return ret;
        }

    }

    public static class UserInfo {
        public String objectId;
        public String avatar = String.format(Locale.US, "portrait%02d", RandomUtil.randomId(1, 14));
        public String userId = getCacheUserId();
        public String userName = "User-" + userId;
        public @Status
        int status = Status.END;
        public boolean isEnableVideo = false;
        public boolean isEnableAudio = false;
        public String timestamp = System.currentTimeMillis() + "";

        public int getAvatarResId() {
            //Integer ret = RoomBgResMap.get(avatar);
            //if (ret == null) {
            //    ret = 0;
            //}
            //return ret;
            return RandomUtil.getIconById(userId);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Status.INVITING, Status.ACCEPT, Status.REFUSE, Status.END})
    public @interface Status {
        // 邀请中
        int INVITING = 1;
        // 已接受
        int ACCEPT = 2;
        // 已拒绝
        int REFUSE = 3;
        // 已结束
        int END = 4;
    }


    public interface DataListCallback<T> {
        void onObtained(@NonNull List<T> dataList);
    }

    public interface DataCallback<T> {
        void onObtained(@NonNull T data);
    }

    private class WrapEventListener implements Sync.EventListener{

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
