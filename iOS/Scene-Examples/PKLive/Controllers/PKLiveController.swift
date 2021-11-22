//
//  PKLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit

class PKLiveController: LivePlayerController {
    public lazy var pkInviteButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "PK/pic-PK"), for: .normal)
        button.addTarget(self, action: #selector(clickPKInviteButton), for: .touchUpInside)
        button.isHidden = self.getRole(uid: "\(UserInfo.userId)") == .audience
        return button
    }()
    private lazy var vsImageView: UIImageView = {
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
    private lazy var pkProgressView: PKLiveProgressView = {
        let view = PKLiveProgressView()
        view.isHidden = true
        return view
    }()
    
    private lazy var timer = GCDTimer()
    private var targetChannelName: String = ""
    private var pkApplyInfoModel: PKApplyInfoModel?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    private func setupUI() {
        pkInviteButton.translatesAutoresizingMaskIntoConstraints = false
        vsImageView.translatesAutoresizingMaskIntoConstraints = false
        countTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        pkProgressView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(pkInviteButton)
        pkInviteButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        pkInviteButton.bottomAnchor.constraint(equalTo: bottomView.topAnchor, constant: -20).isActive = true
        
        view.addSubview(vsImageView)
        vsImageView.centerXAnchor.constraint(equalTo: liveCanvasView.centerXAnchor).isActive = true
        
        view.addSubview(countTimeLabel)
        countTimeLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        countTimeLabel.bottomAnchor.constraint(equalTo: liveCanvasView.topAnchor, constant: -1).isActive = true
        
        view.addSubview(pkProgressView)
        pkProgressView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 60).isActive = true
        pkProgressView.topAnchor.constraint(equalTo: liveCanvasView.topAnchor, constant: 15).isActive = true
        pkProgressView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -60).isActive = true
        pkProgressView.heightAnchor.constraint(equalToConstant: 40).isActive = true
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
        vsImageView.isHidden = !isStart
        countTimeLabel.isHidden = !isStart
        pkProgressView.isHidden = !isStart
        pkInviteButton.isHidden = getRole(uid: "\(UserInfo.userId)") == .audience ? true : isStart
        if isStart {
            vsImageView.centerYAnchor.constraint(equalTo: view.topAnchor,
                                                 constant: liveCanvasViewHeight).isActive = true
            timer.scheduledSecondsTimer(withName: sceneType.rawValue, timeInterval: 180, queue: .main) { [weak self] _, duration in
                self?.countTimeLabel.text = "".timeFormat(secounds: duration)
                if duration <= 0 {
                    self?.deleteSubscribe()
                }
            }
        } else {
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
        guard var applyModel = pkApplyInfoModel, !targetChannelName.isEmpty else { return }
        guard applyModel.userId == "\(uid)" || applyModel.targetUserId == "\(uid)" else { return }
        applyModel.status = .end
        SyncUtil.updateCollection(id: targetChannelName,
                                  className: sceneType.rawValue,
                                  objectId: applyModel.objectId,
                                  params: JSONObject.toJson(applyModel),
                                  delegate: nil)
        
        guard var pkInfoModel = pkInfoModel else {
            return
        }
        pkInfoModel.status = .end
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
    
    @objc
    private func clickPKInviteButton() {
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
}
