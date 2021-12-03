//
//  RtmSyncManager.swift
//  RtmSyncManager
//
//  Created by xianing on 2021/9/12.
//

import Foundation
import AgoraRtmKit
import CommonCrypto

public class RtmSyncManager: NSObject, ISyncManager {
    var rtmKit: AgoraRtmKit? = nil
    var appId: String? = nil
    var uid: String? = nil
    var delegates: [AgoraRtmChannel: ISyncManagerEventDelegate] = [AgoraRtmChannel: ISyncManagerEventDelegate]()
    var cachedAttrs: [AgoraRtmChannel: [AgoraRtmChannelAttribute]] = [AgoraRtmChannel: [AgoraRtmChannelAttribute]]()
    var channels: [String:AgoraRtmChannel] = [String:AgoraRtmChannel]()
    var defaultChannel = "defaultchannel"
    
    init(dict: [String:String]) {
        super.init()
        if let channel = dict["defaultChannel"] {
            self.defaultChannel = channel
        }
        self.appId = dict["appId"]!
        self.rtmKit = AgoraRtmKit.init(appId: appId!, delegate: self)!
        self.uid = UUID().uuid16string()
        rtmKit?.login(byToken: nil, user: uid!, completion: nil)
        rtmKit?.createChannel(withId: defaultChannel, delegate: nil)
    }
    
    public func joinScene(_ room: Scene, _ manager: SyncManager, _ delegate: IObjectDelegate?) -> SceneReference {
        let channel = rtmKit?.createChannel(withId: room.id, delegate: self)
        channel?.join(completion: nil)
        channels[room.id] = channel
        let attr = AgoraRtmChannelAttribute()
        attr.key = room.id
        attr.value = room.toJson()
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = false
        rtmKit?.addOrUpdateChannel(defaultChannel, attributes: [attr], options:option, completion: { code in
            delegate?.onSuccess(result: attr.toAttribute())}
        )
        return SceneReference(manager: manager, id: room.id)
    }
    
    public func getScenes(_ delegate: IObjectListDelegate) {
        rtmKit?.getChannelAllAttributes(defaultChannel, completion: { list, errorCode in
            var res = [Attribute]()
            guard let attrs = list else {
                delegate.onFailed(code: -1, msg: "empty scene list")
                return
            }
            for item in attrs {
                let attr = Attribute(key: item.key, value: item.value)
                res.append(attr)
            }
            delegate.onSuccess(result: res)
        })
    }
    
    public func get(_ reference: DocumentReference, _ key: String?, _ delegate: IObjectDelegate) {
        let key = key ?? ""
        rtmKit?.getChannelAllAttributes(reference.className + key, completion: { res, error in
            guard let res = res, res.count > 0 else {
                delegate.onFailed(code: -1, msg: "none")
                return
            }
            delegate.onSuccess(result: Attribute(key: res[0].key ,value: res[0].value))
        })
    }
    
    public func get(_ reference: CollectionReference, _ delegate: IObjectListDelegate) {
        rtmKit?.getChannelAllAttributes(reference.className, completion: { res, error in
            guard let res = res, res.count > 0 else{
                delegate.onFailed(code: -1, msg: "empty")
                return
            }
            if let rtmChannel = self.rtmKit?.createChannel(withId: reference.className, delegate: self){
                if (self.cachedAttrs[rtmChannel]?.isEmpty ?? true) {
                    self.cachedAttrs[rtmChannel] = res
                }
            }
            var list = [Attribute]()
            for item in res {
                list.append(Attribute(key: item.key, value: item.value))
            }
            delegate.onSuccess(result: list)
        })
    }
    
