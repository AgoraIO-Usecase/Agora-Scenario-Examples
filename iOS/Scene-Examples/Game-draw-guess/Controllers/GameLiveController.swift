//
//  DGLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

class GameLiveController: LivePlayerController {
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
    private lazy var pkProgressView: PKLiveProgressView = {
        let view = PKLiveProgressView()
        view.isHidden = true
        return view
    }()
    public lazy var stopBroadcastButton: UIButton = {
        let button = UIButton()
        button.setTitle("停止连麦", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 13)
        button.backgroundColor = .init(hex: "#000000", alpha: 0.7)
        button.layer.cornerRadius = 19
        button.layer.masksToBounds = true
        button.isHidden = true
        button.addTarget(self, action: #selector(clickStopBroadcast), for: .touchUpInside)
        return button
    }()
    private lazy var viewModel = GameViewModel(channleName: channleName,
                                               ownerId: currentUserId)
    
    private lazy var timer = GCDTimer()
    public var targetChannelName: String = ""
    private var pkApplyInfoModel: PKApplyInfoModel?
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
//        view.addSubview(webView)
        view.insertSubview(webView, at: 0)
//        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
//        webView.topAnchor.constraint(equalTo: avatarview.bottomAnchor, constant: 15).isActive = true
//        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
//        webView.bottomAnchor.constraint(equalTo: chatView.topAnchor, constant: -10).isActive = true
        
        webView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: view.topAnchor, constant: -Screen.kNavHeight).isActive = true
        webView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        countTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        pkProgressView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(countTimeLabel)
        countTimeLabel.centerXAnchor.constraint(equalTo: liveCanvasView.centerXAnchor).isActive = true
        countTimeLabel.bottomAnchor.constraint(equalTo: liveCanvasView.topAnchor, constant: -6).isActive = true
        countTimeLabel.widthAnchor.constraint(equalToConstant: 83).isActive = true
        
        liveCanvasView.addSubview(pkProgressView)
        pkProgressView.leadingAnchor.constraint(equalTo: liveCanvasView.leadingAnchor).isActive = true
        pkProgressView.trailingAnchor.constraint(equalTo: liveCanvasView.trailingAnchor).isActive = true
        pkProgressView.bottomAnchor.constraint(equalTo: liveCanvasView.bottomAnchor).isActive = true
        pkProgressView.heightAnchor.constraint(equalToConstant: 20).isActive = true
        
