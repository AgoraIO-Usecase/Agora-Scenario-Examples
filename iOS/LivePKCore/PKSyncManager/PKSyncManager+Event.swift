//
//  PKSyncManager+Event.swift
//  LivePKCore
//
//  Created by ZYP on 2021/9/27.
//

import Foundation
import AgoraRtmKit

extension PKSyncManager: AgoraRtmDelegate {
    func rtmKit(_ kit: AgoraRtmKit, connectionStateChanged state: AgoraRtmConnectionState, reason: AgoraRtmConnectionChangeReason) {
        Log.info(text:"connectionStateChanged \(state.rawValue)", tag: "PKSyncManager")
    }
}

extension PKSyncManager: AgoraRtmChannelDelegate {
    
    func channel(_ channel: AgoraRtmChannel, memberCount count: Int32) {}
    
    public func channel(_ channel: AgoraRtmChannel, attributeUpdate attributes: [AgoraRtmChannelAttribute]) {
        guard let channelName = channels.values.filter({ $0.channel == channel }).first?.name else {
            Log.info(text:"attributeUpdate, can not find channel name")
            return
        }
        delegate?.pkSyncDidUpdateAttribute(manager: self,
                                           channelName: channelName,
                                           attributes: attributes)
    }
}
