//
//  RtmSyncManager.swift
//  RtmSyncManager
//
//  Created by xianing on 2021/9/12.
//

import Foundation
import AgoraRtmKit

public class RtmSyncManager: NSObject {
    var rtmKit: AgoraRtmKit?
    var onCreateBlocks = [AgoraRtmChannel : OnSubscribeBlock]()
    var onUpdatedBlocks = [AgoraRtmChannel : OnSubscribeBlock]()
    var onDeletedBlocks = [AgoraRtmChannel : OnSubscribeBlock]()
    var cachedAttrs = [AgoraRtmChannel : [AgoraRtmChannelAttribute]]()
    var channels = [String : AgoraRtmChannel]()
    var channelName: String!
    var sceneName: String!
    
    /// init
    /// - Parameters:
    ///   - config: config
    ///   - complete: white `code = 0` is success, else error
    init(config: Config,
         complete: SuccessBlockInt?) {
        super.init()
        self.channelName = config.channelName
        self.rtmKit = AgoraRtmKit.init(appId: config.appId,
                                       delegate: self)!
        let uid = UUID().uuid16string()
        rtmKit?.login(byToken: nil,
                      user: uid,
                      completion: { code in
                        complete?(code.rawValue)
                      })
        guard let channel = rtmKit?.createChannel(withId: channelName, delegate: self) else {
            return
        }
        channel.join(completion: nil)
        channels[channelName] = channel
    }
}

extension RtmSyncManager: AgoraRtmDelegate {
    public func rtmKit(_ kit: AgoraRtmKit, connectionStateChanged state: AgoraRtmConnectionState, reason: AgoraRtmConnectionChangeReason) {
        print("connectionStateChanged \(state.rawValue)")
    }
}

extension RtmSyncManager: AgoraRtmChannelDelegate {
    public func channel(_ channel: AgoraRtmChannel, attributeUpdate attributes: [AgoraRtmChannelAttribute]) {
        /// Log
        let channelName = channels.filter({ $0.value == channel }).first?.key ?? "channel name not found"
        let attributeStrings = attributes.map({ "\($0.key) : \($0.value)" })
        Log.info(text: "--- attributeUpdate in channel \(channel.description), name: \(channelName)", tag: "RtmSyncManager")
        Log.info(text: "--- attributeUpdate attrs: \(attributeStrings)", tag: "RtmSyncManager")
        
        notifyObserver(channel: channel, attributes: attributes)
    }
    
    public func channel(_ channel: AgoraRtmChannel, memberJoined member: AgoraRtmMember) {
        print("memberJoined \(member.userId)")
    }
}
