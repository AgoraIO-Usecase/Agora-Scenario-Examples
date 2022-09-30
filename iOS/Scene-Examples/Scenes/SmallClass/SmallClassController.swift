//
//  SignleLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit
import Fastboard
import Agora_Scene_Utils

class SmallClassController: BaseViewController {
    private lazy var fastRoom: FastRoom = {
        let config = FastRoomConfiguration(appIdentifier: BOARD_APP_ID,
                                           roomUUID: BOARD_ROOM_UUID,
                                           roomToken: BOARD_ROOM_TOKEN,
                                           region: .CN,
                                           userUID: UserInfo.uid)
        let fastRoom = Fastboard.createFastRoom(withFastRoomConfig: config)
        fastRoom.view.layer.cornerRadius = 10
        fastRoom.view.layer.masksToBounds = true
        return fastRoom
    }()
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.itemSize = CGSize(width: 78, height: 78)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 15
        view.delegate = self
        view.scrollDirection = .horizontal
        view.register(AgoraUserCollectionViewCell.self,
                      forCellWithReuseIdentifier: AgoraUserCollectionViewCell.description())
        return view
    }()
    private lazy var videoButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "SmallClass/video_default"), for: .normal)
        button.setImage(UIImage(named: "SmallClass/video_selected"), for: .selected)
        button.addTarget(self, action: #selector(onTapVideoButton(sender:)), for: .touchUpInside)
        button.isSelected = true
        return button
    }()
    private lazy var audioButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "SmallClass/audio_default"), for: .normal)
        button.setImage(UIImage(named: "SmallClass/audio_selected"), for: .selected)
        button.addTarget(self, action: #selector(onTapAudioButton(sender:)), for: .touchUpInside)
        button.isSelected = true
        return button
    }()
    private lazy var closeButton: AGEButton = {
        let button = AGEButton()
        button.layer.cornerRadius = 19
        button.layer.masksToBounds = true
        button.backgroundColor = UIColor(hex: "#000000", alpha: 0.6)
        button.setImage(UIImage(named: "icon-close-gray"), for: .normal)
        button.addTarget(self, action: #selector(onTapCloseLive), for: .touchUpInside)
        return button
    }()
    
    /// 顶部头像昵称
    public lazy var avatarview = LiveAvatarView()
    
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
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        option.publishMicrophoneTrack = .of(true)
        option.publishCameraTrack = .of(true)
        option.autoSubscribeAudio = .of(true)
        option.autoSubscribeVideo = .of(true)
        return option
    }()
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    private var collectionViewCons: NSLayoutConstraint?
    private var dataArray: [AgoraUsersModel] = [] {
        didSet {
            collectionView.dataArray = dataArray
            var w = CGFloat(78 * dataArray.count) + CGFloat((dataArray.count + 1) * 15)
            w = w > Screen.width - 30 ? Screen.width - 30 : w
            collectionViewCons?.constant = w
            collectionViewCons?.isActive = true
        }
    }
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    init(channelName: String, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channleName = channelName
        self.agoraKit = agoraKit
        self.currentUserId = userId
        avatarview.setName(with: channelName)
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
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: true)
        let appdelegate = UIApplication.shared.delegate as? AppDelegate
        appdelegate?.blockRotation = .landscapeRight
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        joinChannel(channelName: channleName, uid: UserInfo.userId)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is SmallClassCreateController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        leaveChannel()
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.smallClass.rawValue)
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).document().unsubscribe(key: "")
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        if getRole(uid: UserInfo.uid) == .broadcaster {
            fastRoom.dismissAllSubPanels()
        }
        fastRoom.disconnectRoom()
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        AgoraRtcEngineKit.destroy()
    }
    
    private func setupUI() {
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        fastRoom.view.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        avatarview.translatesAutoresizingMaskIntoConstraints = false
        videoButton.translatesAutoresizingMaskIntoConstraints = false
        audioButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(fastRoom.view)
        view.addSubview(collectionView)
        view.addSubview(avatarview)
        view.addSubview(videoButton)
        view.addSubview(audioButton)
        view.addSubview(closeButton)
        
        collectionView.topAnchor.constraint(equalTo: view.topAnchor, constant: 10).isActive = true
        collectionView.heightAnchor.constraint(equalToConstant: 78).isActive = true
        collectionViewCons = collectionView.widthAnchor.constraint(equalToConstant: 78)
        collectionView.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        
        fastRoom.view.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        fastRoom.view.topAnchor.constraint(equalTo: collectionView.bottomAnchor, constant: 10).isActive = true
        fastRoom.view.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -10).isActive = true
        fastRoom.view.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        
        avatarview.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 5).isActive = true
        avatarview.topAnchor.constraint(equalTo: view.topAnchor, constant: 5).isActive = true
        
        audioButton.leadingAnchor.constraint(equalTo: fastRoom.view.leadingAnchor, constant: 15).isActive = true
        audioButton.bottomAnchor.constraint(equalTo: fastRoom.view.bottomAnchor, constant: -25).isActive = true
        
        videoButton.leadingAnchor.constraint(equalTo: audioButton.leadingAnchor).isActive = true
        videoButton.bottomAnchor.constraint(equalTo: audioButton.topAnchor, constant: -15).isActive = true
        
        closeButton.trailingAnchor.constraint(equalTo: fastRoom.view.trailingAnchor, constant: -15).isActive = true
        closeButton.bottomAnchor.constraint(equalTo: audioButton.bottomAnchor).isActive = true
        closeButton.widthAnchor.constraint(equalToConstant: 38).isActive = true
        closeButton.heightAnchor.constraint(equalToConstant: 38).isActive = true
    }
    
   private func getUserStatus() {
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).get(success: { results in
            let datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(AgoraUsersModel.self, value: $0 )})
                .sorted(by: { $0.timestamp < $1.timestamp })
            self.dataArray = datas
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    public func eventHandler() {
        SyncUtil.scene(id: channleName)?.subscribe(key: SYNC_SCENE_ROOM_USER_COLLECTION, onCreated: nil, onUpdated: { object in
            guard let userInfo = JSONObject.toModel(AgoraUsersModel.self, value: object.toJson()) else { return }
            if let index = self.dataArray.firstIndex(where: { $0.userId == userInfo.userId }) {
                self.dataArray[index] = userInfo
            } else {
                self.dataArray.append(userInfo)
            }
        }, onDeleted: { object in
            if let index = self.dataArray.firstIndex(where: { $0.objectId == object.getId() }) {
                self.dataArray.remove(at: index)
            }
        }, onSubscribed: nil, fail: { error in
            LogUtils.log(message: error.description, level: .error)
        })
        
        guard getRole(uid: UserInfo.uid) == .audience else { return }
        SyncUtil.scene(id: channleName)?.subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            
        }, onDeleted: { object in
            self.showAlert(title: "live_broadcast_over".localized, message: "") {
                self.navigationController?.popViewController(animated: true)
            }
        }, onSubscribed: {
            
        }, fail: { error in
            
        })
    }
    
    @objc
    private func onTapCloseLive() {
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                SyncUtil.scene(id: self?.channleName ?? "")?.deleteScenes()
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            navigationController?.popViewController(animated: true)
        }
    }
        
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        let roleOptions = AgoraClientRoleOptions()
        roleOptions.audienceLatencyLevel = getRole(uid: currentUserId) == .audience ? .ultraLowLatency : .lowLatency
        agoraKit?.setClientRole(getRole(uid: currentUserId), options: roleOptions)
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String, uid: UInt) {
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           uid: uid,
                                           mediaOptions: channelMediaOptions,
                                           joinSuccess: nil)
        if result == 0 {
            LogUtils.log(message: "进入房间", level: .info)
        }
        var model = AgoraUsersModel()
        model.isEnableAudio = true
        model.isEnableVideo = true
        dataArray.append(model)
        agoraKit?.startPreview()
        fastRoom.joinRoom()
                
        let params = JSONObject.toJson(model)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).add(data: params, success: { object in
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                self.getUserStatus()
            }
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func leaveChannel() {
        guard let model = dataArray.first(where: { $0.userId == UserInfo.uid }) else { return }
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .document(id: model.objectId ?? "")
            .delete(success: { objects in
                LogUtils.log(message: "\(objects.count)", level: .info)
            }, fail: { error in
                LogUtils.log(message: error.description, level: .error)
            })
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
    }
    
    @objc
    private func onTapVideoButton(sender: AGEButton) {
        guard let index = dataArray.firstIndex(where: { $0.userId == UserInfo.uid }) else { return }
        sender.isSelected = !sender.isSelected
        var model = dataArray[index]
        model.isEnableVideo = sender.isSelected
        dataArray[index] = model
        
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .document(id: model.objectId ?? "")
            .update(key: "", data: JSONObject.toJson(model), success: nil, fail: nil)
        
        let option = AgoraRtcChannelMediaOptions()
        option.publishCameraTrack = .of(sender.isSelected)
        agoraKit?.updateChannel(with: option)
    }
    @objc
    private func onTapAudioButton(sender: AGEButton) {
        guard let index = dataArray.firstIndex(where: { $0.userId == UserInfo.uid }) else { return }
        sender.isSelected = !sender.isSelected
        var model = dataArray[index]
        model.isEnableAudio = sender.isSelected
        dataArray[index] = model
        
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .document(id: model.objectId ?? "")
            .update(key: "", data: JSONObject.toJson(model), success: nil, fail: nil)
        
        let option = AgoraRtcChannelMediaOptions()
        option.publishMicrophoneTrack = .of(sender.isSelected)
        agoraKit?.updateChannel(with: option)
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension SmallClassController: AgoraRtcEngineDelegate {
    
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
//        var model = AgoraUsersModel()
//        model.userId = "\(uid)"
//        model.userName = "User-\(uid)"
//        model.isEnableAudio = true
//        model.isEnableVideo = true
//        dataArray.append(model)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
//        guard let index = dataArray.firstIndex(where: { $0.userId == "\(uid)" }) else { return }
//        dataArray.remove(at: index)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, reportRtcStats stats: AgoraChannelStats) {
//        localVideo.statsInfo?.updateChannelStats(stats)
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

extension SmallClassController: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: AgoraUserCollectionViewCell.description(),
                                                      for: indexPath) as! AgoraUserCollectionViewCell
        cell.avatariImageView.layer.cornerRadius = 10
        cell.canvasView.layer.cornerRadius = 10
        let model = dataArray[indexPath.item]
        let canvas = AgoraRtcVideoCanvas()
        canvas.renderMode = .hidden
        canvas.uid = UInt(model.userId) ?? 0
        canvas.view = cell.canvasView
        if model.userId == UserInfo.uid {
            agoraKit?.setupLocalVideo(canvas)
        } else {
            agoraKit?.setupRemoteVideo(canvas)
        }
        cell.setupData(model: model)
        return cell
    }

}

