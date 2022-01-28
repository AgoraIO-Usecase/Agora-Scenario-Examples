//
//  SignleLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit

class SignleLiveController: BaseViewController {
    public lazy var liveView: LiveBaseView = {
        let view = LiveBaseView(channelName: channleName, currentUserId: currentUserId)
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
        option.autoSubscribeVideo = .of(true)
        return option
    }()
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    private(set) var sceneType: SceneType = .singleLive
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    init(channelName: String, sceneType: SceneType, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channleName = channelName
        self.sceneType = sceneType
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
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            joinBroadcasterChannel(channelName: channleName, uid: UserInfo.userId)
        } else {
            joinAudienceChannel(channelName: channleName, pkUid: UInt(currentUserId) ?? 0)
        }
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is CreateLiveController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        agoraKit?.muteAllRemoteAudioStreams(true)
        agoraKit?.muteAllRemoteVideoStreams(true)
        agoraKit?.destroyMediaPlayer(nil)
        
        leaveChannel(uid: UserInfo.userId, channelName: channleName, isExit: true)
        SyncUtil.unsubscribe(id: channleName, key: sceneType.rawValue)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GIFT_INFO)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        liveView.translatesAutoresizingMaskIntoConstraints = false
    
        view.addSubview(liveView)
        
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
    
    public func eventHandler() {
        liveView.onClickCloseLiveClosure = { [weak self] in
            self?.clickCloseLive()
        }
        liveView.onClickSwitchCameraClosure = { [weak self] isSelected in
            self?.agoraKit?.switchCamera()
        }
        liveView.onClickIsMuteCameraClosure = { [weak self] isSelected in
            self?.agoraKit?.muteLocalVideoStream(isSelected)
        }
        liveView.onClickIsMuteMicClosure = { [weak self] isSelected in
            self?.agoraKit?.muteLocalAudioStream(isSelected)
        }
        liveView.setupLocalVideoClosure = { [weak self] canvas in
            self?.agoraKit?.setupLocalVideo(canvas)
            self?.agoraKit?.startPreview()
        }
        liveView.setupRemoteVideoClosure = { [weak self] canvas, connection in
            self?.agoraKit?.setupRemoteVideoEx(canvas, connection: connection)
        }
        
        guard getRole(uid: UserInfo.uid) == .audience else { return }
        SyncUtil.subscribe(id: channleName, key: nil, onDeleted: { _ in
            self.showAlert(title: "直播已结束", message: "") {
                self.navigationController?.popViewController(animated: true)
            }
        })
    }
    
    private func clickCloseLive() {
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                self?.closeLiveHandler()
                SyncUtil.delete(id: self?.channleName ?? "")
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            closeLiveHandler()
            navigationController?.popViewController(animated: true)
        }
    }
    
    /// 关闭直播
    public func closeLiveHandler() { }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(getRole(uid: currentUserId))
        if getRole(uid: currentUserId) == .broadcaster {
            agoraKit?.enableVideo()
            agoraKit?.enableAudio()
        }
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    /// 主播加入channel
    public func joinBroadcasterChannel(channelName: String, uid: UInt) {
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: uid)
        let connection = LiveCanvasModel.createConnection(channelName: channelName, uid: uid)
        canvasModel.connection = connection
        canvasModel.channelName = channelName
        liveView.setupCanvasData(data: canvasModel)
        
        if getRole(uid: "\(uid)") == .broadcaster && channelName == self.channleName {
            channelMediaOptions.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
            channelMediaOptions.publishAudioTrack = .of(true)
            channelMediaOptions.publishCameraTrack = .of(true)
            channelMediaOptions.autoSubscribeVideo = .of(true)
            channelMediaOptions.autoSubscribeAudio = .of(true)
        }
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token, channelId: channelName, uid: uid, mediaOptions: channelMediaOptions, joinSuccess: nil)
        if result == 0 {
            LogUtils.log(message: "主播进入房间", level: .info)
        }
        liveView.sendMessage(message: "\(UserInfo.userId)加入房间", messageType: .message)
    }
    
    /// 观众加入channel
    /// - Parameters:
    ///   - channelName: 频道名
    ///   - pkUid: pk主播的UserID
    public func joinAudienceChannel(channelName: String, pkUid: UInt = 0) {
        let isContainer = liveView.canvasDataArray.contains(where: { $0.channelName == channelName && $0.canvas?.uid == pkUid })
        guard !isContainer else {
            LogUtils.log(message: "当前用户存在 channelName == \(channelName) pkUid == \(pkUid)", level: .warning)
            let value = liveView.canvasDataArray.map({ "channleName == \($0.channelName) userId == \($0.connection?.localUid ?? 0)" })
            LogUtils.log(message: "所有用户 \(value))", level: .warning)
            liveView.reloadData()
            return
        }
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: pkUid)
        let connection = LiveCanvasModel.createConnection(channelName: channelName, uid: UserInfo.userId)
        canvasModel.connection = connection
        canvasModel.channelName = channelName
        liveView.setupCanvasData(data: canvasModel)
        
        channelMediaOptions.clientRoleType = .of((Int32)(AgoraClientRole.audience.rawValue))
        let joinResult = agoraKit?.joinChannelEx(byToken: KeyCenter.Token, connection: connection, delegate: self, mediaOptions: channelMediaOptions, joinSuccess: nil) ?? -1000
        if joinResult == 0 {
            LogUtils.log(message: "join audience success uid == \(pkUid) channelName == \(channelName)", level: .info)
            let userId = pkUid == (UInt(currentUserId) ?? 0) ? UserInfo.userId : pkUid
            liveView.sendMessage(message: "\(userId)加入房间", messageType: .message)
            return
        }
        LogUtils.log(message: "join audience error uid == \(pkUid) channelName == \(channelName)", level: .error)
    }
    public func leaveChannel(uid: UInt, channelName: String, isExit: Bool = false) {
        if isExit && "\(uid)" == currentUserId {
            liveView.canvasDataArray.forEach({
                if let connection = $0.connection {
                    agoraKit?.leaveChannelEx(connection, leaveChannelBlock: nil)
                }
            })
        }
        guard liveView.canvasDataArray.count > 1 else { return }
        if let connection = liveView.canvasDataArray.filter({ $0.connection?.localUid == uid && $0.channelName == channelName }).first?.connection {
            agoraKit?.leaveChannelEx(connection, leaveChannelBlock: { state in
                LogUtils.log(message: "left channel: \(connection.channelId) uid == \(connection.localUid)",
                             level: .info)
            })
        }
        if let connectionIndex = liveView.canvasDataArray.firstIndex(where: { $0.connection?.localUid == uid && $0.channelName == channelName }) {
            liveView.removeData(index: connectionIndex)
        }
    }
    
    public func didOfflineOfUid(uid: UInt) {
        let index = liveView.canvasDataArray.firstIndex(where: { $0.connection?.localUid == uid && $0.channelName != channleName }) ?? -1
        if index > -1 && liveView.canvasDataArray.count > 1 {
            liveView.removeData(index: index)
        }
        guard "\(uid)" != currentUserId else { return }
        liveView.sendMessage(message: "\(uid)离开房间", messageType: .message)
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension SignleLiveController: AgoraRtcEngineDelegate {
    
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
        didOfflineOfUid(uid: uid)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, reportRtcStats stats: AgoraChannelStats) {
//        localVideo.statsInfo?.updateChannelStats(stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, localVideoStats stats: AgoraRtcLocalVideoStats) {
//        localVideo.statsInfo?.updateLocalVideoStats(stats)
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
