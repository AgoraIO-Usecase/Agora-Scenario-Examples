//
//  LivePlayerController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit

class LivePlayerController: BaseViewController {
    enum LiveLayoutPostion {
        case full, center, bottom
    }
    public lazy var liveCanvasView: BaseCollectionViewLayout = {
        let view = BaseCollectionViewLayout()
        view.itemSize = CGSize(width: Screen.width, height: Screen.height)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 0
        view.delegate = self
        view.scrollDirection = .vertical
        view.showsVerticalScrollIndicator = false
        view.isUserInteractionEnabled = false
        view.register(LivePlayerCell.self,
                      forCellWithReuseIdentifier: LivePlayerCell.description())
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
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
        option.autoSubscribeAudio = .of(true)
        option.autoSubscribeVideo = .of(true)
        return option
    }()
    /// 顶部头像昵称
    public lazy var avatarview = LiveAvatarView()
    /// 聊天
    public lazy var chatView = LiveChatView()
    /// 设置直播的工具弹窗
    public lazy var liveToolView = LiveToolView()
    /// 礼物
    private lazy var giftView = LiveGiftView()
    public lazy var playGifView: GIFImageView = {
        let view = GIFImageView()
        view.isHidden = true
        return view
    }()
    /// 底部功能
    public lazy var bottomView: LiveBottomView = {
        let view = LiveBottomView(type: [.gift, .tool, .close])
        return view
    }()
    
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    public var canvasDataArray = [LiveCanvasModel]()
    private(set) var sceneType: SceneType = .singleLive
    private var canvasLeadingConstraint: NSLayoutConstraint?
    private var canvasTopConstraint: NSLayoutConstraint?
    private var canvasTrailingConstraint: NSLayoutConstraint?
    private var canvasBottomConstraint: NSLayoutConstraint?
    private(set) var liveCanvasViewHeight: CGFloat = 0
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
    init(channelName: String, sceneType: SceneType, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channleName = channelName
        self.sceneType = sceneType
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
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true)
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
        guard let index = controllers.firstIndex(where: { $0 is CreateLiveController }) else { return }
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
        SyncUtil.unsubscribe(id: channleName, key: sceneType.rawValue)
        SyncUtil.unsubscribe(id: channleName, key: SYNC_MANAGER_GIFT_INFO)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        liveCanvasView.translatesAutoresizingMaskIntoConstraints = false
        avatarview.translatesAutoresizingMaskIntoConstraints = false
        chatView.translatesAutoresizingMaskIntoConstraints = false
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        playGifView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(liveCanvasView)
        view.addSubview(avatarview)
        view.addSubview(chatView)
        view.addSubview(bottomView)
        view.addSubview(playGifView)
        
        canvasLeadingConstraint = liveCanvasView.leadingAnchor.constraint(equalTo: view.leadingAnchor)
        canvasTopConstraint = liveCanvasView.topAnchor.constraint(equalTo: view.topAnchor, constant: -Screen.kNavHeight)
        canvasBottomConstraint = liveCanvasView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        canvasTrailingConstraint = liveCanvasView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        canvasTopConstraint?.isActive = true
        canvasBottomConstraint?.isActive = true
        canvasLeadingConstraint?.isActive = true
        canvasTrailingConstraint?.isActive = true
        
        avatarview.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        avatarview.topAnchor.constraint(equalTo: view.topAnchor, constant: Screen.statusHeight() + 15).isActive = true
        