class AgoraUserCollectionViewCell: UICollectionViewCell {
    lazy var avatariImageView: AGEButton = {
        let imageView = AGEButton(style: .imageName(name: "portrait01"))
        imageView.cornerRadius = 10.fit
        imageView.contentMode = .scaleAspectFit
        imageView.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        imageView.isUserInteractionEnabled = false
        return imageView
    }()
    lazy var canvasView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 10.fit
        view.layer.masksToBounds = true
        return view
    }()
    private lazy var nameLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        return label
    }()
    private lazy var muteAudioImageView: AGEImageView = {
        let imageView = AGEImageView(systemName: "mic.slash", imageColor: .red)
        imageView.isHidden = true
        return imageView
    }()
    private lazy var muteVideoImageView: AGEImageView = {
        let imageView = AGEImageView(systemName: "video.slash", imageColor: .red)
        imageView.isHidden = true
        return imageView
    }()
    
    var defaultImageName: String?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupData(model: AgoraUsersModel) {
        avatariImageView.setImage(UIImage(named: model.avatar), for: .normal)
        muteVideoImageView.isHidden = model.isEnableVideo ?? false
        muteAudioImageView.isHidden = model.isEnableAudio ?? false
        canvasView.isHidden = model.isEnableVideo == false
        nameLabel.text = model.userName
    }
    
    func setupData(model: ClassUsersModel) {
        avatariImageView.setImage(UIImage(named: model.avatar), for: .normal)
        muteVideoImageView.isHidden = model.isEnableVideo ?? false
        muteAudioImageView.isHidden = model.isEnableAudio ?? false
        canvasView.isHidden = model.isEnableVideo == false
        nameLabel.text = model.userName
    }
    
    private func setupUI() {
        avatariImageView.translatesAutoresizingMaskIntoConstraints = false
        canvasView.translatesAutoresizingMaskIntoConstraints = false
        muteAudioImageView.translatesAutoresizingMaskIntoConstraints = false
        muteVideoImageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(avatariImageView)
        contentView.addSubview(canvasView)
        contentView.addSubview(nameLabel)
        contentView.addSubview(muteVideoImageView)
        contentView.addSubview(muteAudioImageView)
        avatariImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        avatariImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        avatariImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        avatariImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        canvasView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        canvasView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        canvasView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        canvasView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        nameLabel.leadingAnchor.constraint(equalTo: canvasView.leadingAnchor, constant: 5).isActive = true
        nameLabel.bottomAnchor.constraint(equalTo: canvasView.bottomAnchor, constant: -5).isActive = true
        
        muteVideoImageView.rightAnchor.constraint(equalTo: avatariImageView.rightAnchor, constant: -5).isActive = true
        muteVideoImageView.topAnchor.constraint(equalTo: avatariImageView.topAnchor, constant: 5).isActive = true
        muteVideoImageView.widthAnchor.constraint(equalToConstant: 12).isActive = true
        muteVideoImageView.heightAnchor.constraint(equalToConstant: 16).isActive = true
        muteAudioImageView.leadingAnchor.constraint(equalTo: avatariImageView.leadingAnchor, constant: 5).isActive = true
        muteAudioImageView.topAnchor.constraint(equalTo: avatariImageView.topAnchor, constant: 5).isActive = true
        muteAudioImageView.widthAnchor.constraint(equalTo: muteVideoImageView.widthAnchor).isActive = true
        muteAudioImageView.heightAnchor.constraint(equalTo: muteVideoImageView.heightAnchor).isActive = true
    }
}
