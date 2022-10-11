//
//  SignleLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit
import Fastboard
import Agora_Scene_Utils

class LargeClassController: BaseViewController {
    private lazy var layoutView: LargeClassView = {
        let view = LargeClassView(channelName: channleName, role: getRole(uid: UserInfo.uid) == .broadcaster ? .boradcast : .audience)
        return view
    }()
    
    public var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        return config
    }()
    public lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
        option.publishLocalAudio = true
        option.publishLocalVideo = true
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        return option
    }()
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    init(channelName: String, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channleName = channelName
        self.agoraKit = agoraKit
        self.currentUserId = userId
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupAgoraKit()
        eventHandler()
        // 设置屏幕常亮
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: true)
        let appdelegate = UIApplication.shared.delegate as? AppDelegate
        appdelegate?.blockRotation = .landscapeRight
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel(channelName: channleName, uid: UserInfo.userId)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is LargeClassCreateController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        leaveChannel()
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.largeClass.rawValue)
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).document().unsubscribe(key: "")
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        if getRole(uid: UserInfo.uid) == .broadcaster {
            layoutView.dismissAllSubPanels()
        }
        layoutView.disconnectRoom()
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        AgoraRtcEngineKit.destroy()
    }
    
    private func setupUI() {
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        layoutView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(layoutView)
        layoutView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        layoutView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        layoutView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        layoutView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
    
    public func eventHandler() {
        layoutView.setupVideoCanvasClosure = { [weak self] uid, canvasView in
            guard let self = self else { return }
            let canvas = AgoraRtcVideoCanvas()
            canvas.renderMode = .hidden
            canvas.uid = uid
            canvas.view = canvasView
            if uid == UserInfo.userId {
                self.agoraKit?.setupLocalVideo(canvas)
            } else {
                self.agoraKit?.setupRemoteVideo(canvas)
            }
        }
        layoutView.onpublishMicrophoneTrackClosure = { [weak self] isPublish in
            guard let self = self else { return }
            self.agoraKit?.setClientRole(isPublish ? .broadcaster : .audience)
            self.agoraKit?.muteLocalAudioStream(!isPublish)
        }
        layoutView.onPublishCameraTrackClosre = { [weak self] isPublish in
            guard let self = self else { return }
            self.agoraKit?.muteLocalVideoStream(!isPublish)
        }
        layoutView.onTapCloseButtonClosure = { [weak self] in
            self?.onTapCloseLive()
        }

        guard getRole(uid: UserInfo.uid) == .audience else { return }
        SyncUtil.scene(id: channleName)?.subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            
        }, onDeleted: { object in
            self.showAlert(title: "live_broadcast_over".localized, message: "") {
                self.navigationController?.popViewController(animated: true)
            }
        }, onSubscribed: {
            
        }, fail: { error in
            
        })
    }
    
    private func onTapCloseLive() {
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                SyncUtil.scene(id: self?.channleName ?? "")?.deleteScenes()
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            navigationController?.popViewController(animated: true)
        }
    }
        
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setChannelProfile(.liveBroadcasting)
        agoraKit?.setClientRole(getRole(uid: UserInfo.uid))
        let roleOptions = AgoraClientRoleOptions()
        roleOptions.audienceLatencyLevel = getRole(uid: currentUserId) == .audience ? .ultraLowLatency : .lowLatency
        agoraKit?.setClientRole(getRole(uid: UserInfo.uid), options: roleOptions)
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String, uid: UInt) {
        agoraKit?.muteLocalVideoStream(getRole(uid: UserInfo.uid) == .audience)
        agoraKit?.muteLocalAudioStream(getRole(uid: UserInfo.uid) == .audience)
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           info: nil,
                                           uid: uid,
                                           options: channelMediaOptions)
        if result == 0 {
            LogUtils.log(message: "进入房间", level: .info)
        }
        agoraKit?.startPreview()
        layoutView.joinRoom(role: getRole(uid: UserInfo.uid) == .broadcaster ? .boradcast : .audience)
    }
    
    private func leaveChannel() {
        guard let model = layoutView.currentModel else { return }
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .document(id: model.objectId ?? "")
            .delete(success: { objects in
                LogUtils.log(message: "\(objects.count)", level: .info)
            }, fail: { error in
                LogUtils.log(message: error.description, level: .error)
            })
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension LargeClassController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        LogUtils.log(message: "warning: \(warningCode.description)", level: .warning)
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        LogUtils.log(message: "error: \(errorCode)", level: .error)
        showAlert(title: "Error", message: "Error \(errorCode.description) occur")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "Join \(channel) with uid \(uid) elapsed \(elapsed)ms", level: .info)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "remote user join: \(uid) \(elapsed)ms", level: .info)
//        var model = AgoraUsersModel()
//        model.userId = "\(uid)"
//        model.userName = "User-\(uid)"
//        model.isEnableAudio = true
//        model.isEnableVideo = true
//        dataArray.append(model)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
//        guard let index = dataArray.firstIndex(where: { $0.userId == "\(uid)" }) else { return }
//        dataArray.remove(at: index)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, reportRtcStats stats: AgoraChannelStats) {
//        localVideo.statsInfo?.updateChannelStats(stats)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, localAudioStats stats: AgoraRtcLocalAudioStats) {
//        localVideo.statsInfo?.updateLocalAudioStats(stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteVideoStats stats: AgoraRtcRemoteVideoStats) {
//        remoteVideo.statsInfo?.updateVideoStats(stats)
        LogUtils.log(message: "remoteVideoWidth== \(stats.width) Height == \(stats.height)", level: .info)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteAudioStats stats: AgoraRtcRemoteAudioStats) {
//        remoteVideo.statsInfo?.updateAudioStats(stats)
    }
}
