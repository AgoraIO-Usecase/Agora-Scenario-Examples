//
//  MainVM+RTC.swift
//  LivePK
//
//  Created by ZYP on 2021/9/26.
//

import Foundation
import AgoraRtcKit

extension MainVM {
    func joinRtcChannelLocal(channelName: String) {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.areaCode = AgoraAreaCode.GLOB.rawValue

        let logConfig = AgoraLogConfig()
        logConfig.level = .info
        config.logConfig = logConfig
        
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: config, delegate: self)
        agoraKit.enableVideo()
        agoraKit.setVideoEncoderConfiguration(AgoraVideoEncoderConfiguration(size: CGSize(width: 320, height: 640),
                                                                             frameRate: .fps15,
                                                                             bitrate: AgoraVideoBitrateStandard,
                                                                             orientationMode: .fixedPortrait))
        
        agoraKit.setChannelProfile(.liveBroadcasting)
        agoraKit.setDefaultAudioRouteToSpeakerphone(true)
        agoraKit.setParameters("{\"che.video.setQuickVideoHighFec\":true}")
        agoraKit.setParameters("{\"rtc.enable_quick_rexfer_keyframe\":true}")
        
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.autoSubscribeAudio = true
        mediaOptions.autoSubscribeVideo = true
        mediaOptions.publishLocalAudio = true
        mediaOptions.publishLocalVideo = true
        
        channelLocal = agoraKit.createRtcChannel(channelName)
        channelLocal?.setClientRole(.broadcaster)
        channelLocal?.setRtcChannelDelegate(self)
        let result = channelLocal?.join(byToken: nil, info: nil, uid: 0, options: mediaOptions) ?? -1
        if result != 0 {
            print("launchLocalChannel error: \(result)")
            return
        }
        print("launchLocalChannel success")
    }
    
    func joinRtcChannelRemote(channelName: String) {
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.autoSubscribeAudio = true
        mediaOptions.autoSubscribeVideo = true
        mediaOptions.publishLocalAudio = false
        mediaOptions.publishLocalVideo = false
        
        channelRemote = agoraKit.createRtcChannel(channelName)
        channelRemote?.setClientRole(.audience)
        channelRemote?.setRtcChannelDelegate(self)
        let result = channelRemote?.join(byToken: nil, info: nil, uid: 0, options: mediaOptions) ?? -1
        if result != 0 {
            print("launchRemoteChannel error: \(result)")
            return
        }
        print("launchRemoteChannel success")
    }
}
