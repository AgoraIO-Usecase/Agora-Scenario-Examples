//
//  AgoraVoiceController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/26.
//

import UIKit
import AgoraRtcKit
import AgoraSyncManager

class AgoraVoiceController: BaseViewController {
    /// 顶部头像昵称
    public lazy var avatarview = LiveAvatarView()
    /// 聊天
    public lazy var chatView = LiveChatView()
    /// 在线人数view
    private lazy var onlineView = LiveOnlineView()
    /// 底部功能
    private lazy var bottomView: LiveBottomView = {
        let view = LiveBottomView(type: [.gift, .tool, .close])
        return view
    }()
    private lazy var usersView: AgoraVoiceUsersView = {
        let view = AgoraVoiceUsersView(channelName: channelName, role: getRole(uid: UserInfo.uid))
        return view
    }()
    private lazy var belCantoView: AgoraVoiceBelCantoView = {
        let view = AgoraVoiceBelCantoView(type: .belCanto)
        return view
    }()
    private lazy var soundEffectView: AgoraVoiceBelCantoView = {
        let view = AgoraVoiceBelCantoView(type: .soundEffect)
        return view
    }()
    private lazy var toolView = AgoraVoiceToolView()
    private lazy var changeRoomView: ChangeRoomBgView = {
        let view = ChangeRoomBgView()
        view.didSelectedBgImageClosure = { [weak self] imageNmae in
            guard let self = self else { return }
            self.view.layer.contents = UIImage(named: imageNmae)?.cgImage
            self.roomInfo?.backgroundId = imageNmae
            let params = JSONObject.toJson(self.roomInfo)
            SyncUtil.update(id: self.channelName, key: nil, params: params)
        }
        return view
    }()
    private lazy var mucisView = AgoraVoiceMusicView()
    private lazy var realTimeView = RTCRealTimeDataView()

