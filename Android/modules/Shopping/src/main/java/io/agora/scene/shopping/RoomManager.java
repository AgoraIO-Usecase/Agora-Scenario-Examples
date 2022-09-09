package io.agora.scene.shopping;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;
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
    private static final String SYNC_MANAGER_USER_COLLECTION = "userCollection";
    private static final String SYNC_MANAGER_GIFT_INFO = "giftInfo";
    private static final String SYNC_MANAGER_MESSAGE_INFO = "messageInfo";
    private static final String SYNC_MANAGER_PK_INFO = "pkInfo";
    private static final String SYNC_MANAGER_PK_APPLY_INFO = "shopping";
    private static final String SYNC_MANAGER_SHOPPING_INFO = "shoppingInfo";

    private static volatile RoomManager INSTANCE;
    private static volatile boolean isInitialized = false;

    private final Map<String, SceneReference> sceneMap = new HashMap<>();
    private final List<WrapSyncEventListener> eventListeners = new ArrayList<>();
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

    private void notifyErrorHandler(Exception error) {
        Log.e(TAG, error.toString());
        if (errorHandler != null) {
            errorHandler.onObtained(error);
        }
    }

    public UserInfo getLocalUserInfo() {
        return localUserInfo;
    }

    public void init(Context context, String appId, DataCallback<Exception> error) {
        if (isInitialized) {
            return;
        }
        initData(context);
        PreferenceUtil.init(context);
        isInitialized = true;
        errorHandler = error;
        HashMap<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("defaultChannel", "shopping");
        Sync.Instance().init(context, params, new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                if (error != null) {
                    error.onObtained(exception);
                }
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
                    callback.onObtained(null);
                }
                notifyErrorHandler(exception);
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

    private void login(String roomId) {
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

    private void logout(String roomId) {
        if (localUserInfo == null) {
            return;
        }
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
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

    public void getRoomUserList(String roomId, DataListCallback<UserInfo> success) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        sceneReference.collection(SYNC_MANAGER_USER_COLLECTION)
                .get(new Sync.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        if (success != null) {
                            List<UserInfo> ret = new ArrayList<>();
                            for (IObject iObject : result) {
                                ret.add(iObject.toObject(UserInfo.class));
                            }
                            success.onObtained(ret);
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
                if (success != null) {
                    success.onObtained(null);
                }
                notifyErrorHandler(exception);
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
                if (success != null) {
                    success.onObtained(null);
                }
                notifyErrorHandler(exception);
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
        pkInfoModel.roomId = roomId.equals(applyInfo.roomId) ? applyInfo.targetRoomId : applyInfo.roomId;
        pkInfoModel.userId = applyInfo.targetUserId;
        sceneReference.update(SYNC_MANAGER_PK_INFO, pkInfoModel, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
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
                notifyErrorHandler(exception);
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
                notifyErrorHandler(exception);
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
                notifyErrorHandler(exception);
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
                notifyErrorHandler(exception);
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

    public void subscribeUserListChangeEvent(String roomId, DataListCallback<UserInfo> change) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                getRoomUserList(roomId, change);
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                getRoomUserList(roomId, change);
            }

            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                getRoomUserList(roomId, change);
            }
        };
        sceneReference.collection(SYNC_MANAGER_USER_COLLECTION)
                .subscribe(listener);
        eventListeners.add(listener);
    }

    public void subscribeMessageReceiveEvent(String roomId, DataCallback<MessageInfo> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId) {
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
        sceneReference.subscribe(SYNC_MANAGER_MESSAGE_INFO, listener);
        eventListeners.add(listener);
    }

    public void subscribePKApplyInfoEvent(String roomId, DataCallback<PKApplyInfoModel> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId, addOrUpdate) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(PKApplyInfoModel.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(PKApplyInfoModel.class));
                }
            }
        };
        sceneReference.subscribe(SYNC_MANAGER_PK_APPLY_INFO, listener);
        eventListeners.add(listener);
    }

    public void unSubscribePKApplyInfoEvent(String roomId, DataCallback<PKApplyInfoModel> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = null;
        for (WrapSyncEventListener eventListener : eventListeners) {
            if (roomId.equals(eventListener.roomId) && addOrUpdate == eventListener.tag) {
                listener = eventListener;
                break;
            }
        }
        if (listener != null) {
            sceneReference.unsubscribe(listener);
            eventListeners.remove(listener);
        }
    }

    public void subscribePKInfoEvent(String roomId, DataCallback<PKInfoModel> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(PKInfoModel.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(PKInfoModel.class));
                }
            }
        };
        sceneReference.subscribe(SYNC_MANAGER_PK_INFO, listener);
        eventListeners.add(listener);
    }

    public void subscribeGiftReceiveEvent(String roomId, DataCallback<GiftInfo> addOrUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId) {
            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(GiftInfo.class));
                }
            }

            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(GiftInfo.class));
                }
            }
        };
        sceneReference.subscribe(SYNC_MANAGER_GIFT_INFO, listener);
        eventListeners.add(listener);
    }

    public void subscriptRoomEvent(String roomId, DataCallback<RoomInfo> addOrUpdate, DataCallback<String> delete) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(RoomInfo.class));
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (addOrUpdate != null) {
                    addOrUpdate.onObtained(item.toObject(RoomInfo.class));
                }
            }

            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                if (delete != null) {
                    delete.onObtained(item.getId());
                }
            }
        };

        sceneReference.subscribe(listener);
        eventListeners.add(listener);
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

    public void leaveRoom(RoomInfo roomInfo) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomInfo.roomId);
        if (sceneReference == null) {
            return;
        }
        logout(roomInfo.roomId);

        for (WrapSyncEventListener eventListener : eventListeners) {
            if (roomInfo.roomId.equals(eventListener.roomId)) {
                sceneReference.unsubscribe(eventListener);
            }
        }
        eventListeners.clear();
        sceneMap.remove(roomInfo.roomId);

        if (roomInfo.userId.equals(getCacheUserId())) {
            sceneReference.delete(new Sync.Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFail(SyncManagerException exception) {
                    notifyErrorHandler(exception);
                }
            });
        }
    }

    public void getNormalShoppingModels(String roomId, DataListCallback<ShoppingModel> callback) {
        getShoppingModels(roomId, ShoppingStatus.list, callback);
    }

    public void getUpShoppingModels(String roomId, DataListCallback<ShoppingModel> callback) {
        getShoppingModels(roomId, ShoppingStatus.goodsShelves, callback);
    }

    private void getShoppingModels(String roomId, @ShoppingStatus int status, DataListCallback<ShoppingModel> callback) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null || callback == null) {
            return;
        }
        sceneReference.collection(SYNC_MANAGER_SHOPPING_INFO)
                .get(new Sync.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        List<ShoppingModel> allModels = new ArrayList<>(ShoppingModelList);

                        List<ShoppingModel> upModels = new ArrayList<>();
                        List<ShoppingModel> normalModels = new ArrayList<>();
                        for (IObject iObject : result) {
                            ShoppingModel model = iObject.toObject(ShoppingModel.class);
                            model.objectId = iObject.getId();
                            if (model.status == ShoppingStatus.goodsShelves) {
                                upModels.add(model);
                            }
                        }

                        for (ShoppingModel model : allModels) {
                            boolean hasUp = false;
                            for (ShoppingModel upModel : upModels) {
                                if (upModel.imageName.equals(model.imageName)) {
                                    hasUp = true;
                                    break;
                                }
                            }
                            if (!hasUp) {
                                normalModels.add(model);
                            }
                        }

                        if (status == ShoppingStatus.list) {
                            callback.onObtained(normalModels);
                        } else {
                            callback.onObtained(upModels);
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        if (status == ShoppingStatus.list) {
                            callback.onObtained(new ArrayList<>(ShoppingModelList));
                        } else {
                            callback.onObtained(new ArrayList<>());
                        }
                        notifyErrorHandler(exception);
                    }
                });
    }

    public void upShoppingModel(String roomId, ShoppingModel model) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        ShoppingModel _model = new ShoppingModel(model.title, model.imageName, model.desc, model.price);
        _model.status = ShoppingStatus.goodsShelves;
        sceneReference.collection(SYNC_MANAGER_SHOPPING_INFO).add(_model, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void downShoppingModel(String roomId, ShoppingModel model) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        ShoppingModel _model = new ShoppingModel(model.title, model.imageName, model.desc, model.price);
        _model.status = ShoppingStatus.list;
        sceneReference.collection(SYNC_MANAGER_SHOPPING_INFO).delete(model.objectId, new Sync.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }


    public void subscriptShoppingModelEvent(String roomId, Runnable onUpdate) {
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if (sceneReference == null) {
            return;
        }
        WrapSyncEventListener listener = new WrapSyncEventListener(roomId, onUpdate) {
            @Override
            public void onCreated(IObject item) {
                super.onCreated(item);
                if (onUpdate != null) {
                    onUpdate.run();
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if (onUpdate != null) {
                    onUpdate.run();
                }
            }

            @Override
            public void onDeleted(IObject item) {
                super.onDeleted(item);
                if (onUpdate != null) {
                    onUpdate.run();
                }
            }
        };
        sceneReference.collection(SYNC_MANAGER_SHOPPING_INFO).subscribe(listener);
        eventListeners.add(listener);
    }

    public void unSubscriptShoppingModelEvent(String roomId, Runnable onUpdate) {
        Iterator<WrapSyncEventListener> iterator = eventListeners.iterator();
        while (iterator.hasNext()) {
            WrapSyncEventListener next = iterator.next();
            if (next.roomId.equals(roomId) && next.tag == onUpdate) {
                iterator.remove();
                break;
            }
        }
    }

    public void destroy() {
        if (isInitialized) {
            Sync.Instance().destroy();
            eventListeners.clear();
            localUserInfo = null;
            sceneMap.clear();
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
    public static final Map<String, Integer> ShoppingImgResMap;
    public static final Map<String, Integer> ShoppingBigImgResMap;
    private static final List<ShoppingModel> ShoppingModelList = new ArrayList<>();

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

        ShoppingImgResMap = new HashMap<>();
        ShoppingImgResMap.put("pic-1", R.drawable.shopping_icon_product_1);
        ShoppingImgResMap.put("pic-2", R.drawable.shopping_icon_product_2);
        ShoppingImgResMap.put("pic-3", R.drawable.shopping_icon_product_3);
        ShoppingImgResMap.put("pic-4", R.drawable.shopping_icon_product_4);

        ShoppingBigImgResMap = new HashMap<>();
        ShoppingBigImgResMap.put("pic-1", R.drawable.shopping_product_picture_1);
        ShoppingBigImgResMap.put("pic-2", R.drawable.shopping_product_picture_2);
        ShoppingBigImgResMap.put("pic-3", R.drawable.shopping_product_picture_3);
        ShoppingBigImgResMap.put("pic-4", R.drawable.shopping_product_picture_4);
    }

    private void initData(Context context) {
        ShoppingModelList.add(new ShoppingModel(
                context.getString(R.string.shopping_product_name_1),
                "pic-1",
                context.getString(R.string.shopping_product_desp_1), 7000.0));
        ShoppingModelList.add(new ShoppingModel(
                context.getString(R.string.shopping_product_name_2),
                "pic-2",
                context.getString(R.string.shopping_product_desp_2), 3399.0));
        ShoppingModelList.add(new ShoppingModel(
                context.getString(R.string.shopping_product_name_3),
                "pic-3",
                context.getString(R.string.shopping_product_desp_3), 4988.0));
        ShoppingModelList.add(new ShoppingModel(
                context.getString(R.string.shopping_product_name_4),
                "pic-4",
                context.getString(R.string.shopping_product_desp_4), 988.0));
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

    public @interface ShoppingStatus {
        int list = 0; // 未上架商品
        int goodsShelves = 1; // 已上架商品
    }

    public static class ShoppingModel {
        public String imageName;
        public String title;
        public double price;
        public String desc;
        public String objectId;
        public @ShoppingStatus
        int status = ShoppingStatus.list;

        public ShoppingModel(String title, String imageName, String desc, double price) {
            this.title = title;
            this.imageName = imageName;
            this.desc = desc;
            this.price = price;
        }
    }


    public interface DataListCallback<T> {
        void onObtained(List<T> dataList);
    }

    public interface DataCallback<T> {
        void onObtained(T data);
    }

    private class WrapSyncEventListener implements Sync.EventListener {
        private final String roomId;
        private final Object tag;

        private WrapSyncEventListener(String roomId) {
            this(roomId, null);
        }

        private WrapSyncEventListener(String roomId, Object tag) {
            this.roomId = roomId;
            this.tag = tag;
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
