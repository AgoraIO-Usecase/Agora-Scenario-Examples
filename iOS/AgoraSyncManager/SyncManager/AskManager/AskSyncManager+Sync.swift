//
//  AskManager+Internal.swift
//  AgoraSyncManager
//
//  Created by ZYP on 2022/2/7.
//

import Foundation
import AgoraSyncKit

extension AskSyncManager {
    func createSceneSync(scene: Scene,
                         success: SuccessBlockVoid?,
                         fail: FailBlock?) {
        ///  add room
        let ret = addScene(id: scene.id, data: scene.toJson())
        switch ret {
        case .success(_):
            Log.info(text: "add scenne success: \(scene.toJson())", tag: "AskSyncManager.createSceneSync")
            Log.info(text: "createScene ok", tag: "AskSyncManager.createSceneSync")
            DispatchQueue.main.async {
                success?()
            }
            break
        case .failure(let error):
            Log.errorText(text: error.description, tag: "AskSyncManager.createSceneSync.addScene")
            DispatchQueue.main.async {
                fail?(error)
            }
            break
        }
    }
    
    func joinSceneSync(sceneId: String,
                       manager: AgoraSyncManager,
                       success: SuccessBlockObjSceneRef?,
                       fail: FailBlock?) {
        guard let roomDocument = roomsCollection.createDocument(withName: sceneId) else {
            Log.errorText(text: "createDocument fail", tag: "AskSyncManager.joinSceneSync")
            return
        }
        
        let sceneRef = SceneReference(manager: manager,
                                      document: roomDocument,
                                      id: sceneId)
        self.sceneName = sceneId
        Log.info(text: "joinScene ok", tag: "AskSyncManager.joinSceneSync")
        DispatchQueue.main.async {
            success?(sceneRef)
        }
    }
    
