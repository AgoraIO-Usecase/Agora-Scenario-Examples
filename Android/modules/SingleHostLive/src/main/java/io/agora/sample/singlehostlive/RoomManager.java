package io.agora.sample.singlehostlive;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static volatile RoomManager INSTANCE;
    private static volatile boolean isInitialized = false;

    private final Map<String, SceneReference> sceneMap = new HashMap<>();

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
        params.put("defaultChannel", "signleLive");
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

    public void createRoom(String roomName, DataCallback<RoomInfo> callback) {
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
                if (callback != null) {
                    callback.onSuccess(roomInfo);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (callback != null) {
                    callback.onFailed(exception);
                }
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
                        callback.onSuccess(ret);
                    } catch (Exception exception) {
                        callback.onFailed(exception);
                    }

                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (callback != null) {
                    callback.onFailed(exception);
                }
            }
        });
    }

    public void joinRoom(String roomId, Runnable successRun) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            Log.d(TAG, "The room of " + roomId + " has joined.");
            if(successRun != null){
                successRun.run();
            }
            return;
        }
        Sync.Instance().joinScene(roomId, new Sync.JoinSceneCallback() {
            @Override
            public void onSuccess(SceneReference sceneReference) {
                sceneMap.put(roomId, sceneReference);
                if(successRun != null){
                    successRun.run();
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                sceneMap.remove(roomId);
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

                    }
                });
    }

    public void subscribeGiftReceiveEvent(String roomId, WeakReference<DataCallback<GiftInfo>> callback){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.subscribe(SYNC_MANAGER_GIFT_INFO, new Sync.EventListener() {
            @Override
            public void onCreated(IObject item) {

            }

            @Override
            public void onUpdated(IObject item) {
                GiftInfo giftInfo = item.toObject(GiftInfo.class);
                if(callback != null){
                    DataCallback<GiftInfo> cb = callback.get();
                    if(cb != null){
                        cb.onSuccess(giftInfo);
                    }
                }
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {
                if(callback != null){
                    DataCallback<GiftInfo> cb = callback.get();
                    if(cb != null){
                        cb.onFailed(ex);
                    }
                }
            }
        });
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

    public void destroyRoom(String roomId) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference != null) {
            sceneReference.delete(new Sync.Callback() {
                @Override
                public void onSuccess() {
                    sceneMap.remove(roomId);
                }

                @Override
                public void onFail(SyncManagerException exception) {

                }
            });
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
        RoomBgResMap.put("portrait01", R.drawable.user_profile_image_1);
        RoomBgResMap.put("portrait02", R.drawable.user_profile_image_2);
        RoomBgResMap.put("portrait03", R.drawable.user_profile_image_3);
        RoomBgResMap.put("portrait04", R.drawable.user_profile_image_4);
        RoomBgResMap.put("portrait05", R.drawable.user_profile_image_5);
        RoomBgResMap.put("portrait06", R.drawable.user_profile_image_6);
        RoomBgResMap.put("portrait07", R.drawable.user_profile_image_7);
        RoomBgResMap.put("portrait08", R.drawable.user_profile_image_8);
        RoomBgResMap.put("portrait09", R.drawable.user_profile_image_9);
        RoomBgResMap.put("portrait10", R.drawable.user_profile_image_10);
        RoomBgResMap.put("portrait11", R.drawable.user_profile_image_11);
        RoomBgResMap.put("portrait12", R.drawable.user_profile_image_12);
        RoomBgResMap.put("portrait13", R.drawable.user_profile_image_13);
        RoomBgResMap.put("portrait14", R.drawable.user_profile_image_14);
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
            if(id != null){
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

        public int getIconId(){
            Integer ret = IconNameResMap.get(iconName);
            if(ret == null){
                ret = 0;
            }
            return ret;
        }

        public int getGifId(){
            Integer ret = GifNameResMap.get(gifName);
            if(ret == null){
                ret = 0;
            }
            return ret;
        }

    }


    public interface DataListCallback<T> {
        void onSuccess(List<T> dataList);

        void onFailed(Exception e);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onFailed(Exception e);
    }
}
