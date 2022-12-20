//
//  AgoraClubController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/3/21.
//

import UIKit
import AgoraRtcKit
import Agora_Scene_Utils

class AgoraClubController: BaseViewController {
    public var agoraKit: AgoraRtcEngineKit?
    private var mediaPlayerKit: AgoraRtcMediaPlayerProtocol?
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
        option.autoSubscribeVideo = .of(true)
        option.publishMicrophoneTrack = .of(true)
        option.publishCameraTrack = .of(false)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        return option
    }()
    private lazy var headerView: ClubHeaderView = {
        let view = ClubHeaderView()
        view.onTapVideoViewClosure = { [weak self] isSelected in
            self?.navigationController?.setNavigationBarHidden(isSelected, animated: true)
        }
        return view
    }()
    private lazy var usersView: SeatView = {
        let view = SeatView(channelName: channelName,
                            role: getRole(uid: UserInfo.uid),
                            agoraKit: agoraKit,
                            mediaOptions: channelMediaOptions,
                            connection: connection!)
        return view
    }()
    /// 聊天
    private lazy var chatView = LiveChatView()
    /// 底部功能
    private lazy var bottomView: LiveBottomView = {
        let view = LiveBottomView(type: [.gift, .close])
        return view
    }()
    /// 设置直播的工具弹窗
    private lazy var liveToolView: LiveToolView = {
        let view = LiveToolView()
        view.updateToolType(type: [.switch_camera, .camera, .mic], isSelected: true)
        return view
    }()
    /// 礼物
    private lazy var giftView = LiveGiftView()
    public lazy var playGifView: GIFImageView = {
        let view = GIFImageView()
        view.isHidden = true
        return view
    }()
    
    private var channelName: String = ""
    private var videoUrl: String?
    private var headerHeightCons: NSLayoutConstraint?
    private var currentUserModel: AgoraUsersModel?
    private var currentUserId: String = ""
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    private var _connection: AgoraRtcConnection?
    private var connection: AgoraRtcConnection? {
        set {
            _connection = newValue
        }
        get {
            if _connection != nil {
                return _connection
            }
            let connection = AgoraRtcConnection()
            connection.channelId = channelName
            connection.localUid = UserInfo.userId
            _connection = connection
            return connection
        }
    }
    
    init(userId: String, channelName: String?, videoUrl: String?, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channelName = channelName ?? ""
        self.videoUrl = videoUrl
        self.agoraKit = agoraKit
        currentUserId = userId
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
        
        setupMediaPlayKit()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: false)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel()
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is LiveBroadcastingCreateController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).delete(id: currentUserModel?.objectId ?? "", success: { _ in
            
        }, fail: { error in
            ToastView.show(text: error.message)
        })
        leaveChannel()
        SyncUtil.leaveScene(id: channelName)
        if getRole(uid: UserInfo.uid) == .broadcaster {
            SyncUtil.scene(id: channelName)?.delete(success: { objects in
                
            }, fail: { error in
                ToastView.show(text: error.message)
            })
        }
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        agoraKit?.disableAudio()
        agoraKit?.muteAllRemoteAudioStreams(true)
        agoraKit?.destroyMediaPlayer(mediaPlayerKit)
        AgoraRtcEngineKit.destroy()
    }
    
    /// 监听礼物
    private func subscribeGift(channelName: String) {
        SyncUtil.scene(id: channelName)?.subscribe(key: SYNC_MANAGER_GIFT_INFO, onCreated: { object in
            
        }, onUpdated: { object in
            LogUtils.log(message: "onUpdated gift == \(String(describing: object.toJson()))", level: .info)
            guard let model = JSONObject.toModel(LiveGiftModel.self, value: object.toJson()) else { return }
            self.playGifView.isHidden = false
            self.playGifView.loadGIFName(gifName: model.gifName)
            let messageModel = ChatMessageModel(content: model.userId + "i_gave_one_away".localized + model.title, messageType: .message)
            self.chatView.sendMessage(messageModel: messageModel)
        }, onDeleted: { object in
            
        }, onSubscribed: {
            LogUtils.log(message: "subscribe gift", level: .info)
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func eventHandler() {
        subscribeGift(channelName: channelName)
        SyncUtil.scene(id: channelName)?.subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            
        }, onDeleted: { object in
            LogUtils.log(message: "删除房间 == \(object.getId())", level: .info)
            AGEToastView.show(text: "room_is_closed".localized, duration: 2)
            self.navigationController?.popViewController(animated: true)
        }, onSubscribed: {
            
        }, fail: { error in
            ToastView.show(text: error.message)
        })
        // 在麦上显示工具栏
        usersView.updateBottomTypeClosure = { isMic in
            self.bottomView.updateButtonType(type: isMic ? [.gift, .tool, .close] : [.gift, .close])
        }
        // 更新用户上麦或下麦状态
        usersView.updateUserStatusClosure = { [weak self] userModel in
            self?.currentUserModel = userModel
        }
        // gif播放完成回调
        playGifView.gifAnimationFinishedClosure = { [weak self] in
            guard let self = self else { return }
            self.playGifView.isHidden = true
        }
        // 聊天发送
        bottomView.onTapChatButtonClosure = { [weak self] message in
            guard let self = self else { return }
            let model = ChatMessageModel(content: message, messageType: .message)
            self.chatView.sendMessage(messageModel: model)
        }
        // 点击聊天消息
        chatView.didSelectRowAt = { [weak self] messageModel in
            self?.chatView.sendMessage(messageModel: messageModel)
        }
        // 底部功能回调
        bottomView.onTapBottomButtonTypeClosure = { [weak self] type in
            guard let self = self else { return }
            switch type {
            case .close:
                self.onTapBackButton()
                
            case .tool:
                self.liveToolView.onTapItemClosure = { itemType, isSelected in
                    switch itemType {
                    case .switch_camera:
                        self.agoraKit?.switchCamera()
                        
                    case .camera:
                        self.channelMediaOptions.publishCameraTrack = .of(!isSelected)
                        self.agoraKit?.updateChannelEx(with: self.channelMediaOptions,
                                                       connection: self.connection!)
                        
                        if var userModel = self.currentUserModel {
                            userModel.isEnableVideo = !isSelected
                            let params = JSONObject.toJson(userModel)
                            SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).update(id: userModel.objectId ?? "", data: params, success: {
                                
                            }, fail: { error in
                                ToastView.show(text: error.message)
                            })
                            self.currentUserModel = userModel
                        }
                    
                    case .mic:
                        self.channelMediaOptions.publishMicrophoneTrack = .of(!isSelected)
                        self.agoraKit?.updateChannelEx(with: self.channelMediaOptions,
                                                       connection: self.connection!)
                        
                        if var userModel = self.currentUserModel {
                            userModel.isEnableAudio = !isSelected
                            let params = JSONObject.toJson(userModel)
                            SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).update(id: userModel.objectId ?? "", data: params, success: {
                                
                            }, fail: { error in
                                ToastView.show(text: error.message)
                            })
                            self.currentUserModel = userModel
                        }
                    
                    default: break
                    }
                }
                AlertManager.show(view: self.liveToolView, alertPostion: .bottom)
            
            case .gift:
                self.giftView.onTapGiftItemClosure = { giftModel in
                    LogUtils.log(message: "gif == \(giftModel.gifName)", level: .info)
                    let params = JSONObject.toJson(giftModel)
                    /// 发送礼物
                    SyncUtil.scene(id: self.channelName)?.update(key: SYNC_MANAGER_GIFT_INFO, data: params, success: { objects in
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                }
                AlertManager.show(view: self.giftView, alertPostion: .bottom)
                
            case .game:
                break
                
            case .exitgame:
                break
                
            default: break
            }
        }
    }
    
    private func setupUI() {
        
        view.backgroundColor = .init(hex: "#0E141D")
        title = channelName
        backButton.setImage(UIImage(systemName: "chevron.backward")?
                                .withTintColor(.white, renderingMode: .alwaysOriginal),
                            for: .normal)
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: backButton)
        let rightButton = UIButton()
        rightButton.setImage(UIImage(systemName: "command")?
                                .withTintColor(.white, renderingMode: .alwaysOriginal), for: .normal)
        rightButton.addTarget(self, action: #selector(onTapRightButton), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: rightButton)
        
        
        view.addSubview(headerView)
        headerView.translatesAutoresizingMaskIntoConstraints = false
        headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        headerView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        headerHeightCons = headerView.heightAnchor.constraint(equalToConstant: 220.fit)
        headerHeightCons?.isActive = true
        
        view.addSubview(usersView)
        usersView.translatesAutoresizingMaskIntoConstraints = false
        usersView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        usersView.topAnchor.constraint(equalTo: view.topAnchor, constant: 240.fit).isActive = true
        usersView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        view.addSubview(playGifView)
        playGifView.translatesAutoresizingMaskIntoConstraints = false
        playGifView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        playGifView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        playGifView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        playGifView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        view.addSubview(bottomView)
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(chatView)
        chatView.translatesAutoresizingMaskIntoConstraints = false
        chatView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        chatView.topAnchor.constraint(equalTo: usersView.bottomAnchor, constant: 15).isActive = true
        chatView.bottomAnchor.constraint(equalTo: bottomView.topAnchor).isActive = true
        chatView.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.5).isActive = true
    
        bottomView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        bottomView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        bottomView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        bottomView.heightAnchor.constraint(equalToConstant: 50).isActive = true
    }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else {
            agoraKit?.delegate = self
            return
        }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setVideoEncoderConfiguration(
            AgoraVideoEncoderConfiguration(size: CGSize(width: 320, height: 240),
                                           frameRate: .fps30,
                                           bitrate: AgoraVideoBitrateStandard,
                                           orientationMode: .fixedPortrait,
                                           mirrorMode: .auto))
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func setupMediaPlayKit() {
        mediaPlayerKit = agoraKit?.createMediaPlayer(with: self)
        mediaPlayerKit?.setView(headerView.localVideoView)
        mediaPlayerKit?.setRenderMode(.hidden)
        
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.view = headerView.localVideoView
        videoCanvas.renderMode = .hidden
        videoCanvas.sourceType = .mediaPlayer
        videoCanvas.sourceId = mediaPlayerKit?.getMediaPlayerId() ?? 0
        agoraKit?.setupLocalVideo(videoCanvas)
        mediaPlayerKit?.open(videoUrl ?? "", startPos: 0)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            self.mediaPlayerKit?.play()            
        }
        
        let option = channelMediaOptions
        option.publishMediaPlayerId = .of((Int32)(mediaPlayerKit?.getMediaPlayerId() ?? 0))
        let connection = AgoraRtcConnection()
        connection.channelId = channelName
        connection.localUid = UserInfo.userId
        agoraKit?.updateChannelEx(with: option, connection: connection)
    }

    /// 加入channel
    private func joinChannel() {
        let result = agoraKit?.joinChannelEx(byToken: KeyCenter.Token,
                                             connection: connection!,
                                             delegate: self,
                                             mediaOptions: channelMediaOptions,
                                             joinSuccess: nil)
        
        if result == 0 {
            LogUtils.log(message: "加入房间成功", level: .info)
        }
        
        var messageModel = ChatMessageModel()
        messageModel.content = "\(UserInfo.uid) " + "Join_Live_Room".localized
        chatView.sendMessage(messageModel: messageModel)
        var userModel = AgoraUsersModel()
        if getRole(uid: UserInfo.uid) == .broadcaster {
            userModel.status = .accept
        } else {
            userModel.status = .end
        }
        let params = JSONObject.toJson(userModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).add(data: params, success: { object in
            let model = JSONObject.toModel(AgoraUsersModel.self, value: object.toJson())
            self.currentUserModel = model
            self.usersView.fetchAgoraVoiceUserInfoData()
            self.channelMediaOptions.publishMicrophoneTrack = .of(false)
            self.agoraKit?.updateChannelEx(with: self.channelMediaOptions, connection: self.connection!)
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func leaveChannel() {
        agoraKit?.leaveChannelEx(connection!, leaveChannelBlock: { state in
            LogUtils.log(message: "leave channel state == \(state)", level: .info)
        })
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).delete(id: currentUserModel?.objectId ?? "", success: { _ in 
            
        }, fail: { error in
            ToastView.show(text: error.message)
        })
        currentUserModel = nil
        _connection = nil
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }
    
    override var prefersStatusBarHidden: Bool {
        false
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        navigationController?.setNavigationBarHidden(size.width > size.height, animated: true)
        if size.width > size.height {
            headerHeightCons?.constant = Screen.width
        } else {
            headerHeightCons?.constant = 220.fit
        }
        headerHeightCons?.isActive = true
        usersView.isHidden = size.width > size.height
        chatView.isHidden = size.width > size.height
        bottomView.isHidden = size.width > size.height
    }
    
    override func onTapBackButton() {
        if headerView.fullButton.isSelected {
            let appdelegate = UIApplication.shared.delegate as? AppDelegate
            appdelegate?.blockRotation = .portrait
            headerView.fullButton.isSelected = false
            return
        }
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                SyncUtil.scene(id: self?.channelName ?? "")?.deleteScenes()
                self?.navigationController?.popViewController(animated: true)
            }
        } else {
            super.onTapBackButton()
        }
    }
    
    @objc
    private func onTapRightButton() {
        let alertView = ClubRoomListView(videoUrl: videoUrl ?? "")
        alertView.didClubItemClosure = { [weak self] roomInfo in
            guard let self = self, roomInfo.roomId != self.channelName else { return }
            self.leaveChannel()
            if self.getRole(uid: UserInfo.uid) == .broadcaster {
                SyncUtil.scene(id: self.channelName)?.delete(success: { object in
                    
                }, fail: { error in
                    ToastView.show(text: error.message)
                })
            }
            self.title = roomInfo.roomId
            self.channelName = roomInfo.roomId
            self.currentUserId = roomInfo.userId
            self.videoUrl = roomInfo.videoUrl
            self.liveToolView.updateStatus(type: .camera, isSelected: true)
            self.liveToolView.updateStatus(type: .mic, isSelected: true)
            self.bottomView.updateButtonType(type: [.gift, .close])
            let params = JSONObject.toJson(roomInfo)
            ToastView.show(text: "切换房间中...")
            SyncUtil.joinScene(id: roomInfo.roomId, userId: roomInfo.userId, property: params, success: { result in
                self.joinChannel()
                self.usersView.updateParams(channelName: roomInfo.roomId,
                                            role: self.getRole(uid: UserInfo.uid),
                                            agoraKit: self.agoraKit,
                                            mediaOptions: self.channelMediaOptions,
                                            connection: self.connection!)
                self.eventHandler()
                AlertManager.hiddenView()
            })
        }
        AlertManager.show(view: alertView, alertPostion: .bottom, didCoverDismiss: true)
    }
}

extension AgoraClubController: AgoraRtcEngineDelegate {
    
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

extension AgoraClubController: AgoraRtcMediaPlayerDelegate {
    func agoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedTo state: AgoraMediaPlayerState, error: AgoraMediaPlayerError) {
        LogUtils.log(message: "player rtc channel publish helper state changed to: \(state.rawValue), error: \(error.rawValue)", level: .info)
    }
    
    func agoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedToPosition position: Int) {
    
    }
}
