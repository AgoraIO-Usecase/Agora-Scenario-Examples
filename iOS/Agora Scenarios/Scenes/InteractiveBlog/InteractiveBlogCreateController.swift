//
//  CreateLiveController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import AgoraRtcKit

class InteractiveBlogCreateController: BaseViewController {
    private lazy var randomNameView: LiveRandomNameView = {
        let view = LiveRandomNameView()
        return view
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
 
    private var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
       let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
       let option = AgoraRtcChannelMediaOptions()
        option.publishLocalAudio = true
        option.publishLocalVideo = false
        option.autoSubscribeVideo = false
        option.autoSubscribeAudio = true
        return option
    }()
            
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
                
        ToastView.show(text: "Limit_Toast".localized,
                       tagImage: UIImage(named: "icon-yellow-caution"),
                       postion: .bottom,
                       view: view)
    }
    
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: nil)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.setChannelProfile(.liveBroadcasting)
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
        agoraKit?.enableAudio()
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
        let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
        
        NetworkManager.shared.generateToken(channelName: channelName ?? "", uid: UserInfo.userId) {
            let livePlayerVC = InteractiveBlogController(channelName: channelName ?? "",
                                                         userId: "\(UserInfo.userId)",
                                                         agoraKit: self.agoraKit)
            self.navigationController?.pushViewController(livePlayerVC, animated: true)
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}
