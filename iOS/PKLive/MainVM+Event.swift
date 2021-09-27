//
//  MainVM+Event.swift
//  LivePK
//
//  Created by ZYP on 2021/9/26.
//

import Foundation
import AgoraRtcKit
import AgoraMediaPlayer

extension MainVM: PKSyncManagerDelegate {
    func pkSyncDidUpdateAttribute(manager: PKSyncManager, channelName: String, attributes: [PKSyncManager.Attribute]) {
        if let formChannelName = attributes.filter({ $0.key == "PK" }).map({ $0.value }).first {
            guard renderInfos.count == 1 else {
                return
            }
            if Thread.current.isMainThread {
                joinRtcChannelRemote(channelName: formChannelName)
            }
            else {
                DispatchQueue.main.async {[weak self] in
                    self?.joinRtcChannelRemote(channelName: formChannelName)
                }
            }
        }
        else {
            if let remoteChannelName = renderInfos.filter({ !$0.isLocal }).first?.roomName {
                manager.leaveChannel(channelName: remoteChannelName)
            }
            renderInfos = renderInfos.filter({ $0.isLocal })
            invokeDidUpdateRenderInfos(renders: renderInfos)
            channelRemote?.leave()
            channelRemote = nil
        }
    }
}

extension MainVM: AgoraRtcEngineDelegate {
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        print("warning: \(warningCode)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        print("error: \(errorCode)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        print("didJoinedOfUid")
    }
}

extension MainVM: AgoraRtcChannelDelegate {
    
    func rtcChannelDidJoin(_ rtcChannel: AgoraRtcChannel, withUid uid: UInt, elapsed: Int) {
        print("Join \(rtcChannel.getId() ?? "") with uid \(uid) elapsed \(elapsed)ms")
        if rtcChannel == channelLocal {
            channelLocal?.addPublishStreamUrl("rtmp://mdetest.push.agoramde.agoraio.cn/live/\(loginInfo.roomName)", transcodingEnabled: false)
        }
        
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didOccurWarning warningCode: AgoraWarningCode) {
        print("channel: \(rtcChannel.getId() ?? ""), warning: \(warningCode)")
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didOccurError errorCode: AgoraErrorCode) {
        print("error: \(errorCode)")
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didJoinedOfUid uid: UInt, elapsed: Int) {
        print("--- didJoinedOfUid: \(uid)")
        if rtcChannel != channelLocal {
            let info = RenderInfo(isLocal: false, uid: uid, roomName: rtcChannel.getId() ?? "", type: .rtc)
            renderInfos.append(info)
            invokeDidUpdateRenderInfos(renders: renderInfos)
        }
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        print("didOfflineOfUid: \(uid)")
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, rtmpStreamingChangedToState url: String, state: AgoraRtmpStreamingState, errorCode: AgoraRtmpStreamingErrorCode) {
        print("rtmpStreamingChangedToState \(state.rawValue)")
    }
}

extension MainVM: AgoraMediaPlayerDelegate {
    func agoraMediaPlayer(_ playerKit: AgoraMediaPlayer, didChangedTo state: AgoraMediaPlayerState, error: AgoraMediaPlayerError) {
        print("agoraMediaPlayer  didChangedTo \(state.rawValue)")
        if state == .openCompleted {
            playerKit.play()
        }
    }
}
