//
//  PKSyncManager.swift
//  LivePK
//
//  Created by ZYP on 2021/9/26.
//

import Foundation
import AgoraRtmKit
import UIKit


protocol PKSyncManagerDelegate: NSObjectProtocol {
    func pkSyncDidUpdateAttribute(manager: PKSyncManager,
                                  channelName: String,
                                  attributes: [PKSyncManager.Attribute])
}

extension PKSyncManager {
    struct ChannelInfo {
        let name: String
        let channel: AgoraRtmChannel
    }
}


class PKSyncManager: NSObject {
    typealias Attribute = AgoraRtmChannelAttribute
    typealias CompletedBlock = (SyncError?) -> ()
    typealias ReadAttributeBlock = ([Attribute]?, SyncError?) -> ()
    typealias ChannelName = String
    
    let appId: String
    var rtm: AgoraRtmKit!
    var uid: String!
    weak var delegate: PKSyncManagerDelegate?
    var channels = [ChannelName : ChannelInfo]()
    
    init(appId: String) {
        self.appId = appId
        super.init()
        rtm = AgoraRtmKit(appId: appId, delegate: self)
        uid = String(UInt.random(in: 1001...2000))
    }
    
    deinit {
        for info in channels.values {
            info.channel.leave()
        }
        rtm.logout(completion: nil)
    }
    
    func login() throws {
        let semp = DispatchSemaphore(value: 0)
        var result: AgoraRtmLoginErrorCode = .ok
        
        rtm.login(byToken: nil, user: uid) { code in
            result = code
            semp.signal()
        }
        
        semp.wait()
        if result != .ok { throw SyncError(domain: .login, code: result.rawValue) }
    }
    
    func join(channelName: ChannelName) throws {
        guard let channel = rtm.createChannel(withId: channelName, delegate: self) else {
            fatalError("createChannel fail")
        }
        channels[channelName] = ChannelInfo(name: channelName, channel: channel)
        
        let semp = DispatchSemaphore(value: 0)
        var result: AgoraRtmJoinChannelErrorCode = .channelErrorOk
        channel.join(completion: { (code) in
            result = code
            semp.signal()
        })
        
        semp.wait()
        if result != .channelErrorOk {
            print("join error: \(result)")
            throw SyncError(domain: .join, code: result.rawValue)
        }
        print("PKSyncManager join channel success \(channelName)")
    }
    
    private func joinChannel(channelName: ChannelName,
                     completed: @escaping CompletedBlock) {
        guard let channel = rtm.createChannel(withId: channelName, delegate: self) else {
            fatalError("createChannel fail")
        }
        
        channels[channelName] = ChannelInfo(name: channelName, channel: channel)
        channel.join(completion: { (code) in
            code == .channelErrorOk ? completed(nil) : completed(SyncError(domain: .join, code: code.rawValue))
        })
    }
    
    func updateAttribute(channelName: ChannelName,
                         attributes: [Attribute],
                         completed: @escaping CompletedBlock) {
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        rtm.addOrUpdateChannel(channelName,
                               attributes: attributes,
                               options: option,
                               completion: { code in
            code == .attributeOperationErrorOk ? completed(nil) : completed(SyncError(domain: .updateAttributes, code: code.rawValue))
        })
    }
    
    func fetchAttributes(channelName: String,
                         completed: @escaping ReadAttributeBlock) {
        rtm.getChannelAllAttributes(channelName) { attrs, code in
            code == .attributeOperationErrorOk ? completed(attrs, nil) : completed(nil, SyncError(domain: .getAttributes, code: code.rawValue))
        }
    }
    
    func getAttributes(channelName: String) throws -> [Attribute] {
        let semp = DispatchSemaphore(value: 0)
        var result: AgoraRtmProcessAttributeErrorCode = .attributeOperationErrorOk
        var attributes: [Attribute]?
        rtm.getChannelAllAttributes(channelName) { attrs, code in
            result = code
            attributes = attrs
            semp.signal()
        }
        
        semp.wait()
        guard result == .attributeOperationErrorOk, let attributes = attributes else {
            throw SyncError(domain: .getAttributes, code: result.rawValue)
        }
        
        return attributes
    }
    
    func delete(channelName: String, keys: [String]) {
        guard channels.map({ $0.0 }).contains(channelName) else {
            return
        }
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        rtm.deleteChannel(channelName, attributesByKeys: keys, options: option) { code in
            if code != .attributeOperationErrorOk { print("PKSyncManager delete fail") }
            else { print("PKSyncManager delete success \(channelName)") }
        }
    }
    
    func leaveChannel(channelName: String) {
        guard channels.map({ $0.0 }).contains(channelName) else {
            return
        }
        print("PKSyncManager leave \(channelName)")
        let channel = channels.values.filter({ $0.name == channelName }).map({ $0.channel }).first
        channel?.leave(completion: { code in
            if code == .ok { print("PKSyncManager leave \(channelName)") }
            else {
                print("PKSyncManager leave error \(code.rawValue)")
            }
        })
        channels = channels.filter({ $0.key != channelName })
    }
    
}

extension PKSyncManager: AgoraRtmDelegate {
    func rtmKit(_ kit: AgoraRtmKit, connectionStateChanged state: AgoraRtmConnectionState, reason: AgoraRtmConnectionChangeReason) {
        print("PKSyncManager connectionStateChanged \(state.rawValue)")
    }
}

extension PKSyncManager: AgoraRtmChannelDelegate {
    
    func channel(_ channel: AgoraRtmChannel, memberCount count: Int32) {
        print("PKSyncManager memberCount \(count)")
    }
    
    public func channel(_ channel: AgoraRtmChannel, attributeUpdate attributes: [AgoraRtmChannelAttribute]) {
        print("PKSyncManager attributeUpdate update: \(attributes.map({ $0.key + ":" + $0.value }))")
        guard let channelName = channels.values.filter({ $0.channel == channel }).first?.name else {
            print("can not find channel name")
            return
        }
        delegate?.pkSyncDidUpdateAttribute(manager: self,
                                           channelName: channelName,
                                           attributes: attributes)
    }
}
