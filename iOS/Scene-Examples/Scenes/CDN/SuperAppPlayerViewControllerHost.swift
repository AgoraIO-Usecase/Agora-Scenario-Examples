//
//  SuperAppPlayerViewControllerHost.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/28.
//

import UIKit
import AgoraRtcKit

class SuperAppPlayerViewControllerHost: BaseViewController {
    let mainView = SuperAppMainView()
    var syncUtil: SuperAppSyncUtil!
    var pushUrlString: String!
    var agoraKit: AgoraRtcEngineKit!
    var config: Config!
    var mode: Mode!
    let liveTranscoding = AgoraLiveTranscoding.default()
    /// log tag
    let defaultLogTag = "HostVC"
    var audioIsMute = false
    var allowChangeToPushMode: Bool!

    public init(config: Config) {
        self.config = config
        self.mode = config.mode
        self.pushUrlString = "rtmp://examplepush.agoramde.agoraio.cn/live/" + config.sceneId
        self.allowChangeToPushMode = mode == .push
        let userId = SupperAppStorageManager.uuid
        let userName = SupperAppStorageManager.userName
        self.syncUtil = SuperAppSyncUtil(appId: config.appId,
                                         sceneId: config.sceneId,
                                         sceneName: config.sceneName,
                                         userId: userId,
                                         userName: userName)
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        
        syncUtil.delegate = self
        syncUtil.joinByHost(roomInfo: config.roomItem, complted: joinCompleted(error:))
        
        config.mode == .push ? joinRtcByPush() : joinRtcByPassPush()
    }
    
    private func setupUI() {
        mainView.setPersonViewHidden(hidden: false)
        view.addSubview(mainView)
        mainView.frame = view.bounds
        mainView.delegate = self
        mainView.setPersonViewHidden(hidden: false)
        let imageName = SupperAppStorageManager.uuid.headImageName
        let info = SuperAppMainView.Info(title: config.sceneName + "(\(config.sceneId))",
                                 imageName: imageName,
                                 userCount: 0)
        mainView.update(info: info)
    }

    private func changeToByPassPush() {
        guard mode != .byPassPush else {
            return
        }
        LogUtils.log(message: "changeToByPassPush", level: .info)
        mode = .byPassPush
        leaveRtcByPush()
    }
    
    private func changeToPush() {
        guard mode != .push else {
            return
        }
        LogUtils.log(message: "changeToPush", level: .info)
        mode = .push
        leaveRtcByPassPush()
        mainView.setRemoteViewHidden(hidden: true)
    }
    
    /// `true` is mute
    func getLocalAudioMuteState() -> Bool {
        return audioIsMute
    }
    
    private func destroy() {
        syncUtil.leaveByHost()
        destroyRtc()
    }
    
    private func joinCompleted(error: LocalizedError?) {
        if let e = error {
            let msg = "joinByAudience fail: \(e.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .info)
            return
        }
        syncUtil.subscribePKInfo()
    }
}

// MRK: - SuperAppSyncUtilDelegate
extension SuperAppPlayerViewControllerHost: SuperAppSyncUtilDelegate {
    func superAppSyncUtilDidPkCancleForOther(util: SuperAppSyncUtil) {
        LogUtils.log(message: "下麦", level: .info)
        if allowChangeToPushMode { changeToPush() }
        else { mainView.setRemoteViewHidden(hidden: true) }
    }
    
    func superAppSyncUtilDidPkAcceptForMe(util: SuperAppSyncUtil, userIdPK: String) {}
    func superAppSyncUtilDidPkCancleForMe(util: SuperAppSyncUtil) {}
    func superAppSyncUtilDidPkAcceptForOther(util: SuperAppSyncUtil) {}
    func superAppSyncUtilDidSceneClose(util: SuperAppSyncUtil) {}
}

// MARK: - UI Event MainViewDelegate
extension SuperAppPlayerViewControllerHost: SuperAppMainViewDelegate {
    func mainView(_ view: SuperAppMainView, didTap action: SuperAppMainView.Action) {
        switch action {
        case .member:
            let inviteView = SuperAppInvitationView()
            inviteView.delegate = self
            inviteView.startFetch(manager: .init(syncUtil: syncUtil))
            AlertManager.show(view: inviteView, alertPostion: .bottom)
            return
        case .more:
            let toolView = SuperAppToolView()
            let open = getLocalAudioMuteState()
            toolView.setMicState(open: open)
            toolView.delegate = self
            AlertManager.show(view: toolView, alertPostion: .bottom)
            return
        case .close:
            destroy()
            dismiss(animated: true, completion: nil)
            return
        case .closeRemote:
            syncUtil.resetPKInfo()
            if allowChangeToPushMode { changeToPush() }
            else { mainView.setRemoteViewHidden(hidden: true) }
            return
        }
    }
}

extension SuperAppPlayerViewControllerHost: SuperAppToolViewDelegate, SuperAppInvitationViewDelegate {
    func invitationView(_ view: SuperAppInvitationView, didSelected info: SuperAppInvitationView.Info) {
        changeToByPassPush()
        syncUtil.updatePKInfo(userIdPK: info.userId)
    }
    
    func toolView(_ view: SuperAppToolView, didTap action: SuperAppToolView.Action) {
        switch action {
        case .camera:
            switchCamera()
        case .mic:
            audioIsMute = !audioIsMute
            muteLocalAudio(mute: audioIsMute)
        }
    }
}

// MARK: - Data Struct
extension SuperAppPlayerViewControllerHost {
    enum Mode: Int {
        /// 直推模式
        case push = 1
        /// 旁路推流模式
        case byPassPush = 2
    }
    
    struct Config {
        let appId: String
        let roomItem: SuperAppRoomInfo
        
        var sceneId: String {
            return roomItem.roomId
        }
        
        var sceneName: String {
            return roomItem.roomName
        }
        
        var mode: Mode {
            return roomItem.liveMode == .push ? .push : .byPassPush
        }
    }
}