    public var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.channelProfile = .liveBroadcasting
        config.areaCode = .global
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
        option.autoSubscribeAudio = .of(true)
        option.publishAudioTrack = .of(true)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        return option
    }()
    private var roomInfo: LiveRoomInfo?
    private var channelName: String = ""
    private var currentUserId: String = ""
    private var currentUserModel: AgoraVoiceUsersModel?
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    init(roomInfo: LiveRoomInfo?, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.roomInfo = roomInfo
        self.channelName = roomInfo?.roomId ?? ""
        self.agoraKit = agoraKit
        self.currentUserId = roomInfo?.userId ?? ""
        view.layer.contents = UIImage(named: roomInfo?.backgroundId ?? "BG01")?.cgImage
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
        joinChannel()
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != UserInfo.uid
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is CreateLiveController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        agoraKit?.disableAudio()
        agoraKit?.muteAllRemoteAudioStreams(true)
        agoraKit?.destroyMediaPlayer(nil)
        if getRole(uid: UserInfo.uid) == .broadcaster {
            SyncUtil.delete(id: channelName)
        }
        leaveChannel()
        SyncUtil.leaveScene(id: channelName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    /// 加入channel
    private func joinChannel() {
        channelMediaOptions.clientRoleType = .of((Int32)(getRole(uid: UserInfo.uid).rawValue))
        channelMediaOptions.publishAudioTrack = .of(getRole(uid: UserInfo.uid) == .broadcaster)
        channelMediaOptions.autoSubscribeAudio = .of(true)
        
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           uid: UserInfo.userId,
                                           mediaOptions: channelMediaOptions,
                                           joinSuccess: nil)
        if result == 0 {
            LogUtils.log(message: "加入房间成功", level: .info)
        }
        var messageModel = ChatMessageModel()
        messageModel.message = "\(UserInfo.uid) 加入房间"
        chatView.sendMessage(messageModel: messageModel)
        let params = JSONObject.toJson(AgoraVoiceUsersModel())
        SyncUtil.addCollection(id: channelName, className: SYNC_MANAGER_AGORA_VOICE_USERS, params: params, success: { object in
            let model = JSONObject.toModel(AgoraVoiceUsersModel.self, value: object.toJson())
            self.currentUserModel = model
        })
    }
    
    private func leaveChannel() {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel state == \(state)", level: .info)
        })
        SyncUtil.deleteDocument(id: channelName,
                                className: SYNC_MANAGER_AGORA_VOICE_USERS,
                                objectId: currentUserModel?.objectId ?? "")
    }
    
    private func muteAudioHandler(isVoice: Bool) {
        let option = channelMediaOptions
        option.publishAudioTrack = getRole(uid: UserInfo.uid) == .broadcaster ? .of(true) : .of(isVoice)
        option.publishCameraTrack = .of(getRole(uid: UserInfo.uid) == .broadcaster)
        if getRole(uid: UserInfo.uid) == .audience {
            option.clientRoleType = isVoice ? .of((Int32)(AgoraClientRole.broadcaster.rawValue)) : .of((Int32)(AgoraClientRole.audience.rawValue))
        }
        agoraKit?.updateChannel(with: option)
        if getRole(uid: UserInfo.uid) == .audience {
            agoraKit?.enableLocalAudio(isVoice)
        }
    }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(getRole(uid: UserInfo.uid))
        if getRole(uid: UserInfo.uid) == .broadcaster {
            agoraKit?.enableAudio()
        }
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func eventHandler() {
        bottomView.clickBottomButtonTypeClosure = { [weak self] type in
            guard let self = self else { return }
            switch type {
            case .belcanto:
                AlertManager.show(view: self.belCantoView, alertPostion: .bottom)
                
            case .effect:
                AlertManager.show(view: self.soundEffectView, alertPostion: .bottom)
                
            case .effect_tool:
                AlertManager.show(view: self.toolView, alertPostion: .bottom)
                
            case .close:
                self.navigationController?.popViewController(animated: true)
                
            default: break
            }
        }
        belCantoView.didAgoraVoiceBelCantoItemClosure = { [weak self] model in
            guard let self = self, let model = model else { return }
            if model.title == "女性" {
                self.agoraKit?.setVoiceBeautifierParameters(model.voiceBeautifierPreset, param1: 2, param2: 3)
                return
            }
            self.agoraKit?.setVoiceBeautifierPreset(model.voiceBeautifierPreset)
        }
        soundEffectView.clickSwitchClosure = { [weak self] isOn in
            self?.agoraKit?.setAudioEffectPreset(isOn ? .pitchCorrection : .off)
        }
        soundEffectView.didAgoraVoiceSoundEffectItemClosure = { [weak self] model, index in
            guard let self = self, let model = model else { return }
            if model.effectPreset == .pitchCorrection {
                self.agoraKit?.setAudioEffectParameters(model.effectPreset, param1: index, param2: Int32(model.pitchCorrectionValue))
                return
            }
            self.agoraKit?.setAudioEffectPreset(model.effectPreset)
        }
        
        mucisView.clickPlayButtonClosure = { [weak self] mucisModel, isPlay in
            guard let model = mucisModel else { return }
            if isPlay {
                self?.agoraKit?.startAudioMixing(model.url, loopback: true, replace: false, cycle: 1)
                return
            }
            self?.agoraKit?.stopAudioMixing()
        }
        
        mucisView.clickSliderValueChangeClosure = { [weak self] value in
            self?.agoraKit?.adjustAudioMixingVolume(Int(value * 100))
        }
        
        toolView.clickItemClosure = { [weak self] type, isSelected in
            guard let self = self else { return }
            switch type {
            case .mic:
                self.agoraKit?.muteLocalAudioStream(isSelected)
                
            case .earphone_monitor:
                self.agoraKit?.enable(inEarMonitoring: isSelected)
                
            case .music:
                AlertManager.show(view: self.mucisView, alertPostion: .bottom)
                
            case .backgroundImage:
                AlertManager.show(view: self.changeRoomView, alertPostion: .bottom)
                
            case .real_time_data:
                AlertManager.show(view: self.realTimeView, alertPostion: .center)
                
            default: break
            }
        }
        usersView.muteAudioClosure = { [weak self] isMute in
            self?.muteAudioHandler(isMute: isMute)
        }
        
        guard getRole(uid: UserInfo.uid) == .audience else { return }
        SyncUtil.subscribe(id: channelName, key: nil, onUpdated: { object in
            self.updateBGImage(object: object)
        }, onDeleted: { _ in
            self.showAlert(title: "房间已关闭", message: "") {
                self.navigationController?.popViewController(animated: true)
            }
        })
        SyncUtil.fetch(id: channelName, key: nil, success: { object in
            self.updateBGImage(object: object)
        })
    }
    
    private func updateBGImage(object: IObject?) {
        let roomInfo = JSONObject.toModel(LiveRoomInfo.self, value: object?.toJson())
        view.layer.contents = UIImage(named: roomInfo?.backgroundId ?? "BG01")?.cgImage
        self.roomInfo = roomInfo
    }
    
    private func setupUI() {
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        avatarview.translatesAutoresizingMaskIntoConstraints = false
        chatView.translatesAutoresizingMaskIntoConstraints = false
        onlineView.translatesAutoresizingMaskIntoConstraints = false
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        usersView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(avatarview)
        view.addSubview(chatView)
        view.addSubview(onlineView)
        view.addSubview(bottomView)
        view.addSubview(usersView)
        
        avatarview.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        avatarview.topAnchor.constraint(equalTo: view.topAnchor, constant: Screen.statusHeight() + 15).isActive = true
        avatarview.setName(with: "\(currentUserId) roomId: \(channelName)")
        
        chatView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        chatView.bottomAnchor.constraint(equalTo: bottomView.topAnchor, constant: -15).isActive = true
        chatView.trailingAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        let chatViewW = Screen.width / 2 * 0.9
        chatView.heightAnchor.constraint(equalToConstant: chatViewW).isActive = true
        
        onlineView.topAnchor.constraint(equalTo: avatarview.topAnchor).isActive = true
        onlineView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        onlineView.leadingAnchor.constraint(equalTo: avatarview.trailingAnchor, constant: 15).isActive = true
        
        bottomView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        bottomView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor).isActive = true
        bottomView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        usersView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        usersView.topAnchor.constraint(equalTo: onlineView.bottomAnchor, constant: 30).isActive = true
        usersView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        if getRole(uid: UserInfo.uid) == .broadcaster {
            bottomView.updateButtonType(type: [.belcanto, .effect, .effect_tool, .close])
        } else {
            bottomView.updateButtonType(type: [.close])
        }
    }
}
extension AgoraVoiceController: AgoraRtcEngineDelegate {
    
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
        var messageModel = ChatMessageModel()
        messageModel.message = "\(uid) 加入房间"
        chatView.sendMessage(messageModel: messageModel)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
        var messageModel = ChatMessageModel()
        messageModel.message = "\(uid) 离开房间"
        chatView.sendMessage(messageModel: messageModel)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, reportRtcStats stats: AgoraChannelStats) {
//        localVideo.statsInfo?.updateChannelStats(stats)
        realTimeView.setupData(channelStatus: stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, localVideoStats stats: AgoraRtcLocalVideoStats) {
//        localVideo.statsInfo?.updateLocalVideoStats(stats)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, localAudioStats stats: AgoraRtcLocalAudioStats) {
//        localVideo.statsInfo?.updateLocalAudioStats(stats)
        realTimeView.setupData(localAudioStatus: stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteVideoStats stats: AgoraRtcRemoteVideoStats) {
//        remoteVideo.statsInfo?.updateVideoStats(stats)
        LogUtils.log(message: "remoteVideoWidth== \(stats.width) Height == \(stats.height)", level: .info)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteAudioStats stats: AgoraRtcRemoteAudioStats) {
//        remoteVideo.statsInfo?.updateAudioStats(stats)
    }
}