        view.addSubview(stopBroadcastButton)
        stopBroadcastButton.translatesAutoresizingMaskIntoConstraints = false
        stopBroadcastButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        stopBroadcastButton.widthAnchor.constraint(equalToConstant: 83).isActive = true
        stopBroadcastButton.heightAnchor.constraint(equalToConstant: 38).isActive = true
        stopBroadcastButton.bottomAnchor.constraint(equalTo: bottomView.topAnchor, constant: -10).isActive = true
    }
    
    // 游戏PK
    override func clickGamePKHandler() {
//        updatePKUIStatus(isStart: true)
//        return
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
                                         delegate: PKInviteInfoTargetDelegate(vc: self))
            
            // 订阅对方收到的礼物
            SyncUtil.subscribeCollection(id: id,
                                         className: SYNC_MANAGER_GIFT_INFO,
                                         delegate: LiveGiftDelegate(vc: self, type: .target))
            // 订阅对方的游戏
            SyncUtil.subscribeCollection(id: id,
                                         className: SYNC_MANAGER_GAME_INFO,
                                         delegate: GameInfoDelegate(vc: self))
        }
        AlertManager.show(view: pkInviteListView, alertPostion: .bottom)
    }
    
    @objc
    private func clickStopBroadcast() { /// 停止连麦
        showAlert(title: "终止连麦", message: "", cancel: nil) { [weak self] in
            self?.updatePKInfoStatusToEnd()
            self?.updateGameInfoStatus(isStart: false)
            self?.stopBroadcastButton.isHidden = true
        }
    }
    
    // 退出游戏
    override func exitGameHandler() {
        let roleType: GameRoleType = targetChannelName.isEmpty ? .audience : .broadcast
        showAlert(title: "退出游戏", message: "退出游戏将会终止游戏PK", cancel: nil) { [weak self] in
            self?.viewModel.leaveGame(roleType: roleType)
            self?.updatePKUIStatus(isStart: false)
            self?.webView.reset()
            self?.updateGameInfoStatus(isStart: false)
        }
//        self?.viewModel.postBarrage()
//        viewModel.postGiftHandler(type: .delay)
    }
        
    override func closeLiveHandler() {
        updatePKInfoStatusToEnd()
    }
    override func eventHandler() {
        super.eventHandler()
        // 监听主播发起PK
        SyncUtil.subscribeCollection(id: channleName,
                                     className: sceneType.rawValue,
                                     delegate: PKInviteInfoDelegate(vc: self))
        
        // 监听PKinfo 让观众加入到PK的channel
        SyncUtil.subscribeCollection(id: channleName,
                                     className: SYNC_MANAGER_PK_INFO,
                                     delegate: PKInfoDelegate(vc: self))

        // 监听游戏
        SyncUtil.subscribeCollection(id: channleName,
                                     className: SYNC_MANAGER_GAME_INFO,
                                     delegate: GameInfoDelegate(vc: self))
        
        // pk开始回调
        pkLiveStartClosure = { [weak self] applyModel in
            guard let self = self else { return }
            self.pkApplyInfoModel = applyModel
            self.updatePKUIStatus(isStart: true)
            self.updateGameInfoStatus(isStart: true)
        }
        
        // pk 结束回调
        pkLiveEndClosure = { [weak self] applyModel in
            self?.pkApplyInfoModel = applyModel
            self?.updatePKUIStatus(isStart: false)
            self?.updateLiveLayout(postion: .full)
            self?.stopBroadcastButton.isHidden = true
            self?.deleteSubscribe()
        }
        // 收到礼物回调
        LiveReceivedGiftClosure = { [weak self] giftModel, type in
            if type == .me {
                self?.pkProgressView.updateProgressValue(at: giftModel.coin)
                self?.viewModel.postGiftHandler(type: giftModel.giftType)
            } else {
                self?.pkProgressView.updateTargetProgressValue(at: giftModel.coin)
            }
        }
    }
    
    public func updatePKUIStatus(isStart: Bool) {
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
        }
    }
    
    /// 更新游戏状态
    private func updateGameInfoStatus(isStart: Bool) {
        let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
        if isStart {
            var gameInfoModel = GameInfoModel()
            gameInfoModel.status = .playing
            SyncUtil.addCollection(id: channelName,
                                   className: SYNC_MANAGER_GAME_INFO,
                                   params: JSONObject.toJson(gameInfoModel),
                                   delegate: nil)
            return
        }
        gameInfoModel?.status =  .end
        let params = JSONObject.toJson(gameInfoModel)
        SyncUtil.updateCollection(id: channelName,
                                  className: SYNC_MANAGER_GAME_INFO,
                                  objectId: gameInfoModel?.objectId ?? "",
                                  params: params,
                                  delegate: nil)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        SyncUtil.unsubscribe(id: channleName, className: SYNC_MANAGER_PK_INFO)
        SyncUtil.unsubscribe(id: channleName, className: SYNC_MANAGER_GAME_INFO)
        deleteSubscribe()
    }
    
    override func didOfflineOfUid(uid: UInt) {
        super.didOfflineOfUid(uid: uid)
        LogUtils.log(message: "pklive leave == \(uid)", level: .info)
        guard let applyModel = pkApplyInfoModel,
              applyModel.userId == "\(uid)" || applyModel.targetUserId == "\(uid)" else { return }
        updatePKInfoStatusToEnd()
    }
    
    private func updatePKInfoStatusToEnd() {
        guard var applyModel = pkApplyInfoModel else { return }
        applyModel.status = .end
        let channelName = targetChannelName.isEmpty ? channleName : targetChannelName
        SyncUtil.updateCollection(id: channelName,
                                  className: sceneType.rawValue,
                                  objectId: applyModel.objectId,
                                  params: JSONObject.toJson(applyModel),
                                  delegate: nil)
        
        guard var pkInfoModel = pkInfoModel else {
            return
        }
        pkInfoModel.status = .end
        SyncUtil.updateCollection(id: channelName,
                                  className: SYNC_MANAGER_PK_INFO,
                                  objectId: pkInfoModel.objectId,
                                  params: JSONObject.toJson(pkInfoModel),
                                  delegate: nil)
    }
    
    private func deleteSubscribe() {
        if !targetChannelName.isEmpty {
            leaveChannel(uid: UserInfo.userId, channelName: targetChannelName)
            SyncUtil.unsubscribe(id: targetChannelName, className: sceneType.rawValue)
            SyncUtil.unsubscribe(id: targetChannelName, className: SYNC_MANAGER_GIFT_INFO)
            SyncUtil.unsubscribe(id: targetChannelName, className: SYNC_MANAGER_GAME_INFO)
            SyncUtil.leaveScene(id: targetChannelName)
        }
        timer.destoryTimer(withName: sceneType.rawValue)
    }
}
