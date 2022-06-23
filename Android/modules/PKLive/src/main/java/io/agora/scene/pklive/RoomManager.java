package io.agora.scene.pklive;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    private static final String SYNC_MANAGER_PK_INFO = "pkInfo";
    private static final String SYNC_MANAGER_PK_APPLY_INFO = "pkApplyInfo";

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
        params.put("defaultChannel", "pkApplyInfo");
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
                        exception.printStackTrace();
                    }

                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (exception.getMessage().contains("empty")) {
                    if (callback != null) {
                        callback.onObtained(null);
                    }
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
            }
        });
    }

    public void getPkInfo(String roomId, DataCallback<PKInfoModel> success) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.get(SYNC_MANAGER_PK_INFO, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                if (success != null) {
                    PKInfoModel data = null;
                    try {
                        data = result.toObject(PKInfoModel.class);
                        data.objectId = result.getId();
                    } catch (Exception e) {
                        // do nothing
                    }
                    success.onObtained(data);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (exception.getMessage().contains("empty")) {
                    if (success != null) {
                        success.onObtained(null);
                    }
                }
            }
        });
    }

    public void getPkApplyInfo(String roomId, DataCallback<PKApplyInfoModel> success) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.get(SYNC_MANAGER_PK_APPLY_INFO, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                if (success != null) {
                    PKApplyInfoModel data = null;
                    try {
                        data = result.toObject(PKApplyInfoModel.class);
                        data.objectId = result.getId();
                    } catch (Exception e) {
                        // do nothing
                    }
                    success.onObtained(data);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (exception.getMessage().contains("empty")) {
                    if (success != null) {
                        success.onObtained(null);
                    }
                }
            }
        });
    }

    public void inviteToPK(String roomId, String pkRoomId, String pkUserId, DataCallback<PKApplyInfoModel> success, DataCallback<Exception> error) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }

        joinRoom(pkRoomId, () -> {
            getPkApplyInfo(roomId, data -> {
                if (data != null && data.status == PKApplyInfoStatus.accept) {
                    if (error != null) {
                        error.onObtained(new RuntimeException("PK_Invite_Fail"));
                    }
                } else {
                    PKApplyInfoModel infoModel = new PKApplyInfoModel();
                    infoModel.roomId = roomId;
                    infoModel.targetRoomId = pkRoomId;
                    infoModel.targetUserId = pkUserId;
                    if (success != null) {
                        success.onObtained(infoModel);
                    }
                }
            });
        });
    }

    public void startPKNow(String roomId, PKApplyInfoModel applyInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }

        if (applyInfo.status != PKApplyInfoStatus.accept) {
            return;
        }
        PKInfoModel pkInfoModel = new PKInfoModel();
        pkInfoModel.status = applyInfo.status;
        pkInfoModel.roomId = applyInfo.targetUserId.equals(getCacheUserId()) ? roomId : applyInfo.targetRoomId;
        pkInfoModel.userId = applyInfo.targetUserId;
        sceneReference.update(SYNC_MANAGER_PK_INFO, pkInfoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void stopPKNow(String roomId, PKApplyInfoModel applyInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }

        if (applyInfo.status != PKApplyInfoStatus.end) {
            return;
        }
        PKInfoModel pkInfoModel = new PKInfoModel();
        pkInfoModel.status = applyInfo.status;
        pkInfoModel.roomId = applyInfo.targetRoomId;
        pkInfoModel.userId = getCacheUserId();
        sceneReference.update(SYNC_MANAGER_PK_INFO, pkInfoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void applyPKInvite(PKApplyInfoModel applyInfo) {
        checkInitialized();
        String roomId = applyInfo.targetRoomId;
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        PKApplyInfoModel infoModel = new PKApplyInfoModel();
        infoModel.roomId = applyInfo.roomId;
        infoModel.targetRoomId = roomId;
        infoModel.targetUserId = applyInfo.targetUserId;
        infoModel.status = PKApplyInfoStatus.invite;
        sceneReference.update(SYNC_MANAGER_PK_APPLY_INFO, infoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void applyPKAccept(PKApplyInfoModel pkApplyInfoModel) {
        checkInitialized();
        String roomId = pkApplyInfoModel.targetRoomId;
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        PKApplyInfoModel infoModel = new PKApplyInfoModel();
        infoModel.roomId = pkApplyInfoModel.roomId;
        infoModel.userId = pkApplyInfoModel.userId;
        infoModel.targetRoomId = roomId;
        infoModel.targetUserId = pkApplyInfoModel.targetUserId;
        infoModel.status = PKApplyInfoStatus.accept;
        sceneReference.update(SYNC_MANAGER_PK_APPLY_INFO, infoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void applyPKRefuse(PKApplyInfoModel pkApplyInfoModel) {
        checkInitialized();
        String roomId = pkApplyInfoModel.targetRoomId;
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        PKApplyInfoModel infoModel = new PKApplyInfoModel();
        infoModel.roomId = pkApplyInfoModel.roomId;
        infoModel.userId = pkApplyInfoModel.userId;
        infoModel.targetRoomId = roomId;
        infoModel.targetUserId = pkApplyInfoModel.targetUserId;
        infoModel.status = PKApplyInfoStatus.refuse;
        sceneReference.update(SYNC_MANAGER_PK_APPLY_INFO, infoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void applyPKEnd(PKApplyInfoModel pkApplyInfoModel) {
        checkInitialized();
        String roomId = pkApplyInfoModel.targetRoomId;
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        PKApplyInfoModel infoModel = new PKApplyInfoModel();
        infoModel.roomId = pkApplyInfoModel.roomId;
        infoModel.targetRoomId = roomId;
        infoModel.targetUserId = pkApplyInfoModel.targetUserId;
        infoModel.status = PKApplyInfoStatus.end;
        sceneReference.update(SYNC_MANAGER_PK_APPLY_INFO, infoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

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

    public void subscribePKApplyInfoEvent(String roomId, WeakReference<DataCallback<PKApplyInfoModel>> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.subscribe(SYNC_MANAGER_PK_APPLY_INFO, new Sync.EventListener() {
            @Override
            public void onCreated(IObject item) {
                PKApplyInfoModel info = item.toObject(PKApplyInfoModel.class);
                info.objectId = item.getId();
                if (addOrUpdate != null) {
                    DataCallback<PKApplyInfoModel> cb = addOrUpdate.get();
                    if (cb != null) {
                        cb.onObtained(info);
                    }
                }
            }

            @Override
            public void onUpdated(IObject item) {
                PKApplyInfoModel info = item.toObject(PKApplyInfoModel.class);
                info.objectId = item.getId();
                if (addOrUpdate != null) {
                    DataCallback<PKApplyInfoModel> cb = addOrUpdate.get();
                    if (cb != null) {
                        cb.onObtained(info);
                    }
                }
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        });
    }

    private final Map<DataCallback<PKApplyInfoModel>, Sync.EventListener> pkApplyInfoEvent = new HashMap<>();

    public void subscribePKApplyInfoEvent(String roomId, DataCallback<PKApplyInfoModel> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        Sync.EventListener listener = new Sync.EventListener() {
            @Override
            public void onCreated(IObject item) {
                PKApplyInfoModel info = item.toObject(PKApplyInfoModel.class);
                info.objectId = item.getId();
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(info);
                }
            }

            @Override
            public void onUpdated(IObject item) {
                PKApplyInfoModel info = item.toObject(PKApplyInfoModel.class);
                info.objectId = item.getId();
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(info);
                }
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        };
        sceneReference.subscribe(SYNC_MANAGER_PK_APPLY_INFO, listener);
    }

    public void unSubscribePKApplyInfoEvent(String roomId, DataCallback<PKApplyInfoModel> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        Sync.EventListener eventListener = pkApplyInfoEvent.get(addOrUpdate);
        if (eventListener != null) {
            sceneReference.unsubscribe(eventListener);
            pkApplyInfoEvent.remove(addOrUpdate);
        }
    }

    public void subscribePKInfoEvent(String roomId, WeakReference<DataCallback<PKInfoModel>> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.subscribe(SYNC_MANAGER_PK_INFO, new Sync.EventListener() {
            @Override
            public void onCreated(IObject item) {
                PKInfoModel info = item.toObject(PKInfoModel.class);
                info.objectId = item.getId();
                if (addOrUpdate != null) {
                    DataCallback<PKInfoModel> cb = addOrUpdate.get();
                    if (cb != null) {
                        cb.onObtained(info);
                    }
                }
            }

            @Override
            public void onUpdated(IObject item) {
                PKInfoModel info = item.toObject(PKInfoModel.class);
                info.objectId = item.getId();
                if (addOrUpdate != null) {
                    DataCallback<PKInfoModel> cb = addOrUpdate.get();
                    if (cb != null) {
                        cb.onObtained(info);
                    }
                }
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        });
    }

    public void subscribeGiftReceiveEvent(String roomId, WeakReference<DataCallback<GiftInfo>> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.subscribe(SYNC_MANAGER_GIFT_INFO, new Sync.EventListener() {
            @Override
            public void onCreated(IObject item) {
                GiftInfo giftInfo = item.toObject(GiftInfo.class);
                if (addOrUpdate != null) {
                    DataCallback<GiftInfo> cb = addOrUpdate.get();
                    if (cb != null) {
                        cb.onObtained(giftInfo);
                    }
                }
            }

            @Override
            public void onUpdated(IObject item) {
                GiftInfo giftInfo = item.toObject(GiftInfo.class);
                if (addOrUpdate != null) {
                    DataCallback<GiftInfo> cb = addOrUpdate.get();
                    if (cb != null) {
                        cb.onObtained(giftInfo);
                    }
                }
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        });
    }

    public void subscriptRoomEvent(String roomId, WeakReference<DataCallback<RoomInfo>> addOrUpdate, WeakReference<DataCallback<String>> delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.subscribe(new Sync.EventListener() {
            @Override
            public void onCreated(IObject item) {
                RoomInfo roomInfo = item.toObject(RoomInfo.class);
                if (addOrUpdate != null) {
                    DataCallback<RoomInfo> callback = addOrUpdate.get();
                    if (callback != null) {
                        callback.onObtained(roomInfo);
                    }
                }
            }

            @Override
            public void onUpdated(IObject item) {
                RoomInfo roomInfo = item.toObject(RoomInfo.class);
                if (addOrUpdate != null) {
                    DataCallback<RoomInfo> callback = addOrUpdate.get();
                    if (callback != null) {
                        callback.onObtained(roomInfo);
                    }
                }
            }

            @Override
            public void onDeleted(IObject item) {
                if (delete != null) {
                    DataCallback<String> callback = delete.get();
                    if (callback != null) {
                        callback.onObtained(item.getId());
                    }
                }
            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

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

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PKApplyInfoStatus.invite, PKApplyInfoStatus.accept, PKApplyInfoStatus.refuse, PKApplyInfoStatus.end})
    public @interface PKApplyInfoStatus {
        // 邀请中
        int invite = 1;
        // 已接受
        int accept = 2;
        // 已拒绝
        int refuse = 3;
        // 已结束
        int end = 4;
    }

    public static class PKApplyInfoModel {
        public String objectId = "";
        public String userId = getCacheUserId();
        public String targetUserId;
        public String userName = "";
        public @PKApplyInfoStatus
        int status;
        public String roomId;
        public String targetRoomId;
        public String timestamp = System.currentTimeMillis() + "";
    }

    public static class PKInfoModel {
        public String objectId;
        public @PKApplyInfoStatus
        int status;
        public String roomId;
        public String userId;
        public String timestamp = System.currentTimeMillis() + "";
    }


    public interface DataListCallback<T> {
        void onObtained(List<T> dataList);
    }

    public interface DataCallback<T> {
        void onObtained(T data);
    }
}
