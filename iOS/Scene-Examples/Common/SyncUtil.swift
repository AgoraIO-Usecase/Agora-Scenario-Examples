//
//  SyncUtil.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/3.
//

import UIKit

class SyncUtil: NSObject {
    private static var manager: SyncManager?
    private override init() { }
    private static var subscribeCollectionDelegate: ISyncManagerLiveQuery?
    private static var sceneRefs: [SceneReference] = [SceneReference]()
    
    static func initSyncManager(sceneId: String) {
        let dict = [SYNC_MANAGER_PARAM_KEY_APPID: KeyCenter.AppId,
                       SYNC_MANAGER_PARAM_KEY_ID: sceneId]
        manager = SyncManager(dict: dict)
    }
    
    class func joinScene(id: String,
                         userId: String,
                         property: [String: Any]?,
                         delegate: IObjectDelegate? = nil) {
        guard let manager = manager else { return }
        let jsonString = JSONObject.toJsonString(dict: property) ?? ""
        let params = JSONObject.toDictionary(jsonStr: jsonString)
        let scene = Scene(id: id, userId: userId, property: params)
        let sceneRef = manager.joinScene(with: scene, delegate: delegate)
        guard !sceneRefs.contains(where: { $0.id == id }) else { return }
        sceneRefs.append(sceneRef)
    }
    
    class func fetchAll(delegate: IObjectListDelegate) {
        manager?.getScenes(delegate: delegate)
    }
    
    class func fetchCollection(id: String,
                               className: String,
                               delegate: IObjectListDelegate) {
        let sceneRef = sceneRefs.filter({ $0.id == id }).first
        sceneRef?.collection(className: className).get(delegate: delegate)
    }
    
    class func addCollection(id: String,
                             className: String,
                             params: [String: Any],
                             delegate: IObjectDelegate?) {
        let sceneRef = sceneRefs.filter({ $0.id == id }).first
        sceneRef?.collection(className: className)
            .add(data: params, delegate: delegate)
    }
    
    class func updateCollection(id: String,
                                className: String,
                                objectId: String,
                                params: [String: Any],
                                delegate: IObjectDelegate?) {
        let sceneRef = sceneRefs.filter({ $0.id == id }).first
        sceneRef?.collection(className: className).document(id: objectId).update(data: params, delegate: delegate)
    }
    
    class func subscribeCollection(id: String,
                                   className: String,
                                   documentId: String? = nil,
                                   delegate: ISyncManagerEventDelegate) {
        let sceneRef = sceneRefs.filter({ $0.id == id }).first
        if documentId == nil {
            subscribeCollectionDelegate = sceneRef?.collection(className: className)
                .document()
                .subscribe(delegate: delegate)
        } else {
            subscribeCollectionDelegate = sceneRef?.collection(className: className)
                .document(id: documentId ?? "")
                .subscribe(delegate: delegate)
        }
    }
    
    class func unsubscribe(id: String, className: String) {
        let sceneRef = sceneRefs.filter({ $0.id == id }).first
        sceneRef?.collection(className: className).document().unsubscribe()
    }
    
    class func leaveScene(id: String) {
        guard let index = sceneRefs.firstIndex(where: { $0.id == id }) else { return }
        sceneRefs.remove(at: index)
    }
    
    class func deleteDocument(id: String,
                                className: String,
                                objectId: String?,
                                delegate: IDocumentReferenceDelegate?) {
        guard let sceneRef = sceneRefs.filter({ $0.id == id }).first else { return }
        /// 删除单条数据
        sceneRef.collection(className: className).document(id: objectId ?? "").delete(delegate: delegate)
    }
    
    class func deleteCollection(id: String,
                                className: String,
                                delegate: IDocumentReferenceDelegate?) {
        guard let sceneRef = sceneRefs.filter({ $0.id == id }).first else { return }
        /// 删除数据
        sceneRef.collection(className: className).delete(delegate: delegate)
    }
}