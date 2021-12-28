//
//  PlayTogetherViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/20.
//

import UIKit

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
    private var gameRoleType: GameRoleType {
        getRole(uid: UserInfo.uid) == .audience ? .broadcast : .audience
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
                self.sendMessage(messageModel: ChatMessageModel(message: "", messageType: .notice))
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
                LogUtils.log(message: "gameInfo == \(object.toJson() ?? "")", level: .info)
                if model.status == .playing {
                    self?.liveView.sendMessage(message: "", messageType: .notice)
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
    }
    
    // 游戏
    private func clickGamePKHandler() {
//        let modeView = GameModeView()
//        modeView.didGameModeItemClosure = { model in
//
//        }
//        AlertManager.show(view: modeView, alertPostion: .bottom)
        let gameCenterView = GameCenterView()
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
//        self?.viewModel.postBarrage()
//        viewModel.postGiftHandler(type: .delay)
    }
    
    /// 收到礼物
    private func receiveGiftHandler(giftModel: LiveGiftModel, type: RecelivedType) {
        liveView.playGifView.isHidden = !webView.isHidden
        viewModel.postGiftHandler(type: giftModel.giftType)
    }
    /// 发消息
    private func sendMessage(messageModel: ChatMessageModel) {
        viewModel.postBarrage()
        if getRole(uid: UserInfo.uid) == .audience && messageModel.message == "主播yyds" && gameInfoModel?.status == .playing {
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
            webView.loadUrl(urlString: gameCenterModel?.type.gameUrl ?? "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html",
                            roomId: channleName,
                            roleType: gameRoleType)
            
            liveView.updateLiveLayout(postion: .signle)
            
        } else {
            liveView.updateLiveLayout(postion: .full)
            webView.reset()
            // 离开游戏接口
            viewModel.leaveGame(roleType: gameRoleType)
        }
    }
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        var gameInfoModel = GameInfoModel()
        gameInfoModel.gameId = .you_draw_i_guess
        gameInfoModel.gameUid = currentUserId
        gameInfoModel.status = isStart ? .playing : .no_start
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
