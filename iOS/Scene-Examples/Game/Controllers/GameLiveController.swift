//
//  DGLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit
import AgoraRtcKit
import ReplayKit
import AgoraScreenShare
import AgoraSyncManager

class GameLiveController: PKLiveController {
    private lazy var webView: GameWebView = {
        let view = GameWebView()
        view.isHidden = true
        return view
    }()
    
    private lazy var countTimeLabel: UIButton = {
        let button = UIButton()
        button.backgroundColor = UIColor(hex: "#000000", alpha: 0.6)
        button.setTitle("PK剩余 00:00", for: .normal)
        button.setTitleColor(.init(hex: "#999999"), for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 9)
        button.layer.cornerRadius = 8.5
        button.layer.masksToBounds = true
        button.isHidden = true
        return button
    }()

    public lazy var viewModel = GameViewModel(channleName: channleName,
                                              ownerId: currentUserId)
    
    private var isLoadScreenShare: Bool = false
    private lazy var screenConnection: AgoraRtcConnection = {
        let connection = AgoraRtcConnection()
        connection.localUid = screenUserID
        connection.channelId = channleName
        return connection
    }()
    private lazy var timer = GCDTimer()
    public var gameApplyInfoModel: GameApplyInfoModel?
    public var gameInfoModel: GameInfoModel?
    private var gameRoleType: GameRoleType {
        let role: GameRoleType = targetChannelName.isEmpty ? .audience : .broadcast
        let gameId = gameCenterModel?.gameId ?? gameApplyInfoModel?.gameId
        return gameId == .kingdom && getRole(uid: UserInfo.uid) == .broadcaster ? .broadcast : role
    }
    public var screenUserID: UInt {
        UserInfo.userId + 10000
    }
    
