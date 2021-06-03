//
//  SyncManagerProxy.swift
//  Scene-Examples_LeanCloud
//
//  Created by XC on 2021/6/1.
//
#if FIREBASE
import Foundation
import Firebase
import FirebaseFirestoreSwift
import Core

class AgoraObject: IAgoraObject {
    private let document: DocumentSnapshot
    
    init(document: DocumentSnapshot) {
        self.document = document
    }
    
    func toObject<T>() throws -> T where T : Decodable {
        return try document.data(as: T.self)!
    }
}

class AgoraLiveQuery: ISyncManagerLiveQuery {
    private var listenerRegistration: ListenerRegistration?
    private let className: String
    
    init(className: String, listenerRegistration: ListenerRegistration?) {
        self.className = className
        self.listenerRegistration = listenerRegistration
    }
    
    func unsubscribe() {
        listenerRegistration?.remove()
        listenerRegistration = nil
        Logger.log(message: "----- unsubscribe \(className) success -----", level: .info)
    }
}

class FirebaseSyncProxy: ISyncManagerProxy {
    
    func createAgoraRoom(_ room: AgoraRoom, _ delegate: AgoraObjectDelegate) {
        let className = AgoraRoom.TABLE
        let data: [String: Any] = ["createdAt": Timestamp()]
        var ref: DocumentReference? = nil
        ref = Database.db.collection(className).addDocument(data: data) { err in
            if let err = err {
                delegate.onFailed(code: -1, msg: err.localizedDescription)
            } else {
                ref!.getDocument { (document, error) in
                    if let error = error {
                        delegate.onFailed(code: -1, msg: error.localizedDescription)
                    } else if let document = document, document.exists {
                        delegate.onSuccess(result: AgoraObject(document: document))
                    } else {
                        delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(ref!.documentID)) does not exist!")
                    }
                }
            }
        }
    }
    
    func getAgoraRooms(_ delegate: AgoraObjectListDelegate) {
        let className = AgoraRoom.TABLE
        Database.db.collection(className).getDocuments { (querySnapshot: QuerySnapshot?, error: Error?) in
            if let error = error {
                delegate.onFailed(code: -1, msg: error.localizedDescription)
            } else if let querySnapshot = querySnapshot {
                delegate.onSuccess(result: querySnapshot.documents.map { snapshot in
                    return AgoraObject(document: snapshot)
                })
            } else {
                delegate.onFailed(code: -1, msg: "getAgoraRooms querySnapshot is nil!")
            }
        }
    }
    
    func get(_ reference: AgoraDocumentReference, _ delegate: AgoraObjectDelegate) {
        let className = reference.className
        let objectId = reference.id
        let query = Database.db.collection(className).document(objectId)
        query.getDocument { (document, error) in
            if let error = error {
                delegate.onFailed(code: -1, msg: error.localizedDescription)
            } else if let document = document, document.exists {
                delegate.onSuccess(result: AgoraObject(document: document))
            } else {
                delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(objectId)) does not exist!")
            }
        }
    }
    
    func get(_ reference: AgoraCollectionReference, _ delegate: AgoraObjectListDelegate) {
        let roomId = reference.parent.id
        let roomObject = Database.document(table: AgoraRoom.TABLE, id: roomId)
        let query = Database.db.collection(reference.className).whereField("roomId", isEqualTo: roomObject)
        query.getDocuments { (querySnapshot: QuerySnapshot?, error: Error?) in
            if let error = error {
                delegate.onFailed(code: -1, msg: error.localizedDescription)
            } else if let querySnapshot = querySnapshot {
                delegate.onSuccess(result: querySnapshot.documents.map { snapshot in
                    return AgoraObject(document: snapshot)
                })
            } else {
                delegate.onFailed(code: -1, msg: "get AgoraCollectionReference querySnapshot is nil!")
            }
        }
    }
    
    func add(_ reference: AgoraCollectionReference, _ data: [String : Any], _ delegate: AgoraObjectDelegate) {
        let className = reference.className
        let roomId = reference.parent.id
        let roomObject = Database.document(table: AgoraRoom.TABLE, id: roomId)
        var ref: DocumentReference? = nil
        var data = data
        data["createdAt"] = Timestamp()
        data["roomId"] = roomObject
        ref = Database.db.collection(className).addDocument(data: data) { error in
            if let error = error {
                delegate.onFailed(code: -1, msg: error.localizedDescription)
            } else {
                ref!.getDocument { (document, error) in
                    if let error = error {
                        delegate.onFailed(code: -1, msg: error.localizedDescription)
                    } else if let document = document, document.exists {
                        delegate.onSuccess(result: AgoraObject(document: document))
                    } else {
                        delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(ref!.documentID)) does not exist!")
                    }
                }
            }
        }
    }
    
    func update(_ reference: AgoraDocumentReference, _ key: String, _ value: Any, _ delegate: AgoraObjectDelegate) {
        let className = reference.className
        let ref = Database.document(table: className, id: reference.id)
        ref.updateData([key: value]) { error in
            if let error = error {
                delegate.onFailed(code: -1, msg: error.localizedDescription)
            } else {
                ref.getDocument { (document, error) in
                    if let error = error {
                        delegate.onFailed(code: -1, msg: error.localizedDescription)
                    } else if let document = document, document.exists {
                        delegate.onSuccess(result: AgoraObject(document: document))
                    } else {
                        delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(ref.documentID)) does not exist!")
                    }
                }
            }
        }
    }
    
    func delete(_ reference: AgoraDocumentReference, _ delegate: AgoraDocumentReferenceDelegate) {
        let className = reference.className
        let ref = Database.document(table: className, id: reference.id)
        ref.delete { error in
            if let error = error {
                delegate.onFailed(code: -1, msg: error.localizedDescription)
            } else {
                delegate.onSuccess()
            }
        }
    }
    
    func subscribe(_ reference: AgoraDocumentReference, _ delegate: SyncManagerEventDelegate) -> ISyncManagerLiveQuery {
        let listenerRegistration: ListenerRegistration
        let className = reference.className
        var query: Query = Database.db.collection(className)
        for eqs in reference.whereEQ {
            let key = eqs.key
            let value = eqs.value
            if (value is AgoraDocumentReference) {
                let ref: AgoraDocumentReference = value as! AgoraDocumentReference
                query = query.whereField(key, isEqualTo: Database.document(table: ref.className, id: ref.id))
            } else {
                query = query.whereField(key, isEqualTo: value)
            }
        }
        listenerRegistration = query.addSnapshotListener({ (querySnapshot: QuerySnapshot?, error: Error?) in
            if let error = error {
                Logger.log(message: "subscribe1 \(className) error:\(error)", level: .error)
                delegate.onError(code: -1, msg: error.localizedDescription)
            } else if let querySnapshot = querySnapshot {
                Logger.log(message: "liveQueryEvent \(className) event", level: .info)
                querySnapshot.documentChanges.forEach { change in
                    switch change.type {
                    case .added:
                        delegate.onCreated(object: AgoraObject(document: change.document))
                    case .modified:
                        delegate.onUpdated(object: AgoraObject(document: change.document))
                    case .removed:
                        delegate.onDeleted(objectId: change.document.documentID)
                    default: break
                    }
                }
            } else {
                Logger.log(message: "subscribe0 \(className) error: snapshot is nil", level: .error)
                delegate.onError(code: -1, msg: "unknown error")
            }
        })
        return AgoraLiveQuery(className: className, listenerRegistration: listenerRegistration)
    }
}
#endif
