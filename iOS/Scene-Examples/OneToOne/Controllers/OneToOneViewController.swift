//
//  OneToOneViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/21.
//

import UIKit
import AgoraRtcKit
import AgoraUIKit

class OneToOneViewController: BaseViewController {
    public lazy var localView = AGEView()
    public lazy var remoteView: AGEButton = {
        let button = AGEButton()
        button.setTitle("远程视频", for: .normal)
        button.cornerRadius = 5
        button.shadowOffset = CGSize(width: 0, height: 2)
        button.shadowColor = .init(hex: "#000000", alpha: 0.3)
        button.shadowRadius = 5
        button.buttonStyle = .filled(backgroundColor: .gray)
        return button
    }()
    private lazy var containerView: AGEView = {
        let view = AGEView()
        return view
    }()
    private lazy var controlView: OneToOneControlView = OneToOneControlView()
    private lazy var webView: GameWebView = {
        let webView = GameWebView()
        webView.isHidden = true
        return webView
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
        option.publishAudioTrack = .of(true)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        return option
    }()
    
    public lazy var viewModel = GameViewModel(channleName: channelName,
                                              ownerId: UserInfo.uid)
    
    private(set) var channelName: String = ""
    public var canvasDataArray = [LiveCanvasModel]()
    private(set) var sceneType: SceneType = .singleLive
    private var roleType: GameRoleType {
        currentUserId == UserInfo.uid ? .broadcast : .audience
    }
    private(set) var currentUserId: String = ""
    
    init(channelName: String, sceneType: SceneType, userId: String, agoraKit: AgoraRtcEngineKit? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channelName = channelName
        self.currentUserId = userId
        self.sceneType = sceneType
        self.agoraKit = agoraKit
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
        joinChannel(channelName: channelName)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
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
        leaveChannel()
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    private func eventHandler() {
        controlView.onClickControlButtonClosure = { [weak self] type, isSelected in
            guard let self = self else { return }
            switch type {
            case .switchCamera:
                self.agoraKit?.switchCamera()
                
            case .game:
                self.clickGameHandler()
            
            case .mic:
                self.agoraKit?.muteLocalAudioStream(isSelected)
                
            case .exit:
                self.showAlert(title: "退出游戏", message: "确定退出退出游戏 ？") {
                    self.controlView.updateControlUI(types: [.switchCamera, .game, .mic])
                    self.showOrHiddenControlView(isHidden: false)
                    AlertManager.hiddenView()
                    self.viewModel.leaveGame(roleType: self.roleType)
                }
                
            case .close:
                self.showAlert(title: "关闭直播间", message: "关闭直播间后，其他用户将不能再和您连线。确定关闭 ？") {
                    self.navigationController?.popViewController(animated: true)
                }
            }
        }
    }
    
    private func clickGameHandler() {
        let gameCenterView = GameCenterView()
        gameCenterView.didGameCenterItemClosure = { [weak self] gameCenterModel in
            guard let self = self else { return }
            self.webView.loadUrl(urlString: gameCenterModel.type.gameUrl, roomId: self.channelName, roleType: self.roleType)
            self.webView.isHidden = false
            AlertManager.show(view: self.webView, alertPostion: .bottom, didCoverDismiss: false, controller: self)
            self.showOrHiddenControlView(isHidden: true)
            self.controlView.updateControlUI(types: [.switchCamera, .game, .mic, .exit])
        }
        AlertManager.show(view: gameCenterView, alertPostion: .bottom)
    }
    
    private func showOrHiddenControlView(isHidden: Bool) {
        UIView.animate(withDuration: 0.25) {
            self.controlView.alpha = isHidden ? 0.0 : 1.0
        }
    }
    @objc
    private func clickTapView() {
        showOrHiddenControlView(isHidden: controlView.alpha == 1.0)
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: UIView())
        containerView.translatesAutoresizingMaskIntoConstraints = false
        localView.translatesAutoresizingMaskIntoConstraints = false
        remoteView.translatesAutoresizingMaskIntoConstraints = false
        controlView.translatesAutoresizingMaskIntoConstraints = false
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(localView)
        view.addSubview(containerView)
        containerView.addSubview(webView)
        containerView.addSubview(remoteView)
        containerView.addSubview(controlView)
        
        containerView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        containerView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        containerView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        containerView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        webView.widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        webView.heightAnchor.constraint(equalToConstant: Screen.height - 152.fit).isActive = true
        
        localView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        localView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        localView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        localView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        remoteView.trailingAnchor.constraint(equalTo: localView.trailingAnchor, constant: -15).isActive = true
        remoteView.topAnchor.constraint(equalTo: localView.safeAreaLayoutGuide.topAnchor, constant: 15).isActive = true
        remoteView.widthAnchor.constraint(equalToConstant: 105.fit).isActive = true
        remoteView.heightAnchor.constraint(equalToConstant: 140.fit).isActive = true
        
        controlView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        controlView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        controlView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        let tap = UITapGestureRecognizer(target: self, action: #selector(clickTapView))
        containerView.addGestureRecognizer(tap)
    }
    
    private func setupAgoraKit() {
        guard agoraKit == nil else { return }
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.enableVideo()
        agoraKit?.setVideoEncoderConfiguration(
            AgoraVideoEncoderConfiguration(size: CGSize(width: 320, height: 240),
                                           frameRate: .fps30,
                                           bitrate: AgoraVideoBitrateStandard,
                                           orientationMode: .fixedPortrait,
                                           mirrorMode: .auto))
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func createAgoraVideoCanvas(uid: UInt, isLocal: Bool = false) {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.renderMode = .hidden
        if isLocal {
            canvas.view = localView
            agoraKit?.setupLocalVideo(canvas)
        } else {
            canvas.view = remoteView
            agoraKit?.setupRemoteVideo(canvas)
        }
        agoraKit?.startPreview()
    }
    
    public func joinChannel(channelName: String) {
        self.channelName = channelName
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           uid: UserInfo.userId,
                                           mediaOptions: channelMediaOptions)
        guard result != 0 else { return }
        // Error code description can be found at:
        // en: https://docs.agora.io/en/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        // cn: https://docs.agora.io/cn/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        self.showAlert(title: "Error", message: "joinChannel call failed: \(String(describing: result)), please check your params")
    }
    public func leaveChannel() {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "left channel, duration: \(state.duration)", level: .info)
        })
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
}
extension OneToOneViewController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        LogUtils.log(message: "warning: \(warningCode.description)", level: .warning)
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        LogUtils.log(message: "error: \(errorCode)", level: .error)
        showAlert(title: "Error", message: "Error \(errorCode.description) occur")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "Join \(channel) with uid \(uid) elapsed \(elapsed)ms", level: .info)
        createAgoraVideoCanvas(uid: uid, isLocal: true)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "remote user join: \(uid) \(elapsed)ms", level: .info)
        createAgoraVideoCanvas(uid: uid, isLocal: false)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
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