    public func add(_ reference: CollectionReference, _ data: [String : Any?], _ delegate: IObjectDelegate?) {
        if channels[reference.className] == nil {
            let rtmChannel = rtmKit?.createChannel(withId: reference.className, delegate: self)
            channels[reference.className] = rtmChannel
            rtmChannel?.join(completion: nil)
        }
        let attr = AgoraRtmChannelAttribute()
        attr.key = UUID().uuid16string()
        attr.value = Utils.getJson(dict: data as NSDictionary)
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        rtmKit?.addOrUpdateChannel(reference.className, attributes: [attr], options: option, completion: { error in
            if let channel = self.channels[reference.className] {
                if var cache = self.cachedAttrs[channel] {
                    cache.append(attr)
                }
                else {
                    self.cachedAttrs[channel] = [attr]
                }
                if let delegate = self.delegates[channel] {
                    delegate.onCreated(object: attr.toAttribute())
                }
            }
            delegate?.onSuccess(result: Attribute(key: attr.key, value: attr.toAttribute().toJson() ?? ""))
        })
    }
    
    public func update(_ reference: DocumentReference, _ key: String?, _ data: [String : Any?], _ delegate: IObjectDelegate?) {
        let attr = AgoraRtmChannelAttribute()
        let item = Utils.getJson(dict: data as NSDictionary)
        attr.key = key ?? reference.id
        attr.value = item
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        let key = key ?? ""
        rtmKit?.addOrUpdateChannel(reference.className + key, attributes: [attr], options: option, completion: { error in
            if let channel = self.channels[reference.className + key] {
                if let delegate = self.delegates[channel] {
                    delegate.onUpdated(object: attr.toAttribute())
                }
            }
            delegate?.onSuccess(result: Attribute(key: reference.id, value: item))
        })
    }
    
    public func delete(_ reference: DocumentReference, _ delegate: IDocumentReferenceDelegate?) {
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        let keys = reference.id.isEmpty ? nil : [reference.id]
        rtmKit?.deleteChannel(reference.className, attributesByKeys: keys, options: option, completion: { error in
            if let channel = self.channels[reference.className] {
                if let delegate = self.delegates[channel], let cache = self.cachedAttrs[channel]{
                    var tempAttrs = [AgoraRtmChannelAttribute]()
                    let _ = cache.map {
                        if $0.key != reference.id {
                            tempAttrs.append($0)
                        } else {
                            delegate.onDeleted(object: $0.toAttribute())
                        }
                    }
                    self.cachedAttrs[channel] = tempAttrs
                }
            }
            delegate?.onSuccess()
        })
    }
    
    public func delete(_ reference: CollectionReference, _ delegate: IDocumentReferenceDelegate?) {
        let option = AgoraRtmChannelAttributeOptions()
        option.enableNotificationToChannelMembers = true
        rtmKit?.clearChannel(reference.className, options: option, attributesWithCompletion: { [weak self] error in
            guard let self = self else { return }
            if let channel = self.channels[reference.className] {
                if let delegate = self.delegates[channel] {
                    self.cachedAttrs.removeValue(forKey: channel)
                    delegate.onDeleted(object: nil)
                }
            }
            delegate?.onSuccess()
        })
    }
    
    @discardableResult
    public func subscribe(_ reference: DocumentReference, key: String?, _ delegate: ISyncManagerEventDelegate?) -> ISyncManagerLiveQuery {
        let callback = RtmLiveQuery.init(name: reference.className)
        let key = key ?? ""
        guard let rtmChannel = rtmKit?.createChannel(withId: reference.className + key, delegate: self) else {
            delegate?.onError(code: -1, msg: "yet join channel")
            return callback
        }
        channels[reference.className + key] = rtmChannel
        rtmChannel.join(completion: nil)
        self.delegates[rtmChannel] = delegate
        delegate?.onSubscribed()
        return callback
    }
    
