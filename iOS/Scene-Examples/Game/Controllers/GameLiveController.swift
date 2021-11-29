//
//  DGLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

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
    
    private lazy var timer = GCDTimer()
    public var targetChannelName: String = ""
    private var pkApplyInfoModel: PKApplyInfoModel?
    public var gameApplyInfoModel: GameApplyInfoModel?
    public var gameInfoModel: GameInfoModel?
    
    private var gameCenterModel: GameCenterModel?
    override func viewDidLoad() {
        super.viewDidLoad()

        setupUI()
    }
    
    private func setupUI() {
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == "\(UserInfo.userId)" ? [.game, .tool, .close] : [.gift, .close]
        bottomView.updateButtonType(type: bottomType)
        
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.insertSubview(webView, at: 0)
        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        webView.topAnchor.constraint(equalTo: avatarview.bottomAnchor, constant: 15).isActive = true
        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        webView.bottomAnchor.constraint(equalTo: chatView.topAnchor, constant: -10).isActive = true
        
//        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
//        webView.topAnchor.constraint(equalTo: view.topAnchor, constant: -Screen.kNavHeight).isActive = true
//        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
//        webView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        countTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        pkProgressView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(countTimeLabel)
        countTimeLabel.centerXAnchor.constraint(equalTo: liveCanvasView.centerXAnchor).isActive = true
        countTimeLabel.bottomAnchor.constraint(equalTo: liveCanvasView.topAnchor, constant: -6).isActive = true
        countTimeLabel.widthAnchor.constraint(equalToConstant: 83).isActive = true
        
        pkProgressView.removeConstraints(pkProgressView.constraints)
        pkProgressView.removeFromSuperview()
        pkProgressView.reset()
        liveCanvasView.addSubview(pkProgressView)
        pkProgressView.leadingAnchor.constraint(equalTo: liveCanvasView.leadingAnchor).isActive = true
        pkProgressView.trailingAnchor.constraint(equalTo: liveCanvasView.trailingAnchor).isActive = true
        pkProgressView.bottomAnchor.constraint(equalTo: liveCanvasView.bottomAnchor).isActive = true
        pkProgressView.heightAnchor.constraint(equalToConstant: 20).isActive = true
    }
    
    // 游戏PK
    override func clickGamePKHandler() {
        updatePKUIStatus(isStart: true)
        return
        let modeView = GameModeView()
        modeView.didGameModeItemClosure = { model in
            let gameCenterView = GameCenterView()
            gameCenterView.didGameCenterItemClosure = { [weak self] gameCenterModel in
                self?.gameCenterModel = gameCenterModel
                self?.inviteBroadcastHandler()
            }
            AlertManager.show(view: gameCenterView, alertPostion: .bottom)
        }
        AlertManager.show(view: modeView, alertPostion: .bottom)
    }
    
    private func inviteBroadcastHandler() {
        let pkInviteListView = PKLiveInviteView(channelName: channleName, sceneType: sceneType)
        pkInviteListView.pkInviteSubscribe = { [weak self] id in
            guard let self = self else { return }
            self.targetChannelName = id
            // 加入到对方的channel 订阅对方
            SyncUtil.subscribeCollection(id: id,
                                         className: self.sceneType.rawValue,
                                         delegate: PKInviteInfoDelegate(vc: self))
            
            // 订阅对方收到的礼物
            SyncUtil.subscribeCollection(id: id,
                                         className: SYNC_MANAGER_GIFT_INFO,
                                         delegate: LiveGiftDelegate(vc: self, type: .target))
            // 订阅对方的游戏
            SyncUtil.subscribeCollection(id: id,
                                         className: SYNC_MANAGER_GAME_APPLY_INFO,
                                         delegate: GameApplyInfoDelegate(vc: self))
        }
        AlertManager.show(view: pkInviteListView, alertPostion: .bottom)
    }
    
    
    // 退出游戏
    override func exitGameHandler() {
        showAlert(title: "退出游戏", message: "退出游戏将会终止游戏PK", cancel: nil) { [weak self] in
            self?.updatePKUIStatus(isStart: false)
            self?.updateGameInfoStatus(isStart: false)
        }
//        self?.viewModel.postBarrage()
//        viewModel.postGiftHandler(type: .delay)
    }

    override func eventHandler() {
        super.eventHandler()
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            // 监听游戏
            SyncUtil.subscribeCollection(id: channleName,
                                         className: SYNC_MANAGER_GAME_APPLY_INFO,
                                         delegate: GameApplyInfoDelegate(vc: self))
        }
        
        // 更新观众游戏状态
        SyncUtil.subscribeCollection(id: channleName,
                                     className: SYNC_MANAGER_GAME_INFO,
                                     delegate: GameInfoDelegate(vc: self))
    }
    
    /// pk开始
    override func pkLiveStartHandler() {
        super.pkLiveStartHandler()
        updateGameInfoStatus(isStart: true)
    }
    
    /// pk结束
    override func pkLiveEndHandler() {
        super.pkLiveEndHandler()
        updateLiveLayout(postion: .full)
    }
    
    /// 收到礼物
    override func receiveGiftHandler(giftModel: LiveGiftModel, type: PKLiveType) {
        super.receiveGiftHandler(giftModel: giftModel, type: type)
        if type == .me {
            viewModel.postGiftHandler(type: giftModel.giftType)
        }
    }
    
    override func updatePKUIStatus(isStart: Bool) {
        vsImageView.isHidden = true
        countTimeLabel.isHidden = !isStart
        pkProgressView.isHidden = !isStart
        webView.isHidden = !isStart
        if currentUserId == "\(UserInfo.userId)" && isStart {
            bottomView.updateButtonType(type: [.exitgame, .tool, .close])
        } else if currentUserId == "\(UserInfo.userId)" && !isStart {
            bottomView.updateButtonType(type: [.game, .tool, .close])
        } else {
            bottomView.updateButtonType(type: [.gift, .close])
        }
        if isStart {
            ToastView.show(text: "游戏开始", postion: .top, duration: 3)
            if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
                let roleType: GameRoleType = targetChannelName.isEmpty ? .audience : .broadcast
                let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
                webView.loadUrl(urlString: gameCenterModel?.type.gameUrl ?? "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html",
                                roomId: channelName,
                                roleType: roleType)
            } else { // 观众拉取屏幕共享流
                
            }
            
            updateLiveLayout(postion: .bottom)
            timer.scheduledSecondsTimer(withName: sceneType.rawValue, timeInterval: 900, queue: .main) { [weak self] _, duration in
                self?.countTimeLabel.setTitle("PK剩余 \("".timeFormat(secounds: duration))", for: .normal)
                if duration <= 0 {
                    self?.updatePKUIStatus(isStart: false)
                }
            }
        } else {
            updateLiveLayout(postion: .center)
            pkProgressView.reset()
            let roleType: GameRoleType = targetChannelName.isEmpty ? .audience : .broadcast
            viewModel.leaveGame(roleType: roleType)
            webView.reset()
        }
    }
    
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
        if isStart {
            var gameApplyModel = GameApplyInfoModel()
            gameApplyModel.status = .playing
            SyncUtil.addCollection(id: channelName,
                                   className: SYNC_MANAGER_GAME_APPLY_INFO,
                                   params: JSONObject.toJson(gameApplyModel),
                                   delegate: nil)
            return
        }
        gameApplyInfoModel?.status = .end
        let params = JSONObject.toJson(gameApplyInfoModel)
        SyncUtil.updateCollection(id: channelName,
                                  className: SYNC_MANAGER_GAME_APPLY_INFO,
                                  objectId: gameApplyInfoModel?.objectId ?? "",
                                  params: params,
                                  delegate: nil)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        SyncUtil.unsubscribe(id: channleName, className: SYNC_MANAGER_GAME_APPLY_INFO)
        SyncUtil.unsubscribe(id: channleName, className: SYNC_MANAGER_GAME_INFO)
        if !targetChannelName.isEmpty {
            SyncUtil.unsubscribe(id: targetChannelName, className: SYNC_MANAGER_GAME_APPLY_INFO)
        }
    }
    
    override func deleteSubscribe() {
        super.deleteSubscribe()
        if !targetChannelName.isEmpty {
            SyncUtil.unsubscribe(id: targetChannelName, className: SYNC_MANAGER_GAME_APPLY_INFO)
        }
    }
}