    private var gameCenterModel: GameCenterModel?
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    private func setupUI() {
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == "\(UserInfo.userId)" ? [.game, .tool, .close] : [.gift, .close]
        liveView.updateBottomButtonType(type: bottomType)
        
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)
        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: liveView.avatarview.bottomAnchor, constant: 15).isActive = true
        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: liveView.chatView.topAnchor, constant: -10).isActive = true
        
        countTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        pkProgressView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(countTimeLabel)
        countTimeLabel.centerXAnchor.constraint(equalTo: liveView.liveCanvasView.centerXAnchor).isActive = true
        countTimeLabel.topAnchor.constraint(equalTo: liveView.liveCanvasView.topAnchor).isActive = true
        countTimeLabel.widthAnchor.constraint(equalToConstant: 83).isActive = true
        
        pkProgressView.removeConstraints(pkProgressView.constraints)
        pkProgressView.removeFromSuperview()
        pkProgressView.reset()
        liveView.liveCanvasView.addSubview(pkProgressView)
        pkProgressView.leadingAnchor.constraint(equalTo: liveView.liveCanvasView.leadingAnchor).isActive = true
        pkProgressView.trailingAnchor.constraint(equalTo: liveView.liveCanvasView.trailingAnchor).isActive = true
        pkProgressView.bottomAnchor.constraint(equalTo: liveView.liveCanvasView.bottomAnchor).isActive = true
        pkProgressView.heightAnchor.constraint(equalToConstant: 20).isActive = true
    }
    
    override func eventHandler() {
        super.eventHandler()
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            // 监听游戏
            subscibeGameApplyInfo(channelName: channleName)
        }
        
        // 更新观众游戏状态
        SyncUtil.subscribe(id: channleName, key: SYNC_MANAGER_GAME_INFO) { object in
            guard let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
            self.gameInfoModel = model
        } onUpdated: { object in
            LogUtils.log(message: "onUpdated game info == \(String(describing: object.toJson()))", level: .info)
            guard self.getRole(uid: "\(UserInfo.userId)") == .audience,
                  let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
            self.gameInfoModel = model
            if model.status == .no_start {
                self.updatePKUIStatus(isStart: false)
            } else if model.status == .playing {
                self.updatePKUIStatus(isStart: true)
                self.view.layer.contents = model.gameId?.bgImage?.cgImage
            } else {
                self.updatePKUIStatus(isStart: false)
            }
            self.stopBroadcastButton.isHidden = true
        } onSubscribed: {
            LogUtils.log(message: "onSubscribed game info", level: .info)
        }

        liveView.onClickGameButtonClosure = { [weak self] in
            self?.clickGamePKHandler()
        }
        
        liveView.onClickExitGameButtonClosure = { [weak self] in
            self?.exitGameHandler()
        }
        /// 发消息
        liveView.onClickSendMessageClosure = { [weak self] meesageModel in
            let gameId = (self?.gameInfoModel?.gameId ?? self?.gameCenterModel?.gameId ?? self?.gameApplyInfoModel?.gameId)?.rawValue ?? ""
            self?.viewModel.postBarrage(gameId: gameId)
        }
        /// 发礼物
        liveView.onSendGiftClosure = { [weak self] giftModel in
            let gameId = (self?.gameInfoModel?.gameId ?? self?.gameCenterModel?.gameId ?? self?.gameApplyInfoModel?.gameId)?.rawValue ?? ""
            self?.viewModel.postGiftHandler(gameId: gameId, giftType: giftModel.giftType)
        }
    }
    
    private func subscibeGameApplyInfo(channelName: String) {
        SyncUtil.subscribe(id: channelName, key: SYNC_MANAGER_GAME_APPLY_INFO) { object in
            LogUtils.log(message: "onCreated applyGameInfo == \(String(describing: object.toJson()))", level: .info)
            self.gameApplySubscibeHandler(object: object)
        } onUpdated: { object in
            LogUtils.log(message: "onUpdated applyGameInfo == \(String(describing: object.toJson()))", level: .info)
            self.gameApplySubscibeHandler(object: object)
        } onSubscribed: {
            LogUtils.log(message: "onSubscribed applyGameInfo", level: .info)
        }
    }
    
    private func gameApplySubscibeHandler(object: IObject) {
        guard let model = JSONObject.toModel(GameApplyInfoModel.self, value: object.toJson()) else { return }
        gameApplyInfoModel = model
        var gameInfoModel = GameInfoModel()
        gameInfoModel.status = model.status
        gameInfoModel.gameUid = "\(screenUserID)"
        gameInfoModel.gameId = model.gameId
        
        if model.status == .no_start {
            updatePKUIStatus(isStart: false)
        } else if model.status == .playing {
            view.layer.contents = model.gameId.bgImage?.cgImage
            updatePKUIStatus(isStart: true)
            // 通知观众拉取屏幕流
            SyncUtil.update(id: channleName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfoModel))
            
        } else {
            updatePKUIStatus(isStart: false)
            // 更新观众状态
            SyncUtil.update(id: channleName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfoModel))
        }

        guard getRole(uid: "\(UserInfo.userId)") == .broadcaster else { return }
        stopBroadcastButton.isHidden = model.status != .end
    }
    
    // 游戏PK
    private func clickGamePKHandler() {
//        let modeView = GameModeView()
//        modeView.didGameModeItemClosure = { model in
//
//        }
//        AlertManager.show(view: modeView, alertPostion: .bottom)
        let gameCenterView = GameCenterView()
        gameCenterView.didGameCenterItemClosure = { [weak self] gameCenterModel in
            self?.gameCenterModel = gameCenterModel
            self?.inviteBroadcastHandler()
        }
        AlertManager.show(view: gameCenterView, alertPostion: .bottom)
    }
    
    private func inviteBroadcastHandler() {
        let pkInviteListView = PKLiveInviteView(channelName: channleName, sceneType: sceneType)
        pkInviteListView.pkInviteSubscribe = { [weak self] id in
            guard let self = self else { return }
            self.targetChannelName = id
            // 加入到对方的channel 订阅对方
            SyncUtil.subscribe(id: id, key: self.sceneType.rawValue, onUpdated: { object in
                self.pkSubscribeHandler(object: object)
            }, onSubscribed: {
                LogUtils.log(message: "onSubscribed pkApplyInfo", level: .info)
            })

            // 订阅对方收到的礼物
            self.liveView.subscribeGift(channelName: id, type: .target)
            
            // 订阅对方的游戏
            self.subscibeGameApplyInfo(channelName: id)

        }
        AlertManager.show(view: pkInviteListView, alertPostion: .bottom)
    }

    // 退出游戏
    private func exitGameHandler() {
        showAlert(title: "退出游戏", message: "退出游戏将会终止游戏PK", cancel: nil) { [weak self] in
            self?.updatePKUIStatus(isStart: false)
            self?.updateGameInfoStatus(isStart: false)
        }
//        self?.viewModel.postBarrage()
//        viewModel.postGiftHandler(type: .delay)
    }
    
    /// 获取pk状态
    override func getBroadcastPKStatus() {
        super.getBroadcastPKStatus()
        SyncUtil.fetch(id: channleName, key: SYNC_MANAGER_GAME_INFO, success: { result in
            self.gameInfoModel = JSONObject.toModel(GameInfoModel.self, value: result?.toJson())
            self.updatePKUIStatus(isStart: self.gameInfoModel?.status == .playing && self.pkInfoModel?.status == .accept)
            if self.gameInfoModel?.status != .playing && self.pkInfoModel?.status == .accept {
                self.liveView.updateLiveLayout(postion: .center)
            } else if self.gameInfoModel?.status == .playing && self.pkInfoModel?.status == .accept {
                self.liveView.updateLiveLayout(postion: .bottom)
            } else {
                self.liveView.updateLiveLayout(postion: .full)
            }
        })
    }
    
    /// pk开始
    override func pkLiveStartHandler() {
        super.pkLiveStartHandler()
        guard !targetChannelName.isEmpty else { return }
        updateGameInfoStatus(isStart: true)
    }
    
    /// pk结束
    override func pkLiveEndHandler() {
        super.pkLiveEndHandler()
        updatePKUIStatus(isStart: false)
        liveView.updateLiveLayout(postion: .full)
        if getRole(uid: UserInfo.uid) == .broadcaster {
            liveView.updateBottomButtonType(type: [.game, .tool, .close])
        }
    }

    /// 收到礼物
    override func receiveGiftHandler(giftModel: LiveGiftModel, type: RecelivedType) {
        super.receiveGiftHandler(giftModel: giftModel, type: type)
        liveView.playGifView.isHidden = getRole(uid: UserInfo.uid) == .broadcaster && pkInfoModel?.status == .accept
    }
    
    override func applicationWillTerminate() {
        super.applicationWillTerminate()
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        updateGameInfoStatus(isStart: false)
        AgoraScreenShare.shareInstance().stopService()
    }
    
    private func updatePKUIStatus(isStart: Bool) {
        vsImageView.isHidden = true
        countTimeLabel.isHidden = true//!isStart
        webView.isHidden = !isStart
        if currentUserId == UserInfo.uid && isStart {
            liveView.updateBottomButtonType(type: [.exitgame, .tool, .close])
        } else if currentUserId == UserInfo.uid && !isStart {
            liveView.updateBottomButtonType(type: [.game, .tool, .close])
        } else {
            liveView.updateBottomButtonType(type: [.gift, .close])
        }
        if isStart {
            ToastView.show(text: "游戏开始", postion: .top, duration: 3)
            if getRole(uid: UserInfo.uid) == .broadcaster {
                let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
                webView.loadUrl(gameId: (gameCenterModel?.gameId ?? gameApplyInfoModel?.gameId)?.rawValue ?? "",
                                roomId: channelName,
                                roleType: gameRoleType)
                // 调用屏幕共享
                onClickScreenShareButton()
                viewModel.channelName = channelName
            } else { // 观众拉取屏幕共享流
                guard let gameInfoModel = gameInfoModel else { return }
                if gameInfoModel.gameId == .kingdom {
                    self.liveView.sendMessage(message: "", messageType: .notice)
                }
                let canvas = AgoraRtcVideoCanvas()
                canvas.uid = UInt(gameInfoModel.gameUid ?? "0") ?? 0
                canvas.view = webView.webView
                canvas.renderMode = .fit
                screenConnection.localUid = UserInfo.userId
                joinScreenShare(isBroadcast: false)
                agoraKit?.setupRemoteVideoEx(canvas, connection: screenConnection)
            }
            
            liveView.updateLiveLayout(postion: .bottom)
            pkProgressView.isHidden = false
//            timer.scheduledSecondsTimer(withName: sceneType.rawValue, timeInterval: 900, queue: .main) { [weak self] _, duration in
//                self?.countTimeLabel.setTitle("PK剩余 \("".timeFormat(secounds: duration))", for: .normal)
//                if duration <= 0 {
//                    self?.updatePKUIStatus(isStart: false)
//                    self?.updateGameInfoStatus(isStart: false)
//                }
//            }
        } else {
            liveView.updateLiveLayout(postion: .center)
            pkProgressView.isHidden = true
            pkProgressView.reset()
            webView.reset()
            // 主播调用离开游戏接口
            if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
                let gameId = (gameInfoModel?.gameId ?? gameCenterModel?.gameId ?? gameApplyInfoModel?.gameId)?.rawValue ?? ""
                viewModel.leaveGame(gameId: gameId, roleType: gameRoleType)
                AgoraScreenShare.shareInstance().stopService()
                agoraKit?.leaveChannelEx(screenConnection, leaveChannelBlock: nil)
            }
            isLoadScreenShare = false
        }
    }
    
    private func joinScreenShare(isBroadcast: Bool) {
        // 屏幕共享辅频道
        let optionEx = AgoraRtcChannelMediaOptions()
        optionEx.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
        // 不订阅
        optionEx.autoSubscribeAudio = AgoraRtcBoolOptional.of(!isBroadcast)
        optionEx.autoSubscribeVideo = AgoraRtcBoolOptional.of(!isBroadcast)
        // 关闭辅频道麦克风(通过主频道开启即可)
        optionEx.publishAudioTrack = .of(false)
        optionEx.publishCustomVideoTrack = .of(isBroadcast)
        optionEx.publishCustomAudioTrack = .of(isBroadcast)
        let config = AgoraVideoEncoderConfiguration()
        config.frameRate = .fps15
        config.dimensions = CGSize(width: Screen.width, height: webView.frame.height)
        agoraKit?.setVideoEncoderConfigurationEx(config, connection: screenConnection)
        agoraKit?.joinChannelEx(byToken: KeyCenter.Token,
                                connection: screenConnection,
                                delegate: nil,
                                mediaOptions: optionEx,
                                joinSuccess: nil)

        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            // mute 辅频道流
            agoraKit?.muteRemoteAudioStream(screenUserID, mute: true)
            agoraKit?.muteRemoteVideoStream(screenUserID, mute: true)
        }
    }
    
    /// 屏幕共享
    private func onClickScreenShareButton() {
        guard let agoraKit = agoraKit, isLoadScreenShare == false else { return }
        isLoadScreenShare = true
        joinScreenShare(isBroadcast: true)
        AgoraScreenShare.shareInstance().startService(with: agoraKit, connection: screenConnection, regionRect: webView.frame)
        if #available(iOS 12.0, *) {
            let systemBroadcastPicker = RPSystemBroadcastPickerView(frame: .zero)
            systemBroadcastPicker.showsMicrophoneButton = false
            systemBroadcastPicker.autoresizingMask = [.flexibleTopMargin, .flexibleRightMargin]
            if let url = Bundle.main.url(forResource: "Agora-ScreenShare-Extension", withExtension: "appex", subdirectory: "PlugIns") {
                if let bundle = Bundle(url: url) {
                    systemBroadcastPicker.preferredExtension = bundle.bundleIdentifier
                }
            }
            let button = systemBroadcastPicker.subviews.first { view in
                view.isKind(of: UIButton.self)
            }
            if let button = button {
                (button as! UIButton).sendActions(for: .allTouchEvents)
            }
        } else {
            showAlert(message: "Minimum support iOS version is 12.0")
        }
    }
    
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
        if isStart {
            var gameApplyModel = GameApplyInfoModel()
            gameApplyModel.status = .playing
            gameApplyModel.gameId = gameCenterModel?.gameId ?? .guess
            SyncUtil.update(id: channelName, key: SYNC_MANAGER_GAME_APPLY_INFO, params: JSONObject.toJson(gameApplyModel))
            return
        }
        gameApplyInfoModel?.status = .end
        let params = JSONObject.toJson(gameApplyInfoModel)
        SyncUtil.update(id: channelName, key: SYNC_MANAGER_GAME_APPLY_INFO, params: params)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GAME_APPLY_INFO)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GAME_INFO)
        if !targetChannelName.isEmpty {
            SyncUtil.unsubscribe(id: targetChannelName, key: SYNC_MANAGER_GAME_APPLY_INFO)
        }
    }
    
    override func deleteSubscribe() {
        super.deleteSubscribe()
        if !targetChannelName.isEmpty {
            SyncUtil.unsubscribe(id: targetChannelName, key: SYNC_MANAGER_GAME_APPLY_INFO)
        }
    }
}
