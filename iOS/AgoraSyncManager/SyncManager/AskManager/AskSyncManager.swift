//
//  AskManager.swift
//  AgoraSyncManager
//
//  Created by ZYP on 2022/2/7.
//

import Foundation
import AgoraSyncKit

class AskSyncManager: NSObject {
    typealias DocumentName = String
    var defaultChannelName: String!
    var sceneName: String!
    var askKit: AgoraSyncEngineKit!
    var askContext: AgoraSyncContext!
    var roomsCollection: AgoraSyncCollection!
    var collections = [DocumentName : AgoraSyncCollection]()
    let roomListKey = "rooms"
    
    /// 保存在collection的doc
    var documentDict = [String : AgoraSyncDocument]()
    let queue = DispatchQueue(label: "AskManager.queue")
    
    var onCreateBlocks = [AgoraSyncDocument : OnSubscribeBlock]()
    var onUpdatedBlocks = [AgoraSyncDocument : OnSubscribeBlock]()
    var onDeletedBlocks = [AgoraSyncDocument : OnSubscribeBlock]()
    var subscribedSceneDoc = [String : AgoraSyncDocument]()
    
    /// init
    /// - Parameters:
    ///   - config: config
    init(config: Config,
         complete: SuccessBlockInt?) {
        super.init()
        self.defaultChannelName = config.channelName
        askKit = AgoraSyncEngineKit(appId: config.appId)
        askContext = askKit.createContext()
        roomsCollection = askContext.createSlice(withName: defaultChannelName)?.createCollection(withName: roomListKey)
        Log.info(text: "defaultChannelName = \(config.channelName)", tag: "AskSyncManager.init")
        Log.info(text: "init ok", tag: "AskSyncManager.init")
        complete?(0)
    }
}
