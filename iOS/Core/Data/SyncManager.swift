//
//  SyncManager.swift
//  Core
//
//  Created by XC on 2021/5/31.
//

import Foundation

public class AgoraRoomMember: Codable, IAgoraModel {
    public var id: String
    public var roomId: String
    public var streamId: UInt
    public var isMuted: Bool
    public var isSelfMuted: Bool
    public var role: Int = 0

    init(id: String, roomId: String, streamId: UInt, isMuted: Bool, isSelfMuted: Bool, role: Int) {
        self.id = id
        self.roomId = roomId
        self.streamId = streamId
        self.isMuted = isMuted
        self.isSelfMuted = isSelfMuted
        self.role = role
    }
    
    public func toDictionary() -> [String : Any] {
        return [
            "id": self.id,
            "roomId": self.roomId,
            "streamId": self.streamId,
            "isMuted": self.isMuted,
            "isSelfMuted": self.isSelfMuted,
            "role": self.role
        ]
    }
}

public class AgoraRoom: Codable {
    public static let TABLE: String = "AGORA_ROOM"
    
    public var id: String
    public var streamId: UInt
    
    init(id: String, streamId: UInt) {
        self.id = id
        self.streamId = streamId
    }
    
    public func toDictionary() -> [String : Any] {
        return [
            "id": self.id,
            "streamId": self.streamId,
        ]
    }
}

public protocol IAgoraModel {
    var roomId: String { get set }
}

public protocol IAgoraObject {
    func toObject<T>() throws -> T where T : Decodable
}

public protocol AgoraObjectDelegate {
    func onSuccess(result: IAgoraObject) -> Void
    func onFailed(code: Int, msg: String) -> Void
}

public protocol AgoraObjectListDelegate {
    func onSuccess(result: Array<IAgoraObject>) -> Void
    func onFailed(code: Int, msg: String) -> Void
}

public protocol AgoraDocumentReferenceDelegate {
    func onSuccess() -> Void
    func onFailed(code: Int, msg: String) -> Void
}

public class AgoraDocumentReference {
    public var className: String {
        return self.parent!.className
    }
    
    public let id: String
    public let parent: AgoraCollectionReference?
    
    public var whereEQ: [String: Any] = [String: Any]()
    
    public init(parent: AgoraCollectionReference?, id: String) {
        self.parent = parent
        self.id = id
    }
    
    public func whereEqual(key: String, value: AnyObject) -> AgoraDocumentReference {
        self.whereEQ[key] = value
        return self
    }
    
    public func get(delegate: AgoraObjectDelegate) {
        SyncManager.shared.get(reference: self, delegate: delegate)
    }
    
    public func update(key: String, value: Any, delegate: AgoraObjectDelegate) {
        SyncManager.shared.update(reference: self, key: key, value: value, delegate: delegate)
    }
    
    public func delete(delegate: AgoraDocumentReferenceDelegate) {
        SyncManager.shared.delete(reference: self, delegate: delegate)
    }
    
    public func subscribe(delegate: SyncManagerEventDelegate) -> ISyncManagerLiveQuery {
        return SyncManager.shared.subscribe(reference: self, delegate: delegate)
    }
}

public class AgoraRoomReference: AgoraDocumentReference {
    public override var className: String {
        return AgoraRoom.TABLE
    }
    
    public init(id: String) {
        super.init(parent: nil, id: id)
    }
    
    public func collection(className: String) -> AgoraCollectionReference {
        return AgoraCollectionReference(parent: self, className: className)
    }
}

public class AgoraCollectionReference {
    private var documentRef: AgoraDocumentReference? = nil
    
    public let className: String
    public let parent: AgoraRoomReference
    
    public init(parent: AgoraRoomReference, className: String) {
        self.className = className
        self.parent = parent
    }
    
    public func document(id: String) -> AgoraDocumentReference {
        let ref = AgoraDocumentReference(parent: self, id: id)
        self.documentRef = ref
        return ref
    }
    
    public func add(data: [String: Any], delegate: AgoraObjectDelegate) {
        SyncManager.shared.add(reference: self, data: data, delegate: delegate)
    }
    
    public func get(delegate: AgoraObjectListDelegate) {
        SyncManager.shared.get(reference: self, delegate: delegate)
    }
}

public protocol SyncManagerEventDelegate {
    func onCreated(object: IAgoraObject) -> Void
    func onUpdated(object: IAgoraObject) -> Void
    func onDeleted(objectId: String) -> Void
    func onError(code: Int, msg: String) -> Void
}

public protocol ISyncManagerLiveQuery {
    func unsubscribe() -> Void
}

public protocol ISyncManager {
    func createAgoraRoom(_ room: AgoraRoom, _ delegate: AgoraObjectDelegate) -> Void
    func getAgoraRooms(_ delegate: AgoraObjectListDelegate) -> Void
    func get(_ reference: AgoraDocumentReference, _ delegate: AgoraObjectDelegate) -> Void
    func get(_ reference: AgoraCollectionReference, _ delegate: AgoraObjectListDelegate) -> Void
    func add(_ reference: AgoraCollectionReference, _ data: [String: Any], _ delegate: AgoraObjectDelegate) -> Void
    func update(_ reference: AgoraDocumentReference, _ key: String, _ value: Any, _ delegate: AgoraObjectDelegate) -> Void
    func delete(_ reference: AgoraDocumentReference, _ delegate: AgoraDocumentReferenceDelegate) -> Void
    func subscribe(_ reference: AgoraDocumentReference, _ delegate: SyncManagerEventDelegate) -> ISyncManagerLiveQuery
}

public class SyncManager {
    private var proxy: ISyncManager!
    public static var shared: SyncManager = SyncManager()

    private init() {
        self.proxy = InjectionService.shared.resolve(ISyncManager.self)
    }

    public func getRoom(id: String) -> AgoraRoomReference {
        return AgoraRoomReference(id: id)
    }

    public func createAgoraRoom(room: AgoraRoom, delegate: AgoraObjectDelegate) {
        proxy.createAgoraRoom(room, delegate)
    }

    public func getAgoraRooms(delegate: AgoraObjectListDelegate) {
        proxy.getAgoraRooms(delegate)
    }

    public func get(reference: AgoraDocumentReference, delegate: AgoraObjectDelegate) {
        proxy.get(reference, delegate)
    }

    public func get(reference: AgoraCollectionReference, delegate: AgoraObjectListDelegate) {
        proxy.get(reference, delegate)
    }

    public func add(reference: AgoraCollectionReference, data: [String: Any], delegate: AgoraObjectDelegate) {
        proxy.add(reference, data, delegate)
    }

    public func update(reference: AgoraDocumentReference, key: String, value: Any, delegate: AgoraObjectDelegate) {
        proxy.update(reference, key, value, delegate)
    }

    public func delete(reference: AgoraDocumentReference, delegate: AgoraDocumentReferenceDelegate) {
        proxy.delete(reference, delegate)
    }

    public func subscribe(reference: AgoraDocumentReference, delegate: SyncManagerEventDelegate) -> ISyncManagerLiveQuery {
        return proxy.subscribe(reference, delegate)
    }
}
