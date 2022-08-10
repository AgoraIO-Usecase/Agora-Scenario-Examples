//
//  SuperAppPlayerViewControllerHost+RTC.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/28.
//

import AgoraRtcKit

extension CDNPlayerViewControllerHost {
    var videoSize: CGSize { .init(width: 640, height: 360) }
    
    func joinRtcByPassPush() { /** 旁推方式加入 **/
        LogUtils.log(message: "旁推方式加入", level: .info)
        let channelId = self.config.sceneId
        
        let config = AgoraRtcEngineConfig()
        config.appId = self.config.appId
        
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: config,
                                                  delegate: self)
        agoraKit.setChannelProfile(.liveBroadcasting)
        agoraKit.setClientRole(.broadcaster)
        
        let videoConfig = AgoraVideoEncoderConfiguration(size: videoSize,
                                                         frameRate: .fps15,
                                                         bitrate: 700,
                                                         orientationMode: .fixedPortrait,
                                                         mirrorMode: .auto)
        agoraKit.setVideoEncoderConfiguration(videoConfig)
        agoraKit.enableVideo()
        setupLocalVideo(view: mainView.renderViewLocal)
        
        agoraKit.setDefaultAudioRouteToSpeakerphone(true)

        let ret = agoraKit.joinChannel(byToken: KeyCenter.Token,
                                       channelId: channelId,
                                       info: nil,
                                       uid: UserInfo.userId,
                                       joinSuccess: nil)
        
        if ret != 0 {
            LogUtils.log(message: "joinRtcByPush error \(ret)", level: .info)
        }
    }
    
    func leaveRtcByPassPush() { /** 离开旁推方式 **/
        agoraKit.removeInjectStreamUrl(pushUrlString)
    }
    
    func joinRtcByPush() { /** 直推方式加入 **/
        LogUtils.log(message: "直推方式加入", level: .info)
        
        let config = AgoraRtcEngineConfig()
        config.appId = config.appId
        
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: config,
                                                  delegate: self)
        agoraKit.setChannelProfile(.liveBroadcasting)
        agoraKit.setClientRole(.broadcaster)
        
        let videoConfig = AgoraVideoEncoderConfiguration(size: videoSize,
                                                         frameRate: .fps15,
                                                         bitrate: 700,
                                                         orientationMode: .fixedPortrait,
                                                         mirrorMode: .disabled)
        agoraKit.setVideoEncoderConfiguration(videoConfig)
        agoraKit.enableVideo()
        setupLocalVideo(view: mainView.renderViewLocal)

        agoraKit.setDirectCdnStreamingAudioConfiguration(.default)
        agoraKit.setDirectCdnStreamingVideoConfiguration(videoConfig)
        let options = AgoraDirectCdnStreamingMediaOptions()
        options.publishCameraTrack = .of(true)
        options.publishMicrophoneTrack = .of(true)
        let ret = agoraKit.startDirectCdnStreaming(self,
                                         publishUrl: pushUrlString,
                                         mediaOptions: options)
        
        agoraKit.enableAudio()
        agoraKit.setDefaultAudioRouteToSpeakerphone(true)
        
        if ret != 0 {
            LogUtils.log(message: "joinRtcByPush error \(ret)", level: .info)
        }
    }
    
    func leaveRtcByPush() { /** 离开直推方式 **/
        LogUtils.log(message: "leaveRtcByPush", level: .info)
        agoraKit.stopDirectCdnStreaming()
    }
    
    private func setupLocalVideo(view: UIView) {
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.uid = 0
        videoCanvas.view = view
        videoCanvas.renderMode = .hidden
        agoraKit.setupLocalVideo(videoCanvas)
        agoraKit.startPreview()
    }
    
    func setupRemoteVideo(view: UIView, uid: UInt) {
        view.backgroundColor = .gray
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.uid = uid
        videoCanvas.view = view
        videoCanvas.renderMode = .hidden
        agoraKit.setupRemoteVideo(videoCanvas)
    }
    
    func switchCamera() {
        agoraKit.switchCamera()
    }
    
    func muteLocalAudio(mute: Bool) {
        agoraKit.muteLocalAudioStream(audioIsMute)
        agoraKit.adjustRecordingSignalVolume(audioIsMute ? 0 : 100)
    }
    
    func setMergeVideoLocal(engine: AgoraRtcEngineKit, uid: UInt) { /** 设置旁路推流合图（本地） **/
        LogUtils.log(message: "设置旁路推流合图（本地）", level: .info)
        let user = AgoraLiveTranscodingUser()
        user.rect = CGRect(x: 0,
                           y: 0,
                           width: videoSize.height,
                           height: videoSize.width)
        user.uid = uid
        user.zOrder = 1
        liveTranscoding.size = CGSize(width: videoSize.height, height: videoSize.width)
        liveTranscoding.videoFramerate = 15
        liveTranscoding.add(user)
        engine.updateRtmpTranscoding(liveTranscoding)
    }
    
    func setMergeVideoRemote(engine: AgoraRtcEngineKit, uid: UInt) { /** 设置旁路推流合图（远程） **/
        LogUtils.log(message: "旁路合图设置(远程)", level: .info)
        let user = AgoraLiveTranscodingUser()
        user.rect = CGRect(x: 0.5 * videoSize.height,
                           y: 0.1 * videoSize.width,
                           width: 0.5 * videoSize.height,
                           height: 0.5 * videoSize.width)
        user.uid = uid
        user.zOrder = 2
        liveTranscoding.add(user)
        engine.updateRtmpTranscoding(liveTranscoding)
    }
    
    func destroyRtc() {
        agoraKit.delegate = nil
        
        if mode == .push {
            leaveRtcByPush()
        }
        else {
            leaveRtcByPassPush()
        }
        agoraKit = nil
        AgoraRtcEngineKit.destroy()
    }
}

extension CDNPlayerViewControllerHost: AgoraRtcEngineDelegate {
    func rtcEngine(_ engine: AgoraRtcEngineKit,
                   didJoinChannel channel: String,
                   withUid uid: UInt,
                   elapsed: Int) {
        LogUtils.log(message: "didJoinChannel", level: .info)
        setMergeVideoLocal(engine: engine, uid: uid)
        engine.startRtmpStream(withTranscoding: pushUrlString,
                               transcoding: liveTranscoding)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit,
                   didJoinedOfUid uid: UInt,
                   elapsed: Int) {
        LogUtils.log(message: "didJoinChannel", level: .info)
        
        setMergeVideoRemote(engine: engine, uid: uid)
        mainView.setRemoteViewHidden(hidden: false)
        setupRemoteVideo(view: mainView.renderViewRemote,
                             uid: uid)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, streamUnpublishedWithUrl url: String) {
        LogUtils.log(message: "streamUnpublishedWithUrl", level: .info)
        let option = AgoraLeaveChannelOptions()
        option.stopMicrophoneRecording = false
        agoraKit.leaveChannel(option)

        if mode == .push {
            DispatchQueue.main.async { [weak self] in
                self?.joinRtcByPush()
            }
        }
    }
}

extension CDNPlayerViewControllerHost: AgoraDirectCdnStreamingEventDelegate {
    func onDirectCdnStreamingStateChanged(_ state: AgoraDirectCdnStreamingState,
                                          error: AgoraDirectCdnStreamingError,
                                          message: String?) {
        if state == .stopped {
            if mode == .byPassPush {
                DispatchQueue.main.async { [weak self] in
                    self?.joinRtcByPassPush()
                }
            }
        }
    }
}
