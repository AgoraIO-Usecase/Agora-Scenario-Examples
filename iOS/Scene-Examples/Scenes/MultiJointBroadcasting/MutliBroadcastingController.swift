//
//  SignleLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit
import Agora_Scene_Utils

class MutliBroadcastingController: BaseViewController {
    private lazy var liveView: LiveBaseView = {
        let view = LiveBaseView(channelName: channleName, currentUserId: currentUserId)
        view.updateLiveLayout(postion: .mutli)
        return view
    }()
    private lazy var mutliView: MutliView = {
        let view = MutliView(channelName: channleName, role: getRole(uid: UserInfo.uid), agoraKit: agoraKit)
        return view
    }()
    private var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
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
        setupAgoraKit()
        setupUI()
        eventHandler()
        // 设置屏幕常亮
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: true)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel(channelName: channleName, uid: UserInfo.userId)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is MutliBroadcastingCreateController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        mutliView.leavl()
        leaveChannel(uid: UserInfo.userId, channelName: channleName, isExit: true)
        liveView.leave(channelName: channleName)
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.singleLive.rawValue)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        AgoraRtcEngineKit.destroy()
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        liveView.translatesAutoresizingMaskIntoConstraints = false
    
        view.addSubview(liveView)
        view.addSubview(mutliView)
        
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        mutliView.translatesAutoresizingMaskIntoConstraints = false
        mutliView.leadingAnchor.constraint(equalTo: liveView.liveCanvasView.trailingAnchor).isActive = true
        mutliView.topAnchor.constraint(equalTo: liveView.liveCanvasView.topAnchor).isActive = true
        mutliView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        mutliView.bottomAnchor.constraint(equalTo: liveView.liveCanvasView.bottomAnchor).isActive = true
    }
    
    public func eventHandler() {
        liveView.onTapCloseLiveClosure = { [weak self] in
            self?.onTapCloseLive()
        }
        liveView.onTapSwitchCameraClosure = { [weak self] isSelected in
            self?.agoraKit?.switchCamera()
        }
        liveView.onTapIsMuteCameraClosure = { [weak self] isSelected in
            self?.agoraKit?.muteLocalVideoStream(isSelected)
        }
        liveView.onTapIsMuteMicClosure = { [weak self] isSelected in
            self?.agoraKit?.muteLocalAudioStream(isSelected)
        }
        liveView.setupRemoteVideoClosure = { [weak self] canvas in
            guard let canvas = canvas.canvas else { return }
            self?.agoraKit?.setupRemoteVideo(canvas)
        }
        
        mutliView.joinTheBroadcasting = { [weak self] isBroadcast in
            guard let self = self else { return }
            self.agoraKit?.setClientRole(isBroadcast ? .broadcaster : .audience)
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
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(getRole(uid: currentUserId))
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String, uid: UInt) {
        channelMediaOptions.publishLocalAudio = getRole(uid: "\(uid)") == .broadcaster
        channelMediaOptions.publishLocalVideo = getRole(uid: "\(uid)") == .broadcaster
        channelMediaOptions.autoSubscribeVideo = true
        channelMediaOptions.autoSubscribeAudio = true
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           info: nil,
                                           uid: uid,
                                           options: channelMediaOptions)
        if result == 0 {
            LogUtils.log(message: "进入房间", level: .info)
        }
        let canvas = AgoraRtcVideoCanvas()
        canvas.view = liveView.liveCanvasView
        canvas.renderMode = .hidden
        if getRole(uid: "\(uid)") == .broadcaster {
            canvas.uid = uid
            agoraKit?.setupLocalVideo(canvas)
        } else {
            canvas.uid = UInt(currentUserId) ?? 0
            agoraKit?.setupRemoteVideo(canvas)
        }
        agoraKit?.startPreview()
        liveView.sendMessage(userName: UserInfo.uid, message: "Join_Live_Room".localized, messageType: .message)
    }
    
    public func leaveChannel(uid: UInt, channelName: String, isExit: Bool = false) {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
    }
    
    public func didOfflineOfUid(uid: UInt) {
        guard "\(uid)" != currentUserId else { return }
        liveView.sendMessage(userName: "\(uid)",
                             message: "Leave_Live_Room".localized,
                             messageType: .message)
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension MutliBroadcastingController: AgoraRtcEngineDelegate {
    
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
        liveView.sendMessage(userName: "\(uid)", message: "Join_Live_Room".localized, messageType: .message)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
        didOfflineOfUid(uid: uid)
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
