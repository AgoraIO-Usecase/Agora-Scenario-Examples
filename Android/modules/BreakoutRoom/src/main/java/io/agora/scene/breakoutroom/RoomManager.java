package io.agora.scene.breakoutroom;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

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
    public static final String SYNC_DEFAULT_CHANNEL = "BreakOutRoom";
    public static final String SYNC_SUB_ROOM = "SubRoom";

    private static volatile RoomManager INSTANCE;
    private static volatile boolean isInitialized = false;

    private final Map<String, SceneReference> sceneMap = new HashMap<>();
    private final List<WrapEventListener> eventListeners = new ArrayList<>();
    private DataCallback<Exception> errorHandler;

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
        params.put("defaultChannel", SYNC_DEFAULT_CHANNEL);
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
        RoomInfo roomInfo = new RoomInfo();
        roomInfo.id = roomName;

        Scene room = new Scene();
        room.setId(roomInfo.id);
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

    public void createSubRoom(String roomId, String subRoomId){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if(sceneReference == null){
            return;
        }
        SubRoomInfo subRoomInfo = new SubRoomInfo();
        subRoomInfo.subRoom = subRoomId;
        sceneReference.collection(SYNC_SUB_ROOM).add(subRoomInfo, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {
                notifyErrorHandler(exception);
            }
        });
    }

    public void getAllSubRooms(String roomId, DataListCallback<SubRoomInfo> success){
        checkInitialized();
        SceneReference sceneReference = sceneMap.get(roomId);
        if(sceneReference == null){
            return;
        }
        sceneReference.collection(SYNC_SUB_ROOM).get(new Sync.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {
                List<SubRoomInfo> list = new ArrayList<>();
                for (IObject iObject : result) {
                    SubRoomInfo subRoom = iObject.toObject(SubRoomInfo.class);
                    list.add(subRoom);
                }
                if(success != null){
                    success.onObtained(list);
                }
            }

            @Override
            public void onFail(SyncManagerException exception) {
                Log.e(TAG, "getAllSubRooms >> " + exception.toString());
                if(success != null){
                    success.onObtained(new ArrayList<>());
                }
            }
        });
    }

    public void subscribeSubRoomEvent(String roomId, DataCallback<SubRoomInfo> addOrUpdate){
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
                    SubRoomInfo data = item.toObject(SubRoomInfo.class);
                    addOrUpdate.onObtained(data);
                }
            }

            @Override
            public void onUpdated(IObject item) {
                super.onUpdated(item);
                if(addOrUpdate != null){
                    SubRoomInfo data = item.toObject(SubRoomInfo.class);
                    addOrUpdate.onObtained(data);
                }
            }
        };
        eventListeners.add(listener);
        sceneReference.collection(SYNC_SUB_ROOM).subscribe(listener);
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

    public void leaveRoom(String roomId) {
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
            sceneReference.unsubscribe(null);
            sceneMap.remove(roomId);
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
        RoomBgResMap.put("cover/portrait01", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait02", R.drawable.random_icon2_02);
        RoomBgResMap.put("cover/portrait03", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait04", R.drawable.random_icon2_02);
        RoomBgResMap.put("cover/portrait05", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait06", R.drawable.random_icon2_02);
        RoomBgResMap.put("cover/portrait07", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait08", R.drawable.random_icon2_02);
        RoomBgResMap.put("cover/portrait09", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait10", R.drawable.random_icon2_02);
        RoomBgResMap.put("cover/portrait11", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait12", R.drawable.random_icon2_02);
        RoomBgResMap.put("cover/portrait13", R.drawable.random_icon2_01);
        RoomBgResMap.put("cover/portrait14", R.drawable.random_icon2_02);
    }

    public static class RoomInfo implements Serializable {
        public String id;
        public String userId = getCacheUserId();
        public String backgroundId = String.format(Locale.US, "cover/portrait%02d", RandomUtil.randomId(1, 14));

        public int getAndroidBgId() {
            if (TextUtils.isEmpty(backgroundId)) {
                return 0;
            }
            int bgResId = R.drawable.random_icon2_01;
            Integer id = RoomBgResMap.get(backgroundId);
            if (id != null) {
                bgResId = id;
            }
            return bgResId;
        }

        public Map<String, String> toMap() {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", id);
            map.put("userId", userId);
            map.put("backgroundId", backgroundId);
            return map;
        }
    }

    public static final class SubRoomInfo {
        // 子房间名
        public String subRoom;
        // 13位时间戳
        private final String createTime = String.valueOf(System.currentTimeMillis());
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
