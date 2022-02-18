//
//  PlayTogetherViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/20.
//

import UIKit
import AgoraRtcKit

class PlayTogetherViewController: BaseViewController {
    private lazy var liveView: LiveBaseView = {
        let view = LiveBaseView(channelName: channleName, currentUserId: currentUserId)
        return view
    }()
    private var agoraKit: AgoraRtcEngineKit?
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
    private lazy var webView: GameWebView = {
        let view = GameWebView()
        view.isHidden = true
        return view
    }()
    private lazy var gameView: UIView = {
        let view = UIView()
        view.isHidden = true
        return view
    }()
    
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    private(set) var sceneType: SceneType = .playTogether
    private var gameInfoModel: GameInfoModel?
    private var gameCenterModel: GameCenterModel?
    private lazy var viewModel = GameViewModel(channleName: channleName,
                                               ownerId: UserInfo.uid)
    private var fsmApp2MG: ISudFSTAPP?
    /// 用户角色
    private func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    private var _gameRoleType: GameRoleType?
    private var gameRoleType: GameRoleType {
        get {
            if _gameRoleType != nil {
                return _gameRoleType ?? .audience
            }
            let role: GameRoleType = getRole(uid: UserInfo.uid) == .broadcaster ? .broadcast : .audience
            _gameRoleType = role
            return role
        }
        set {
            _gameRoleType = newValue
        }
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
        getBroadcastGameStatus()
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
        SyncUtil.unsubscribe(id: channleName, key: sceneType.rawValue)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GAME_APPLY_INFO)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GIFT_INFO)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GAME_INFO)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        fsmApp2MG?.destroyMG()
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }

    private func setupUI() {
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == "\(UserInfo.userId)" ? [.game, .gift, .tool, .close] : [.gift, .close]
        liveView.updateBottomButtonType(type: bottomType)
        
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        liveView.translatesAutoresizingMaskIntoConstraints = false
    
        view.addSubview(liveView)
        
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        webView.translatesAutoresizingMaskIntoConstraints = false
        liveView.insertSubview(webView, belowSubview: liveView.playGifView)
        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: liveView.avatarview.bottomAnchor, constant: 15).isActive = true
        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: liveView.chatView.topAnchor, constant: -10).isActive = true
        
        gameView.translatesAutoresizingMaskIntoConstraints = false
        liveView.insertSubview(gameView, belowSubview: liveView.playGifView)
        gameView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        gameView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        gameView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        gameView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
    
    /// 加入channel
    private func joinChannel() {
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: UInt(currentUserId) ?? 0)
        let connection = LiveCanvasModel.createConnection(channelName: channleName, uid: UserInfo.userId)
        canvasModel.connection = connection
        canvasModel.channelName = channleName
        liveView.setupCanvasData(data: canvasModel)
        
        if getRole(uid: UserInfo.uid) == .broadcaster {
            channelMediaOptions.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
            channelMediaOptions.publishAudioTrack = .of(true)
            channelMediaOptions.publishCameraTrack = .of(true)
            channelMediaOptions.autoSubscribeVideo = .of(true)
            channelMediaOptions.autoSubscribeAudio = .of(true)
        }
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channleName,
                                           uid: UserInfo.userId,
                                           mediaOptions: channelMediaOptions,
                                           joinSuccess: nil)
        if result == 0 {
            LogUtils.log(message: "加入房间成功", level: .info)
        }
        liveView.sendMessage(message: "\(UserInfo.userId)加入房间", messageType: .message)
    }
    
    private func leaveChannel() {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel state == \(state)", level: .info)
        })
    }
    
    /// 获取主播游戏状态
    private func getBroadcastGameStatus() {
        SyncUtil.fetch(id: channleName, key: SYNC_MANAGER_GAME_INFO, success: { result in
            let gameInfoModel = JSONObject.toModel(GameInfoModel.self, value: result?.toJson())
            self.gameInfoModel = gameInfoModel
            if gameInfoModel?.status == .playing {
                self.updateUIStatus(isStart: true)
            }
        })
    }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(getRole(uid: UserInfo.uid))
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()

        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func eventHandler() {
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
            self?.agoraKit?.setupRemoteVideo(canvas)
        }
        /// 监听游戏
        if getRole(uid: UserInfo.uid) == .audience {
            SyncUtil.subscribe(id: channleName, key: SYNC_MANAGER_GAME_INFO, onUpdated: { [weak self] object in
                guard self?.getRole(uid: UserInfo.uid) == .audience,
                      let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
                self?.gameInfoModel = model
                LogUtils.log(message: "gameInfo == \(object.toJson() ?? "")", level: .info)
                if model.status == .playing {
                    self?.updateUIStatus(isStart: true)
                } else {
                    self?.updateUIStatus(isStart: false)
                }
            })
            SyncUtil.subscribe(id: channleName, key: nil, onDeleted: { _ in
                self.showAlert(title: "直播已结束", message: "") {
                    self.navigationController?.popViewController(animated: true)
                }
            })
        }
        liveView.onClickGameButtonClosure = { [weak self] in
            self?.clickGamePKHandler()
        }
        liveView.onClickExitGameButtonClosure = { [weak self] in
            self?.exitGameHandler()
        }
        liveView.onReceivedGiftClosure = { [weak self] giftModel, type in
            self?.receiveGiftHandler(giftModel: giftModel, type: type)
        }
        liveView.onClickSendMessageClosure = { [weak self] messageModel in
            self?.sendMessage(messageModel: messageModel)
        }
        
        webView.onMuteAudioClosure = { [weak self] isMute in
            guard let self = self else { return }
            let option = self.channelMediaOptions
            option.publishAudioTrack = self.getRole(uid: UserInfo.uid) == .broadcaster ? .of(true) : .of(isMute)
            option.publishCameraTrack = .of(self.getRole(uid: UserInfo.uid) == .broadcaster)
            if self.getRole(uid: UserInfo.uid) == .audience {
                option.clientRoleType = isMute ? .of((Int32)(AgoraClientRole.broadcaster.rawValue)) : .of((Int32)(AgoraClientRole.audience.rawValue))
            }
            self.agoraKit?.updateChannel(with: option)
            if self.getRole(uid: UserInfo.uid) == .audience {
                self.agoraKit?.enableLocalAudio(isMute)
            }
        }
        
        webView.onLeaveGameClosure = { [weak self] in
            self?.updateUIStatus(isStart: false)
        }
        
        webView.onChangeGameRoleClosure = { [weak self] oldRole, newRole in
            let gameId = self?.gameInfoModel?.gameId ?? self?.gameCenterModel?.gameId
            self?.viewModel.changeRole(gameId: gameId?.rawValue ?? "", oldRole: oldRole, newRole: newRole)
            self?.gameRoleType = newRole
        }
    }
    
    private func clickCloseLive() {
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                SyncUtil.delete(id: self?.channleName ?? "")
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            navigationController?.popViewController(animated: true)
        }
    }
    
    // 游戏
    private func clickGamePKHandler() {
        let gameCenterView = GameCenterView(sceneType: .playTogether)
        gameCenterView.didGameCenterItemClosure = { [weak self] gameCenterModel in
            self?.gameCenterModel = gameCenterModel
            self?.updateUIStatus(isStart: true)
            if self?.getRole(uid: UserInfo.uid) == .broadcaster {
                self?.updateGameInfoStatus(isStart: true)
            }
            AlertManager.hiddenView()
        }
        AlertManager.show(view: gameCenterView, alertPostion: .bottom)
    }
    // 退出游戏
    private func exitGameHandler() {
        showAlert(title: "退出游戏", message: "", cancel: nil) { [weak self] in
            self?.updateUIStatus(isStart: false)
            self?.updateGameInfoStatus(isStart: false)
        }
    }
    
    /// 收到礼物
    private func receiveGiftHandler(giftModel: LiveGiftModel, type: RecelivedType) {
        liveView.playGifView.isHidden = !webView.isHidden
        let playerId = gameInfoModel?.status == .playing ? UserInfo.uid : currentUserId
        viewModel.postGiftHandler(gameId:gameInfoModel?.gameId?.rawValue ?? "",
                                  giftType: giftModel.giftType,
                                  playerId: playerId)
    }
    /// 发消息
    private func sendMessage(messageModel: ChatMessageModel) {
        let playerId = gameInfoModel?.status == .playing ? UserInfo.uid : currentUserId
        viewModel.postBarrage(gameId: gameInfoModel?.gameId?.rawValue ?? "",
                              playerId: playerId)
        if getRole(uid: UserInfo.uid) == .audience
            && messageModel.message.trimmingCharacters(in: .whitespacesAndNewlines) == "主播yyds"
            && gameInfoModel?.status == .playing {
            updateUIStatus(isStart: true)
        }
    }
    
    override func applicationWillTerminate() {
        super.applicationWillTerminate()
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        updateGameInfoStatus(isStart: false)
    }
    
    private func updateUIStatus(isStart: Bool) {
        let sources = gameCenterModel?.sources ?? gameInfoModel?.sources
        webView.isHidden = sources == .sud || !isStart
        gameView.isHidden = sources == .yuanqi || !isStart
        
        if currentUserId == UserInfo.uid && isStart {
            liveView.updateBottomButtonType(type: [.exitgame, .gift, .tool, .close])
        } else if currentUserId == UserInfo.uid && !isStart {
            liveView.updateBottomButtonType(type: [.game, .gift, .tool, .close])
        } else {
            liveView.updateBottomButtonType(type: [.gift, .close])
        }
        if isStart {
            ToastView.show(text: "游戏开始", postion: .top, duration: 3, view: view)
            let gameId = Int64((gameCenterModel?.gameId ?? gameInfoModel?.gameId)?.rawValue ?? "") ?? 0
            if sources == .sud {
                fsmApp2MG = SudMGP.loadMG(UserInfo.uid, roomId: channleName, code: NetworkManager.shared.gameToken, mgId: gameId, language: "zh-CN", fsmMG: self, rootView: gameView)
            } else {
                webView.loadUrl(gameId: (gameCenterModel?.gameId ?? gameInfoModel?.gameId)?.rawValue ?? "",
                                roomId: channleName,
                                roleType: gameRoleType)
            }
            liveView.updateLiveLayout(postion: .signle)
            
        } else {
            liveView.updateLiveLayout(postion: .full)
            if sources == .yuanqi {
                let gameId = (gameInfoModel?.gameId ?? gameCenterModel?.gameId)?.rawValue ?? ""
                // 离开游戏接口
                viewModel.leaveGame(gameId: gameId, roleType: gameRoleType)
                webView.reset()
            } else {
                fsmApp2MG?.destroyMG()
            }
        }
    }
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        var gameInfoModel = GameInfoModel()
        gameInfoModel.gameId = gameCenterModel?.gameId ?? .guess
        gameInfoModel.gameUid = currentUserId
        gameInfoModel.status = isStart ? .playing : .end
        gameInfoModel.sources = gameCenterModel?.sources ?? .yuanqi
        SyncUtil.update(id: channleName,
                        key: SYNC_MANAGER_GAME_INFO,
                        params: JSONObject.toJson(gameInfoModel))
    }
}

