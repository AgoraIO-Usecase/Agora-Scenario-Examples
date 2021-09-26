//
//  MainVM.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import Foundation
import AgoraRtcKit
import AgoraMediaPlayer
import SyncManager

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
    
    deinit {
        channelLocal?.leave()
    }
    
    init(loginInfo: LoginInfo) {
        super.init()
        self.loginInfo = loginInfo
        self.manager = PKSyncManager(appId: KeyCenter.AppId)
    }
    
    func start() {
        manager.delegate = self
        switch loginInfo.role {
        case .audience:
            makeConnect(roomName: loginInfo.roomName)
            break
        case .broadcaster:
            joinChannelLocal()
            break
        }
    }
    
    func joinChannelLocal() {
        let channelName = loginInfo.roomName
        queue.async { [weak self] in
            guard let self = self else { return }
            do {
                /// rtm
                try self.manager.login()
                try self.manager.join(channelName: channelName)
                
                /// rtc
                DispatchQueue.main.sync { [weak self] in
                    guard let self = self else { return }
                    self.joinRtcChannelLocal(channelName: channelName)
                    let info = RenderInfo(isLocal: true, uid: 0, roomName: channelName, type: .rtc)
                    self.renderInfos.append(info)
                    self.invokeDidUpdateRenderInfos(renders: self.renderInfos)
                }
                
                let attributes = try self.manager.getAttributes(channelName: channelName)
                let isPking = self.isPking(attributes: attributes)
                if isPking, let pkName = attributes.filter({ $0.key == "PK" }).map({ $0.value }).first, pkName != channelName {
                    DispatchQueue.main.sync { [weak self] in
                        self?.joinRtcChannelRemote(channelName: pkName)
                    }
                }
                
            } catch let error {
                self.invokeShouldShowTips(tips: error.localizedDescription)
            }
        }
    }
                  
    func joinChannelRemote(channelName: String) {
        queue.async { [weak self] in
            guard let self = self else { return }
            self.joinChannelRemoteInternal(channelName: channelName)
        }
    }
    
    func joinChannelRemoteInternal(channelName: String) {
        do {
            /// get info
            try manager.join(channelName: channelName)
//            let attributes = try manager.getAttributes(channelName: channelName)
//            let isPking = isPking(attributes: attributes)
            
//            if isPking, attributes.filter({ $0.key == "PK" }).map({ $0.value }).first != channelName { /** check should pk or not **/
//                /// TODO: leave channel
//                invokeShouldShowTips(tips: "is pking \(attributes)")
//                return
//            }
            
            /// set pk info
            let attr = PKSyncManager.Attribute()
            attr.key = "PK"
            attr.value = loginInfo.roomName
            manager.updateAttribute(channelName: channelName,
                                    attributes: [attr],
                                    completed: { _ in })
            
            /// rtc
            DispatchQueue.main.sync { [weak self] in
                guard let self = self else { return }
                self.joinRtcChannelRemote(channelName: channelName)
            }
            
        } catch let error {
            invokeShouldShowTips(tips: (error as! SyncError).description)
        }
    }
    
    func subscribeVideoLocal(view: UIView) {
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.uid = 0
        videoCanvas.view = view
        videoCanvas.renderMode = .hidden
        agoraKit.setupLocalVideo(videoCanvas)
        agoraKit.startPreview()
    }
    
    func subscribeVideoRemote(view: UIView, uid: UInt) {
        guard let channel = channelRemote ?? channelLocal else {
            return
        }
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.uid = uid
        videoCanvas.view = view
        videoCanvas.renderMode = .hidden
        videoCanvas.channelId = channel.getId()
        agoraKit.setupRemoteVideo(videoCanvas)
    }
    
    func subscribeMedia(view: UIView, roomName: String) {
        let player = players[roomName] ?? AgoraMediaPlayer(delegate: self)
        players[roomName] = player

        player.setView(view)
        player.open("rtmp://mdetest.pull.agoramde.agoraio.cn/live/\(roomName)", startPos: 0)
        player.play()
    }
    
    func makeConnect(roomName: String) {
        let info = RenderInfo(isLocal: false, uid: 0, roomName: roomName, type: .cdn)
        renderInfos.append(info)
        delegate?.mainVMDidUpdateRenderInfos(renders: renderInfos)
    }
    
    func exitPk() {
        if let remoteChannelName = renderInfos.filter({ !$0.isLocal }).map({ $0.roomName }).first {
            manager.delete(channelName: remoteChannelName, keys: ["PK"])
            manager.leaveChannel(channelName: remoteChannelName)
        }
        
        
        channelRemote?.leave()
        channelRemote = nil
        renderInfos = renderInfos.filter({ $0.isLocal })
        invokeDidUpdateRenderInfos(renders: renderInfos)
        
        
        if let channelName = channelLocal?.getId() {
            manager.delete(channelName: channelName, keys: ["PK"])
            renderInfos = renderInfos.filter({ $0.isLocal })
            invokeDidUpdateRenderInfos(renders: renderInfos)
        }
        
    }
}
