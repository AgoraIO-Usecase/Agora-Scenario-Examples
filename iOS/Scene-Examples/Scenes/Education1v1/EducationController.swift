//
//  SignleLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit
import Fastboard

let BOARD_APP_ID: String = "283/VGiScM9Wiw2HJg"
let BOARD_ROOM_UUID: String = "6d181120525e11ec89361798d9c15050"
let BOARD_ROOM_TOKEN: String = "WHITEcGFydG5lcl9pZD15TFExM0tTeUx5VzBTR3NkJnNpZz0wZWIzMmY5M2IzMGUzZTBiMTQ4NzQ3NGVmZTRhNTlkNWM2MjY4ZjFjOmFrPXlMUTEzS1N5THlXMFNHc2QmY3JlYXRlX3RpbWU9MTYzODMzMjYwNTUwNiZleHBpcmVfdGltZT0xNjY5ODY4NjA1NTA2Jm5vbmNlPTE2MzgzMzI2MDU1MDYwMCZyb2xlPXJvb20mcm9vbUlkPTZkMTgxMTIwNTI1ZTExZWM4OTM2MTc5OGQ5YzE1MDUwJnRlYW1JZD05SUQyMFBRaUVldTNPNy1mQmNBek9n"
class EducationController: BaseViewController {
    private lazy var fastRoom: FastRoom = {
        let config = FastRoomConfiguration(appIdentifier: BOARD_APP_ID,
                                           roomUUID: BOARD_ROOM_UUID,
                                           roomToken: BOARD_ROOM_TOKEN,
                                           region: .CN,
                                           userUID: UserInfo.uid)
        let fastRoom = Fastboard.createFastRoom(withFastRoomConfig: config)
        return fastRoom
    }()
    private lazy var localView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 10
        view.layer.masksToBounds = true
        view.backgroundColor = .systemPink
        return view
    }()
    private lazy var remoteView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 10
        view.layer.masksToBounds = true
        view.backgroundColor = .white
        view.tag = 0
        return view
    }()
    private lazy var remoteTipsLabel: UILabel = {
        let label = UILabel()
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        label.text = "Waiting for remote users to join".localized
        return label
    }()
    
    /// 顶部头像昵称
    public lazy var avatarview = LiveAvatarView()
    /// 底部功能
    public lazy var bottomView: LiveBottomView = {
        let view = LiveBottomView(type: [.tool, .close])
        view.hiddenOrShowChatView(isHidden: true)
        return view
    }()
    /// 设置直播的工具弹窗
    private lazy var liveToolView = LiveToolView()
    
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
        avatarview.setName(with: channelName)
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
        guard let index = controllers.firstIndex(where: { $0 is EducationCreateController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        leaveChannel(uid: UserInfo.userId, channelName: channleName, isExit: true)
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.singleLive.rawValue)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        if getRole(uid: UserInfo.uid) == .broadcaster {
            fastRoom.dismissAllSubPanels()
        }
        fastRoom.disconnectRoom()
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        AgoraRtcEngineKit.destroy()
    }
    
    private func setupUI() {
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        fastRoom.view.translatesAutoresizingMaskIntoConstraints = false
        localView.translatesAutoresizingMaskIntoConstraints = false
        remoteView.translatesAutoresizingMaskIntoConstraints = false
        remoteTipsLabel.translatesAutoresizingMaskIntoConstraints = false
        avatarview.translatesAutoresizingMaskIntoConstraints = false
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(fastRoom.view)
        view.addSubview(localView)
        view.addSubview(remoteView)
        remoteView.addSubview(remoteTipsLabel)
        view.addSubview(avatarview)
        view.addSubview(bottomView)
        
        fastRoom.view.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 5).isActive = true
        fastRoom.view.topAnchor.constraint(equalTo: view.topAnchor, constant: 5).isActive = true
        fastRoom.view.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -5).isActive = true
        
        localView.leadingAnchor.constraint(equalTo: fastRoom.view.trailingAnchor, constant: 5).isActive = true
        localView.topAnchor.constraint(equalTo: fastRoom.view.topAnchor).isActive = true
        localView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -5).isActive = true
        localView.widthAnchor.constraint(equalToConstant: (Screen.width - 15) / 2.0).isActive = true
        localView.heightAnchor.constraint(equalToConstant: (Screen.width - 15) / 2.0).isActive = true
        remoteView.leadingAnchor.constraint(equalTo: localView.leadingAnchor).isActive = true
        remoteView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -5).isActive = true
        remoteView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -5).isActive = true
        remoteView.topAnchor.constraint(equalTo: localView.bottomAnchor, constant: 5).isActive = true
        remoteTipsLabel.centerYAnchor.constraint(equalTo: remoteView.centerYAnchor).isActive = true
        remoteTipsLabel.centerXAnchor.constraint(equalTo: remoteView.centerXAnchor).isActive = true
        
        avatarview.leadingAnchor.constraint(equalTo: fastRoom.view.leadingAnchor, constant: 5).isActive = true
        avatarview.topAnchor.constraint(equalTo: fastRoom.view.topAnchor, constant: 5).isActive = true
        
        bottomView.bottomAnchor.constraint(equalTo: fastRoom.view.bottomAnchor, constant: 5).isActive = true
        bottomView.trailingAnchor.constraint(equalTo: fastRoom.view.trailingAnchor, constant: 5).isActive = true
    }
    
    public func eventHandler() {
        // 底部功能回调
        bottomView.onTapBottomButtonTypeClosure = { [weak self] type in
            guard let self = self else { return }
            switch type {
            case .close:
                self.onTapCloseLive()
                
            case .tool:
                self.liveToolView.onTapItemClosure = { itemType, isSelected in
                    switch itemType {
                    case .switch_camera:
                        self.agoraKit?.switchCamera()
                        
                    case .camera:
                        _ = isSelected ? self.agoraKit?.stopPreview() : self.agoraKit?.startPreview()
                        self.agoraKit?.muteLocalVideoStream(isSelected)
                    
                    case .mic:
                        self.agoraKit?.muteLocalAudioStream(isSelected)
                    
                    default: break
                    }
                }
                AlertManager.show(view: self.liveToolView, alertPostion: .bottom)
                
            default: break
            }
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
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String, uid: UInt) {
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           info: nil,
                                           uid: uid,
                                           options: channelMediaOptions)
        if result == 0 {
            LogUtils.log(message: "进入房间", level: .info)
        }
        let canvas = AgoraRtcVideoCanvas()
        canvas.renderMode = .hidden
        canvas.view = localView
        canvas.uid = uid
        agoraKit?.setupLocalVideo(canvas)
        agoraKit?.startPreview()
        fastRoom.joinRoom()
    }
    
    public func leaveChannel(uid: UInt, channelName: String, isExit: Bool = false) {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension EducationController: AgoraRtcEngineDelegate {
    
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
        if remoteView.tag <= 0 {
            remoteView.tag = Int(uid)
            let canvas = AgoraRtcVideoCanvas()
            canvas.uid = uid
            canvas.renderMode = .hidden
            canvas.view = remoteView
            agoraKit?.setupRemoteVideo(canvas)
            remoteView.subviews.forEach({
                if !($0 is UILabel) {
                    // show stream player
                    $0.isHidden = false
                    return
                }
            })
        }
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
        if remoteView.tag > 0 {
            remoteView.tag = 0
            remoteView.subviews.forEach({
                if !($0 is UILabel) {
                    // hidden stream player
                    $0.isHidden = true
                    return
                }
            })
        }
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
