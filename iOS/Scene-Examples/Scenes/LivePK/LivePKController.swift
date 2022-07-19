//
//  PKLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit
import AgoraRtcKit

class LivePKController: BaseViewController {
    public lazy var liveView: LiveBaseView = {
        let view = LiveBaseView(channelName: channleName, currentUserId: currentUserId)
        view.updateLiveLayout(postion: .full)
        return view
    }()
    public var agoraKit: AgoraRtcEngineKit?
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
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    
    public lazy var stopBroadcastButton: UIButton = {
        let button = UIButton()
        button.setTitle("stop_wheat".localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 13)
        button.backgroundColor = .init(hex: "#000000", alpha: 0.7)
        button.layer.cornerRadius = 19
        button.layer.masksToBounds = true
        button.isHidden = true
        button.addTarget(self, action: #selector(onTapStopBroadcast), for: .touchUpInside)
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
    
    init(channelName: String, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channleName = channelName
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
        // 设置屏幕常亮
        UIApplication.shared.isIdleTimerDisabled = true
        if getRole(uid: UserInfo.uid) == .audience {
            getBroadcastPKApplyInfo()
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: true)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            joinBroadcasterChannel(channelName: channleName, uid: UserInfo.userId)
        } else {
            joinAudienceChannel(channelName: channleName, pkUid: UInt(currentUserId) ?? 0)
        }
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is LivePKCreateController }) else { return }
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
        
