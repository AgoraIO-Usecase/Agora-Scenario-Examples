//
//  AgoraClubController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/3/21.
//

import UIKit
import AgoraRtcKit
import AgoraUIKit_iOS

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
        option.publishAudioTrack = .of(false)
        option.publishCameraTrack = .of(false)
        option.clientRoleType = .of((Int32)(AgoraClientRole.audience.rawValue))
        return option
    }()
    private lazy var headerView: ClubHeaderView = {
        let view = ClubHeaderView()
        view.clickVideoViewClosure = { [weak self] isSelected in
            self?.navigationController?.setNavigationBarHidden(isSelected, animated: true)
        }
        return view
    }()
    private lazy var usersView: SeatView = {
        let view = SeatView(channelName: channelName,
                            role: getRole(uid: UserInfo.uid),
                            agoraKit: agoraKit,
                            mediaOptions: channelMediaOptions,
                            connection: connection)
        return view
    }()
    /// 聊天
    private lazy var chatView = LiveChatView()
    /// 底部功能
    private lazy var bottomView: LiveBottomView = {
        let view = LiveBottomView(type: [.gift, .tool, .close])
        return view
    }()
    /// 设置直播的工具弹窗
    private lazy var liveToolView = LiveToolView()
    /// 礼物
    private lazy var giftView = LiveGiftView()
    public lazy var playGifView: GIFImageView = {
        let view = GIFImageView()
        view.isHidden = true
        return view
    }()
    
    private var channelName: String = ""
    private var headerHeightCons: NSLayoutConstraint?
    private var currentUserModel: AgoraVoiceUsersModel?
    private var currentUserId: String = ""
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    private lazy var connection: AgoraRtcConnection = {
        let connection = AgoraRtcConnection()
        connection.channelId = channelName
        connection.localUid = UserInfo.userId
        return connection
    }()
    
    init(userId: String, channelName: String?, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channelName = channelName ?? ""
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
        
        subscribeGift(channelName: channelName)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false, isHiddenNavBar: false)
        let image = UIImage().color(.black, height: Screen.kNavHeight)
        navigationController?.navigationBar.isTranslucent = false
        navigationController?.navigationBar.setBackgroundImage(image, for: .any, barMetrics: .default)
        navigationController?.navigationBar.tintColor = .white
        navigationController?.navigationBar.barTintColor = .white
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.view.backgroundColor = view.backgroundColor
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel()
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is CreateLiveController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        agoraKit?.disableAudio()
        agoraKit?.muteAllRemoteAudioStreams(true)
        agoraKit?.destroyMediaPlayer(mediaPlayerKit)
        leaveChannel()
        SyncUtil.deleteDocument(id: channelName,
                                className: SYNC_MANAGER_AGORA_CLUB_USERS,
                                objectId: currentUserModel?.objectId ?? "")
        SyncUtil.leaveScene(id: channelName)
        if getRole(uid: UserInfo.uid) == .broadcaster {
            SyncUtil.delete(id: channelName)
        }
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    /// 监听礼物
    private func subscribeGift(channelName: String) {
        SyncUtil.subscribe(id: channelName, key: SYNC_MANAGER_GIFT_INFO, onUpdated: { object in
            LogUtils.log(message: "onUpdated gift == \(String(describing: object.toJson()))", level: .info)
            guard let model = JSONObject.toModel(LiveGiftModel.self, value: object.toJson()) else { return }
            self.playGifView.isHidden = false
            self.playGifView.loadGIFName(gifName: model.gifName)
            let messageModel = ChatMessageModel(message: model.userId + "i_gave_one_away".localized + model.title, messageType: .message)
            self.chatView.sendMessage(messageModel: messageModel)
        }, onSubscribed: {
            LogUtils.log(message: "subscribe gift", level: .info)
        })
    }
    
    private func eventHandler() {
        SyncUtil.subscribe(id: channelName, key: nil, onDeleted: { object in
            LogUtils.log(message: "删除房间 == \(object.toJson() ?? "")", level: .info)
            AGEToastView.show(text: "room_is_closed".localized, duration: 2)
            self.navigationController?.popViewController(animated: true)
        })
        
        // gif播放完成回调
        playGifView.gifAnimationFinishedClosure = { [weak self] in
            guard let self = self else { return }
            self.playGifView.isHidden = true
        }
        // 聊天发送
        bottomView.clickChatButtonClosure = { [weak self] message in
            guard let self = self else { return }
            let model = ChatMessageModel(message: message, messageType: .message)
            self.chatView.sendMessage(messageModel: model)
        }
        // 点击聊天消息
        chatView.didSelectRowAt = { [weak self] messageModel in
            self?.chatView.sendMessage(messageModel: messageModel)
        }
        // 底部功能回调
        bottomView.clickBottomButtonTypeClosure = { [weak self] type in
            guard let self = self else { return }
            switch type {
            case .close:
                self.clickBackButton()
                
            case .tool:
                self.liveToolView.clickItemClosure = { itemType, isSelected in
                    switch itemType {
                    case .switch_camera:
                        self.agoraKit?.switchCamera()
                        
                    case .camera:
                        self.agoraKit?.muteLocalVideoStream(isSelected)
                    
                    case .mic:
                        self.agoraKit?.muteLocalAudioStream(isSelected)
                    
                    default: break
                    }
                }
                AlertManager.show(view: self.liveToolView, alertPostion: .bottom)
            
            case .gift:
                self.giftView.clickGiftItemClosure = { giftModel in
                    LogUtils.log(message: "gif == \(giftModel.gifName)", level: .info)
                    let params = JSONObject.toJson(giftModel)
                    /// 发送礼物
                    SyncUtil.update(id: self.channelName, key: SYNC_MANAGER_GIFT_INFO, params: params)
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
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
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
        mediaPlayerKit?.open("https://webdemo.agora.io/agora-web-showcase/examples/Agora-Custom-VideoSource-Web/assets/sample.mp4", startPos: 0)
        mediaPlayerKit?.play()
        
        let option = channelMediaOptions
        option.publishMediaPlayerId = .of((Int32)(mediaPlayerKit?.getMediaPlayerId() ?? 0))
        let connection = AgoraRtcConnection()
        connection.channelId = channelName
        connection.localUid = UserInfo.userId
        agoraKit?.updateChannelEx(with: option, connection: connection)
    }

    /// 加入channel
    private func joinChannel() {
        channelMediaOptions.clientRoleType = .of((Int32)(getRole(uid: UserInfo.uid).rawValue))
        channelMediaOptions.publishAudioTrack = .of(getRole(uid: UserInfo.uid) == .broadcaster)
        channelMediaOptions.autoSubscribeAudio = .of(true)
        
        let result = agoraKit?.joinChannelEx(byToken: KeyCenter.Token,
                                             connection: connection,
                                             delegate: self,
                                             mediaOptions: channelMediaOptions,
                                             joinSuccess: nil)
        
        if result == 0 {
            LogUtils.log(message: "加入房间成功", level: .info)
        }
        agoraKit?.setClientRole(.audience)
        
        var messageModel = ChatMessageModel()
        messageModel.message = "\(UserInfo.uid) " + "Join_Live_Room".localized
        chatView.sendMessage(messageModel: messageModel)
        let params = JSONObject.toJson(AgoraVoiceUsersModel())
        SyncUtil.addCollection(id: channelName, className: SYNC_MANAGER_AGORA_CLUB_USERS, params: params, success: { object in
            let model = JSONObject.toModel(AgoraVoiceUsersModel.self, value: object.toJson())
            self.currentUserModel = model
            self.usersView.reloadData()
        })
    }
    
    private func leaveChannel() {
        agoraKit?.leaveChannelEx(connection, leaveChannelBlock: { state in
            LogUtils.log(message: "leave channel state == \(state)", level: .info)
        })
        SyncUtil.deleteDocument(id: channelName,
                                className: SYNC_MANAGER_AGORA_CLUB_USERS,
                                objectId: currentUserModel?.objectId ?? "")
    }
    
    private func muteAudioHandler(isVoice: Bool) {
        let option = channelMediaOptions
        option.publishAudioTrack = getRole(uid: UserInfo.uid) == .broadcaster ? .of(true) : .of(isVoice)
        option.publishCameraTrack = .of(getRole(uid: UserInfo.uid) == .broadcaster)
        if getRole(uid: UserInfo.uid) == .audience {
            option.clientRoleType = isVoice ? .of((Int32)(AgoraClientRole.broadcaster.rawValue)) : .of((Int32)(AgoraClientRole.audience.rawValue))
        }
        bottomView.updateButtonType(type: isVoice ? [.belcanto, .effect, .close] : [.close])
        agoraKit?.updateChannel(with: option)
        if getRole(uid: UserInfo.uid) == .audience {
            agoraKit?.enableLocalAudio(isVoice)
        }
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
    
    override func clickBackButton() {
        if headerView.fullButton.isSelected {
            let appdelegate = UIApplication.shared.delegate as? AppDelegate
            appdelegate?.blockRotation = .portrait
            headerView.fullButton.isSelected = false
            return
        }
        super.clickBackButton()
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

extension AgoraClubController: AgoraRtcMediaPlayerDelegate {
    func agoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedTo state: AgoraMediaPlayerState, error: AgoraMediaPlayerError) {
        LogUtils.log(message: "player rtc channel publish helper state changed to: \(state.rawValue), error: \(error.rawValue)", level: .info)
    }
    
    func agoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedToPosition position: Int) {
    
    }
}