    func getScenesSync(success: SuccessBlock?, fail: FailBlock?) {
        let field = "scene"
        roomsCollection.getRemote { errorCode, snapshots in
            if errorCode == .codeNoError {
                guard let list = snapshots else { /** can not get list **/
                    fatalError("snapshots must not nil")
                }
                
                let jsonStrings = list.compactMap({ $0.data() }).map({ $0.getJsonString(field: field) }).compactMap({ $0 })
                guard !jsonStrings.isEmpty else { /** list is empty **/
                    Log.info(text: "getScenes success, jsonStrings is empty", tag: "AskSyncManager.getScenesSync")
                    DispatchQueue.main.async {
                        success?([])
                    }
                    return
                }
                
                /// decode scene
                let decoder = JSONDecoder()
                var results = [Attribute]()
                
                for str in jsonStrings {
                    if let data = str.data(using: .utf8) {
                        do {
                            let sceneObj = try decoder.decode(Scene.self, from: data)
                            let result = Attribute(key: sceneObj.id, value: str)
                            results.append(result)
                        } catch let error {
                            Log.errorText(text: "decode error \(error.localizedDescription)", tag: "AskManager.getScenesSync")
                        }
                    }
                }
                
                Log.info(text: "getScenes success", tag: "AskSyncManager.getScenesSync")
                DispatchQueue.main.async {
                    success?(results)
                }
                return
            }
            else {
                let e = SyncError.ask(message: "getScenesSync fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.getScenesSync")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
    }
    
    func getSync(documentRef: DocumentReference,
                 key: String,
                 success: SuccessBlockObjOptional?,
                 fail: FailBlock?) {
        let field = key
        if field == "" { fatalError("key must not empty") }
        documentRef.internalDocument.getRemote(field) { errorCode, json in
            if errorCode == .codeNoError {
                guard let result = json else {
                    Log.info(text: "json is nil", tag: "AskManager.get(documentRef)")
                    
                    DispatchQueue.main.async {
                        success?(nil)
                    }
                    return
                }
                
                guard let string = result.getStringValue() else {
                    Log.info(text: "can not get json value", tag: "AskManager.get(documentRef)")
                    DispatchQueue.main.async {
                        success?(nil)
                    }
                    return
                }
                
                let attr = Attribute(key: field, value: string)
                Log.info(text: "get ok", tag: "AskSyncManager.getSync(document)")
                DispatchQueue.main.async {
                    success?(attr)
                }
            }
            else {
                let e = SyncError.ask(message: "get(documentRef) fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.getSync(document)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
    }
    
    func getSync(collectionRef: CollectionReference,
                 success: SuccessBlock?,
                 fail: FailBlock?) {
        collectionRef.internalCollection.getRemote { errorCode, snapshots in
            if errorCode == .codeNoError {
                guard let list = snapshots else {
                    fatalError("snapshots should not nil when errorCode equal 0")
                }
                let jsonDecoder = JSONDecoder()
                let strings = list.compactMap({ $0.data() }).map({ $0.getJsonString(field: "") }).compactMap({ $0 })
                let attrs = strings.map({ str -> Attribute? in
                    guard let id = CollectionItem.getObjId(jsonString:str, decoder:jsonDecoder) else {
                        return nil
                    }
                    return Attribute(key: id, value: str)
                }).compactMap({ $0 })
                Log.info(text: "get ok", tag: "AskSyncManager.getSync(collection)")
                DispatchQueue.main.async {
                    success?(attrs)
                }
            }
            else {
                let e = SyncError.ask(message: "get(collectionRef) fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.getSync(collection)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
    }
    
    func addSync(reference: CollectionReference,
                 data: [String : Any?],
                 success: SuccessBlockObj?,
                 fail: FailBlock?) {
        let uid = UUID().uuid16string()
        var temp = data
        temp["objectId"] = uid
        let value = Utils.getJson(dict: temp as NSDictionary)
        let json = AgoraJson()
        json.setString(value)
        Log.info(text: "will add \(value)", tag: "AskSyncManager.addSync(collection)")
        reference.internalCollection.add(json) { [weak self](errorCode, document) in
            if errorCode == .codeNoError {
                guard let doc = document else {
                    assertionFailure("document should not nil when errorCode equal 0")
                    return
                }
                
                let attr = Attribute(key: uid, value: value)
                self?.documentDict[uid] = doc
                Log.info(text: "add ok \(value)", tag: "AskSyncManager.addSync(collection)")
                DispatchQueue.main.async {
                    success?(attr)
                }
            }
            else {
                let e = SyncError.ask(message: "add fail \(value)", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.addSync(collection)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
    }
    
    func updateSync(reference: CollectionReference,
                    id: String,
                    data: [String : Any?],
                    success: SuccessBlockVoid?,
                    fail: FailBlock?) {
        guard let document = documentDict[id] else {
            let e = SyncError.ask(message: "id not found in local, it is add by remote?", code: -1)
            Log.errorText(text: e.description, tag: "AskSyncManager.updateSync")
            fail?(e)
            return
        }
        var temp = data
        temp["objectId"] = id
        let value = Utils.getJson(dict: temp as NSDictionary)
        let json = AgoraJson()
        json.setString(value)
        Log.info(text: "will update collection \(value)", tag: "AskSyncManager.updateSync(collection)")
        document.set("", json: json) { errorCode in
            if errorCode == .codeNoError {
                Log.info(text: "update success \(value)", tag: "AskSyncManager.updateSync(collection)")
                success?()
            }
            else {
                let e = SyncError.ask(message: "document.set fail: \(value)", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.updateSync(collection)")
                fail?(e)
            }
        }
    }
    
    func deleteSync(reference: CollectionReference,
                    id: String,
                    success: SuccessBlockVoid?,
                    fail: FailBlock?) {
        guard let document = documentDict[id] else {
            let e = SyncError.ask(message: "id not found in local, it is add by remote?", code: -1)
            fail?(e)
            return
        }
        document.set("",
                     json: AgoraJson(),
                     completion: { errorCode in
            if errorCode == .codeNoError {
                DispatchQueue.main.async {
                    Log.info(text: "delete success \(id)", tag: "AskSyncManager.deleteSync(collection)")
                    success?()
                }
            }
            else {
                let e = SyncError.ask(message: "delete(collectionRef) fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.deleteSync(CollectionReference)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        })
    }
    
    func updateSync(reference: DocumentReference,
                    key: String,
                    data: [String : Any?],
                    success: SuccessBlock?,
                    fail: FailBlock?) {
        let field = key
        if field == "" { fatalError("key must not empty") }
        let json = AgoraJson()
        let value = Utils.getJson(dict: data as NSDictionary)
        json.setString(value)
        let id = reference.id
        reference.internalDocument.set(field, json: json) { errorCode in
            if errorCode == .codeNoError {
                let attr = Attribute(key: "", value: value)
                DispatchQueue.main.async {
                    Log.info(text: "updateSync success in scene:\(id) key:(\(field)) \(value)", tag: "AskSyncManager.updateSync(document)")
                    success?([attr])
                }
            }
            else {
                let e = SyncError.ask(message: "updateSync(reference) fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.updateSync(document)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
    }
    
    func deleteSync(documentRef: DocumentReference,
                    success: SuccessBlock?,
                    fail: FailBlock?) {
        documentRef.internalDocument.set("", json: AgoraJson()) { errorCode in
            if errorCode == .codeNoError {
                DispatchQueue.main.async {
                    Log.info(text: "deleteSync success", tag: "AskSyncManager.deleteSync(document)")
                    success?([])
                }
            }
            else {
                let e = SyncError.ask(message: "delete(documentRef) fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.deleteSync(document)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
        
        /// delete collection
        for collection in collections.values {
            collection.remove { errorCode in
                Log.info(text: "delete collection ret \(errorCode)", tag: "AskSyncManager.deleteSync(document)")
            }
        }
        collections.removeAll()
    }
    
    func deleteSync(collectionRef: CollectionReference,
                    success: SuccessBlock?,
                    fail: FailBlock?) {
        collectionRef.internalCollection.remove { errorCode in
            if errorCode == .codeNoError {
                DispatchQueue.main.async {
                    Log.info(text: "deleteSync success", tag: "AskSyncManager.deleteSync(collection)")
                    success?([])
                }
            }
            else {
                let e = SyncError.ask(message: "delete(collectionRef) fail", code: errorCode.rawValue)
                Log.errorText(text: e.description, tag: "AskSyncManager.deleteSync(collection)")
                DispatchQueue.main.async {
                    fail?(e)
                }
            }
        }
    }
    
    func subscribeSync(reference: DocumentReference,
                       key: String,
                       onCreated: OnSubscribeBlock?,
                       onUpdated: OnSubscribeBlock?,
                       onDeleted: OnSubscribeBlock?,
                       onSubscribed: OnSubscribeBlockVoid?,
                       fail: FailBlock?) {
        guard let sceneId = sceneName  else { fatalError("never call this") }
        if reference.className == sceneName, let sceneDocument = roomsCollection.createDocument(withName: sceneId) { /** 监听sceneRef 事件 **/
            if key == "" { fatalError("key must not empty") }
            Log.info(text: "subscribe key: \(key)", tag: "AskSyncManager.subscribe")
            subscribedSceneDoc[key] = sceneDocument
            sceneDocument.subscribe({ errorCode in
                if errorCode != .codeNoError {
                    let e = SyncError.ask(message: "subscribe scene \(sceneId) fail", code: errorCode.rawValue)
                    fail?(e)
                    return
                }
                if Thread.isMainThread {
                    onSubscribed?()
                }
                else {
                    DispatchQueue.main.async {
                        onSubscribed?()
                    }
                }
                Log.info(text: "subscribe scene success key: \(key) for scene:\(sceneId)", tag: "AskSyncManager.subscribe")
            }, eventCompletion: { (eventType, snapshot, details) in
                Log.info(text: " scene eventCompletion", tag: "AskSyncManager.subscribe")
                
                guard let tempDetail = details else {
                    Log.errorText(text: "not detail", tag: "AskSyncManager.subscribe")
                    return
                }
                
                guard tempDetail.fieldsModified?.contains(key) ?? false ||
                        tempDetail.fieldsAdded?.contains(key) ?? false ||
                        tempDetail.fieldsRemoved?.contains(key) ?? false else {
                            let string = "can not mach key:\(key) fieldsModified:\(tempDetail.fieldsModified ?? []) fieldsAdded:\(tempDetail.fieldsAdded ?? []) fieldsRemoved:\(tempDetail.fieldsRemoved ?? [])"
                            Log.errorText(text: string, tag: "AskSyncManager.subscribe")
                            return
                        }
                
                guard let value = snapshot?.data()?.getJsonString(field: key) else {
                    Log.errorText(text: "get snapshot data fail", tag: "AskSyncManager.subscribe")
                    return
                }
                let attr = Attribute(key: sceneId, value: value)
                switch eventType {
                case .addDocumentEvent:
                    if Thread.isMainThread {
                        onCreated?(attr)
                    }
                    else {
                        DispatchQueue.main.async {
                            onCreated?(attr)
                        }
                    }
                    break
                case .modifyDocumentEvent:
                    if Thread.isMainThread {
                        onUpdated?(attr)
                    }
                    else {
                        DispatchQueue.main.async {
                            onUpdated?(attr)
                        }
                    }
                    break
                case .removeDocumentEvent:
                    if Thread.isMainThread {
                        onDeleted?(attr)
                    }
                    else {
                        DispatchQueue.main.async {
                            onDeleted?(attr)
                        }
                    }
                    break
                @unknown default:
                    fatalError("never call this")
                }
            })
            return
        }
        
        let name = sceneName + reference.className + key
        
        /** 监听 collection 事件 **/
        guard let collection = collections[name] else {
            let e = SyncError.ask(message: "subscribe collection \(name) not find", code: -1)
            fail?(e)
            return
        }
        
        collection.subscribe { errorCode, _ in
            if errorCode != .codeNoError {
                let e = SyncError.ask(message: "subscribe collection \(name) fail", code: errorCode.rawValue)
                fail?(e)
                return
            }
            Log.info(text: "subscribe collection success \(name)", tag: "AskSyncManager.subscribe")
            onSubscribed?()
        } eventCompletion: { (eventType, snapshot, details) in
            Log.info(text: "collection eventCompletion", tag: "AskSyncManager.subscribe")
            guard let value = snapshot?.data()?.getStringValue() else {
                Log.errorText(text: "get snapshot data fail",
                              tag: "AskSyncManager.subscribe")
                return
            }
            guard let item = CollectionItem.decodeWithString(jsonString: value,
                                                             decoder: .init()) else {
                Log.errorText(text: "can not decode CollectionItem",
                              tag: "AskSyncManager.subscribe")
                return
            }
            let objId = item.objectId
            let attr = Attribute(key: objId, value: value)
            switch eventType {
            case .addDocumentEvent:
                if Thread.isMainThread {
                    onCreated?(attr)
                }
                else {
                    DispatchQueue.main.async {
                        onCreated?(attr)
                    }
                }
                break
            case .modifyDocumentEvent:
                if Thread.isMainThread {
                    onUpdated?(attr)
                }
                else {
                    DispatchQueue.main.async {
                        onUpdated?(attr)
                    }
                }
                break
            case .removeDocumentEvent:
                if Thread.isMainThread {
                    onDeleted?(attr)
                }
                else {
                    DispatchQueue.main.async {
                        onDeleted?(attr)
                    }
                }
                break
            @unknown default:
                fatalError("never call this")
            }
        }
    }
    
    func unsubscribeSync(reference: DocumentReference, key: String) {
        guard let sceneId = sceneName else {
            fatalError("never call this")
        }
        
        if reference.className == sceneId, let sceneDocument = subscribedSceneDoc[key] { /** 取消监听sceneRef 事件 **/
            sceneDocument.unsubscribe { errorCode in
                if errorCode == .codeNoError { return }
                Log.errorText(text: "unsubscribeSync scene \(sceneId) errorCode: \(errorCode.rawValue)", tag: "AskSyncManager.unsubscribeSync")
            }
            Log.info(text: "unsubscribe key: \(key)", tag: "AskSyncManager.unsubscribe")
            return
        }
        
        /** 取消监听 collection 事件 **/
        let name = sceneId + reference.className + key
        guard let collection = collections[name] else {
            Log.errorText(text: "subscribe collection \(name) not find", tag: "AskSyncManager.unsubscribeSync")
            return
        }
        
        collection.unsubscribe { errorCode in
            if errorCode == .codeNoError { return }
            Log.errorText(text: "unsubscribeSync collection \(name) errorCode: \(errorCode.rawValue)", tag: "AskSyncManager.unsubscribeSync")
        }
    }
    
    func subscribeSceneSync(reference: SceneReference,
                            onDeleted: OnSubscribeBlockVoid? = nil,
                            fail: FailBlock? = nil) {
        let sceneDocument = reference.internalDocument
        guard let sceneId = sceneName else {
            return
        }
        Log.info(text: "subscribeScene delete event", tag: "AskSyncManager.subscribeSceneSync")
        sceneDocument.subscribe({ errorCode in
            if errorCode != .codeNoError {
                let e = SyncError.ask(message: "subscribe scene \(sceneId) fail", code: errorCode.rawValue)
                fail?(e)
                return
            }
            Log.info(text: "subscribe scene success", tag: "AskSyncManager.subscribeSceneSync")
        }, eventCompletion: { (eventType, snapshot, details) in
            Log.info(text: " scene eventCompletion", tag: "AskSyncManager.subscribeSceneSync")
            switch eventType {
            case .addDocumentEvent:
                break
            case .modifyDocumentEvent:
                break
            case .removeDocumentEvent:
                if Thread.isMainThread {
                    onDeleted?()
                }
                else {
                    DispatchQueue.main.async {
                        onDeleted?()
                    }
                }
                break
            @unknown default:
                fatalError("never call this")
            }
        })
    }
    
    func unsubscribeSceneSync(reference: SceneReference,
                              fail: FailBlock? = nil) {
        let sceneDocument = reference.internalDocument
        guard let sceneId = sceneName else {
            return
        }
        sceneDocument.unsubscribe { errorCode in
            if errorCode != .codeNoError {
                let e = SyncError.ask(message: "unsubscribe scene \(sceneId) fail", code: errorCode.rawValue)
                fail?(e)
                return
            }
            Log.info(text: "unsubscribe scene success", tag: "AskSyncManager.unsubscribeSceneSync")
        }
    }
}


