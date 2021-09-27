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
        rtm = AgoraRtmKit(appId: appId, delegate: self)
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
            Log.errorText(text: "rtm join error: \(result.rawValue)", tag: "PKSyncManager")
            throw SyncError(domain: .join, code: result.rawValue)
        }
        
        Log.info(text: "rtm join channel success \(channelName)", tag: "PKSyncManager")
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
    
//    func getAttributes(channelName: String) throws -> [Attribute] {
//        let semp = DispatchSemaphore(value: 0)
//        var result: AgoraRtmProcessAttributeErrorCode = .attributeOperationErrorOk
//        var attributes: [Attribute]?
//        rtm.getChannelAllAttributes(channelName) { attrs, code in
//            result = code
//            attributes = attrs
//            semp.signal()
//        }
//
//        semp.wait()
//        guard result == .attributeOperationErrorOk, let `attributes` = attributes else {
//            throw SyncError(domain: .getAttributes, code: result.rawValue)
//        }
//
//        return attributes
//    }
    
    func getAttributes(channelName: String) -> [Attribute]? {
        let semp = DispatchSemaphore(value: 0)
        var result: AgoraRtmProcessAttributeErrorCode = .attributeOperationErrorOk
        var attributes: [Attribute]?
        rtm.getChannelAllAttributes(channelName) { attrs, code in
            result = code
            attributes = attrs
            semp.signal()
        }

        semp.wait()
        guard result == .attributeOperationErrorOk, let `attributes` = attributes else {
            return nil
        }

        return attributes
    }
    
    func deleteAttributes(channelName: String, keys: [String]) {
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        
        rtm.deleteChannel(channelName, attributesByKeys: keys, options: option, completion: { code in
            if code != .attributeOperationErrorOk {
                Log.info(text: "rtm attr success \(channelName)", tag: "PKSyncManager")
            }
            else {
                Log.info(text: "rtm delete attr success \(channelName)", tag: "PKSyncManager")
            }
        })
    }
}