    public func unsubscribe(_ reference: DocumentReference, key: String?) {
        let key = key ?? ""
        guard let rtmChannel = channels[reference.className + key] else {
            return
        }
        self.delegates.removeValue(forKey: rtmChannel)
    }
    
    
    func notifyObserver(channel: AgoraRtmChannel, attributes: [AgoraRtmChannelAttribute]) {
        // 根据channel, 判断出是哪种类型的更新 1. room属性 2. collection 3. roomlist(暂不支持)
        // room属性有一个delegate对象, 每一个collection也有一个delegate对象, 存放在一个map中
        // map的key是 channel名 或者是collection的classname
        guard let delegate = delegates[channel] else {
            return
        }
        if !channels.filter({ $0.value == channel }).isEmpty {
            let attribute = attributes.first
            guard let key = attribute?.key, let value = attribute?.value else {
                return
            }
            delegate.onUpdated(object: Attribute(key: key, value: value))
        } else {
            if let cache = self.cachedAttrs[channel] {
                var onlyA = [IObject]()
                var onlyB = [IObject]()
                var both = [IObject]()
                var temp = [String: AgoraRtmChannelAttribute]()
                
                for i in cache {
                    temp[i.key] = i
                }
                
                for b in attributes {
                    if let i = temp[b.key] {
                        if b.value != i.value {
                            both.append(b.toAttribute())
                        }
                        temp.removeValue(forKey: b.key)
                    }
                    else{
                        onlyB.append(b.toAttribute())
                    }
                }

                for i in temp.values {
                    onlyA.append(i.toAttribute())
                }
                
                for i in both {
                    delegate.onUpdated(object: i)
                }
                for i in onlyB {
                    delegate.onCreated(object: i)
                }
                for i in onlyA {
                    delegate.onDeleted(object: i)
                }
            }
        }
        cachedAttrs[channel] = attributes
    }
}

class RtmLiveQuery: ISyncManagerLiveQuery {
    private let className: String

    init(name: String) {
        self.className = name
    }

    func unsubscribe() {
        // do nothing for now
    }
}

extension RtmSyncManager: AgoraRtmDelegate {
    public func rtmKit(_ kit: AgoraRtmKit, connectionStateChanged state: AgoraRtmConnectionState, reason: AgoraRtmConnectionChangeReason) {
        print("connectionStateChanged \(state.rawValue)")
    }
}

extension RtmSyncManager: AgoraRtmChannelDelegate {
    public func channel(_ channel: AgoraRtmChannel, attributeUpdate attributes: [AgoraRtmChannelAttribute]) {
        print("attributeUpdate \(channel)")
        notifyObserver(channel: channel, attributes: attributes)
    }
    
    public func channel(_ channel: AgoraRtmChannel, memberJoined member: AgoraRtmMember) {
        print("memberJoined \(member.userId)")
    }
}

/* key 是对象存储中的id, 在rtm中是channelAttribute的key, value在对象存储中是一条记录, 在rtm中是一个json字符串 */
class Attribute: IObject, Equatable {
    
    
    var object: String
    var key: String
    func getId() throws -> String {
        return key
    }
    
    func getPropertyWith(key: String, type: Any.Type) throws -> Any? {
        let dict = Utils.getDict(text: object)
        return dict?[key]
    }
    
    func toJson() -> String? {
        var dict = Utils.toDictionary(jsonString: object)
        dict["objectId"] = key
        return Utils.toJsonString(dict: dict)
    }
    
    init(key: String, value: String) {
        self.key = key
        self.object = value
    }
    
    static func == (lhs: Attribute, rhs: Attribute) -> Bool {
        return lhs.object == rhs.object &&
        lhs.key == rhs.key
    }
    
    func toObject<T>() throws -> T? where T : Decodable {
        let jsonDecoder = JSONDecoder()
        if let data = object.data(using: .utf8) {
            return try jsonDecoder.decode(T.self, from: data)
        }
        return nil
    }
    
}

extension AgoraRtmChannelAttribute {
    func toAttribute() -> IObject {
        return Attribute(key: self.key, value: self.value)
    }
}

extension UUID {
    func uuid16string() -> String {
        return String(self.uuidString.md5)
    }
}

public extension String {
    /* ################################################################## */
    /**
     - returns: the String, as an MD5 hash.
     */
    var md5: String {
        let str = self.cString(using: String.Encoding.utf8)
        let strLen = CUnsignedInt(self.lengthOfBytes(using: String.Encoding.utf8))
        let digestLen = 16
        let result = UnsafeMutablePointer<CUnsignedChar>.allocate(capacity: digestLen)
        CC_MD5(str!, strLen, result)

        let hash = NSMutableString()

        for i in 0..<digestLen {
            hash.appendFormat("%02x", result[i])
        }

        result.deallocate()
        return hash as String
    }
}
