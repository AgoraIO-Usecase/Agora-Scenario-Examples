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
    
    private lazy var timer = GCDTimer()
    private var targetChannelName: String = ""
    private var pkApplyInfoModel: PKApplyInfoModel?
    
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
    }
    
    // 游戏PK
    override func gamePKHandler() {
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
        let pkInviteListView = PKLiveInviteView(channelName: channleName)
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
        }
        AlertManager.show(view: pkInviteListView, alertPostion: .bottom)
    }
    
    // 退出游戏
    override func exitGameHandler() {
//        updatePKUIStatus(isStart: false)
        let roleType: GameRoleType = targetChannelName.isEmpty ? .audience : .broadcast
        var params: [String: Any] = ["user_id": "\(UserInfo.userId)",
                                     "app_id": KeyCenter.gameAppId,
                                     "room_id": channleName,
                                     "identity": roleType.rawValue,
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "nonce_str": "".timeStamp16]
        let sign = NetworkManager.shared.generateSignature(params: params, token: KeyCenter.gameAppSecrets)
        params["sign"] = sign
        
        NetworkManager.shared.postRequest(urlString: "http://testgame.yuanqihuyu.com/guess/leave", params: params) { result in
            print("result == \(result)")
        } failure: { error in
            print("error == \(error)")
        }
        
        // 发弹幕
//        let barrage = GameBarrageType.allCases.randomElement() ?? .salvo
//        let params: [String: Any] = ["user_id": "\(UserInfo.userId)",
//                                     "app_id": KeyCenter.gameAppId,
//                                     "room_id": channleName,
//                                     "name": "User-\(UserInfo.userId)",
//                                     "token": KeyCenter.gameAppSecrets,
//                                     "timestamp": "".timeStamp,
//                                     "nonce_str": "".timeStamp,
//                                     "barrage": barrage.rowValue,
//                                     "count": 1, "player": currentUserId,
//                                     "sign": KeyCenter.gameAppSecrets]
//        NetworkManager.shared.postRequest(urlString: "http://testgame.yuanqihuyu.com/guess/barrage", params: params, success: { result in
//
//        }, failure: { error in
//
//        })
    }
    
    override func eventHandler() {
        super.eventHandler()
        // 监听主播发起PK
        SyncUtil.subscribeCollection(id: channleName,
                                     className: sceneType.rawValue,
                                     delegate: PKInviteInfoDelegate(vc: self))
        
        // 监听PKinfo 让观众加入到PK的channel
        SyncUtil.subscribeCollection(id: channleName,
                                     className: SceneType.pkInfo.rawValue,
                                     delegate: PKInfoDelegate(vc: self))

        // pk开始回调
        pkLiveStartClosure = { [weak self] applyModel in
            guard let self = self else { return }
            self.pkApplyInfoModel = applyModel
            self.updatePKUIStatus(isStart: true)
        }
        
        // pk 结束回调
        pkLiveEndClosure = { [weak self] applyModel in
            self?.pkApplyInfoModel = applyModel
            self?.updatePKUIStatus(isStart: false)
        }
        // 收到礼物回调
        LiveReceivedGiftClosure = { [weak self] giftModel, type in
            if type == .me {
                self?.pkProgressView.updateProgressValue(at: giftModel.coin)
            } else {
                self?.pkProgressView.updateTargetProgressValue(at: giftModel.coin)
            }
        }
    }
    
    private func updatePKUIStatus(isStart: Bool) {
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
            let roleType: GameRoleType = targetChannelName.isEmpty ? .audience : .broadcast
            webView.loadUrl(urlString: gameCenterModel?.type.gameUrl ?? "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html",
                            roomId: channleName,
                            roleType: roleType)
            
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
            deleteSubscribe()
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        SyncUtil.unsubscribe(id: channleName, className: SceneType.pkInfo.rawValue)
        deleteSubscribe()
    }
    
    override func didOfflineOfUid(uid: UInt) {
        super.didOfflineOfUid(uid: uid)
        LogUtils.log(message: "pklive leave == \(uid)", level: .info)
        guard var applyModel = pkApplyInfoModel else { return }
        guard applyModel.userId == "\(uid)" || applyModel.targetUserId == "\(uid)" else { return }
        applyModel.status = .end
        SyncUtil.updateCollection(id: channleName,
                                  className: sceneType.rawValue,
                                  objectId: applyModel.objectId,
                                  params: JSONObject.toJson(applyModel),
                                  delegate: nil)
        SyncUtil.updateCollection(id: targetChannelName,
                                  className: sceneType.rawValue,
                                  objectId: applyModel.objectId,
                                  params: JSONObject.toJson(applyModel),
                                  delegate: nil)
        
        guard var pkInfoModel = pkInfoModel else {
            return
        }
        pkInfoModel.status = .end
        SyncUtil.updateCollection(id: channleName,
                                  className: SceneType.pkInfo.rawValue,
                                  objectId: pkInfoModel.objectId,
                                  params: JSONObject.toJson(pkInfoModel),
                                  delegate: nil)
        
        SyncUtil.updateCollection(id: targetChannelName,
                                  className: SceneType.pkInfo.rawValue,
                                  objectId: pkInfoModel.objectId,
                                  params: JSONObject.toJson(pkInfoModel),
                                  delegate: nil)
    }
    
    private func deleteSubscribe() {
        SyncUtil.deleteCollection(id: targetChannelName, className: sceneType.rawValue, delegate: nil)
        SyncUtil.deleteCollection(id: channleName, className: sceneType.rawValue, delegate: nil)
        
        if !targetChannelName.isEmpty {
            leaveChannel(uid: UserInfo.userId, channelName: targetChannelName)
            SyncUtil.unsubscribe(id: targetChannelName, className: sceneType.rawValue)
            SyncUtil.unsubscribe(id: targetChannelName, className: SYNC_MANAGER_GIFT_INFO)
            SyncUtil.leaveScene(id: targetChannelName)
        }
        timer.destoryTimer(withName: sceneType.rawValue)
    }
}
