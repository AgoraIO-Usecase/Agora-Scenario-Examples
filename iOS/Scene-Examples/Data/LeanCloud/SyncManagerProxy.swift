//
//  SyncManagerProxy.swift
//  Scene-Examples_LeanCloud
//
//  Created by XC on 2021/6/1.
//
#if LEANCLOUD
import Foundation
import Core
import LeanCloud

class AgoraObject: IAgoraObject {
    private let object: LCObject
    
    init(object: LCObject) {
        self.object = object
    }
    
    func toObject<T>() throws -> T where T : Decodable {
        let data: Data
        if #available(iOS 11.0, *) {
            data = try NSKeyedArchiver.archivedData(withRootObject: self.object, requiringSecureCoding: false)
        } else {
            data = NSKeyedArchiver.archivedData(withRootObject: self.object)
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}

class AgoraLiveQuery: ISyncManagerLiveQuery {
    private var liveQuery: LiveQuery?
    private let className: String
    
    init(className: String, liveQuery: LiveQuery?) {
        self.className = className
        self.liveQuery = liveQuery
    }
    
    func unsubscribe() {
        self.liveQuery?.unsubscribe { (result) in
            switch result {
            case .success:
                Logger.log(message: "----- unsubscribe \(self.className) success -----", level: .info)
                break
            case .failure(error: let error):
                Logger.log(message: "----- unsubscribe \(self.className) error:\(error) -----", level: .error)
            }
        }
        self.liveQuery = nil
    }
}

class LeanCloudSyncProxy: ISyncManagerProxy {
    
    func createAgoraRoom(_ room: AgoraRoom, _ delegate: AgoraObjectDelegate) {
        let object = LCObject(className: AgoraRoom.TABLE)
        let acl = LCACL()
        acl.setAccess([.read, .write], allowed: true)
        object.ACL = acl
        object.save(completionQueue: Database.completionQueue, completion: { result in
            switch result {
            case .success:
                delegate.onSuccess(result: AgoraObject(object: object))
            case .failure(error: let error):
                delegate.onFailed(code: error.code, msg: error.description)
            }
        })
    }
    
    func getAgoraRooms(_ delegate: AgoraObjectListDelegate) {
        let query = LCQuery(className: AgoraRoom.TABLE)
        query.find (completionQueue: Database.completionQueue, completion: { result in
            switch result {
            case .success(objects: let list):
                delegate.onSuccess(result: list.map { (object: LCObject) in
                    AgoraObject(object: object)
                })
            case .failure(error: let error):
                delegate.onFailed(code: error.code, msg: error.description)
            }
        })
    }
    
    func get(_ reference: AgoraDocumentReference, _ delegate: AgoraObjectDelegate) {
        let query = LCQuery(className: reference.className)
        query.get(reference.id, completionQueue: Database.completionQueue, completion: { result in
            switch result {
            case .success(object: let data):
                delegate.onSuccess(result: AgoraObject(object: data))
            case .failure(error: let error):
                delegate.onFailed(code: error.code, msg: error.description)
            }
        })
    }
    
    func get(_ reference: AgoraCollectionReference, _ delegate: AgoraObjectListDelegate) {
        let roomId = reference.parent.id
        let roomObject = LCObject(className: AgoraRoom.TABLE, objectId: roomId)
        let query = LCQuery(className: reference.className)
        do {
            try query.where("roomId", .equalTo(roomObject))
            query.find (completionQueue: Database.completionQueue, completion: { result in
                switch result {
                case .success(objects: let list):
                    delegate.onSuccess(result: list.map { (object: LCObject) in
                        AgoraObject(object: object)
                    })
                case .failure(error: let error):
                    delegate.onFailed(code: error.code, msg: error.description)
                }
            })
        } catch {
            delegate.onFailed(code: -1, msg: error.localizedDescription)
        }
    }
    
