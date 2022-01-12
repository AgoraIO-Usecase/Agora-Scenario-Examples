//
//  OneToOneViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/21.
//

import UIKit
import AgoraRtcKit
import AgoraUIKit_iOS

class OneToOneViewController: BaseViewController {
    public lazy var localView = AGEView()
    public lazy var remoteView: AGEButton = {
        let button = AGEButton()
        button.setTitle("远程视频", for: .normal)
        button.layer.cornerRadius = 5
        button.shadowOffset = CGSize(width: 0, height: 0)
        button.shadowColor = .init(hex: "#000000")
        button.shadowRadius = 5
        button.shadowOpacity = 0.5
        button.buttonStyle = .filled(backgroundColor: .gray)
        return button
    }()
    private lazy var containerView: AGEView = {
        let view = AGEView()
        return view
    }()
    private lazy var onoToOneGameView: OnoToOneGameView = {
        let topControlView = OnoToOneGameView()
        return topControlView
    }()
    private lazy var controlView = OneToOneControlView()
       
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
        option.autoSubscribeVideo = .of(true)
        option.publishAudioTrack = .of(true)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        return option
    }()
    
    public lazy var viewModel = GameViewModel(channleName: channelName,
                                              ownerId: UserInfo.uid)
    
    private(set) var channelName: String = ""
    public var canvasDataArray = [LiveCanvasModel]()
    private(set) var sceneType: SceneType = .singleLive
    private var roleType: GameRoleType {
        currentUserId == UserInfo.uid ? .broadcast : .player
    }
    private var gameRoleType: GameRoleType {
        if gameInfoModel.gameId == .kingdom && roleType == .audience {
            return GameRoleType.allCases.randomElement() ?? .player
        }
        return roleType
    }
    private lazy var currentGameRoleType: GameRoleType = roleType
    
    private(set) var currentUserId: String = ""
    private var isCloseGame: Bool = false
    private var isSelfExitGame: Bool = false
    private var gameInfoModel = GameInfoModel()
    
    init(channelName: String, sceneType: SceneType, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channelName = channelName
        self.currentUserId = userId
        self.sceneType = sceneType
        self.agoraKit = agoraKit
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
        navigationTransparent(isTransparent: true)
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel(channelName: channelName)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
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
        leaveChannel()
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    private func eventHandler() {
        SyncUtil.subscribe(id: channelName, key: nil, onDeleted: { object in
            LogUtils.log(message: "删除房间 == \(object.toJson() ?? "")", level: .info)
            AGEToastView.show(text: "房间已关闭", duration: 2)
            self.navigationController?.popViewController(animated: true)
        })

        onoToOneGameView.onClickControlButtonClosure = { [weak self] type, isSelected in
            guard let self = self else { return }
            if type == .exit {
                self.controlView.isHidden = false
                self.viewModel.leaveGame(gameId: self.gameInfoModel.gameId?.rawValue ?? "",
                                         roleType: self.currentGameRoleType)
                self.isCloseGame = false
                let gameInfo = GameInfoModel(status: .end, gameUid: UserInfo.uid, gameId: self.gameInfoModel.gameId ?? .guess)
                SyncUtil.update(id: self.channelName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfo))
                self.isSelfExitGame = true
                return
            }
            self.clickControlViewHandler(controlType: type, isSelected: isSelected)
        }
        controlView.onClickControlButtonClosure = { [weak self] type, isSelected in
            self?.clickControlViewHandler(controlType: type, isSelected: isSelected)
        }
        /// 监听游戏开始
        SyncUtil.subscribe(id: channelName, key: SYNC_MANAGER_GAME_INFO, onUpdated: { [weak self] object in
            guard let self = self else { return }
            let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson())
            self.gameInfoModel.gameId = model?.gameId
            if model?.status == .playing {
                self.isSelfExitGame = false
                self.showAlert(title: "对方邀请您玩游戏", message: "") {
                    self.onoToOneGameView.setLoadUrl(gameId: model?.gameId?.rawValue ?? "",
                                                     roomId: self.channelName,
                                                     roleType: self.currentGameRoleType)
                    AlertManager.show(view: self.onoToOneGameView, alertPostion: .bottom, didCoverDismiss: false)
                }
            } else if model?.status == .end && !self.isSelfExitGame{
                AlertManager.hiddenView()
                ToastView.show(text: "游戏已结束", view: self.view)
                self.viewModel.leaveGame(gameId: model?.gameId?.rawValue ?? "",
                                         roleType: self.currentGameRoleType)
                self.onoToOneGameView.reset()
                self.isSelfExitGame = false
            }
        }, onSubscribed: {
            LogUtils.log(message: "onSubscribed One To One", level: .info)
        })
        
        onoToOneGameView.onLeaveGameClosure = { [weak self] in
            guard let self = self else { return }
            AlertManager.hiddenView()
            self.viewModel.leaveGame(gameId: self.gameInfoModel.gameId?.rawValue ?? "",
                                     roleType: self.currentGameRoleType)
            self.onoToOneGameView.reset()
            self.isSelfExitGame = false
        }
    }
    
    private func clickControlViewHandler(controlType: OneToOneControlType, isSelected: Bool) {
        switch controlType {
        case .switchCamera:
            agoraKit?.switchCamera()
            
        case .game:
            clickGameHandler()
        
        case .mic:
            agoraKit?.muteLocalAudioStream(isSelected)
            
        case .exit:
            showAlert(title: "退出游戏", message: "确定退出退出游戏 ？") {
                self.controlView.isHidden = false
                AlertManager.hiddenView()
                self.onoToOneGameView.reset()
                let gameId = self.gameInfoModel.gameId?.rawValue ?? ""
                self.viewModel.leaveGame(gameId: gameId, roleType: self.currentGameRoleType)
            }
            
        case .back:
            showAlert(title: "关闭直播间", message: "关闭直播间后，其他用户将不能再和您连线。确定关闭 ？") {
                self.viewModel.leaveGame(gameId: self.gameInfoModel.gameId?.rawValue ?? "",
                                         roleType: self.currentGameRoleType)
                let gameInfo = GameInfoModel(status: .end, gameUid: UserInfo.uid, gameId: self.gameInfoModel.gameId)
                SyncUtil.update(id: self.channelName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfo))
                if self.roleType == .broadcast {
//                    SyncUtil.delete(id: self.channelName)
                }
                self.navigationController?.popViewController(animated: true)
            }
            
        case .close:
            AlertManager.hiddenView()
            controlView.isHidden = false
            isCloseGame = true
        }
    }
    
    private func clickGameHandler() {
        isSelfExitGame = false
        if isCloseGame {
            AlertManager.show(view: onoToOneGameView, alertPostion: .bottom, didCoverDismiss: false)
            return
        }
        let gameCenterView = GameCenterView(sceneType: .oneToOne)
        gameCenterView.didGameCenterItemClosure = { [weak self] gameCenterModel in
            guard let self = self else { return }
            self.gameInfoModel.gameId = gameCenterModel.gameId
            self.onoToOneGameView.setLoadUrl(gameId: gameCenterModel.gameId.rawValue, roomId: self.channelName, roleType: self.currentGameRoleType)
            AlertManager.show(view: self.onoToOneGameView, alertPostion: .bottom, didCoverDismiss: false)
            let gameInfo = GameInfoModel(status: .playing, gameUid: UserInfo.uid, gameId: gameCenterModel.gameId)
            SyncUtil.update(id: self.channelName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfo))
        }
        AlertManager.show(view: gameCenterView, alertPostion: .bottom)
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        containerView.translatesAutoresizingMaskIntoConstraints = false
        localView.translatesAutoresizingMaskIntoConstraints = false
        remoteView.translatesAutoresizingMaskIntoConstraints = false
        controlView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(localView)
        view.addSubview(containerView)
        containerView.addSubview(remoteView)
        containerView.addSubview(controlView)
        
        containerView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        containerView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        containerView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        containerView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: Screen.safeAreaBottomHeight()).isActive = true
    
        localView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        localView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        localView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        localView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        remoteView.trailingAnchor.constraint(equalTo: localView.trailingAnchor, constant: -15).isActive = true
        remoteView.topAnchor.constraint(equalTo: localView.safeAreaLayoutGuide.topAnchor, constant: 15).isActive = true
        remoteView.widthAnchor.constraint(equalToConstant: 105.fit).isActive = true
        remoteView.heightAnchor.constraint(equalToConstant: 140.fit).isActive = true
        
        controlView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        controlView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        controlView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
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
    
    private func createAgoraVideoCanvas(uid: UInt, isLocal: Bool = false) {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.renderMode = .hidden
        if isLocal {
            canvas.view = remoteView
            agoraKit?.setupLocalVideo(canvas)
        } else {
            canvas.view = localView
            agoraKit?.setupRemoteVideo(canvas)
        }
        agoraKit?.startPreview()
    }
    
    public func joinChannel(channelName: String) {
        self.channelName = channelName
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           uid: UserInfo.userId,
                                           mediaOptions: channelMediaOptions)
        guard result != 0 else { return }
        // Error code description can be found at:
        // en: https://docs.agora.io/en/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        // cn: https://docs.agora.io/cn/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        self.showAlert(title: "Error", message: "joinChannel call failed: \(String(describing: result)), please check your params")
    }
    public func leaveChannel() {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "left channel, duration: \(state.duration)", level: .info)
        })
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}
extension OneToOneViewController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        LogUtils.log(message: "warning: \(warningCode.description)", level: .warning)
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        LogUtils.log(message: "error: \(errorCode)", level: .error)
        showAlert(title: "Error", message: "Error \(errorCode.description) occur")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "Join \(channel) with uid \(uid) elapsed \(elapsed)ms", level: .info)
        agoraKit?.enableLocalAudio(true)
        createAgoraVideoCanvas(uid: uid, isLocal: true)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "remote user join: \(uid) \(elapsed)ms", level: .info)
        createAgoraVideoCanvas(uid: uid, isLocal: false)
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
