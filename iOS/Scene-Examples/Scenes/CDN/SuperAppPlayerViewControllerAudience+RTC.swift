//
//  SuperAppPlayerViewControllerAudience+RTC.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/28.
//

import UIKit
import AgoraRtcKit

extension SuperAppPlayerViewControllerAudience {
    var videoSize: CGSize { .init(width: 640, height: 360) }
    
    func initMediaPlayer(useAgoraCDN: Bool) {
        let rtcConfig = AgoraRtcEngineConfig()
        rtcConfig.appId = config.appId
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcConfig,
                                                  delegate: self)
        mediaPlayer = agoraKit.createMediaPlayer(with: self)
        mediaPlayer.setView(mainView.renderViewLocal)
        mediaPlayer.setRenderMode(.hidden)
        if useAgoraCDN {
            LogUtils.log(message: "use AgoraCDN", level: .info)
            mediaPlayer.open(pullUrlString,
                             startPos: 0)
        }
        else {
            LogUtils.log(message: "not use AgoraCDN", level: .info)
            mediaPlayer.open(pullUrlString, startPos: 0)
        }
        LogUtils.log(message: pullUrlString, level: .info)
    }
    
    func joinRtc() {
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
                                                         mirrorMode: .auto)
        agoraKit.enableVideo()
        agoraKit.setVideoEncoderConfiguration(videoConfig)
        setupLocalVideo(view: mainView.renderViewLocal)
        
        agoraKit.setDefaultAudioRouteToSpeakerphone(true)
        
        let ret = agoraKit.joinChannel(byToken: nil,
                                       channelId: self.config.sceneId,
                                       info: nil,
                                       uid: 0,
                                       joinSuccess: nil)
        if ret != 0 {
            LogUtils.log(message: "joinRtcByPush error \(ret)", level: .error)
            return
        }
    }
    
    func leaveRtc() { /** 离开RTC方式 **/
        agoraKit.leaveChannel(nil)
    }
    
    func setupLocalVideo(view: UIView) {
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
    
    func destroyRtc() {
        if mode == .pull {
            if mediaPlayer != nil {
                mediaPlayer.stop()
                agoraKit.destroyMediaPlayer(mediaPlayer)
                mediaPlayer = nil
            }
        }
        else {
            agoraKit.leaveChannel(nil)
        }
    }
}

// MARK: - AgoraRtcEngineDelegate
extension SuperAppPlayerViewControllerAudience: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit,
                   didJoinChannel channel: String,
                   withUid uid: UInt,
                   elapsed: Int) {
        LogUtils.log(message: "didJoinChannel channel: \(channel), uid: \(uid) ", level: .info)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit,
                   didJoinedOfUid uid: UInt,
                   elapsed: Int) {
        LogUtils.log(message: "didJoinedOfUid", level: .info)
        mainView.setRemoteViewHidden(hidden: false)
        setupRemoteVideo(view: mainView.renderViewRemote,
                         uid: uid)
    }
}

// MARK: - AgoraRtcMediaPlayerDelegate
extension SuperAppPlayerViewControllerAudience: AgoraRtcMediaPlayerDelegate {
    func agoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol,
                             didChangedTo state: AgoraMediaPlayerState,
                             error: AgoraMediaPlayerError) {
        LogUtils.log(message: "agoraRtcMediaPlayer didChangedTo \(state.rawValue) \(error.rawValue)", level: .info)
        if state == .openCompleted {
            LogUtils.log(message: "openCompleted", level: .info)
            mediaPlayer.play()
        }
    }
}
