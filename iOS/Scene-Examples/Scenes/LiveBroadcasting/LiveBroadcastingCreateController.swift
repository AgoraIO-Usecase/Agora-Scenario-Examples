//
//  CreateLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import AgoraRtcKit
//import AgoraSyncManager

class LiveBroadcastingCreateController: BaseViewController {
    private lazy var randomNameView: LiveRandomNameView = {
        let view = LiveRandomNameView()
        return view
    }()
    private lazy var localView: UIView = {
        let view = UIView()
        return view
    }()
    private lazy var cameraChangeButton: UIButton = {
        let button = UIButton()
        let imageName = "icon-camera-change"
        button.setImage(UIImage(named: imageName), for: .normal)
        button.addTarget(self, action: #selector(onTapCameraChangeButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var settingButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "create_setting"), for: .normal)
        button.addTarget(self, action: #selector(onTapSettingLiveButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var startLiveButton: UIButton = {
        let button = UIButton()
        button.setTitle("Create_Start".localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16)
        button.backgroundColor = .blueColor
        button.layer.cornerRadius = 8
        button.layer.masksToBounds = true
        button.addTarget(self, action: #selector(onTapStartLiveButton), for: .touchUpInside)
        return button
    }()
    private lazy var changeRoomView: ChangeRoomBgView = {
        let view = ChangeRoomBgView()
        view.didSelectedBgImageClosure = { [weak self] imageNmae in
            self?.bgImageName = imageNmae
            self?.view.layer.contents = UIImage(named: imageNmae)?.cgImage
        }
        return view
    }()
    
    private var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
       let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.channelProfile = .liveBroadcasting
        config.areaCode = .global
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
       let option = AgoraRtcChannelMediaOptions()
        option.publishMicrophoneTrack = .of(true)
        option.publishCameraTrack = .of(true)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        option.autoSubscribeVideo = .of(true)
        option.autoSubscribeAudio = .of(true)
        return option
    }()
    private var liveSettingModel: LiveSettingUseData?
    private var bgImageName: String = "BG01"
            
    override func viewDidLoad() {
        super.viewDidLoad()
        setupAgoraKit()
        setupUI()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: false)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationTransparent(isTransparent: false, isHiddenNavBar: false)
    }
    
    private func setupUI() {
        backButton.setImage(UIImage(systemName: "chevron.backward")?
                                .withTintColor(.white, renderingMode: .alwaysOriginal),
                            for: .normal)
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: backButton)
        view.backgroundColor = .init(hex: "#404B54")
        randomNameView.translatesAutoresizingMaskIntoConstraints = false
        startLiveButton.translatesAutoresizingMaskIntoConstraints = false
        localView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(localView)
        view.addSubview(randomNameView)
        view.addSubview(startLiveButton)
        
        localView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        localView.topAnchor.constraint(equalTo: view.topAnchor, constant: -Screen.kNavHeight).isActive = true
        localView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        localView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        randomNameView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 38).isActive = true
        randomNameView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 93).isActive = true
        randomNameView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -38).isActive = true
        randomNameView.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        startLiveButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 38).isActive = true
        startLiveButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -38).isActive = true
        startLiveButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -42).isActive = true
        startLiveButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        navigationItem.rightBarButtonItems = [UIBarButtonItem(customView: cameraChangeButton), UIBarButtonItem(customView: settingButton)]
        
        ToastView.show(text: "Limit_Toast".localized,
                       tagImage: UIImage(named: "icon-yellow-caution"),
                       postion: .bottom,
                       view: view)
    }
    
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: nil)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.setVideoEncoderConfiguration(
            AgoraVideoEncoderConfiguration(size: CGSize(width: 320, height: 240),
                                           frameRate: .fps30,
                                           bitrate: AgoraVideoBitrateStandard,
                                           orientationMode: .fixedPortrait,
                                           mirrorMode: .auto))
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)

        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = UserInfo.userId
        canvas.renderMode = .hidden
        canvas.view = localView
        agoraKit?.setupLocalVideo(canvas)
        agoraKit?.enableAudio()
        agoraKit?.enableVideo()
        agoraKit?.startPreview()
    }
    
    @objc
    private func onTapCameraChangeButton(sender: UIButton) {
        agoraKit?.switchCamera()
    }
    @objc
    private func onTapSettingLiveButton(sender: UIButton) {
        let settingView = LiveSettingView(title: "Live_Room_Settings".localized,
                                          datas: LiveSettingModel.settingsData(),
                                          useModel: liveSettingModel)
        settingView.liveSettingFinishedClosure = { [weak self] model in
            guard let self = self else { return }
            self.liveSettingModel = model
            self.agoraKit?.setVideoEncoderConfiguration(
                AgoraVideoEncoderConfiguration(size: model.resolution,
                                               frameRate: model.framedate,
                                               bitrate: model.sliderValue,
                                               orientationMode: .fixedPortrait,
                                               mirrorMode: .auto))
        }
        AlertManager.show(view: settingView,
                          alertPostion: .bottom)
    }
    @objc
    private func onTapStartLiveButton() {
        let roomInfo = LiveRoomInfo(roomName: randomNameView.text)
        let params = JSONObject.toJson(roomInfo)
        SyncUtil.joinScene(id: roomInfo.roomId, userId: roomInfo.userId, property: params, success: { result in
            self.startLiveHandler(result: result)
        })
    }
    
    private func startLiveHandler(result: IObject) {
        LogUtils.log(message: "result == \(result.toJson() ?? "")", level: .info)
        let roomInfo = JSONObject.toModel(LiveRoomInfo.self, value: result.toJson())
        let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
        
        let livePlayerVC = LiveBroadcastingController(channelName: channelName ?? "",
                                                      userId: "\(UserInfo.userId)",
                                                      agoraKit: agoraKit)
        navigationController?.pushViewController(livePlayerVC, animated: true)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}
