//
//  MainVM.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import Foundation
import AgoraRtcKit
import AgoraMediaPlayer

protocol MainVMDelegate: NSObjectProtocol {
    func mainVMDidUpdateRenderInfos(renders: [RenderInfo])
    func mainVMShouldShowTips(tips: String)
}

class MainVM: NSObject {
    var loginInfo: LoginInfo!
    var agoraKit: AgoraRtcEngineKit!
    var channelLocal: AgoraRtcChannel?
    var channelRemote: AgoraRtcChannel?
    weak var delegate: MainVMDelegate?
    var renderInfos = [RenderInfo]()
    var pkRoomName: String?
    var players = [String : AgoraMediaPlayer]()
    var manager: PKSyncManager!
    let queue = DispatchQueue(label: "MainVM.queue")
    var appId: String!
    let kPKKey = "PK"
    
    deinit {
        channelLocal?.leave()
        channelRemote?.leave()
    }
    
    init(loginInfo: LoginInfo,
         appId: String) {
        super.init()
        self.appId = appId
        self.loginInfo = loginInfo
        self.manager = PKSyncManager(appId: appId)
    }
    
    func start() {
        manager.delegate = self
        switch loginInfo.role {
        case .audience:
            makeConnect(roomName: loginInfo.roomName)
            joinAudienceRtmChannel()
            break
        case .broadcaster:
            joinChannelLocal()
            break
        }
    }
    
}
