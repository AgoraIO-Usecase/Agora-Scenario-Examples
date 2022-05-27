//
//  AskManager+ISyncManager.swift
//  AgoraSyncManager
//
//  Created by ZYP on 2022/2/7.
//

import Foundation
import AgoraSyncKit

extension AskSyncManager: ISyncManager {
    func createScene(scene: Scene,
                     success: SuccessBlockVoid?,
                     fail: FailBlock?) {
        Log.info(text: "scene.id = \(scene.id)",
                 tag: "AskSyncManager.createScene")
        queue.async { [weak self] in
            self?.createSceneSync(scene: scene,
                                  success: success,
                                  fail: fail)
        }
    }
    
    func joinScene(sceneId: String,
                   manager: AgoraSyncManager,
                   success: SuccessBlockObjSceneRef?,
                   fail: FailBlock?) {
        Log.info(text: "scene.id = \(sceneId)",
                 tag: "AskSyncManager.joinScene")
        queue.async { [weak self] in
            self?.joinSceneSync(sceneId: sceneId,
                                manager: manager,
                                success: success,
                                fail: fail)
        }
    }
    
    func getScenes(success: SuccessBlock?, fail: FailBlock?) {
        queue.async { [weak self] in
            self?.getScenesSync(success: success, fail: fail)
        }
    }
    
    func deleteScenes(sceneIds: [String],
                      success: SuccessBlockVoid?,
                      fail: FailBlock?) {}
    
    func get(documentRef: DocumentReference,
             key: String,
             success: SuccessBlockObjOptional?,
             fail: FailBlock?) {
        queue.async { [weak self] in
            self?.getSync(documentRef: documentRef,
                          key: key,
                          success: success,
                          fail: fail)
        }
    }
    
    func get(collectionRef: CollectionReference,
             success: SuccessBlock?,
             fail: FailBlock?) {
        queue.async { [weak self] in
            self?.getSync(collectionRef: collectionRef,
                          success: success,
                          fail: fail)
        }
    }
    
    func add(reference: CollectionReference,
             data: [String : Any?],
             success: SuccessBlockObj?,
             fail: FailBlock?) {
        queue.async { [weak self] in
            self?.addSync(reference: reference,
                          data: data,
                          success: success,
                          fail: fail)
        }
    }
    
    func update(reference: CollectionReference,
                id: String,
                data: [String : Any?],
                success: SuccessBlockVoid?,
                fail: FailBlock?) {
        queue.async { [weak self] in
            self?.updateSync(reference: reference,
                             id: id,
                             data: data,
                             success: success,
                             fail: fail)
        }
    }
    
    func delete(reference: CollectionReference,
                id: String,
                success: SuccessBlockVoid?,
                fail: FailBlock?) {
        queue.async { [weak self] in
            self?.deleteSync(reference: reference,
                             id: id,
                             success: success,
                             fail: fail)
        }
    }
    
    func update(reference: DocumentReference,
                key: String,
                data: [String : Any?],
                success: SuccessBlock?,
                fail: FailBlock?) {
        queue.async { [weak self] in
            self?.updateSync(reference: reference,
                             key: key,
                             data: data,
                             success: success,
                             fail: fail)
        }
    }
    
    func delete(documentRef: DocumentReference,
                success: SuccessBlock?,
                fail: FailBlock?) {
        queue.async { [weak self] in
            self?.deleteSync(documentRef: documentRef,
                             success: success,
                             fail: fail)
        }
    }
    
    func delete(collectionRef: CollectionReference,
                success: SuccessBlock?,
                fail: FailBlock?) {
        queue.async { [weak self] in
            self?.deleteSync(collectionRef: collectionRef,
                             success: success,
                             fail: fail)
        }
    }
    
    func subscribe(reference: DocumentReference,
                   key: String,
                   onCreated: OnSubscribeBlock?,
                   onUpdated: OnSubscribeBlock?,
                   onDeleted: OnSubscribeBlock?,
                   onSubscribed: OnSubscribeBlockVoid?,
                   fail: FailBlock?) {
        queue.async { [weak self] in
            self?.subscribeSync(reference: reference,
                                key: key,
                                onCreated: onCreated,
                                onUpdated: onUpdated,
                                onDeleted: onDeleted,
                                onSubscribed: onSubscribed,
                                fail: fail)
        }
    }
    
    func unsubscribe(reference: DocumentReference, key: String) {
        queue.async { [weak self] in
            self?.unsubscribeSync(reference: reference, key: key)
        }
    }
    
    func createCollection(reference: SceneReference,
                          internalClassName: String) -> AgoraSyncCollection? {
        if let collection = collections[internalClassName] {
            return collection
        }
        let sceneDocument = reference.internalDocument
        let collection = sceneDocument.createCollection(with: askContext, documentName: internalClassName)
        collections[internalClassName] = collection
        return collection
    }
    
    func subscribeScene(reference: SceneReference,
                        onDeleted: OnSubscribeBlockVoid?,
                        fail: FailBlock?) {
        queue.async { [weak self] in
            self?.subscribeSceneSync(reference: reference, onDeleted: onDeleted, fail: fail)
        }
    }
    
    func unsubscribeScene(reference: SceneReference, fail: FailBlock?) {
        queue.async { [weak self] in
            self?.unsubscribeSceneSync(reference: reference, fail: fail)
        }
    }
}
