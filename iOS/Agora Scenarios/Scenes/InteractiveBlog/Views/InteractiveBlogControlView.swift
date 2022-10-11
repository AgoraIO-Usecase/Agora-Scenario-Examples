//
//  InteractiveBlogControlView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/8/16.
//

import UIKit
import Agora_Scene_Utils

enum UserRole: Int {
    case boradcast
    case audience
}

class InteractiveBlogControlView: UIView {
    private lazy var avatarImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "portrait05")
        imageView.cornerRadius = 40
        return imageView
    }()
    private lazy var nameLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .large)
        label.text = "asfaf"
        return label
    }()
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .vertical
        stackView.distribution = .fill
        stackView.spacing = 15
        return stackView
    }()
    private lazy var invitationButton: AGEButton = {
        let button = AGEButton(style: .filled(backgroundColor: UIColor(hex: "#F7AF32")), colorStyle: .white, fontStyle: .middle)
        button.setTitle("Invite to speak".localized, for: .normal)
        button.cornerRadius = 20
        button.addTarget(self, action: #selector(onTapInviteButton), for: .touchUpInside)
        button.isHidden = userRole == .boradcast
        return button
    }()
    private lazy var downButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .white, fontStyle: .middle)
        button.setTitle("Become audience".localized, for: .normal)
        button.cornerRadius = 20
        button.addTarget(self, action: #selector(onTapDownButton), for: .touchUpInside)
        button.isHidden = userRole == .audience
        return button
    }()
    private lazy var micButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .white, fontStyle: .middle)
        button.setTitle("Turn off mic".localized, for: .normal)
        button.setTitle("Turn on mic".localized, for: .selected)
        button.cornerRadius = 20
        button.addTarget(self, action: #selector(onTapMicButton(sender:)), for: .touchUpInside)
        button.isHidden = userRole == .audience
        button.isSelected = currentUserModel?.isEnableAudio == false
        return button
    }()
    private var channelName: String = ""
    private var currentUserModel: InteractiveBlogUsersModel?
    private var userRole: UserRole = .boradcast
    
    init(channelName: String, model: InteractiveBlogUsersModel?, userRole: UserRole) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.currentUserModel = model
        self.userRole = userRole
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = UIColor(hex: "#222c3d")
        layer.cornerRadius = 15
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        
        addSubview(avatarImageView)
        addSubview(nameLabel)
        addSubview(statckView)
        statckView.addArrangedSubview(invitationButton)
        statckView.addArrangedSubview(downButton)
        statckView.addArrangedSubview(micButton)
        
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        statckView.translatesAutoresizingMaskIntoConstraints = false
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        avatarImageView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        avatarImageView.topAnchor.constraint(equalTo: topAnchor, constant: 30).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 80).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 80).isActive = true
        
        nameLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        nameLabel.topAnchor.constraint(equalTo: avatarImageView.bottomAnchor, constant: 15).isActive = true
        
        statckView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        statckView.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 15).isActive = true
        statckView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -25).isActive = true
        
        downButton.heightAnchor.constraint(equalToConstant: 40).isActive = true
        downButton.widthAnchor.constraint(equalToConstant: 160).isActive = true
        
        micButton.widthAnchor.constraint(equalTo: downButton.widthAnchor).isActive = true
        micButton.heightAnchor.constraint(equalTo: downButton.heightAnchor).isActive = true
        
        invitationButton.widthAnchor.constraint(equalTo: downButton.widthAnchor).isActive = true
        invitationButton.heightAnchor.constraint(equalTo: downButton.heightAnchor).isActive = true
    }
    
    @objc
    private func onTapInviteButton() {
        currentUserModel?.status = .invite
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: currentUserModel?.objectId ?? "", data: params, success: nil, fail: nil)
        AlertManager.hiddenView()
    }
    
    @objc
    private func onTapDownButton() {
        currentUserModel?.status = .end
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: currentUserModel?.objectId ?? "", data: params, success: nil, fail: nil)
        AlertManager.hiddenView()
    }
    @objc
    private func onTapMicButton(sender: AGEButton) {
        sender.isSelected = !sender.isSelected
        currentUserModel?.isEnableAudio = !sender.isSelected
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: currentUserModel?.objectId ?? "", data: params, success: nil, fail: nil)
        AlertManager.hiddenView()
    }
}