        leaveChannel(uid: UserInfo.userId, channelName: channleName, isExit: true)
        liveView.leave(channelName: channleName)
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.pkApply.rawValue)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        
        let channelName = pkApplyInfoModel?.targetRoomId ?? channleName
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SYNC_MANAGER_PK_INFO)
        SyncUtil.scene(id: channelName)?.collection(className: SceneType.pkApply.rawValue).delete(success: { _ in
            
        }, fail: { _ in
            
        })
        deleteSubscribe()
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        liveView.translatesAutoresizingMaskIntoConstraints = false
    
        view.addSubview(liveView)
        
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
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
    
    public func eventHandler() {
        liveView.onTapCloseLiveClosure = { [weak self] in
            self?.onTapCloseLive()
        }
        liveView.onTapSwitchCameraClosure = { [weak self] isSelected in
            self?.agoraKit?.switchCamera()
        }
        liveView.onTapIsMuteCameraClosure = { [weak self] isSelected in
            self?.agoraKit?.muteLocalVideoStream(isSelected)
        }
        liveView.onTapIsMuteMicClosure = { [weak self] isSelected in
            self?.agoraKit?.muteLocalAudioStream(isSelected)
        }
        liveView.setupLocalVideoClosure = { [weak self] canvas in
            self?.agoraKit?.setupLocalVideo(canvas)
            self?.agoraKit?.startPreview()
        }
        liveView.setupRemoteVideoClosure = { [weak self] canvas, connection in
            self?.agoraKit?.setupRemoteVideoEx(canvas, connection: connection)
        }
        
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            // 监听主播发起PK
            SyncUtil.scene(id: channleName)?.subscribe(key: SceneType.pkApply.rawValue, onCreated: { object in
                
            }, onUpdated: { object in
                self.pkSubscribeHandler(object: object)
            }, onDeleted: { object in
                
            }, onSubscribed: {
                LogUtils.log(message: "onSubscribed pkApplyInfo", level: .info)
            }, fail: { error in
                ToastView.show(text: error.message)
            })
        }
        // 监听PKinfo 让观众加入到PK的channel
        SyncUtil.scene(id: channleName)?.subscribe(key: SYNC_MANAGER_PK_INFO, onCreated: { object in
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
        }, onDeleted: { object in
            
        }, onSubscribed: {
            LogUtils.log(message: "onSubscribed pkInfo", level: .info)
        }, fail: { error in
            ToastView.show(text: error.message)
        })
        
        liveView.onReceivedGiftClosure = { [weak self] giftModel, type in
            self?.receiveGiftHandler(giftModel: giftModel, type: type)
        }
        
        liveView.onTapPKButtonClosure = { [weak self] in
            self?.onTapPKHandler()
        }
        
        guard getRole(uid: UserInfo.uid) == .audience else { return }
        SyncUtil.scene(id: channleName)?.subscribeScene(onDeleted: { _ in
            self.showAlert(title: "live_broadcast_over".localized, message: "") {
                self.navigationController?.popViewController(animated: true)
            }
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func onTapCloseLive() {
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                self?.closeLiveHandler()
                SyncUtil.scene(id: self?.channleName ?? "")?.deleteScenes()
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            closeLiveHandler()
            navigationController?.popViewController(animated: true)
        }
    }
    
    private func closeLiveHandler() {
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            updatePKInfoStatusToEnd()            
        }
    }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(getRole(uid: currentUserId))
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
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
            SyncUtil.scene(id: channleName)?.update(key: SYNC_MANAGER_PK_INFO, data: JSONObject.toJson(pkInfoModel), success: { _ in
                
            }, fail: { error in
                ToastView.show(text: error.message)
            })
            
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
            SyncUtil.scene(id: channleName)?.update(key: SYNC_MANAGER_PK_INFO, data: JSONObject.toJson(pkInfo), success: { results in
                guard let result = results.first else { return }
                guard let model = JSONObject.toModel(PKInfoModel.self, value: result.toJson()) else { return }
                self.pkInfoModel = model
            }, fail: { error in
                ToastView.show(text: error.message)
            })
            
        } else if model.status == .invite && "\(UserInfo.userId)" != model.userId {
            showAlert(title: SceneType.pkApply.alertTitle, message: "") {
                model.status = .refuse
                SyncUtil.scene(id: model.targetRoomId ?? "")?.update(key: SceneType.pkApply.rawValue, data: JSONObject.toJson(model), success: { _ in
                    
                }, fail: { error in
                    ToastView.show(text: error.message)
                })
                
            } confirm: {
                model.status = .accept
                SyncUtil.scene(id: model.targetRoomId ?? "")?.update(key: SceneType.pkApply.rawValue, data: JSONObject.toJson(model), success: { _ in
                    
                }, fail: { error in
                    ToastView.show(text: error.message)
                })
            }
        } else if model.status == .refuse && "\(UserInfo.userId)" == model.userId {
            showAlert(title: "PK_Invite_Reject".localized, message: "")
            deleteSubscribe()
        }
    }
    
    /// 获取PK信息
    private func getBroadcastPKApplyInfo() {
        SyncUtil.scene(id: channleName)?.get(key: SYNC_MANAGER_PK_INFO, success: { [weak self] result in
            guard let self = self,
                  let pkInfoModel = JSONObject.toModel(PKInfoModel.self, value: result?.toJson()) else { return }
            self.pkInfoModel = pkInfoModel
            self.updatePKUIStatus(isStart: pkInfoModel.status == .accept)
            if pkInfoModel.status == .accept {
                self.joinAudienceChannel(channelName: pkInfoModel.roomId, pkUid: UInt(pkInfoModel.userId) ?? 0)
            }
            self.getBroadcastPKStatus()
        }, fail: { error in
            ToastView.show(text: error.message)
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
            timer.scheduledSecondsTimer(withName: SceneType.pkApply.rawValue, timeInterval: 3600, queue: .main) { [weak self] _, duration in
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
            self.pkProgressView.isHidden = true//isHidden
        }
    }
    
    private func updatePKInfoStatusToEnd() {
        guard var applyModel = pkApplyInfoModel else { return }
        applyModel.status = .end
        let channelName = applyModel.targetRoomId ?? channleName
        SyncUtil.scene(id: channelName)?.update(key: SceneType.pkApply.rawValue, data: JSONObject.toJson(applyModel), success: { _ in
            
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    public func deleteSubscribe() {
        timer.destoryTimer(withName: SceneType.pkApply.rawValue)
        let channelName = pkApplyInfoModel?.targetRoomId ?? channleName
        if channelName != self.channleName {
            leaveChannel(uid: UserInfo.userId, channelName: channelName)
            SyncUtil.scene(id: channelName)?.unsubscribe(key: SceneType.pkApply.rawValue)
            SyncUtil.scene(id: channelName)?.unsubscribe(key: SYNC_MANAGER_GIFT_INFO)
            SyncUtil.leaveScene(id: channelName)
        } else {
            guard let applyModel = pkApplyInfoModel else { return }
            leaveChannel(uid: UserInfo.userId, channelName: applyModel.roomId)
        }
    }
    
    public func onTapPKHandler() {
        let pkInviteListView = PKLiveInviteView(channelName: channleName, sceneType: SceneType.pkApply)
        pkInviteListView.pkInviteSubscribe = { [weak self] id in
            guard let self = self else { return }
            // 加入到对方的channel 订阅对方
            SyncUtil.scene(id: id)?.subscribe(key: SceneType.pkApply.rawValue, onCreated: { object in
                
            }, onUpdated: { object in
                self.pkSubscribeHandler(object: object)
            }, onDeleted: { object in
                
            }, onSubscribed: {
                LogUtils.log(message: "subscribe target pk apply info", level: .info)
            }, fail: { error in
                ToastView.show(text: error.message)
            })
            // 订阅对方收到的礼物
            self.liveView.subscribeGift(channelName: id, type: .target)
        }
        AlertManager.show(view: pkInviteListView, alertPostion: .bottom)
    }
    
    @objc
    private func onTapStopBroadcast() { /// 停止连麦
        showAlert(title: "End_Broadcasting".localized, message: "", cancel: nil) { [weak self] in
            self?.updatePKInfoStatusToEnd()
            self?.stopBroadcastButton.isHidden = true
        }
    }
    
    /// 主播加入channel
    public func joinBroadcasterChannel(channelName: String, uid: UInt) {
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: uid)
        let connection = LiveCanvasModel.createConnection(channelName: channelName, uid: uid)
        canvasModel.connection = connection
        canvasModel.channelName = channelName
        liveView.setupCanvasData(data: canvasModel)
        
        if getRole(uid: "\(uid)") == .broadcaster && channelName == self.channleName {
            channelMediaOptions.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
            channelMediaOptions.publishAudioTrack = .of(true)
            channelMediaOptions.publishCameraTrack = .of(true)
            channelMediaOptions.autoSubscribeVideo = .of(true)
            channelMediaOptions.autoSubscribeAudio = .of(true)
        }
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token, channelId: channelName, uid: uid, mediaOptions: channelMediaOptions, joinSuccess: nil)
        if result == 0 {
            LogUtils.log(message: "主播进入房间", level: .info)
        }
        liveView.sendMessage(message: "Join_Live_Room".localized, messageType: .message)
    }
    
    /// Audience joins the channel
    /// - Parameters:
    ///   - channelName: 频道名
    ///   - pkUid: pk主播的UserID
    public func joinAudienceChannel(channelName: String, pkUid: UInt = 0) {
        let isContainer = liveView.canvasDataArray.contains(where: { $0.channelName == channelName && $0.canvas?.uid == pkUid })
        guard !isContainer else {
            LogUtils.log(message: "当前用户存在 channelName == \(channelName) pkUid == \(pkUid)", level: .warning)
            let value = liveView.canvasDataArray.map({ "channleName == \($0.channelName) userId == \($0.connection?.localUid ?? 0)" })
            LogUtils.log(message: "所有用户 \(value))", level: .warning)
            liveView.reloadData()
            return
        }
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: pkUid)
        let connection = LiveCanvasModel.createConnection(channelName: channelName, uid: UserInfo.userId)
        canvasModel.connection = connection
        canvasModel.channelName = channelName
        liveView.setupCanvasData(data: canvasModel)
        
        channelMediaOptions.clientRoleType = .of((Int32)(AgoraClientRole.audience.rawValue))
        let joinResult = agoraKit?.joinChannelEx(byToken: KeyCenter.Token, connection: connection, delegate: self, mediaOptions: channelMediaOptions, joinSuccess: nil) ?? -1000
        if joinResult == 0 {
            LogUtils.log(message: "join audience success uid == \(pkUid) channelName == \(channelName)", level: .info)
            let userId = pkUid == (UInt(currentUserId) ?? 0) ? UserInfo.userId : pkUid
            liveView.sendMessage(message: "Join_Live_Room".localized,
                                 messageType: .message)
            return
        }
        LogUtils.log(message: "join audience error uid == \(pkUid) channelName == \(channelName)", level: .error)
    }
    
    private func leaveChannel(uid: UInt, channelName: String, isExit: Bool = false) {
        if isExit && "\(uid)" == currentUserId {
            liveView.canvasDataArray.forEach({
                if let connection = $0.connection {
                    agoraKit?.leaveChannelEx(connection, leaveChannelBlock: nil)
                }
            })
        }
        guard liveView.canvasDataArray.count > 1 else { return }
        if let connection = liveView.canvasDataArray.filter({ $0.connection?.localUid == uid && $0.channelName == channelName }).first?.connection {
            agoraKit?.leaveChannelEx(connection, leaveChannelBlock: { state in
                LogUtils.log(message: "left channel: \(connection.channelId) uid == \(connection.localUid)",
                             level: .info)
            })
        }
        if let connectionIndex = liveView.canvasDataArray.firstIndex(where: { $0.connection?.localUid == uid && $0.channelName == channelName }) {
            liveView.removeData(index: connectionIndex)
        }
    }
    
    public func didOfflineOfUid(uid: UInt) {
        let index = liveView.canvasDataArray.firstIndex(where: { $0.connection?.localUid == uid && $0.channelName != channleName }) ?? -1
        if index > -1 && liveView.canvasDataArray.count > 1 {
            liveView.removeData(index: index)
        }
        guard "\(uid)" != currentUserId else { return }
        liveView.sendMessage(message: "Leave_Live_Room".localized,
                             messageType: .message)
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}
extension LivePKController: AgoraRtcEngineDelegate {
    
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
        didOfflineOfUid(uid: uid)
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
