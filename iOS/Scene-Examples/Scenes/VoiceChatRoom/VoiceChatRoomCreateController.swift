//
//  CreateLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import AgoraRtcKit
//import AgoraSyncManager

class VoiceChatRoomCreateController: BaseViewController {
    private lazy var randomNameView: LiveRandomNameView = {
        let view = LiveRandomNameView()
        return view
    }()
    private lazy var cameraChangeButton: UIButton = {
        let button = UIButton()
        let imageName = "icon-BG"
        button.setImage(UIImage(named: imageName), for: .normal)
        button.addTarget(self, action: #selector(onTapCameraChangeButton(sender:)), for: .touchUpInside)
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
        view.layer.contents = UIImage(named: "BG01")?.cgImage
        randomNameView.translatesAutoresizingMaskIntoConstraints = false
        startLiveButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(randomNameView)
        view.addSubview(startLiveButton)
                
        randomNameView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 38).isActive = true
        randomNameView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 93).isActive = true
        randomNameView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -38).isActive = true
        randomNameView.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        startLiveButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 38).isActive = true
        startLiveButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -38).isActive = true
        startLiveButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -42).isActive = true
        startLiveButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        navigationItem.rightBarButtonItems = [UIBarButtonItem(customView: cameraChangeButton)]
        
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
        
        agoraKit?.enableAudio()
    }
    
    @objc
    private func onTapCameraChangeButton(sender: UIButton) {
        AlertManager.show(view: changeRoomView, alertPostion: .bottom)
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
        var roomInfo = LiveRoomInfo(roomName: randomNameView.text)
        roomInfo.backgroundId = bgImageName
        let params = JSONObject.toJson(roomInfo)
        SyncUtil.joinScene(id: roomInfo.roomId, userId: roomInfo.userId, property: params, success: { result in
            self.startLiveHandler(result: result)
        })
    }
    
    private func startLiveHandler(result: IObject) {
        LogUtils.log(message: "result == \(result.toJson() ?? "")", level: .info)
        let roomInfo = JSONObject.toModel(LiveRoomInfo.self, value: result.toJson())
        let agoraVoiceVC = VoiceChatRoomController(roomInfo: roomInfo,
                                                   agoraKit: agoraKit)
        navigationController?.pushViewController(agoraVoiceVC, animated: true)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}
