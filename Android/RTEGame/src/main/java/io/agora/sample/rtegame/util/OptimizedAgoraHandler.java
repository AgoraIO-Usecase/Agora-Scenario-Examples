package io.agora.sample.rtegame.util;

import android.graphics.Rect;

import io.agora.rtc2.IAgoraEventHandler;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.UserInfo;

public class OptimizedAgoraHandler implements IAgoraEventHandler {
    @Override
    public void onWarning(int i) {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onApiCallExecuted(int i, String s, String s1) {

    }

    @Override
    public void onCameraReady() {

    }

    @Override
    public void onCameraFocusAreaChanged(Rect rect) {

    }

    @Override
    public void onCameraExposureAreaChanged(Rect rect) {

    }

    @Override
    public void onFacePositionChanged(int i, int i1, IRtcEngineEventHandler.AgoraFacePositionInfo[] agoraFacePositionInfos) {

    }

    @Override
    public void onVideoStopped() {

    }

    @Override
    public void onLeaveChannel(IRtcEngineEventHandler.RtcStats rtcStats) {

    }

    @Override
    public void onRtcStats(IRtcEngineEventHandler.RtcStats rtcStats) {

    }

    @Override
    public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] audioVolumeInfos, int i) {

    }

    @Override
    public void onLastmileQuality(int i) {

    }

    @Override
    public void onLastmileProbeResult(IRtcEngineEventHandler.LastmileProbeResult lastmileProbeResult) {

    }

    @Override
    public void onLocalVideoStat(int i, int i1) {

    }

    @Override
    public void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats remoteVideoStats) {

    }

    @Override
    public void onRemoteAudioStats(IRtcEngineEventHandler.RemoteAudioStats remoteAudioStats) {

    }

    @Override
    public void onLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats localVideoStats) {

    }

    @Override
    public void onLocalAudioStats(IRtcEngineEventHandler.LocalAudioStats localAudioStats) {

    }

    @Override
    public void onFirstLocalVideoFrame(int i, int i1, int i2) {

    }

    @Override
    public void onConnectionLost() {

    }

    @Override
    public void onConnectionInterrupted() {

    }

    @Override
    public void onConnectionStateChanged(int i, int i1) {

    }

    @Override
    public void onNetworkTypeChanged(int i) {

    }

    @Override
    public void onConnectionBanned() {

    }

    @Override
    public void onRefreshRecordingServiceStatus(int i) {

    }

    @Override
    public void onMediaEngineLoadSuccess() {

    }

    @Override
    public void onMediaEngineStartCallSuccess() {

    }

    @Override
    public void onAudioMixingFinished() {

    }

    @Override
    public void onRequestToken() {

    }

    @Override
    public void onAudioRouteChanged(int i) {

    }

    @Override
    public void onAudioMixingStateChanged(int i, int i1) {

    }

    @Override
    public void onFirstLocalAudioFramePublished(int i) {

    }

    @Override
    public void onAudioEffectFinished(int i) {

    }

    @Override
    public void onClientRoleChanged(int i, int i1) {

    }

    @Override
    public void onRtmpStreamingStateChanged(String s, IRtcEngineEventHandler.RTMP_STREAM_PUBLISH_STATE rtmp_stream_publish_state, IRtcEngineEventHandler.RTMP_STREAM_PUBLISH_ERROR rtmp_stream_publish_error) {

    }

    @Override
    public void onStreamPublished(String s, int i) {

    }

    @Override
    public void onStreamUnpublished(String s) {

    }

    @Override
    public void onTranscodingUpdated() {

    }

    @Override
    public void onTokenPrivilegeWillExpire(String s) {

    }

    @Override
    public void onLocalPublishFallbackToAudioOnly(boolean b) {

    }

    @Override
    public void onChannelMediaRelayStateChanged(int i, int i1) {

    }

    @Override
    public void onChannelMediaRelayEvent(int i) {

    }

    @Override
    public void onIntraRequestReceived() {

    }

    @Override
    public void onUplinkNetworkInfoUpdated(IRtcEngineEventHandler.UplinkNetworkInfo uplinkNetworkInfo) {

    }

    @Override
    public void onDownlinkNetworkInfoUpdated(IRtcEngineEventHandler.DownlinkNetworkInfo downlinkNetworkInfo) {

    }

    @Override
    public void onEncryptionError(IRtcEngineEventHandler.ENCRYPTION_ERROR_TYPE encryption_error_type) {

    }

    @Override
    public void onPermissionError(IRtcEngineEventHandler.PERMISSION permission) {

    }

    @Override
    public void onLocalUserRegistered(int i, String s) {

    }

    @Override
    public void onUserInfoUpdated(int i, UserInfo userInfo) {

    }

    @Override
    public void onFirstLocalVideoFramePublished(int i) {

    }

    @Override
    public void onAudioSubscribeStateChanged(String s, int i, IRtcEngineEventHandler.STREAM_SUBSCRIBE_STATE stream_subscribe_state, IRtcEngineEventHandler.STREAM_SUBSCRIBE_STATE stream_subscribe_state1, int i1) {

    }

    @Override
    public void onVideoSubscribeStateChanged(String s, int i, IRtcEngineEventHandler.STREAM_SUBSCRIBE_STATE stream_subscribe_state, IRtcEngineEventHandler.STREAM_SUBSCRIBE_STATE stream_subscribe_state1, int i1) {

    }

    @Override
    public void onAudioPublishStateChanged(String s, IRtcEngineEventHandler.STREAM_PUBLISH_STATE stream_publish_state, IRtcEngineEventHandler.STREAM_PUBLISH_STATE stream_publish_state1, int i) {

    }

    @Override
    public void onVideoPublishStateChanged(String s, IRtcEngineEventHandler.STREAM_PUBLISH_STATE stream_publish_state, IRtcEngineEventHandler.STREAM_PUBLISH_STATE stream_publish_state1, int i) {

    }
}