        chatView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        chatView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -60).isActive = true
        let chatViewW = view.frame.width / 2 * 0.8
        chatView.widthAnchor.constraint(equalToConstant: chatViewW).isActive = true
        chatView.heightAnchor.constraint(equalToConstant: chatViewW).isActive = true
        
        bottomView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        bottomView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        bottomView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        playGifView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        playGifView.topAnchor.constraint(equalTo: view.topAnchor, constant: -Screen.kNavHeight).isActive = true
        playGifView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        playGifView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        avatarview.setName(with: currentUserId + " roomId: " + channleName)
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == "\(UserInfo.userId)" ? [.tool, .close] : [.gift, .close]
        bottomView.updateButtonType(type: bottomType)
    }
    
    public func eventHandler() {
        // gif播放完成回调
        playGifView.gifAnimationFinishedClosure = { [weak self] in
            guard let self = self else { return }
            self.playGifView.isHidden = true
        }
        // 聊天发送
        bottomView.clickChatButtonClosure = { [weak self] message in
            guard let self = self else { return }
            self.chatView.sendMessage(message: message)
        }
        // 底部功能回调
        bottomView.clickBottomButtonTypeClosure = { [weak self] type in
            guard let self = self else { return }
            switch type {
            case .close:
                self.clickCloseLive()
                
            case .tool:
                self.liveToolView.clickItemClosure = { itemType, isSelected in
                    switch itemType {
                    case .switch_camera:
                        self.agoraKit?.switchCamera()
                    case .camera:
                        self.agoraKit?.muteLocalVideoStream(isSelected)
                        self.liveCanvasView.isHidden = isSelected
                        if isSelected {
                            self.liveCanvasView.emptyImage = nil
                            self.liveCanvasView.emptyTitle = "已暂停推流"
                        } else {
                            self.agoraKit?.startPreview()
                        }
                    case .mic:
                        self.agoraKit?.muteLocalAudioStream(isSelected)
                    }
                }
                AlertManager.show(view: self.liveToolView, alertPostion: .bottom)
            
            case .gift:
                self.giftView.clickGiftItemClosure = { giftModel in
                    LogUtils.log(message: "gif == \(giftModel.gifName)", level: .info)
                    let params = JSONObject.toJson(giftModel)
                    /// 发送礼物
                    SyncUtil.update(id: self.channleName,
                                    key: SYNC_MANAGER_GIFT_INFO,
                                    params: params,
                                    delegate: nil)
                }
                AlertManager.show(view: self.giftView, alertPostion: .bottom)
            case .pk:
                self.clickPKHandler()
                
            case .game:
                self.clickGamePKHandler()
                
            case .exitgame:
                self.exitGameHandler()
            }
        }
        
        // 监听礼物
        SyncUtil.subscribe(id: channleName, key: SYNC_MANAGER_GIFT_INFO, delegate: LiveGiftDelegate(vc: self, type: .me))
        
        // 收到礼物
        LiveReceivedGiftClosure = { [weak self] giftModel, type in
            self?.receiveGiftHandler(giftModel: giftModel, type: type)
        }
    }
    
    private func clickCloseLive() {
        if getRole(uid: "\(UserInfo.userId)") == .broadcaster {
            showAlert(title: "Live_End".localized, message: "Confirm_End_Live".localized) { [weak self] in
                self?.closeLiveHandler()
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            closeLiveHandler()
            navigationController?.popViewController(animated: true)
        }
    }
    
    /// 关闭直播
    public func closeLiveHandler() {
        SyncUtil.delete(id: channleName)
    }
    
    /// 主播PK
    public func clickPKHandler() {}
    
    /// 游戏PK
    public func clickGamePKHandler() {}
    
    /// 退出游戏
    public func exitGameHandler() { }
    
    /// 收到礼物
    public func receiveGiftHandler(giftModel: LiveGiftModel, type: PKLiveType) {}
    
    /// 更新直播布局
    public func updateLiveLayout(postion: LiveLayoutPostion) {
        var leading: CGFloat = 0
        var top: CGFloat = -Screen.kNavHeight
        var bottom: CGFloat = Screen.safeAreaBottomHeight()
        var trailing: CGFloat = 0
        var itemWidth: CGFloat = Screen.width
        var itemHeight: CGFloat = Screen.height
        switch postion {
        case .bottom:
            let viewW = Screen.width
            itemWidth = (viewW - 150 - 15) / 2
            itemHeight = viewW / 2 * 0.7
            let topMargin = view.frame.height - itemHeight - 78
            leading = 150
            top = topMargin
            bottom = -70
            trailing = -15
        case .center:
            let viewW = Screen.width
            itemWidth = viewW / 2
            itemHeight = viewW / 2 * 1.2
            top = Screen.kNavHeight + 40
        default: break
        }
        liveCanvasViewHeight = top + itemHeight
        canvasLeadingConstraint?.constant = leading
        canvasTopConstraint?.constant = top
        canvasBottomConstraint?.constant = bottom
        canvasTrailingConstraint?.constant = trailing
        UIView.animate(withDuration: 0.5) {
            self.canvasTopConstraint?.isActive = true
            self.canvasBottomConstraint?.isActive = true
            self.canvasTrailingConstraint?.isActive = true
            self.canvasLeadingConstraint?.isActive = true
            self.liveCanvasView.itemSize = CGSize(width: itemWidth,
                                                  height: itemHeight)
        }
    }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(getRole(uid: currentUserId))
        if getRole(uid: currentUserId) == .broadcaster {
            agoraKit?.enableVideo()
        }
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    /// 主播加入channel
    public func joinBroadcasterChannel(channelName: String, uid: UInt) {
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: uid)
        let connection = LiveCanvasModel.createConnection(channelName: channelName, uid: uid)
        canvasModel.connection = connection
        canvasModel.channelName = channelName
        canvasDataArray.append(canvasModel)
        liveCanvasView.dataArray = canvasDataArray
        
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
        chatView.sendMessage(message: "\(UserInfo.userId)加入房间")
    }
    
    /// 观众加入channel
    /// - Parameters:
    ///   - channelName: 频道名
    ///   - pkUid: pk主播的UserID
    public func joinAudienceChannel(channelName: String, pkUid: UInt = 0) {
        
        let isContainer = canvasDataArray.contains(where: { $0.channelName == channelName && $0.canvas?.uid == pkUid })
        guard !isContainer else {
            LogUtils.log(message: "当前用户存在 channelName == \(channelName) pkUid == \(pkUid)", level: .warning)
            let value = canvasDataArray.map({ "channleName == \($0.channelName) userId == \($0.connection?.localUid ?? 0)" })
            LogUtils.log(message: "所有用户 \(value))", level: .warning)
            liveCanvasView.reloadData()
            return
        }
        
        let canvasModel = LiveCanvasModel()
        canvasModel.canvas = LiveCanvasModel.createCanvas(uid: pkUid)
        let connection = LiveCanvasModel.createConnection(channelName: channelName, uid: UserInfo.userId)
        canvasModel.connection = connection
        canvasModel.channelName = channelName
        canvasDataArray.append(canvasModel)
        liveCanvasView.dataArray = canvasDataArray
        
        channelMediaOptions.clientRoleType = .of((Int32)(AgoraClientRole.audience.rawValue))
        let joinResult = agoraKit?.joinChannelEx(byToken: KeyCenter.Token, connection: connection, delegate: self, mediaOptions: channelMediaOptions, joinSuccess: nil) ?? -1000
        if joinResult == 0 {
            LogUtils.log(message: "join audience success uid == \(pkUid) channelName == \(channelName)", level: .info)
            let userId = pkUid == (UInt(currentUserId) ?? 0) ? UserInfo.userId : pkUid
            chatView.sendMessage(message: "\(userId)加入房间")
            return
        }
        LogUtils.log(message: "join audience error uid == \(pkUid) channelName == \(channelName)", level: .error)
    }
    public func leaveChannel(uid: UInt, channelName: String, isExit: Bool = false) {
        if isExit && "\(uid)" == currentUserId {
            canvasDataArray.forEach({
                if let connection = $0.connection {
                    agoraKit?.leaveChannelEx(connection, leaveChannelBlock: nil)
                }
            })
        }
        guard canvasDataArray.count > 1 else { return }
        if let connection = canvasDataArray.filter({ $0.connection?.localUid == uid && $0.channelName == channelName }).first?.connection {
            agoraKit?.leaveChannelEx(connection, leaveChannelBlock: { state in
                LogUtils.log(message: "left channel: \(connection.channelId) uid == \(connection.localUid)",
                             level: .info)
            })
        }
        if let connectionIndex = canvasDataArray.firstIndex(where: { $0.connection?.localUid == uid && $0.channelName == channelName }) {
            canvasDataArray.remove(at: connectionIndex)
            liveCanvasView.dataArray = canvasDataArray
        }
    }
    
    public func didOfflineOfUid(uid: UInt) {
        let index = canvasDataArray.firstIndex(where: { $0.connection?.localUid == uid && $0.channelName != channleName }) ?? -1
        if index > -1 && canvasDataArray.count > 1 {
            canvasDataArray.remove(at: index)
            liveCanvasView.dataArray = canvasDataArray
        }
        guard "\(uid)" != currentUserId else { return }
        chatView.sendMessage(message: "\(uid)离开房间")
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}

extension LivePlayerController: BaseCollectionViewLayoutDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LivePlayerCell.description(),
                                                      for: indexPath) as! LivePlayerCell
        if indexPath.item >= canvasDataArray.count {
            return cell
        }
        let model = canvasDataArray[indexPath.item]
        cell.setupPlayerCanvas(with: model)
        
        if indexPath.item == 0 && currentUserId == "\(UserInfo.userId)" {// 本房间主播
            agoraKit?.setupLocalVideo(model.canvas)
            agoraKit?.startPreview()
            
        } else { // 观众
            if let connection = model.connection, let canvas = model.canvas {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    self.agoraKit?.setupRemoteVideoEx(canvas, connection: connection)
                }
            }
        }
        return cell
    }
}

extension LivePlayerController: AgoraRtcEngineDelegate {
    
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
