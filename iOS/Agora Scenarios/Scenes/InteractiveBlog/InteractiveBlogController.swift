//
//  SignleLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit

var interactiveBlogDownPullClosure: ((_ channelName: String, _ model: InteractiveBlogUsersModel?, _ agoraKit: AgoraRtcEngineKit?, _ role: AgoraClientRole) -> Void)?
class InteractiveBlogController: BaseViewController {
    
    public lazy var liveView: InteractiveBlogView = {
        let view = InteractiveBlogView(channelName: channleName, role: getRole(uid: UserInfo.uid), isAddUser: isAddUser)
        return view
    }()
    public var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.channelProfile = .liveBroadcasting
        config.areaCode = .global
        return config
    }()
    public lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
        option.autoSubscribeAudio = .of(true)
        option.autoSubscribeVideo = .of(false)
        return option
    }()
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    private var isAddUser: Bool = true
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    init(channelName: String, userId: String, agoraKit: AgoraRtcEngineKit? = nil, isAddUser: Bool = true) {
        super.init(nibName: nil, bundle: nil)
        self.isAddUser = isAddUser
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
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel(channelName: channleName, uid: UserInfo.userId)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is InteractiveBlogCreateController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    private func setupUI() {
        view.layer.contents = nil
        view.backgroundColor = .init(hex: "#10141c")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        liveView.translatesAutoresizingMaskIntoConstraints = false
    
        view.addSubview(liveView)
        
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor, constant: Screen.kNavHeight).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
    
    public func eventHandler() {
        liveView.onTapLeaveButtonClosure = { [weak self] in
            guard let self = self else { return }
            self.onTapCloseLive()
        }
        liveView.onTapDownButtonClosure = { [weak self] currentUserModel in
            guard let self = self else { return }
            interactiveBlogDownPullClosure?(self.channleName, currentUserModel, self.agoraKit, self.getRole(uid: UserInfo.uid))
            self.navigationController?.popViewController(animated: true)
        }
        liveView.onTapCloseButtonClosure = { [weak self] in
            self?.onTapCloseLive()
        }
        liveView.enableAudioClosure = { [weak self] isEnable in
            guard let self = self else { return }
            self.channelMediaOptions.publishMicrophoneTrack = .of(isEnable)
            self.channelMediaOptions.clientRoleType = .of(Int32(isEnable ? AgoraClientRole.broadcaster.rawValue : AgoraClientRole.audience.rawValue))
            self.agoraKit?.updateChannel(with: self.channelMediaOptions)
            self.agoraKit?.setClientRole(isEnable ? .broadcaster : .audience)
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
                self?.leaveChannel()
                SyncUtil.scene(id: self?.channleName ?? "")?.delete(success: nil, fail: nil)
                SyncUtil.leaveScene(id: self?.channleName ?? "")
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            leaveChannel()
            SyncUtil.leaveScene(id: channleName)
            navigationController?.popViewController(animated: true)
        }
    }
        
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        let roleOptions = AgoraClientRoleOptions()
        roleOptions.audienceLatencyLevel = getRole(uid: currentUserId) == .audience ? .ultraLowLatency : .lowLatency
        agoraKit?.setClientRole(getRole(uid: currentUserId), options: roleOptions)
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String, uid: UInt) {
        guard isAddUser else { return }
        channelMediaOptions.clientRoleType = .of((Int32)(getRole(uid: "\(uid)").rawValue))
        channelMediaOptions.publishMicrophoneTrack = .of(getRole(uid: "\(uid)") == .broadcaster)
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token, channelId: channelName, info: nil, uid: uid, joinSuccess: nil)
        if result == 0 {
            LogUtils.log(message: "进入房间", level: .info)
        }
    }
    
    public func leaveChannel() {
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        liveView.leave()
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.interactiveBlog.rawValue)
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SYNC_MANAGER_AGORA_VOICE_USERS)
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
        AgoraRtcEngineKit.destroy()
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension InteractiveBlogController: AgoraRtcEngineDelegate {
    
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
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
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
