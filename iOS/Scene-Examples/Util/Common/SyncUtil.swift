//
//  SyncUtil.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/3.
//

import UIKit
//import AgoraSyncManager

class SyncUtil: NSObject {
    private static var manager: AgoraSyncManager?
    private override init() { }
    private static var sceneRefs: [String: SceneReference] = [String: SceneReference]()
    
    static func initSyncManager(sceneId: String) {
        let config = AgoraSyncManager.RtmConfig(appId: KeyCenter.AppId,
                                                channelName: sceneId)
        manager = AgoraSyncManager(config: config, complete: { code in
            if code == 0 {
                print("SyncManager init success")
            } else {
                print("SyncManager init error")
            }
        })
//        let config = AgoraSyncManager.LeancloudConfig(appId: KeyCenter.LeanCloudAppId,
//                                                appKey: KeyCenter.LeanCloudAppKey,
//                                                channelName: sceneId)
//        manager = AgoraSyncManager(leancloudConfig: config, complete: { code in
//            if code == 0 {
//                print("SyncManager init success")
//            } else {
//                print("SyncManager init error")
//            }
//        })
    }
    
    class func joinScene(id: String,
                         userId: String,
                         property: [String: Any]?,
                         success: SuccessBlockObj? = nil,
                         fail: FailBlock? = nil) {
        guard let manager = manager else { return }
        let jsonString = JSONObject.toJsonString(dict: property) ?? ""
        let params = JSONObject.toDictionary(jsonStr: jsonString)
        let scene = Scene(id: id, userId: userId, property: params)
        manager.createScene(scene: scene, success: {
            manager.joinScene(sceneId: id) { sceneRef in
                sceneRefs[id] = sceneRef
                let attr = Attribute(key: id, value: jsonString)
                success?(attr)
            } fail: { error in
                fail?(error)
            }
        }) { error in
            fail?(error)
        }
    }
    
    class func scene(id: String) -> SceneReference? {
        sceneRefs[id]
    }
    
    class func fetchAll(success: SuccessBlock? = nil, fail: FailBlock? = nil) {
        manager?.getScenes(success: success, fail: fail)
    }
    
    class func leaveScene(id: String) {
        sceneRefs.removeValue(forKey: id)
    }
}
