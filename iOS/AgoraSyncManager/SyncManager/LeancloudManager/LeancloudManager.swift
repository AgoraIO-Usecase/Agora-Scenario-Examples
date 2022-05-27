//
//  LeancloudManager.swift
//  AgoraSyncManager
//
//  Created by zhaoyongqiang on 2022/4/27.
//

import UIKit
import LeanCloud


extension LeancloudManager {
    typealias Config = RtmSyncManager.Config
}

class LeancloudManager: NSObject {
    typealias DocumentName = String
    var defaultChannelName: String!
    var sceneName: String = ""
    
    /// 保存在collection的doc
    let queue = DispatchQueue(label: "leanCloudManager.queue")
    
    var onCreateBlocks = [String : OnSubscribeBlock]()
    var onUpdatedBlocks = [String : OnSubscribeBlock]()
    var onDeletedBlocks = [String : OnSubscribeBlock]()
    var subscribedSceneDoc = [String : LCObject]()
    
    /// init
    /// - Parameters:
    ///   - config: config
    init(config: Config,
         complete: SuccessBlockInt?) {
        super.init()
        self.defaultChannelName = config.channelName
        try? LCApplication.default.set(
            id: config.appId,
            key: config.appKey,
            serverURL: "https://agoraktv.xyz"
        )
//        askKit = AgoraSyncEngineKit(appId: config.appId)
//        askContext = askKit.createContext()
//        roomsCollection = askContext.createSlice(withName: defaultChannelName)?.createCollection(withName: roomListKey)
        Log.info(text: "defaultChannelName = \(config.channelName)", tag: "leanCloudSyncManager.init")
        Log.info(text: "init ok", tag: "AskSyncManager.init")
        complete?(0)
    }
}
