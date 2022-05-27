//
//  LeancloudManager+Sync.swift
//  AgoraSyncManager
//
//  Created by zhaoyongqiang on 2022/4/28.
//

import UIKit
import LeanCloud

extension LCObject {
    func toAttribute() -> IObject {
        let data = value(forKey: "data") as? LCString
        let attr = Attribute(key: objectId?.value ?? "", value: data?.value ?? "")
        return attr
    }
}

extension LeancloudManager {
    func createSceneSync(scene: Scene,
                         success: SuccessBlockVoid?,
                         fail: FailBlock?) {
        ///  add room
        let _ = addScene(id: scene.id, data: scene.toJson())
       success?()
    }
    
    func joinSceneSync(sceneId: String,
                       manager: AgoraSyncManager,
                       success: SuccessBlockObjSceneRef?,
                       fail: FailBlock?) {
        let sceneRef = SceneReference(manager: manager,
                                      id: sceneId,
                                      type: .leancloud)
        self.sceneName = sceneId
        Log.info(text: "joinScene ok", tag: "AskSyncManager.joinSceneSync")
        DispatchQueue.main.async {
            success?(sceneRef)
        }
    }
    
    func getScenesSync(success: SuccessBlock?, fail: FailBlock?) {
        let query = LCQuery(className: defaultChannelName)
        query.find { [weak self] result in
            self?.queryDatasHandler(result: result, success: success, fail: fail)
        }
    }
    private func queryDatasHandler(result: LCQueryResult<LCObject>,
                                   success: SuccessBlock?,
                                   fail: FailBlock?) {
        switch result {
        case .success(let objects):
            var res = [Attribute]()
            for item in objects {
                let value = item.value(forKey: "data") as? LCString
                let attr = Attribute(key: item.objectId?.value ?? "",
                                     value: value?.value ?? "")
                res.append(attr)
            }
            success?(res)
            
        case .failure(let error):
            let error = SyncError(message: error.localizedDescription,
                                  code: error.errorCode)
            fail?(error)
        }
    }
    
    private func addOrUpdateDataHandler(result: LCBooleanResult,
                                        objectId: String?,
                                        success: SuccessBlockObj?,
                                        fail: FailBlock?) {
            switch result {
            case .success:
                let object = LCObject(objectId: objectId ?? "")
                let value = object.value(forKey: "data") as? LCString
                let attr = Attribute(key: objectId ?? "",
                                     value: value?.value ?? "")
                success?(attr)
                
            case .failure(let error):
                let error = SyncError(message: error.localizedDescription,
                                      code: error.errorCode)
                fail?(error)
            }
        }
    
    func getSync(documentRef: DocumentReference,
                 key: String,
                 success: SuccessBlockObjOptional?,
                 fail: FailBlock?) {
        let query = LCQuery(className: documentRef.className + key)
        query.find { [weak self] response in
            self?.queryDatasHandler(result: response, success: { list in
                success?(list.first)
            }, fail: fail)
        }
    }
    
    func getSync(collectionRef: CollectionReference,
                 success: SuccessBlock?,
                 fail: FailBlock?) {
        let query = LCQuery(className: collectionRef.className)
        query.find { [weak self] response in
            self?.queryDatasHandler(result: response, success: success, fail: fail)
        }
    }
    
    func addSync(reference: CollectionReference,
                 data: [String : Any?],
                 success: SuccessBlockObj?,
                 fail: FailBlock?) {

        let todo = LCObject(className: reference.className)
        let value = Utils.getJson(dict: data as NSDictionary)
        try? todo.set("data", value: value)
        todo.save { [weak self] result in
            self?.addOrUpdateDataHandler(result: result,
                                        objectId: todo.objectId?.value,
                                        success: success,
                                        fail: fail)
        }
    }
    
    func updateSync(reference: CollectionReference,
                    id: String,
                    data: [String : Any?],
                    success: SuccessBlockVoid?,
                    fail: FailBlock?) {
        
        let item = Utils.getJson(dict: data as NSDictionary)
        let todo = LCObject(className: reference.className, objectId: id)
        try? todo.set("data", value: item)
        todo.save { [weak self] result in
            self?.addOrUpdateDataHandler(result: result,
                                        objectId: id,
                                        success: { object in
                if let onUpdateBlock = self?.onUpdatedBlocks[reference.className] {
                    onUpdateBlock(object)
                }
                success?()
            },
                                        fail: fail)
        }
    }
    
    func deleteSync(reference: CollectionReference,
                    id: String,
                    success: SuccessBlockVoid?,
                    fail: FailBlock?) {
        
        let todo = LCObject(className: reference.className, objectId: id)
        todo.delete(completionQueue: .main) { [weak self] response in
            self?.addOrUpdateDataHandler(result: response,
                                        objectId: id,
                                        success: { object in
                if let onDeleteBlock = self?.onDeletedBlocks[reference.className] {
                    onDeleteBlock(object)
                }
                success?()
            },
                                        fail: fail)
        }
    }
    
