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
        targetChannelName.isEmpty ? .audience : .broadcast
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
        view.insertSubview(webView, at: 0)
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
            SyncUtil.subscribe(id: channleName,
                               key: SYNC_MANAGER_GAME_APPLY_INFO,
                               delegate: GameApplyInfoDelegate(vc: self))
        }
        // 更新观众游戏状态
        SyncUtil.subscribe(id: channleName,
                           key: SYNC_MANAGER_GAME_INFO,
                           delegate: GameInfoDelegate(vc: self))
        
        liveView.onClickGameButtonClosure = { [weak self] in
            self?.clickGamePKHandler()
        }
        
        liveView.onClickExitGameButtonClosure = { [weak self] in
            self?.exitGameHandler()
        }
        
        /// 发消息
        liveView.onClickSendMessageClosure = { [weak self] _ in
            self?.viewModel.postBarrage()
        }
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
            SyncUtil.subscribe(id: id,
                               key: self.sceneType.rawValue,
                               delegate: PKInviteInfoDelegate(vc: self))
            
            // 订阅对方收到的礼物
            self.liveView.subscribeGift(channelName: id, type: .target)
            
            // 订阅对方的游戏
            SyncUtil.subscribe(id: id,
                               key: SYNC_MANAGER_GAME_APPLY_INFO,
                               delegate: GameApplyInfoDelegate(vc: self))
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
        let fetchGameInfoDelegate = FetchPKInfoDataDelegate()
        fetchGameInfoDelegate.onSuccess = { [weak self] result in
            self?.gameInfoModel = JSONObject.toModel(GameInfoModel.self, value: result?.toJson())
            self?.updatePKUIStatus(isStart: self?.gameInfoModel?.status == .playing && self?.pkInfoModel?.status == .accept)
            if self?.gameInfoModel?.status != .playing && self?.pkInfoModel?.status == .accept {
                self?.liveView.updateLiveLayout(postion: .center)
            } else if self?.gameInfoModel?.status == .playing && self?.pkInfoModel?.status == .accept {
                self?.liveView.updateLiveLayout(postion: .bottom)
            } else {
                self?.liveView.updateLiveLayout(postion: .full)
            }
        }
        SyncUtil.fetch(id: channleName, key: SYNC_MANAGER_GAME_INFO, delegate: fetchGameInfoDelegate)
    }
    
    /// pk开始
    override func pkLiveStartHandler() {
        super.pkLiveStartHandler()
        updateGameInfoStatus(isStart: true)
        viewModel.channelName = pkApplyInfoModel?.targetRoomId ?? ""
    }
    
    /// pk结束
    override func pkLiveEndHandler() {
        super.pkLiveEndHandler()
        liveView.updateLiveLayout(postion: .full)
    }

    /// 收到礼物
    override func receiveGiftHandler(giftModel: LiveGiftModel, type: PKLiveType) {
        super.receiveGiftHandler(giftModel: giftModel, type: type)
        liveView.playGifView.isHidden = getRole(uid: UserInfo.uid) == .broadcaster && pkApplyInfoModel?.status == .accept
        if type == .me {
            viewModel.postGiftHandler(type: giftModel.giftType)
        }
    }
    
    override func applicationWillTerminate() {
        super.applicationWillTerminate()
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        updateGameInfoStatus(isStart: false)
        AgoraScreenShare.shareInstance().stopService()
    }
    
    override func updatePKUIStatus(isStart: Bool) {
        vsImageView.isHidden = true
        countTimeLabel.isHidden = true//!isStart
        pkProgressView.isHidden = !isStart
        webView.isHidden = !isStart
        if currentUserId == "\(UserInfo.userId)" && isStart {
            liveView.updateBottomButtonType(type: [.exitgame, .tool, .close])
        } else if currentUserId == "\(UserInfo.userId)" && !isStart {
            liveView.updateBottomButtonType(type: [.game, .tool, .close])
        } else {
            liveView.updateBottomButtonType(type: [.gift, .close])
        }
        if isStart {
            ToastView.show(text: "游戏开始", postion: .top, duration: 3)
            if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
                let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
                webView.loadUrl(urlString: gameCenterModel?.type.gameUrl ?? "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html",
                                roomId: channelName,
                                roleType: gameRoleType)
                // 调用屏幕共享
                onClickScreenShareButton()
            } else { // 观众拉取屏幕共享流
                guard gameInfoModel != nil else { return }
                let canvas = AgoraRtcVideoCanvas()
                canvas.uid = UInt(gameInfoModel?.gameUid ?? "0") ?? 0
                canvas.view = webView.webView
                canvas.renderMode = .fit
                screenConnection.localUid = UserInfo.userId
                joinScreenShare(isBroadcast: false)
                pkProgressView.isHidden = gameInfoModel == nil
                agoraKit?.setupRemoteVideoEx(canvas, connection: screenConnection)
            }
            
            liveView.updateLiveLayout(postion: .bottom)
//            timer.scheduledSecondsTimer(withName: sceneType.rawValue, timeInterval: 900, queue: .main) { [weak self] _, duration in
//                self?.countTimeLabel.setTitle("PK剩余 \("".timeFormat(secounds: duration))", for: .normal)
//                if duration <= 0 {
//                    self?.updatePKUIStatus(isStart: false)
//                    self?.updateGameInfoStatus(isStart: false)
//                }
//            }
        } else {
            liveView.updateLiveLayout(postion: .center)
            pkProgressView.reset()
            webView.reset()
            // 主播调用离开游戏接口
            if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
                viewModel.leaveGame(roleType: gameRoleType)
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
            SyncUtil.update(id: channelName,
                            key: SYNC_MANAGER_GAME_APPLY_INFO,
                            params: JSONObject.toJson(gameApplyModel),
                            delegate: nil)
            return
        }
        gameApplyInfoModel?.status = .end
        let params = JSONObject.toJson(gameApplyInfoModel)
        SyncUtil.update(id: channelName,
                        key: SYNC_MANAGER_GAME_APPLY_INFO,
                        params: params,
                        delegate: nil)
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
