//
//  MainVM+Handle+Broadcaster.swift
//  LivePKCore
//
//  Created by ZYP on 2021/9/27.
//

import Foundation
import AgoraRtcKit

extension MainVM {
    func joinChannelLocal() {
        queue.async { [weak self] in
            guard let self = self else { return }
            self.joinChannelLocalInternal()
        }
    }
    
    private func joinChannelLocalInternal() {
        let channelName = loginInfo.roomName
        let pkKey = kPKKey
        do {
            /// rtm
            try self.manager.login()
            try self.manager.join(channelName: channelName)
            guard let attributes = self.manager.getAttributes(channelName: channelName) else {
                return
            }
            let isPking = self.isPking(attributes: attributes)
            
            /// rtc
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                self.joinRtcChannelLocal(channelName: channelName)
                let info = RenderInfo(isLocal: true, uid: 0, roomName: channelName, type: .rtc)
                self.renderInfos.append(info)
                self.invokeDidUpdateRenderInfos(renders: self.renderInfos)
                if isPking, let pkName = attributes.filter({ $0.key == pkKey }).map({ $0.value }).first, pkName != channelName {
                    self.joinRtcChannelRemote(channelName: pkName)
                }
            }
        } catch let error {
            self.invokeShouldShowTips(tips: error.localizedDescription)
        }
    }
    
    func joinChannelRemote(channelName: String) {
        queue.async { [weak self] in
            guard let self = self else { return }
            self.joinChannelRemoteInternal(channelName: channelName)
        }
    }
    
    private func joinChannelRemoteInternal(channelName: String) {
        do {
            /// get info
            guard let attributes = manager.getAttributes(channelName: channelName) else {
                return
            }
            let isPking = isPking(attributes: attributes)
            
            if isPking,
               let attr =  attributes.filter({ $0.key == kPKKey }).first,
               attr.value != channelName { /** check should pk or not **/
                invokeShouldShowTips(tips: "\(channelName) is pking \(attr.value)")
                return
            }
            
            /// set pk info remote
            Log.info(text: "update Attribute", tag: "joinChannelRemoteInternal")
            let attr = PKSyncManager.Attribute()
            attr.key = kPKKey
            attr.value = loginInfo.roomName
            manager.updateAttribute(channelName: channelName,
                                    attributes: [attr],
                                    completed: { _ in })
            
            /// set pk info local
            let attr2 = PKSyncManager.Attribute()
            attr2.key = kPKKey
            attr2.value = channelName
            manager.updateAttribute(channelName: loginInfo.roomName,
                                    attributes: [attr2],
                                    completed: { _ in })
            
            
            /// rtc
            DispatchQueue.main.async { [weak self] in
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
        guard let channel = channelRemote else {
            return
        }
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.uid = uid
        videoCanvas.view = view
        videoCanvas.renderMode = .hidden
        videoCanvas.channelId = channel.getId()
        agoraKit.setupRemoteVideo(videoCanvas)
    }
    
    func exitPk() {
        if let remoteChannelName = renderInfos.filter({ !$0.isLocal }).map({ $0.roomName }).first {
            manager.deleteAttributes(channelName: remoteChannelName, keys: [kPKKey])
        }
        
        
        Log.info(text: "channelRemote leave", tag: "exitPk")
        channelRemote?.leave()
        channelRemote = nil
        removeAllRemoteRenders()
        invokeDidUpdateRenderInfos(renders: renderInfos)
        
        
        if let channelName = channelLocal?.getId() {
            manager.deleteAttributes(channelName: channelName, keys: [kPKKey])
            removeAllRemoteRenders()
            invokeDidUpdateRenderInfos(renders: renderInfos)
        }
        
    }
}

extension MainVM {
    func handleAttributiesForBroadcaster(channelName: String, attributes: [PKSyncManager.Attribute]) {
        if let formChannelName = attributes.filter({ $0.key == kPKKey }).map({ $0.value }).first,
           renderInfos.count == 1,
           formChannelName != loginInfo.roomName { /** was pk 被pk的时候启动 **/
            if Thread.current.isMainThread {
                joinRtcChannelRemote(channelName: formChannelName)
            }
            else {
                DispatchQueue.main.async {[weak self] in
                    self?.joinRtcChannelRemote(channelName: formChannelName)
                }
            }
        }
    }
}
