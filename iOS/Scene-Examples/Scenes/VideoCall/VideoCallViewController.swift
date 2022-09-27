//
//  VideoCallViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/7/26.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

class VideoCallViewController: BaseViewController {
    private lazy var localContainerView: AGEView = {
        let view = AGEView()
        view.layer.shadowColor = UIColor.black.withAlphaComponent(0.6).cgColor
        view.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        view.layer.shadowRadius = 8.0
        view.layer.shadowOpacity = 1.0
        view.layer.borderColor = UIColor.white.cgColor
        view.layer.borderWidth = 1
        view.layer.cornerRadius = 8
        let tap = UITapGestureRecognizer(target: self, action: #selector(onTapLocalViewGesture(sender:)))
        let pan = UIPanGestureRecognizer(target: self, action: #selector(onPanLocalViewGesture(sender:)))
        view.addGestureRecognizer(tap)
        view.addGestureRecognizer(pan)
        return view
    }()
    private lazy var localView: AGEView = {
        let view = AGEView()
        view.backgroundColor = .gray
        view.layer.cornerRadius = 8
        view.layer.masksToBounds = true
        return view
    }()
    private lazy var remoteView: AGEView = {
        let view = AGEView()
        return view
    }()
    private lazy var switchCamera: AGEButton = {
        let button = AGEButton(style: .switchCamera(imageColor: .white), colorStyle: .white)
        button.addTarget(self, action: #selector(onTapSwitchCamera(sender:)), for: .touchUpInside)
        button.cornerRadius = 28
        button.backgroundColor = .black.withAlphaComponent(0.6)
        return button
    }()
    private lazy var closButton: AGEButton = {
        let button = AGEButton(style: .imageName(name: "VideoCall/Hangup"), colorStyle: .white)
        button.addTarget(self, action: #selector(onTapCloseButton(sender:)), for: .touchUpInside)
        button.cornerRadius = 28
        button.backgroundColor = .red
        return button
    }()
    private lazy var micButton: AGEButton = {
        let button = AGEButton(style: .mic(imageColor: .white), colorStyle: .white)
        button.addTarget(self, action: #selector(onTapMicButton(sender:)), for: .touchUpInside)
        button.cornerRadius = 28
        button.backgroundColor = .black.withAlphaComponent(0.6)
        return button
    }()
    private lazy var timeLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "00:00"
        return label
    }()
    private var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        option.publishLocalAudio = true
        option.publishLocalVideo = true
        return option
    }()
    private lazy var localCanvas: AgoraRtcVideoCanvas = {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = UserInfo.userId
        canvas.renderMode = .hidden
        canvas.view = localView
        return canvas
    }()
    private lazy var remoteCanvas: AgoraRtcVideoCanvas = {
        let canvas = AgoraRtcVideoCanvas()
        canvas.renderMode = .hidden
        canvas.view = remoteView
        return canvas
    }()
    private var isChangeView: Bool = false
    private var timer = GCDTimer()
    
    private var userObjectId: String = ""
    private(set) var channleName: String = ""
    private(set) var currentUserId: String = ""
    
    /// 用户角色
    public func getRole(uid: String) -> AgoraClientRole {
        uid == currentUserId ? .broadcaster : .audience
    }
    
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
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: true)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = currentUserId != "\(UserInfo.userId)"
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is VideoCallCreateViewController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        leaveChannel(uid: UserInfo.userId, channelName: channleName, isExit: true)
        if !userObjectId.isEmpty {
            SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).delete(id: userObjectId, success: nil, fail: nil)
        }
        if getRole(uid: UserInfo.uid) == .broadcaster {
            SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).delete(success: nil, fail: nil)
        }
        SyncUtil.scene(id: channleName)?.unsubscribe(key: SceneType.singleLive.rawValue)
        SyncUtil.leaveScene(id: channleName)
        navigationTransparent(isTransparent: false)
        UIApplication.shared.isIdleTimerDisabled = false
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        AgoraRtcEngineKit.destroy()
    }
    
    private func eventHandler() {
        SyncUtil.scene(id: channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).get(success: { list in
            if list.count > 1 {
                self.showAlert(title: "房间人数已满", message: "") { [weak self] in
                    self?.navigationController?.popViewController(animated: true)
                }
                return
            }
            // 添加用户
            SyncUtil.scene(id: self.channleName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).add(data: JSONObject.toJson(AgoraUsersModel()), success: { object in
                self.userObjectId = object.getId()
            }, fail: nil)
            self.joinChannel(channelName: self.channleName, uid: UserInfo.userId)
        }, fail: { error in
            Log.error(error: error.message, tag: "")
        })

        guard getRole(uid: UserInfo.uid) == .audience else { return }
        SyncUtil.scene(id: channleName)?.subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            
        }, onDeleted: { [weak self] object in
            self?.showAlert(title: "end_of_call".localized, message: "") {
                self?.navigationController?.popViewController(animated: true)
            }
        }, onSubscribed: {
            
        }, fail: { error in
            
        })
    }
    
    private func onTapCloseLive() {
        if getRole(uid: UserInfo.uid) == .broadcaster {
            showAlert(title: "end_of_call".localized, message: "end_the_call".localized) { [weak self] in
                SyncUtil.scene(id: self?.channleName ?? "")?.deleteScenes()
                self?.navigationController?.popViewController(animated: true)
            }

        } else {
            navigationController?.popViewController(animated: true)
        }
    }
        
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setChannelProfile(.liveBroadcasting)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.enableVideo()
        agoraKit?.enableAudio()
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String, uid: UInt) {
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channelName,
                                           info: nil,
                                           uid: UserInfo.userId,
                                           options: channelMediaOptions)
        if result == 0 {
            LogUtils.log(message: "进入房间", level: .info)
        }
        agoraKit?.setupLocalVideo(localCanvas)
        agoraKit?.startPreview()
    }
    
    private func leaveChannel(uid: UInt, channelName: String, isExit: Bool = false) {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
    }
    
    deinit {
        LogUtils.log(message: "释放 === \(self)", level: .info)
    }
    
    private func setupUI() {
        view.addSubview(remoteView)
        view.addSubview(localContainerView)
        localContainerView.addSubview(localView)
        view.addSubview(switchCamera)
        view.addSubview(closButton)
        view.addSubview(micButton)
        view.addSubview(timeLabel)
        localContainerView.translatesAutoresizingMaskIntoConstraints = false
        remoteView.translatesAutoresizingMaskIntoConstraints = false
        localView.translatesAutoresizingMaskIntoConstraints = false
        switchCamera.translatesAutoresizingMaskIntoConstraints = false
        closButton.translatesAutoresizingMaskIntoConstraints = false
        micButton.translatesAutoresizingMaskIntoConstraints = false
        timeLabel.translatesAutoresizingMaskIntoConstraints = false
        
        
        remoteView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        remoteView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        remoteView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        remoteView.bottomAnchor.constraint(equalTo:  view.bottomAnchor).isActive = true
        localView.leadingAnchor.constraint(equalTo: localContainerView.leadingAnchor).isActive = true
        localView.topAnchor.constraint(equalTo: localContainerView.topAnchor).isActive = true
        localView.trailingAnchor.constraint(equalTo: localContainerView.trailingAnchor).isActive = true
        localView.bottomAnchor.constraint(equalTo: localContainerView.bottomAnchor).isActive = true
        localContainerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -10).isActive = true
        localContainerView.topAnchor.constraint(equalTo: view.topAnchor, constant: Screen.kNavHeight).isActive = true
        localContainerView.widthAnchor.constraint(equalToConstant: 84).isActive = true
        localContainerView.heightAnchor.constraint(equalToConstant: 128).isActive = true
        closButton.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        closButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -(Screen.safeAreaBottomHeight() + 80)).isActive = true
        closButton.widthAnchor.constraint(equalToConstant: 56).isActive = true
        closButton.heightAnchor.constraint(equalToConstant: 56).isActive = true
        switchCamera.centerYAnchor.constraint(equalTo: closButton.centerYAnchor).isActive = true
        switchCamera.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 40).isActive = true
        switchCamera.widthAnchor.constraint(equalToConstant: 56).isActive = true
        switchCamera.heightAnchor.constraint(equalToConstant: 56).isActive = true
        micButton.centerYAnchor.constraint(equalTo: closButton.centerYAnchor).isActive = true
        micButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -40).isActive = true
        micButton.widthAnchor.constraint(equalToConstant: 56).isActive = true
        micButton.heightAnchor.constraint(equalToConstant: 56).isActive = true
        timeLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        timeLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: Screen.statusHeight() + 20).isActive = true
    }
    
    @objc
    private func onTapSwitchCamera(sender: UIButton) {
        agoraKit?.switchCamera()
    }
    @objc
    private func onTapCloseButton(sender: UIButton) {
        onTapCloseLive()
    }
    @objc
    private func onTapMicButton(sender: AGEButton) {
        sender.isSelected = !sender.isSelected
        sender.buttonStyle = sender.isSelected ? .muteMic(imageColor: .white) : .mic(imageColor: .white)
        agoraKit?.muteLocalVideoStream(sender.isSelected)
    }
    @objc
    private func onTapLocalViewGesture(sender: UITapGestureRecognizer) {
        isChangeView = !isChangeView
        if isChangeView {
            localCanvas.view = remoteView
            remoteCanvas.view = localView
        } else {
            localCanvas.view = localView
            remoteCanvas.view = remoteView
        }
        agoraKit?.setupLocalVideo(localCanvas)
        agoraKit?.setupRemoteVideo(remoteCanvas)
    }
    @objc
    private func onPanLocalViewGesture(sender: UIPanGestureRecognizer) {
        let point = sender.location(in: view)
        localContainerView.center = point
        
        switch sender.state {
        case .cancelled, .ended, .failed:
            let minX = localContainerView.frame.minX
            let maxX = localContainerView.frame.maxX
            let minY = localContainerView.frame.minY
            let maxY = localContainerView.frame.maxY
            let halfW = view.frame.width * 0.5
            if minX <= halfW {
                UIView.animate(withDuration: 0.25) {
                    self.localContainerView.frame.origin.x = 15.fit
                }
            } else if maxX > halfW {
                UIView.animate(withDuration: 0.25) {
                    self.localContainerView.frame.origin.x = self.view.frame.maxX - self.localContainerView.frame.width - 15.fit
                }
            }
            if minY < view.frame.origin.y + 20.fit {
                UIView.animate(withDuration: 0.25) {
                    self.localContainerView.frame.origin.y = self.view.frame.origin.y + 20.fit
                }
            } else if maxY > view.frame.height - localContainerView.frame.height - 140.fit {
                UIView.animate(withDuration: 0.25) {
                    self.localContainerView.frame.origin.y = self.view.frame.height - self.localContainerView.frame.height - 140.fit
                }
            }
            
        default: break
        }
    }
}
extension VideoCallViewController: AgoraRtcEngineDelegate {
    
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
        remoteCanvas.uid = uid
        agoraKit?.setupRemoteVideo(remoteCanvas)
        timer.scheduledSecondsTimer(withName: "videoCall", timeInterval: 1, queue: .main) { [weak self] _, duration in
            guard let self = self else { return }
            self.timeLabel.text = "".timeFormat(secounds: duration)
        }
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
        timeLabel.text = "00:00"
        timer.destoryAllTimer()
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
