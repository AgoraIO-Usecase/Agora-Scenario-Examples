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
    private var _gameRoleType: GameRoleType?
    private var gameRoleType: GameRoleType {
        get {
            if _gameRoleType != nil {
                return _gameRoleType ?? .player
            }
            let channelName = pkApplyInfoModel?.targetRoomId ?? channleName
            let role: GameRoleType = channelName == self.channleName ? .player : .broadcast
            _gameRoleType = role
            return role
        }
        set {
            _gameRoleType = newValue
        }
    }
    private var gameId: String {
        let gameId = gameInfoModel?.gameId ?? gameCenterModel?.gameId ?? gameApplyInfoModel?.gameId
        return gameId ?? ""
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
        liveView.insertSubview(webView, belowSubview: liveView.playGifView)
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
                self.updatePKUIStatus(isStart: false, isAudience: true)
            } else if model.status == .playing {
                self.updatePKUIStatus(isStart: true, isAudience: true)
//                self.view.layer.contents = model.gameId?.bgImage?.cgImage
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
        liveView.onClickSendMessageClosure = { [weak self] mesageModel in
            guard let self = self else { return }
            if mesageModel.message.trimmingCharacters(in: .whitespacesAndNewlines) == "主播yyds"
                && self.gameInfoModel?.status == .playing
                && self.getRole(uid: UserInfo.uid) == .audience {
                self.updatePKUIStatus(isStart: true, isAudience: true)
            }
            self.viewModel.postBarrage(gameId: self.gameId)
        }
        /// 发礼物
        liveView.onSendGiftClosure = { [weak self] giftModel in
            self?.viewModel.postGiftHandler(gameId: self?.gameId ?? "", giftType: giftModel.giftType)
        }
        
        webView.onMuteAudioClosure = { [weak self] isMute in
            guard let self = self else { return }
            let option = self.channelMediaOptions
            option.publishAudioTrack = .of(isMute)
            option.publishCameraTrack = .of(self.getRole(uid: UserInfo.uid) == .broadcaster)
            if self.getRole(uid: UserInfo.uid) == .audience {
                option.clientRoleType = isMute ? .of((Int32)(AgoraClientRole.broadcaster.rawValue)) : .of((Int32)(AgoraClientRole.audience.rawValue))
            }
            self.agoraKit?.updateChannel(with: option)
        }
        
        webView.onLeaveGameClosure = { [weak self] in
            self?.updatePKUIStatus(isStart: false)
        }
        
        webView.onChangeGameRoleClosure = { [weak self] oldRole, newRole in
            let gameId = self?.gameInfoModel?.gameId ?? self?.gameCenterModel?.gameId
            self?.viewModel.changeRole(gameId: gameId ?? "", oldRole: oldRole, newRole: newRole)
            self?.gameRoleType = newRole
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
        gameInfoModel.roomId = pkApplyInfoModel?.targetRoomId ?? channleName
        self.gameInfoModel = gameInfoModel
        
        if model.status == .no_start {
            updatePKUIStatus(isStart: false)
        } else if model.status == .playing {
//            view.layer.contents = model.gameId.bgImage?.cgImage
            updatePKUIStatus(isStart: true)
            // 通知观众拉取屏幕流
            SyncUtil.update(id: channleName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfoModel))
            
        } else {
            updatePKUIStatus(isStart: false)
            // 更新观众状态
            SyncUtil.update(id: channleName, key: SYNC_MANAGER_GAME_INFO, params: JSONObject.toJson(gameInfoModel))
        }

        guard getRole(uid: "\(UserInfo.userId)") == .broadcaster else { return }
        stopBroadcastButton.isHidden = pkApplyInfoModel?.status == .end || model.status != .end
    }
    
    // 游戏PK
    private func clickGamePKHandler() {
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
        showAlert(title: "quit_the_game".localized,
                  message: "quitting_game_will_terminate_game_PK".localized,
                  cancel: nil) { [weak self] in
            guard let self = self else { return }
            self.updatePKUIStatus(isStart: false)
            self.updateGameInfoStatus(isStart: false)
        }
    }
    
    /// 获取pk状态
    override func getBroadcastPKStatus() {
        super.getBroadcastPKStatus()
        SyncUtil.fetch(id: channleName, key: SYNC_MANAGER_GAME_INFO, success: { result in
            self.gameInfoModel = JSONObject.toModel(GameInfoModel.self, value: result?.toJson())
            self.updatePKUIStatus(isStart: self.gameInfoModel?.status == .playing && self.pkInfoModel?.status == .accept,
                                  isAudience: true)
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
        guard pkApplyInfoModel?.targetRoomId != channleName else { return }
        updateGameInfoStatus(isStart: true)
    }
    
    /// pk结束
    override func pkLiveEndHandler() {
        super.pkLiveEndHandler()
        updatePKUIStatus(isStart: false)
        updateGameInfoStatus(isStart: false)
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
    }
    
    override func hiddenPkProgressView(isHidden: Bool) { }
    
    private func updatePKUIStatus(isStart: Bool, isAudience: Bool = false) {
        webView.isHidden = !isStart
        if currentUserId == UserInfo.uid && isStart {
            liveView.updateBottomButtonType(type: [.exitgame, .tool, .close])
        } else if currentUserId == UserInfo.uid && !isStart {
            liveView.updateBottomButtonType(type: [.game, .tool, .close])
        } else {
            liveView.updateBottomButtonType(type: [.gift, .close])
        }
        if isStart {
            ToastView.show(text: "game_start".localized, postion: .top, duration: 3)
            let channelName = isAudience ? gameInfoModel?.roomId : pkApplyInfoModel?.targetRoomId
            webView.loadUrl(gameId: gameId,
                            roomId: channelName ?? "",
                            toUser: currentUserId,
                            roleType: isAudience ? .audience : gameRoleType)
            
            viewModel.channelName = channelName ?? ""
            
            liveView.updateLiveLayout(postion: .bottom)
            pkProgressView.isHidden = true
//            timer.scheduledSecondsTimer(withName: sceneType.rawValue, timeInterval: 900, queue: .main) { [weak self] _, duration in
//                self?.countTimeLabel.setTitle("PK剩余 \("".timeFormat(secounds: duration))", for: .normal)
//                if duration <= 0 {
//                    self?.updatePKUIStatus(isStart: false)
//                    self?.updateGameInfoStatus(isStart: false)
//                }
//            }
        } else {
            liveView.updateLiveLayout(postion: .center)
            if pkApplyInfoModel?.status == .end || pkInfoModel?.status == .end {
                liveView.updateLiveLayout(postion: .full)
            }
            pkProgressView.isHidden = true
            viewModel.leaveGame(gameId: gameId, roleType: gameRoleType)
            pkProgressView.reset()
            webView.reset()
            isLoadScreenShare = false
            _gameRoleType = nil
        }
    }
    
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        let channelName = pkApplyInfoModel?.targetRoomId ?? channleName
        if isStart {
            var gameApplyModel = GameApplyInfoModel()
            gameApplyModel.status = .playing
            gameApplyModel.gameId = gameCenterModel?.gameId ?? ""
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
        if pkApplyInfoModel?.targetRoomId != channleName {
            SyncUtil.unsubscribe(id: pkApplyInfoModel?.targetRoomId ?? "",
                                 key: SYNC_MANAGER_GAME_APPLY_INFO)
        }
    }
    
    override func deleteSubscribe() {
        super.deleteSubscribe()
        if pkApplyInfoModel?.targetRoomId != channleName {
            SyncUtil.unsubscribe(id: pkApplyInfoModel?.targetRoomId ?? "",
                                 key: SYNC_MANAGER_GAME_APPLY_INFO)
        }
    }
}
