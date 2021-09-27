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
        let text = "channelName:\(channelName), attributeUpdate update: \(attributes.map({ $0.key + ":" + $0.value }))"
        Log.info(text: text, tag: "pkSyncDidUpdateAttribute")
        
        if loginInfo.role == .audience { /** handle if audience **/
            handleAttributiesForAudience(channelName: channelName, attributes: attributes)
            return
        }
        
        if loginInfo.role == .broadcaster, attributes.filter({ $0.key == kPKKey }).map({ $0.value }).first != nil { /** handle if broadcaster **/
            handleAttributiesForBroadcaster(channelName: channelName, attributes: attributes)
            return
        }
        
        if attributes.filter({ $0.key == kPKKey }).map({ $0.value }).first == nil { /** remove remote **/
            handleAttributiesForRemoveRemote()
            return
        }
    }
}

extension MainVM: AgoraRtcEngineDelegate {
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        Log.info(text: "rtcEngine didOccurWarning \(warningCode.rawValue)", tag: "AgoraRtcEngineDelegate")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        Log.info(text: "rtcEngine didOccurError \(errorCode.rawValue)", tag: "AgoraRtcEngineDelegate")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        Log.info(text: "rtcEngine didJoinedOfUid \(uid)", tag: "AgoraRtcEngineDelegate")
    }
}

extension MainVM: AgoraRtcChannelDelegate {
    
    func rtcChannelDidJoin(_ rtcChannel: AgoraRtcChannel, withUid uid: UInt, elapsed: Int) {
        Log.info(text: "rtcChannelDidJoin  \(rtcChannel.getId() ?? "") with uid \(uid) elapsed \(elapsed)ms",
                 tag: "AgoraRtcChannelDelegate")
        if rtcChannel == channelLocal {
            channelLocal?.addPublishStreamUrl("rtmp://mdetest.push.agoramde.agoraio.cn/live/\(loginInfo.roomName)", transcodingEnabled: false)
        }
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didOccurWarning warningCode: AgoraWarningCode) {
        Log.info(text: "rtcChannel didOccurWarning: \(rtcChannel.getId() ?? ""), warning: \(warningCode)",
                 tag: "AgoraRtcChannelDelegate")
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didOccurError errorCode: AgoraErrorCode) {
        Log.info(text: "rtcChannel didOccurError error: \(errorCode.rawValue)",
                 tag: "AgoraRtcChannelDelegate")
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didJoinedOfUid uid: UInt, elapsed: Int) {
        Log.info(text: "rtcChannel \(rtcChannel.getId() ?? "") didJoinedOfUid: \(uid)",
                 tag: "AgoraRtcChannelDelegate")
        if rtcChannel != channelLocal {
            let info = RenderInfo(isLocal: false, uid: uid, roomName: rtcChannel.getId() ?? "", type: .rtc)
            renderInfos.append(info)
            invokeDidUpdateRenderInfos(renders: renderInfos)
        }
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        Log.info(text: "rtcChannel \(rtcChannel.getId() ?? "") didOfflineOfUid: \(uid)",
                 tag: "AgoraRtcChannelDelegate")
    }
    
    func rtcChannel(_ rtcChannel: AgoraRtcChannel, rtmpStreamingChangedToState url: String, state: AgoraRtmpStreamingState, errorCode: AgoraRtmpStreamingErrorCode) {
        Log.info(text: "rtmpStreamingChangedToState \(state.rawValue)",
                 tag: "AgoraRtcChannelDelegate")
    }
}

extension MainVM: AgoraMediaPlayerDelegate {
    func agoraMediaPlayer(_ playerKit: AgoraMediaPlayer, didChangedTo state: AgoraMediaPlayerState, error: AgoraMediaPlayerError) {
        Log.info(text: "agoraMediaPlayer didChangedTo \(state.rawValue)",
                 tag: "AgoraRtcChannelDelegate")
        if state == .openCompleted {
            playerKit.play()
        }
    }
}
