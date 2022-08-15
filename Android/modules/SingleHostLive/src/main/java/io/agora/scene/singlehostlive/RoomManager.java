package io.agora.scene.singlehostlive;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;

import java.io.Serializable;
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
    private static final String SYNC_MANAGER_USER_COLLECTION = "userCollection";
    private static final String SYNC_MANAGER_GIFT_INFO = "giftInfo";
    private static final String SYNC_MANAGER_MESSAGE_INFO = "messageInfo";

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

    private void notifyErrorHandler(Exception error){
        Log.e(TAG, error.toString());
        if(errorHandler != null){
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
        params.put("token", token);
        params.put("defaultChannel", "signleLive");
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
                if(success != null){
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
                login(roomId);
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

    public void login(String roomId){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        localUserInfo = new UserInfo();
        sceneReference.collection(SYNC_MANAGER_USER_COLLECTION)
                .add(localUserInfo, new Sync.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {
                        localUserInfo.objectId = result.getId();
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void logout(String roomId){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        if(localUserInfo == null){
            return;
        }
        sceneReference.collection(SYNC_MANAGER_USER_COLLECTION)
                .delete(localUserInfo.objectId, new Sync.Callback() {
                    @Override
                    public void onSuccess() {
                        localUserInfo = null;
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });

    }

    public void getRoomUserList(String roomId, DataListCallback<UserInfo> success){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.collection(SYNC_MANAGER_USER_COLLECTION)
                .get(new Sync.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        if(success != null){
                            List<UserInfo> list = new ArrayList<>();
                            for (IObject iObject : result) {
                                UserInfo item = iObject.toObject(UserInfo.class);
                                item.objectId = iObject.getId();
                                list.add(item);
                            }
                            success.onObtained(list);
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        if (success != null) {
                            success.onObtained(new ArrayList<>());
                        }
                        notifyErrorHandler(exception);
                    }
                });
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
                notifyErrorHandler(exception);
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
                notifyErrorHandler(exception);
            }
        });
    }

    public void subscribeUserChangeEvent(String roomId, DataListCallback<UserInfo> userChange) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                getRoomUserList(roomId, userChange);
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                getRoomUserList(roomId, userChange);
            }

            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                getRoomUserList(roomId, userChange);
            }
        };
        eventListeners.add(listener);
        sceneReference.collection(SYNC_MANAGER_USER_COLLECTION).subscribe(listener);
    }

    public void subscribeMessageReceiveEvent(String roomId, DataCallback<MessageInfo> addOrUpdate){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if(addOrUpdate != null){
                    addOrUpdate.onObtained(item.toObject(MessageInfo.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                if(addOrUpdate != null){
                    addOrUpdate.onObtained(item.toObject(MessageInfo.class));
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe(SYNC_MANAGER_MESSAGE_INFO, listener);
    }

    public void subscribeGiftReceiveEvent(String roomId, DataCallback<GiftInfo> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapEventListener listener = new WrapEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if(addOrUpdate != null){
                    addOrUpdate.onObtained(item.toObject(GiftInfo.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                if(addOrUpdate != null){
                    addOrUpdate.onObtained(item.toObject(GiftInfo.class));
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe(SYNC_MANAGER_GIFT_INFO, listener);
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
                if (roomId.equals(item.getId()) && delete != null) {
                    delete.onObtained(item.getId());
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.subscribe(listener);
    }

    public UserInfo getLocalUserInfo() {
        if(localUserInfo == null){
            return new UserInfo();
        }
        return localUserInfo;
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
        logout(roomId);
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            Iterator<WrapEventListener> iterator = eventListeners.iterator();
            while (iterator.hasNext()){
                WrapEventListener next = iterator.next();
                if(next.roomId.equals(roomId)){
                    sceneReference.unsubscribe(next);
                }
            }
            if(delete){
                sceneReference.collection(SYNC_MANAGER_USER_COLLECTION).delete(new Sync.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        notifyErrorHandler(exception);
                    }
                });
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
            }else{
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
        private String objectId;
        public String avatar = String.format(Locale.US, "portrait%02d", RandomUtil.randomId(1, 14));
        public String userId = getCacheUserId();
        public String userName = "User-" + userId;
        public String timestamp = System.currentTimeMillis() + "";

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