extension PlayTogetherViewController: AgoraRtcEngineDelegate {
    
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

extension PlayTogetherViewController: ISudFSMMG {
    func onGameLog(_ dataJson: String) {
        LogUtils.log(message: "onGameLog == \(dataJson)", level: .info)
    }
    
    func onGameStarted() {
        LogUtils.log(message: "onGameStarted", level: .info)
    }
    
    func onGameDestroyed() {
        LogUtils.log(message: "onGameDestroyed", level: .info)
    }
    
    func onExpireCode(_ handle: ISudFSMStateHandle, dataJson: String) {
        LogUtils.log(message: "onExpireCode == \(dataJson)", level: .info)
    }
    
    func onGetGameViewInfo(_ handle: ISudFSMStateHandle, dataJson: String) {
        LogUtils.log(message: "onGetGameViewInfo == \(dataJson)", level: .info)
        let rect = UIScreen.main.bounds
        let scale = UIScreen.main.nativeScale
        let top = liveView.avatarview.frame.maxY
        let width = rect.width * scale
        let height = rect.height * scale
        let bottom = liveView.chatView.frame.minY + 30
        let rectDict = ["top": top, "left": 0, "bottom": bottom, "right": 0]
        let viewDict = ["width": width, "height": height]
        let dataDict = ["ret_code": 0, "ret_msg": "return form APP onGetGameViewInfo", "view_size": viewDict, "view_game_rect": rectDict] as [String : Any]
        handle.success(JSONObject.toJsonString(dict: dataDict) ?? "")
    }
    
    func onGetGameCfg(_ handle: ISudFSMStateHandle, dataJson: String) {
        LogUtils.log(message: "onGetGameCfg == \(dataJson)", level: .info)
    }
    
    func onGameStateChange(_ handle: ISudFSMStateHandle, state: String, dataJson: String) {
        LogUtils.log(message: "onGameStateChange == \(dataJson)", level: .info)
    }
    
    func onPlayerStateChange(_ handle: ISudFSMStateHandle?, userId: String, state: String, dataJson: String) {
        LogUtils.log(message: "onPlayerStateChange == \(dataJson)", level: .info)
    }
}
