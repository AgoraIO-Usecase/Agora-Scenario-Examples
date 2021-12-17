//
//  SyncManager.swift
//  RtmSyncManager
//
//  Created by xianing on 2021/9/12.
//

import Foundation


public class SyncError: NSObject, LocalizedError {
    private let message: String

    override public var description: String {
        return message
    }

    public var errorDescription: String? {
        return message
    }

    public init(message: String) {
        self.message = message
    }
}

open class Scene: Codable {

    public var id: String
    public var userId: String
    public var property: [String:String]?

    public init(id: String, userId: String, property: [String:String]?) {
        self.id = id
        self.userId = userId
        self.property = property
    }
    
    func toJson() -> String {
        var dict = [String:String]()
        dict["id"] = id
        dict["userId"] = userId
        let _ = self.property?.map({ (key,value) in
            dict[key] = value
        })
        return Utils.getJson(dict: dict as NSDictionary)
    }
}

public protocol IAgoraModel {
    var sceneId: String { get set }
    func toDictionary() -> [String: Any?]
}

public protocol IObject {
    func getId() throws -> String
    func toObject<T>() throws -> T? where T: Decodable
    func getPropertyWith(key: String, type: Any.Type) throws -> Any?
    func toJson() -> String?
}

public protocol IObjectDelegate {
    func onSuccess(result: IObject) -> Void
    func onFailed(code: Int, msg: String) -> Void
}

public protocol IObjectListDelegate {
    func onSuccess(result: [IObject]) -> Void
    func onFailed(code: Int, msg: String) -> Void
}

public protocol IDocumentReferenceDelegate {
    func onSuccess() -> Void
    func onFailed(code: Int, msg: String) -> Void
}

public class DocumentReference {
    public var className: String {
        return parent!.className
    }

    public let id: String
    public let parent: CollectionReference?
    let manager: SyncManager

    public var whereEQ = [String: Any]()

    public init(manager: SyncManager, parent: CollectionReference?, id: String) {
        self.manager = manager
        self.parent = parent
        self.id = id
    }

    public func whereEqual(key: String, value: Any) -> DocumentReference {
        whereEQ[key] = value
        return self
    }

    public func get(key: String?, delegate: IObjectDelegate) {
        manager.get(reference: self, key: key, delegate: delegate)
    }

    public func update(data: [String: Any?], key: String?, delegate: IObjectDelegate?) {
        manager.update(reference: self, data: data, key: key, delegate: delegate)
    }

    public func delete(delegate: IDocumentReferenceDelegate?) {
        manager.delete(reference: self, delegate: delegate)
    }

    @discardableResult
    public func subscribe(key: String?, delegate: ISyncManagerEventDelegate) -> ISyncManagerLiveQuery {
        return manager.subscribe(reference: self, key: key, delegate: delegate)
    }
    
    public func unsubscribe(key: String?) {
        return manager.unsubscribe(reference: self, key: key)
    }
}

public class SceneReference: DocumentReference {
    override public var className: String {
        return id
    }

    public init(manager: SyncManager, id: String) {
        super.init(manager: manager, parent: nil, id: id)
    }

    public func collection(className: String) -> CollectionReference {
        return CollectionReference(manager: manager, parent: self, className: className)
    }
}

public class CollectionReference {
    private let manager: SyncManager
    public let className: String
    public let parent: SceneReference

    public init(manager: SyncManager, parent: SceneReference, className: String) {
        self.manager = manager
        self.className = className
        self.parent = parent
    }

    public func document(id: String = "") -> DocumentReference {
        return DocumentReference(manager: manager, parent: self, id: id)
    }

    public func add(data: [String: Any?], delegate: IObjectDelegate?) {
        manager.add(reference: self, data: data, delegate: delegate)
    }

    public func get(delegate: IObjectListDelegate) {
        manager.get(reference: self, delegate: delegate)
    }

    public func delete(delegate: IDocumentReferenceDelegate?) {
        manager.delete(reference: self, delegate: delegate)
    }
}

public protocol ISyncManagerEventDelegate {
    func onCreated(object: IObject) -> Void
    func onUpdated(object: IObject) -> Void
    func onDeleted(object: IObject?) -> Void
    func onSubscribed() -> Void
    func onError(code: Int, msg: String) -> Void
}

public protocol ISyncManagerLiveQuery {
    func unsubscribe() -> Void
}

public protocol ISyncManager {
    func joinScene(_ room: Scene, _ manager: SyncManager, _ delegate: IObjectDelegate?) -> SceneReference
    func getScenes(_ delegate: IObjectListDelegate) -> Void
    func get(_ reference: DocumentReference, _ key: String?, _ delegate: IObjectDelegate) -> Void
    func get(_ reference: CollectionReference, _ delegate: IObjectListDelegate) -> Void
    func add(_ reference: CollectionReference, _ data: [String: Any?], _ delegate: IObjectDelegate?) -> Void
    func update(_ reference: DocumentReference, _ key: String?, _ data: [String: Any?], _ delegate: IObjectDelegate?) -> Void
    func delete(_ reference: DocumentReference, _ delegate: IDocumentReferenceDelegate?) -> Void
    func delete(_ reference: CollectionReference, _ delegate: IDocumentReferenceDelegate?) -> Void
    func subscribe(_ reference: DocumentReference, key: String?, _ delegate: ISyncManagerEventDelegate?) -> ISyncManagerLiveQuery
    func unsubscribe(_ reference: DocumentReference, key: String?) -> Void
}

public class SyncManager: NSObject {
    private var proxy: ISyncManager

    public init(dict: [String:String]) {
        proxy = RtmSyncManager(dict: dict) as ISyncManager
    }
    
    public func joinScene(with: Scene, delegate: IObjectDelegate?) -> SceneReference {
        proxy.joinScene(with, self, delegate)
    }

    public func getScenes(delegate: IObjectListDelegate) {
        proxy.getScenes(delegate)
    }

    public func get(reference: DocumentReference, key: String?, delegate: IObjectDelegate) {
        proxy.get(reference, key, delegate)
    }

    public func get(reference: CollectionReference, delegate: IObjectListDelegate) {
        proxy.get(reference, delegate)
    }

    public func add(reference: CollectionReference, data: [String: Any?], delegate: IObjectDelegate?) {
        proxy.add(reference, data, delegate)
    }

    public func update(reference: DocumentReference, data: [String: Any?], key: String?, delegate: IObjectDelegate?) {
        proxy.update(reference, key, data, delegate)
    }

    public func delete(reference: DocumentReference, delegate: IDocumentReferenceDelegate?) {
        proxy.delete(reference, delegate)
    }

    public func delete(reference: CollectionReference, delegate: IDocumentReferenceDelegate?) {
        proxy.delete(reference, delegate)
    }
    
    @discardableResult
    public func subscribe(reference: DocumentReference, key: String?, delegate: ISyncManagerEventDelegate) -> ISyncManagerLiveQuery {
        proxy.subscribe(reference, key: key, delegate)
    }
    
    public func unsubscribe(reference: DocumentReference, key: String?) {
        proxy.unsubscribe(reference, key: key)
    }
}
