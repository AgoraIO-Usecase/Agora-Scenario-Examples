//package io.agora.livepk.manager;
//
//import android.content.Context;
//import android.util.Log;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//public class RtmManager {
//
//    private volatile boolean isInitialized = false;
//    private Context mContext;
//    private RtmClient mRtmClient;
//    private Map<String, RtmChannel> mRtmChannels = new HashMap<>();
//
//    public void init(Context context, String appId, OnInitializeListener listener) {
//        if(isInitialized){
//            return;
//        }
//        mContext = context.getApplicationContext();
//        try {
//            mRtmClient = RtmClient.createInstance(context, appId, new RtmClientListener() {
//                @Override
//                public void onConnectionStateChanged(int state, int reason) {
//                    if (listener != null) {
//                        if (reason == RtmStatusCode.ConnectionChangeReason.CONNECTION_CHANGE_REASON_LOGIN_SUCCESS) {
//                            listener.onSuccess();
//                        }
//                    }
//                }
//
//                @Override
//                public void onMessageReceived(RtmMessage rtmMessage, String s) {
//
//                }
//
//                @Override
//                public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String s) {
//
//                }
//
//                @Override
//                public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {
//
//                }
//
//                @Override
//                public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
//
//                }
//
//                @Override
//                public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
//
//                }
//
//                @Override
//                public void onTokenExpired() {
//                    if(listener != null){
//                        listener.onError(-999, "token expired");
//                    }
//                }
//
//                @Override
//                public void onPeersOnlineStatusChanged(Map<String, Integer> map) {
//
//                }
//            });
//        } catch (Exception exception) {
//            if(listener != null){
//                listener.onError(-999, "RtmClient create exception: " + exception.toString());
//            }
//        }
//    }
//
//    public void login(String uid, ResultCallback<Void> callback){
//        if(mRtmClient == null){
//            return;
//        }
//        mRtmClient.login(null, uid, callback);
//    }
//
//
//    public void joinChannel(String channelName, OnChannelListener listener) {
//        if(mRtmClient == null){
//            return;
//        }
//        if(mRtmChannels.get(channelName) != null){
//            return;
//        }
//
//        RtmChannel channel = mRtmClient.createChannel(channelName, new RtmChannelListener() {
//            @Override
//            public void onMemberCountUpdated(int i) {
//
//            }
//
//            @Override
//            public void onAttributesUpdated(List<RtmChannelAttribute> list) {
//                if(listener != null){
//                    listener.onChannelAttributesUpdated(list);
//                }
//            }
//
//            @Override
//            public void onMessageReceived(RtmMessage rtmMessage, RtmChannelMember rtmChannelMember) {
//
//            }
//
//            @Override
//            public void onImageMessageReceived(RtmImageMessage rtmImageMessage, RtmChannelMember rtmChannelMember) {
//
//            }
//
//            @Override
//            public void onFileMessageReceived(RtmFileMessage rtmFileMessage, RtmChannelMember rtmChannelMember) {
//
//            }
//
//            @Override
//            public void onMemberJoined(RtmChannelMember rtmChannelMember) {
//
//            }
//
//            @Override
//            public void onMemberLeft(RtmChannelMember rtmChannelMember) {
//
//            }
//        });
//        mRtmChannels.put(channelName, channel);
//        channel.join(new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                if(listener != null){
//                    listener.onJoinSuccess();
//                }
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                mRtmChannels.remove(channelName);
//                if(listener != null){
//                    listener.onError(errorInfo.getErrorCode(), errorInfo.getErrorDescription());
//                }
//            }
//        });
//    }
//
//    public void updateChannelAttributes(String channelId, List<RtmChannelAttribute> attributes, ResultCallback<Void> callback) {
//        if (mRtmClient == null) {
//            return;
//        }
//        ChannelAttributeOptions channelAttributeOptions = new ChannelAttributeOptions();
//        channelAttributeOptions.setEnableNotificationToChannelMembers(true);
//        mRtmClient.addOrUpdateChannelAttributes(channelId, attributes, channelAttributeOptions, new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                if (callback != null) {
//                    callback.onSuccess(aVoid);
//                }
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                if (callback != null) {
//                    callback.onFailure(errorInfo);
//                }
//            }
//        });
//    }
//
//    public void deleteChannelAttribute(String channelId, List<String> keys, ResultCallback<Void> callback){
//        if (mRtmClient == null) {
//            return;
//        }
//        ChannelAttributeOptions channelAttributeOptions = new ChannelAttributeOptions();
//        channelAttributeOptions.setEnableNotificationToChannelMembers(true);
//        mRtmClient.deleteChannelAttributesByKeys(channelId, keys, channelAttributeOptions, new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                if(callback != null){
//                    callback.onSuccess(aVoid);
//                }
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                if(callback != null){
//                    callback.onFailure(errorInfo);
//                }
//            }
//        });
//    }
//
//    public void getChannelAttributes(String channelId, ResultCallback<List<RtmChannelAttribute>> callback) {
//        if (mRtmClient == null) {
//            return;
//        }
//        mRtmClient.getChannelAttributes(channelId, new ResultCallback<List<RtmChannelAttribute>>() {
//            @Override
//            public void onSuccess(List<RtmChannelAttribute> attributes) {
//                if (callback != null) {
//                    callback.onSuccess(attributes);
//                }
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                callback.onFailure(errorInfo);
//            }
//        });
//    }
//
//    public void release() {
//        Set<String> keys = mRtmChannels.keySet();
//        for (String s : keys) {
//            mRtmChannels.get(s).leave(null);
//        }
//        mRtmChannels.clear();
//        isInitialized = false;
//        mContext = null;
//        if(mRtmClient != null){
//            try {
//                mRtmClient.release();
//            }catch (Exception e){
//                Log.e("RtmManager", e.toString());
//            }finally {
//                mRtmClient = null;
//            }
//        }
//    }
//
//    public interface OnInitializeListener {
//        void onError(int code, String message);
//        void onSuccess();
//    }
//
//    public interface OnChannelListener {
//        void onError(int code, String message);
//        void onJoinSuccess();
//        void onChannelAttributesUpdated(List<RtmChannelAttribute> list);
//    }
//}
