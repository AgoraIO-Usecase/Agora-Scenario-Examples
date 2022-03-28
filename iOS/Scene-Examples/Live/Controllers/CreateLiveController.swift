//
//  CreateLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import AgoraRtcKit
import AgoraSyncManager

class CreateLiveController: BaseViewController {
    private lazy var randomNameView: LiveRandomNameView = {
        let view = LiveRandomNameView()
        return view
    }()
    private lazy var localView: UIView = {
        let view = UIView()
        view.isHidden = sceneType == .agoraVoice
        return view
    }()
    private lazy var cameraChangeButton: UIButton = {
        let button = UIButton()
        let imageName = sceneType == .agoraVoice ? "icon-BG" : "icon-camera-change"
        button.setImage(UIImage(named: imageName), for: .normal)
        button.addTarget(self, action: #selector(clickCameraChangeButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var settingButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "icon-setting-normal"), for: .normal)
        button.setImage(UIImage(named: "icon-setting"), for: .selected)
        button.addTarget(self, action: #selector(clickSettingLiveButton(sender:)), for: .touchUpInside)
        button.isHidden = sceneType == .agoraVoice
        return button
    }()
    private lazy var startLiveButton: UIButton = {
        let button = UIButton()
        button.setTitle("Create_Start".localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14)
        button.backgroundColor = .blueColor
        button.layer.cornerRadius = 20
        button.layer.masksToBounds = true
        button.addTarget(self, action: #selector(clickStartLiveButton), for: .touchUpInside)
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
        option.publishAudioTrack = .of(true)
        option.publishCameraTrack = .of(true)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        option.autoSubscribeVideo = .of(true)
        option.autoSubscribeAudio = .of(true)
        return option
    }()
    private var liveSettingModel: LiveSettingUseData?
    private var sceneType: SceneType = .singleLive
    private var bgImageName: String = "BG01"
    
    init(sceneType: SceneType) {
        super.init(nibName: nil, bundle: nil)
        self.sceneType = sceneType
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupAgoraKit()
        setupUI()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationTransparent(isTransparent: false)
    }
    
    private func setupUI() {
        view.backgroundColor = .init(hex: "#404B54")
        randomNameView.translatesAutoresizingMaskIntoConstraints = false
        startLiveButton.translatesAutoresizingMaskIntoConstraints = false
        settingButton.translatesAutoresizingMaskIntoConstraints = false
        localView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(localView)
        view.addSubview(randomNameView)
        view.addSubview(startLiveButton)
        view.addSubview(settingButton)
        
        localView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        localView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        localView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        localView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        randomNameView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 10).isActive = true
        randomNameView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20).isActive = true
        randomNameView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -10).isActive = true
        randomNameView.heightAnchor.constraint(equalToConstant: 40).isActive = true
        
        startLiveButton.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        startLiveButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -35).isActive = true
        startLiveButton.widthAnchor.constraint(equalToConstant: 100).isActive = true
        startLiveButton.heightAnchor.constraint(equalToConstant: 40).isActive = true
        
        settingButton.centerYAnchor.constraint(equalTo: startLiveButton.centerYAnchor).isActive = true
        settingButton.leadingAnchor.constraint(equalTo: startLiveButton.trailingAnchor, constant: 25).isActive = true
        
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: cameraChangeButton)
        
        ToastView.show(text: "Limit_Toast".localized,
                       tagImage: UIImage(named: "icon-yellow-caution"),
                       postion: .bottom,
                       view: view)
        
        if sceneType == .agoraVoice {
            view.layer.contents = UIImage(named: "BG01")?.cgImage
        }
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
        
        if sceneType == .agoraVoice {
            agoraKit?.enableAudio()
            return
        }
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
    private func clickCameraChangeButton(sender: UIButton) {
        if sceneType == .agoraVoice {
            AlertManager.show(view: changeRoomView, alertPostion: .bottom)
            return
        }
        agoraKit?.switchCamera()
    }
    @objc
    private func clickSettingLiveButton(sender: UIButton) {
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
    private func clickStartLiveButton() {
        var roomInfo = LiveRoomInfo(roomName: randomNameView.text)
        if sceneType == .agoraVoice {
            roomInfo.backgroundId = bgImageName            
        }
        let params = JSONObject.toJson(roomInfo)
        SyncUtil.joinScene(id: roomInfo.roomId, userId: roomInfo.userId, property: params, success: { result in
            self.startLiveHandler(result: result)
        })
    }
    
    private func startLiveHandler(result: IObject) {
        LogUtils.log(message: "result == \(result.toJson() ?? "")", level: .info)
        let roomInfo = JSONObject.toModel(LiveRoomInfo.self, value: result.toJson())
        let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
        
        switch sceneType {
        case .singleLive:
            let livePlayerVC = SignleLiveController(channelName: channelName ?? "",
                                                    sceneType: sceneType,
                                                    userId: "\(UserInfo.userId)",
                                                    agoraKit: agoraKit)
            navigationController?.pushViewController(livePlayerVC, animated: true)
            
        case .pkApply:
            let pkLiveVC = PKLiveController(channelName: channelName ?? "",
                                            sceneType: sceneType,
                                            userId: "\(UserInfo.userId)",
                                            agoraKit: agoraKit)
            navigationController?.pushViewController(pkLiveVC, animated: true)
            
        case .game:
            let dgLiveVC = GameLiveController(channelName: channelName ?? "",
                                            sceneType: sceneType,
                                            userId: "\(UserInfo.userId)",
                                            agoraKit: agoraKit)
            navigationController?.pushViewController(dgLiveVC, animated: true)
            
        case .playTogether:
            let playTogetherVC = PlayTogetherViewController(channelName: channelName ?? "",
                                                            sceneType: sceneType,
                                                            userId: "\(UserInfo.userId)",
                                                            agoraKit: agoraKit)
            navigationController?.pushViewController(playTogetherVC, animated: true)
            
        case .oneToOne:
            let oneToOneVC = OneToOneViewController(channelName: channelName ?? "",
                                                    sceneType: sceneType,
                                                    userId: UserInfo.uid,
                                                    agoraKit: agoraKit)
            navigationController?.pushViewController(oneToOneVC, animated: true)
            
        case .agoraVoice:
            let agoraVoiceVC = AgoraVoiceController(roomInfo: roomInfo,
                                                    agoraKit: agoraKit)
            navigationController?.pushViewController(agoraVoiceVC, animated: true)
            
        case .agoraClub:
            let clubVC = AgoraClubController(userId: UserInfo.uid, channelName: channelName, agoraKit: agoraKit)
            navigationController?.pushViewController(clubVC, animated: true)
            
        default: break
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}
