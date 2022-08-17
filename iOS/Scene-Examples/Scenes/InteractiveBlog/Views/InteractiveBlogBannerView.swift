//
//  InteractiveBlogBannerView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/8/16.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

class InteractiveBlogBannerView: UIButton {
    var onTapInteractiveBlogBannerViewClosure: ((String) -> Void)?
    var onTapInteractiveBlogExitClosure: (() -> Void)?
    
    private lazy var avatarImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "portrait06")
        imageView.cornerRadius = 18
        return imageView
    }()
    private lazy var onlineButton: AGEButton = {
        let button = AGEButton(style: .none, colorStyle: .white, fontStyle: .small)
        button.setImage(UIImage(named: "InteractiveBlog/blueBroadcaster"), for: .normal, postion: .right)
        button.setTitle("0/0", for: .normal)
        return button
    }()
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .horizontal
        stackView.distribution = .fill
        stackView.spacing = 10
        return stackView
    }()
    private lazy var exitButton: AGEButton = {
        let button = AGEButton(style: .filled(backgroundColor: UIColor(hex: "#364151")),
                               colorStyle: .white,
                               fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconExit"), for: .normal)
        button.addTarget(self, action: #selector(onTapExitButton), for: .touchUpInside)
        button.cornerRadius = 18
        return button
    }()
    private lazy var raiseHandButton: AGEButton = {
        let button = AGEButton(style: .filled(backgroundColor: UIColor(hex: "#e1b66a")),
                               colorStyle: .white,
                               fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconBlueHandsUp"), for: .normal)
        button.addTarget(self, action: #selector(onTapRaiseHandtButton), for: .touchUpInside)
        button.cornerRadius = 18
        button.isHidden = clientRole == .broadcaster
        return button
    }()
    private lazy var raiseListButton: AGEButton = {
        let button = AGEButton(style: .filled(backgroundColor: UIColor(hex: "#364151")),
                               colorStyle: .white,
                               fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconUpNotice"), for: .normal)
        button.addTarget(self, action: #selector(onTapRaiseListButton), for: .touchUpInside)
        button.cornerRadius = 18
        button.isHidden = clientRole == .audience
        return button
    }()
    private lazy var micButton: AGEButton = {
        let button = AGEButton(style: .filled(backgroundColor: UIColor(hex: "#364151")),
                               colorStyle: .white,
                               fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconMicOn"), for: .normal)
        button.setImage(UIImage(named: "InteractiveBlog/redMic"), for: .selected)
        button.addTarget(self, action: #selector(onTapMicButton(sender:)), for: .touchUpInside)
        button.cornerRadius = 18
        button.isHidden = currentUserModel?.status != .accept
        button.isSelected = currentUserModel?.isEnableAudio ?? false
        return button
    }()
    
    private var channelName: String = ""
    private var currentUserModel: InteractiveBlogUsersModel?
    private var agoraKit: AgoraRtcEngineKit?
    private var clientRole: AgoraClientRole = .broadcaster
   
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
   
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupParams(channelName: String, model: InteractiveBlogUsersModel?, agoraKit: AgoraRtcEngineKit?, role: AgoraClientRole) {
        self.channelName = channelName
        self.clientRole = role
        self.currentUserModel = model
        self.agoraKit = agoraKit
        fetchUsers()
    }
    
    func leave() {
        if clientRole == .broadcaster {
            leaveChannel()
            SyncUtil.scene(id: channelName)?.delete(success: nil, fail: nil)
            SyncUtil.leaveScene(id: channelName)
        } else {
            leaveChannel()
            SyncUtil.leaveScene(id: channelName)
        }
    }
    
    func checkRoom(channelName: String) {
        if channelName != self.channelName {
            leave()
            isHidden = true
        }
    }
    
    private func fetchUsers() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).get(success: { list in
            let users = list.compactMap({ JSONObject.toModel(InteractiveBlogUsersModel.self, value: $0.toJson()) }).sorted(by: { $0.timestamp < $1.timestamp })
            let boradcastList = users.filter({ $0.status == .accept })
            let audienceList = users.filter({ $0.status != .accept })
            self.onlineButton.setTitle("\(boradcastList.count)/\(audienceList.count)", for: .normal)
        }, fail: { error in
            LogUtils.log(message: error.message, level: .error)
        })
    }
    
    private func setupUI() {
        layer.cornerRadius = 28
        layer.masksToBounds = true
        heightAnchor.constraint(equalToConstant: 56).isActive = true
        backgroundColor = UIColor(hex: "#4e586a")
        addTarget(self, action: #selector(onTapSelfHandler), for: .touchUpInside)
        
        addSubview(avatarImageView)
        addSubview(onlineButton)
        addSubview(statckView)
        statckView.addArrangedSubview(exitButton)
        statckView.addArrangedSubview(raiseHandButton)
        statckView.addArrangedSubview(raiseListButton)
        statckView.addArrangedSubview(micButton)
        
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        onlineButton.translatesAutoresizingMaskIntoConstraints = false
        statckView.translatesAutoresizingMaskIntoConstraints = false
        
        avatarImageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        avatarImageView.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        avatarImageView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 36).isActive = true
        
        onlineButton.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 15).isActive = true
        onlineButton.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        statckView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        statckView.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        exitButton.widthAnchor.constraint(equalToConstant: 36).isActive = true
        exitButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        raiseHandButton.widthAnchor.constraint(equalTo: exitButton.widthAnchor).isActive = true
        raiseHandButton.heightAnchor.constraint(equalTo: exitButton.heightAnchor).isActive = true
        
        raiseListButton.widthAnchor.constraint(equalTo: exitButton.widthAnchor).isActive = true
        raiseListButton.heightAnchor.constraint(equalTo: exitButton.heightAnchor).isActive = true
        
        micButton.widthAnchor.constraint(equalTo: exitButton.widthAnchor).isActive = true
        micButton.heightAnchor.constraint(equalTo: exitButton.heightAnchor).isActive = true
    }
    
    private func leaveChannel() {
        agoraKit?.disableAudio()
        agoraKit?.disableVideo()
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).delete(id: currentUserModel?.objectId ?? "", success: nil, fail: nil)
        SyncUtil.scene(id: channelName)?.unsubscribe(key: SceneType.interactiveBlog.rawValue)
        SyncUtil.scene(id: channelName)?.unsubscribe(key: SYNC_MANAGER_AGORA_VOICE_USERS)
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "leave channel: \(state)", level: .info)
        })
    }
    
    @objc
    private func onTapSelfHandler() {
        isHidden = true
        onTapInteractiveBlogBannerViewClosure?(channelName)
    }
    
    @objc
    private func onTapExitButton() {
        if clientRole == .broadcaster {
            let controller = UIApplication.topMostViewController
            let alert = UIAlertController(title: "Live_End".localized, message: "Confirm_End_Live".localized, preferredStyle: .alert)
            let sure = UIAlertAction(title: "Confirm".localized, style: .default) { [weak self] _ in
                self?.leaveChannel()
                SyncUtil.scene(id: self?.channelName ?? "")?.delete(success: nil, fail: nil)
                SyncUtil.leaveScene(id: self?.channelName ?? "")
                self?.onTapInteractiveBlogExitClosure?()
            }
            let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
            alert.addAction(sure)
            alert.addAction(cancel)
            controller?.present(alert, animated: true, completion: nil)
        } else {
            leaveChannel()
            SyncUtil.leaveScene(id: channelName)
            onTapInteractiveBlogExitClosure?()
        }
    }
    @objc
    private func onTapRaiseHandtButton() {
        currentUserModel?.status = .request
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .update(id: currentUserModel?.objectId ?? "", data: params, success: nil, fail: nil)
        ToastView.show(text: "Request received. Please wait ...".localized)
    }
    @objc
    private func onTapRaiseListButton() {
        let view = InteractiveBlogRaiseListView(channelName: channelName)
        AlertManager.show(view: view, alertPostion: .bottom, didCoverDismiss: true)
    }
    @objc
    private func onTapMicButton(sender: AGEButton) {
        sender.isSelected = !sender.isSelected
        currentUserModel?.isEnableAudio = !sender.isSelected
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: currentUserModel?.objectId ?? "", data: params, success: nil, fail: nil)
    }
}