    func updateSync(reference: DocumentReference,
                    key: String,
                    data: [String : Any?],
                    success: SuccessBlock?,
                    fail: FailBlock?) {
    
        let item = Utils.getJson(dict: data as NSDictionary)
        let todo = LCObject(className: reference.className + key)
        try? todo.set("data", value: item)
        todo.save { [weak self] result in
            self?.addOrUpdateDataHandler(result: result,
                                        objectId: todo.objectId?.value,
                                        success: { object in
                if let onUpdateBlock = self?.onUpdatedBlocks[reference.className + key] {
                    onUpdateBlock(object)
                }
                success?([object])
            },
                                        fail: fail)
        }
    }
    
    func deleteSync(documentRef: DocumentReference,
                    success: SuccessBlock?,
                    fail: FailBlock?) {
        let todo = LCObject(className: documentRef.className)
        todo.delete(completionQueue: .main) { [weak self] result in
            self?.addOrUpdateDataHandler(result: result,
                                         objectId: todo.objectId?.value ?? "",
                                         success: { object in
                if let onDeleteBlock = self?.onDeletedBlocks[documentRef.className] {
                    onDeleteBlock(object)
                }
                success?([object])
            },
                                        fail: fail)
        }
    }
    
    func deleteSync(collectionRef: CollectionReference,
                    success: SuccessBlock?,
                    fail: FailBlock?) {
        let todo = LCObject(className: collectionRef.className)
        todo.delete(completionQueue: .main) { [weak self] result in
            self?.addOrUpdateDataHandler(result: result,
                                         objectId: todo.objectId?.value ?? "",
                                         success: { object in
                if let onDeleteBlock = self?.onDeletedBlocks[collectionRef.className] {
                    onDeleteBlock(object)
                }
                success?([object])
            },
                                        fail: fail)
        }
    }
    
    func subscribeSync(reference: DocumentReference,
                       key: String,
                       onCreated: OnSubscribeBlock?,
                       onUpdated: OnSubscribeBlock?,
                       onDeleted: OnSubscribeBlock?,
                       onSubscribed: OnSubscribeBlockVoid?,
                       fail: FailBlock?) {
        onDeletedBlocks[reference.className + key] = onDeleted
        onUpdatedBlocks[reference.className + key] = onDeleted
        onCreateBlocks[reference.className + key] = onDeleted
        let query = LCQuery(className: reference.className + key)
        var liveQuery: LiveQuery?
        
        liveQuery = try? LiveQuery(query: query, eventHandler: { _, event in
            switch event {
            case .create(let object):
                onCreated?(object.toAttribute())
                
            case .enter(let object, let updatedKeys):
                print("enter === \(object)")
                break
                
            case .delete(let object):
                onDeleted?(object.toAttribute())
                
            case .leave(let object, let updatedKeys):
                print("leave === \(object)")
                break
                
            case .update(let object, let updatedKeys):
                onUpdated?(object.toAttribute())
                
            case .login(let user):
                print("login === \(user)")
                break
            case .state(let state):
                print("state == \(state)")
            }
        })
        liveQuery?.subscribe { result in
            switch result {
            case .success:
                onSubscribed?()
                
            case let .failure(error: error):
                let error = SyncError(message: error.localizedDescription,
                                      code: error.errorCode)
                fail?(error)
            }
        }
    }
    
    func unsubscribeSync(reference: DocumentReference, key: String) {
        onCreateBlocks.removeValue(forKey: reference.className + key)
        onUpdatedBlocks.removeValue(forKey: reference.className + key)
        onDeletedBlocks.removeValue(forKey: reference.className + key)
    }
    
    func subscribeSceneSync(reference: SceneReference,
                            onDeleted: OnSubscribeBlockVoid? = nil,
                            fail: FailBlock? = nil) {
        
        let query = LCQuery(className: reference.className)
        var liveQuery: LiveQuery?
        
        liveQuery = try? LiveQuery(query: query, eventHandler: { _, event in
            switch event {
            case .create(let object):
                break
                
            case .enter(let object, let updatedKeys):
                print("enter === \(object)")
                break
                
            case .delete(let object):
                break
                
            case .leave(let object, let updatedKeys):
                print("leave === \(object)")
                break
                
            case .update(let object, let updatedKeys):
                break
                
            case .login(let user):
                print("login === \(user)")
                break
            case .state(let state):
                print("state == \(state)")
            }
        })
        liveQuery?.subscribe { result in
            switch result {
            case .success: break
                
            case let .failure(error: error):
                let error = SyncError(message: error.localizedDescription,
                                      code: error.errorCode)
                fail?(error)
            }
        }
    }
    
    func unsubscribeSceneSync(reference: SceneReference,
                              fail: FailBlock? = nil) {
       
    }
}