    func add(_ reference: AgoraCollectionReference, _ data: [String: Any], _ delegate: AgoraObjectDelegate) {
        let className = reference.className
        let object = LCObject(className: className)
        let roomId = reference.parent.id
        let roomObject = LCObject(className: AgoraRoom.TABLE, objectId: roomId)
        do {
            for value in data {
                try object.append(value.key, element: value.value as! LCValueConvertible)
            }
            try object.append("roomId", element: roomObject)
            let acl = LCACL()
            acl.setAccess([.read, .write], allowed: true)
            object.ACL = acl
            object.save(completionQueue: Database.completionQueue, completion: { result in
                switch result {
                case .success:
                    delegate.onSuccess(result: AgoraObject(object: object))
                case .failure(error: let error):
                    delegate.onFailed(code: error.code, msg: error.description)
                }
            })
        } catch {
            delegate.onFailed(code: -1, msg: error.localizedDescription)
        }
    }
    
    func update(_ reference: AgoraDocumentReference, _ key: String, _ value: Any, _ delegate: AgoraObjectDelegate) {
        let object = LCObject(className: reference.className, objectId: reference.id)
        do {
            try object.append(key, element: value as! LCValueConvertible)
            let acl = LCACL()
            acl.setAccess([.read, .write], allowed: true)
            object.ACL = acl
            object.save(completionQueue: Database.completionQueue, completion: { result in
                switch result {
                case .success:
                    delegate.onSuccess(result: AgoraObject(object: object))
                case .failure(error: let error):
                    delegate.onFailed(code: error.code, msg: error.description)
                }
            })
        } catch {
            delegate.onFailed(code: -1, msg: error.localizedDescription)
        }
    }
    
    func delete(_ reference: AgoraDocumentReference, _ delegate: AgoraDocumentReferenceDelegate) {
        let object = LCObject(className: reference.className, objectId: reference.id)
        object.delete { _result in
            switch _result {
            case .success:
                delegate.onSuccess()
            case .failure(error: let error):
                delegate.onFailed(code: error.code, msg: error.description)
            }
        }
    }
    
    func subscribe(_ reference: AgoraDocumentReference, _ delegate: SyncManagerEventDelegate) -> ISyncManagerLiveQuery {
        let className = reference.className
        let query = LCQuery(className: className)
        var liveQuery: LiveQuery?
        do {
            for eqs in reference.whereEQ {
                let key = eqs.key
                let value = eqs.value
                if (value is AgoraDocumentReference) {
                    let ref: AgoraDocumentReference = value as! AgoraDocumentReference
                    try query.where(key, .equalTo(LCObject(className: ref.className, objectId: ref.id)))
                } else {
                    try query.where(key, .equalTo(value as! LCValueConvertible))
                }
            }
            liveQuery = try LiveQuery(query: query, eventHandler: { (liveQuery, event) in
                Logger.log(message: "liveQueryEvent event:\(event)", level: .info)
                switch event {
                case .create(object: let object):
                    delegate.onCreated(object: AgoraObject(object: object))
                case .delete(object: let object):
                    delegate.onDeleted(objectId: object.objectId!.stringValue!)
                case .update(object: let object, updatedKeys: _):
                    delegate.onUpdated(object: AgoraObject(object: object))
                default: break
                }
            })
            liveQuery!.subscribe { result in
                switch result {
                case .success:
                    Logger.log(message: "----- subscribe \(className) success -----", level: .info)
                    return
                case .failure(error: let error):
                    Logger.log(message: "subscribe1 \(className) error:\(error)", level: .error)
                    delegate.onError(code: error.code, msg: error.description)
                    return
                }
            }
        } catch {
            Logger.log(message: "subscribe0 \(className) error:\(error)", level: .error)
            delegate.onError(code: -1, msg: error.localizedDescription)
        }
        return AgoraLiveQuery(className: className, liveQuery: liveQuery)
    }
}

#endif
