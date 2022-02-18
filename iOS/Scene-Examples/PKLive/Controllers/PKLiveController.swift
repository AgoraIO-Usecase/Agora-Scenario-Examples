//
//  PKLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit
import AgoraSyncManager

class PKLiveController: SignleLiveController {
    public lazy var stopBroadcastButton: UIButton = {
        let button = UIButton()
        button.setTitle("stop_wheat".localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 13)
        button.backgroundColor = .init(hex: "#000000", alpha: 0.7)
        button.layer.cornerRadius = 19
        button.layer.masksToBounds = true
        button.isHidden = true
        button.addTarget(self, action: #selector(clickStopBroadcast), for: .touchUpInside)
        return button
    }()
    public lazy var vsImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "PK/pic-VS"))
        imageView.isHidden = true
        return imageView
    }()
    private lazy var countTimeLabel: UILabel = {
        let label = UILabel()
        label.text = "00:00"
        label.textColor = .blueColor
        label.font = .systemFont(ofSize: 14)
        label.isHidden = true
        return label
    }()
    public lazy var pkProgressView: PKLiveProgressView = {
        let view = PKLiveProgressView()
        view.isHidden = true
        return view
    }()
    
    public var pkInfoModel: PKInfoModel?
    private lazy var timer = GCDTimer()
    public var pkApplyInfoModel: PKApplyInfoModel?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        if getRole(uid: UserInfo.uid) == .audience {
            getBroadcastPKApplyInfo()
        }
    }
    
    private func setupUI() {
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == UserInfo.uid ? [.pk, .tool, .close] : [.gift, .close]
        liveView.updateBottomButtonType(type: bottomType)
        
        stopBroadcastButton.translatesAutoresizingMaskIntoConstraints = false
        vsImageView.translatesAutoresizingMaskIntoConstraints = false
        countTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        pkProgressView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stopBroadcastButton)
        stopBroadcastButton.translatesAutoresizingMaskIntoConstraints = false
        stopBroadcastButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        stopBroadcastButton.widthAnchor.constraint(equalToConstant: 83).isActive = true
        stopBroadcastButton.heightAnchor.constraint(equalToConstant: 38).isActive = true
        stopBroadcastButton.bottomAnchor.constraint(equalTo: liveView.bottomView.topAnchor, constant: -10).isActive = true
        
        view.addSubview(vsImageView)
        vsImageView.centerXAnchor.constraint(equalTo: liveView.liveCanvasView.centerXAnchor).isActive = true
        
        view.addSubview(countTimeLabel)
        countTimeLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        countTimeLabel.bottomAnchor.constraint(equalTo: liveView.liveCanvasView.topAnchor, constant: -1).isActive = true
        
        view.addSubview(pkProgressView)
        pkProgressView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 60).isActive = true
        pkProgressView.topAnchor.constraint(equalTo: liveView.liveCanvasView.topAnchor, constant: 15).isActive = true
        pkProgressView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -60).isActive = true
        pkProgressView.heightAnchor.constraint(equalToConstant: 40).isActive = true
    }
    
    override func closeLiveHandler() {
        super.closeLiveHandler()
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            updatePKInfoStatusToEnd()            
        }
    }
    
    override func eventHandler() {
        super.eventHandler()
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            // 监听主播发起PK
            SyncUtil.subscribe(id: channleName, key: sceneType.rawValue, onUpdated: { object in
                self.pkSubscribeHandler(object: object)
            }, onSubscribed: {
                LogUtils.log(message: "onSubscribed pkApplyInfo", level: .info)
            })
        }
        // 监听PKinfo 让观众加入到PK的channel
        SyncUtil.subscribe(id: channleName, key: SYNC_MANAGER_PK_INFO, onCreated: { object in
            guard self.getRole(uid: "\(UserInfo.userId)") == .audience,
                  let model = JSONObject.toModel(PKInfoModel.self, value: object.toJson()) else { return }
            if model.userId == "\(UserInfo.userId)" { return }
            self.pkInfoModel = model
            self.joinAudienceChannel(channelName: model.roomId, pkUid:  UInt(model.userId) ?? 0)
        }, onUpdated: { object in
            LogUtils.log(message: "onUpdated pkInfo == \(String(describing: object.toJson()))", level: .info)
            guard self.getRole(uid: "\(UserInfo.userId)") == .audience,
                  let model = JSONObject.toModel(PKInfoModel.self, value: object.toJson()) else { return }
            if model.userId == "\(UserInfo.userId)" { return }
            self.pkInfoModel = model
            if model.status == .end {
                if self.channleName != model.roomId {
                    self.leaveChannel(uid: UserInfo.userId, channelName: model.roomId)
                }
                self.liveView.updateLiveLayout(postion: .full)
                self.hiddenPkProgressView(isHidden: true)
            } else {
                self.joinAudienceChannel(channelName: model.roomId, pkUid:  UInt(model.userId) ?? 0)
                self.liveView.updateLiveLayout(postion: .center)
                self.hiddenPkProgressView(isHidden: false)
            }
        }, onSubscribed: {
            LogUtils.log(message: "onSubscribed pkInfo", level: .info)
        })
        
        liveView.onReceivedGiftClosure = { [weak self] giftModel, type in
            self?.receiveGiftHandler(giftModel: giftModel, type: type)
        }
        
        liveView.onClickPKButtonClosure = { [weak self] in
            self?.clickPKHandler()
        }
    }
    
    public func pkSubscribeHandler(object: IObject) {
        LogUtils.log(message: "onUpdated pkApplyInfo == \(String(describing: object.toJson()))", level: .info)
        guard var model = JSONObject.toModel(PKApplyInfoModel.self, value: object.toJson()) else { return }
        pkApplyInfoModel = model
        if model.status == .end {
            print("========== me end ==================")
            // 自己在对方的channel中移除
            let channelName = model.targetUserId == UserInfo.uid ? model.roomId : model.targetRoomId
            leaveChannel(uid: UserInfo.userId, channelName: channelName ?? "")
            liveView.updateLiveLayout(postion: .full)
            hiddenPkProgressView(isHidden: true)
            // PK结束
            pkLiveEndHandler()
            
            guard var pkInfoModel = pkInfoModel else {
                return
            }
            pkInfoModel.status = .end
            SyncUtil.update(id: channleName, key: SYNC_MANAGER_PK_INFO, params: JSONObject.toJson(pkInfoModel))
            
        } else if model.status == .accept {
            liveView.updateLiveLayout(postion: .center)
            hiddenPkProgressView(isHidden: false)
            // 把自己加入到对方的channel
            let channelName = model.targetUserId == UserInfo.uid ? model.roomId : model.targetRoomId
            let userId = model.userId == currentUserId ? model.targetUserId : model.userId
            joinAudienceChannel(channelName: channelName ?? "", pkUid: UInt(userId ?? "0") ?? 0)
            
            // pk开始
            pkLiveStartHandler()
            
            // 通知观众加入到pk的channel
            var pkInfo = PKInfoModel()
            pkInfo.status = model.status
            pkInfo.roomId = channelName ?? ""
            pkInfo.userId = userId ?? ""
            SyncUtil.update(id: channleName, key: SYNC_MANAGER_PK_INFO, params: JSONObject.toJson(pkInfo), success: { results in
                guard let result = results.first else { return }
                guard let model = JSONObject.toModel(PKInfoModel.self, value: result.toJson()) else { return }
                self.pkInfoModel = model
            })
            
        } else if model.status == .invite && "\(UserInfo.userId)" != model.userId {
            let message = sceneType == .game ? String(format: "your_friends_invite_you_join_game".localized, model.userName, model.gameId.title) : ""
            showAlert(title: sceneType.alertTitle, message: message) { [weak self] in
                guard let self = self else { return }
                model.status = .refuse
                SyncUtil.update(id: model.targetRoomId ?? "", key: self.sceneType.rawValue, params: JSONObject.toJson(model))
                
            } confirm: { [weak self] in
                guard let self = self else { return }
                model.status = .accept
                SyncUtil.update(id: model.targetRoomId ?? "", key: self.sceneType.rawValue, params: JSONObject.toJson(model))
            }
        } else if model.status == .refuse && "\(UserInfo.userId)" == model.userId {
            showAlert(title: "PK_Invite_Reject".localized, message: "")
            deleteSubscribe()
        }
    }
    
    /// 获取PK信息
    private func getBroadcastPKApplyInfo() {
        SyncUtil.fetch(id: channleName, key: SYNC_MANAGER_PK_INFO, success: { [weak self] result in
            guard let self = self,
                  let pkInfoModel = JSONObject.toModel(PKInfoModel.self, value: result?.toJson()) else { return }
            self.pkInfoModel = pkInfoModel
            self.updatePKUIStatus(isStart: pkInfoModel.status == .accept)
            if pkInfoModel.status == .accept {
                self.joinAudienceChannel(channelName: pkInfoModel.roomId, pkUid: UInt(pkInfoModel.userId) ?? 0)
            }
            self.getBroadcastPKStatus()
        })
    }
    
    /// 获取当前主播PK状态
    public func getBroadcastPKStatus() { }
    
    /// pk开始
    public func pkLiveStartHandler() {
        updatePKUIStatus(isStart: true)
    }
    /// pk结束
    public func pkLiveEndHandler() {
        updatePKUIStatus(isStart: false)
        deleteSubscribe()
        stopBroadcastButton.isHidden = true
    }
    /// 收到礼物
    public func receiveGiftHandler(giftModel: LiveGiftModel, type: RecelivedType) {
        if type == .me {
            pkProgressView.updateProgressValue(at: giftModel.coin)
        } else {
            pkProgressView.updateTargetProgressValue(at: giftModel.coin)
        }
    }
    /// 程序杀死
    override func applicationWillTerminate() {
        guard getRole(uid: UserInfo.uid) == .broadcaster else { return }
        updatePKInfoStatusToEnd()
    }
    
    private func updatePKUIStatus(isStart: Bool) {
        if currentUserId == "\(UserInfo.userId)" && isStart {
            liveView.updateBottomButtonType(type: [.tool, .close])
        } else if currentUserId == "\(UserInfo.userId)" && !isStart {
            liveView.updateBottomButtonType(type: [.pk, .tool, .close])
        } else {
            liveView.updateBottomButtonType(type: [.gift, .close])
        }
        stopBroadcastButton.isHidden = getRole(uid: "\(UserInfo.userId)") == .audience ? true : !isStart
        if isStart {
            vsImageView.centerYAnchor.constraint(equalTo: view.topAnchor,
                                                 constant: liveView.liveCanvasViewHeight).isActive = true
            timer.scheduledSecondsTimer(withName: sceneType.rawValue, timeInterval: 3600, queue: .main) { [weak self] _, duration in
                self?.countTimeLabel.text = "".timeFormat(secounds: duration)
                if duration <= 0 {
                    self?.updatePKInfoStatusToEnd()
                }
            }
        } else {
            pkProgressView.reset()
        }
    }
    
    public func hiddenPkProgressView(isHidden: Bool) {
        DispatchQueue.main.asyncAfter(deadline: .now() + (isHidden ? 0 : 0.5)) {
            self.vsImageView.isHidden = isHidden
            self.countTimeLabel.isHidden = isHidden
            self.pkProgressView.isHidden = isHidden
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        let channelName = pkApplyInfoModel?.targetRoomId ?? channleName
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_PK_INFO)
        SyncUtil.deleteCollection(id: channelName, className: sceneType.rawValue)
         deleteSubscribe()
    }
    
    override func didOfflineOfUid(uid: UInt) {
        super.didOfflineOfUid(uid: uid)
        LogUtils.log(message: "pklive leave == \(uid)", level: .info)
    }
    
    private func updatePKInfoStatusToEnd() {
        guard var applyModel = pkApplyInfoModel else { return }
        applyModel.status = .end
        let channelName = applyModel.targetRoomId ?? channleName
        SyncUtil.update(id: channelName, key: sceneType.rawValue, params: JSONObject.toJson(applyModel))
    }
    
    public func deleteSubscribe() {
        timer.destoryTimer(withName: sceneType.rawValue)
        let channelName = pkApplyInfoModel?.targetRoomId ?? channleName
        if channelName != self.channleName {
            leaveChannel(uid: UserInfo.userId, channelName: channelName)
            SyncUtil.unsubscribe(id: channelName, key: sceneType.rawValue)
            SyncUtil.unsubscribe(id: channelName, key: SYNC_MANAGER_GIFT_INFO)
            SyncUtil.leaveScene(id: channelName)
        } else {
            guard let applyModel = pkApplyInfoModel else { return }
            leaveChannel(uid: UserInfo.userId, channelName: applyModel.roomId)
        }
    }
    
    public func clickPKHandler() {
        let pkInviteListView = PKLiveInviteView(channelName: channleName, sceneType: sceneType)
        pkInviteListView.pkInviteSubscribe = { [weak self] id in
            guard let self = self else { return }
            // 加入到对方的channel 订阅对方
            SyncUtil.subscribe(id: id, key: self.sceneType.rawValue, onUpdated: { object  in
                self.pkSubscribeHandler(object: object)
            }, onSubscribed: {
                LogUtils.log(message: "subscribe target pk apply info", level: .info)
            })
            // 订阅对方收到的礼物
            self.liveView.subscribeGift(channelName: id, type: .target)
        }
        AlertManager.show(view: pkInviteListView, alertPostion: .bottom)
    }
    
    @objc
    private func clickStopBroadcast() { /// 停止连麦
        showAlert(title: "End_Broadcasting".localized, message: "", cancel: nil) { [weak self] in
            self?.updatePKInfoStatusToEnd()
            self?.stopBroadcastButton.isHidden = true
        }
    }
}
