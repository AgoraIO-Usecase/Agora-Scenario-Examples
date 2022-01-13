//
//  PlayTogetherViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/20.
//

import UIKit
import AgoraRtcKit

class PlayTogetherViewController: SignleLiveController {
    private lazy var webView: GameWebView = {
        let view = GameWebView()
        view.isHidden = true
        return view
    }()
    
    private var gameInfoModel: GameInfoModel?
    private var gameCenterModel: GameCenterModel?
    private lazy var viewModel = GameViewModel(channleName: channleName,
                                               ownerId: UserInfo.uid)
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
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        if getRole(uid: UserInfo.uid) == .audience {
            getBroadcastGameStatus()
        }
    }
    
    private func setupUI() {
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == "\(UserInfo.userId)" ? [.game, .gift, .tool, .close] : [.gift, .close]
        liveView.updateBottomButtonType(type: bottomType)
        
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)
        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: liveView.avatarview.bottomAnchor, constant: 15).isActive = true
        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: liveView.chatView.topAnchor, constant: -10).isActive = true
        
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
    
    override func eventHandler() {
        super.eventHandler()
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
            option.publishAudioTrack = .of(isMute)
            option.publishCameraTrack = .of(self.getRole(uid: UserInfo.uid) == .broadcaster)
            if self.getRole(uid: UserInfo.uid) == .audience {
                option.clientRoleType = isMute ? .of((Int32)(AgoraClientRole.broadcaster.rawValue)) : .of((Int32)(AgoraClientRole.audience.rawValue))
            }
            self.agoraKit?.updateChannel(with: option)
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
        webView.isHidden = !isStart
        if currentUserId == UserInfo.uid && isStart {
            liveView.updateBottomButtonType(type: [.exitgame, .gift, .tool, .close])
        } else if currentUserId == UserInfo.uid && !isStart {
            liveView.updateBottomButtonType(type: [.game, .gift, .tool, .close])
        } else if isStart {
            liveView.updateBottomButtonType(type: [.exitgame, .gift, .close])
        } else {
            liveView.updateBottomButtonType(type: [.gift, .close])
        }
        if isStart {
            ToastView.show(text: "游戏开始", postion: .top, duration: 3)
            webView.loadUrl(gameId: (gameCenterModel?.gameId ?? gameInfoModel?.gameId)?.rawValue ?? "",
                            roomId: channleName,
                            roleType: gameRoleType)
            
            liveView.updateLiveLayout(postion: .signle)
            
        } else {
            liveView.updateLiveLayout(postion: .full)
            let gameId = (gameInfoModel?.gameId ?? gameCenterModel?.gameId)?.rawValue ?? ""
            // 离开游戏接口
            viewModel.leaveGame(gameId: gameId, roleType: gameRoleType)
            webView.reset()
        }
    }
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        var gameInfoModel = GameInfoModel()
        gameInfoModel.gameId = gameCenterModel?.gameId ?? .guess
        gameInfoModel.gameUid = currentUserId
        gameInfoModel.status = isStart ? .playing : .end
        SyncUtil.update(id: channleName,
                        key: SYNC_MANAGER_GAME_INFO,
                        params: JSONObject.toJson(gameInfoModel))
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GAME_APPLY_INFO)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GAME_INFO)
    }
}
