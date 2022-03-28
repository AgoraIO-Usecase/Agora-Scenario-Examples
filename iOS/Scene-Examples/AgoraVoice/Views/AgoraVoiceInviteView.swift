//
//  AgoraVoiceInviteView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/28.
//

import UIKit
import AgoraUIKit_iOS

class AgoraVoiceInviteView: UIView {
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Invite_PK".localized
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray
        return view
    }()
    private lazy var tableView: AGETableView = {
        let view = AGETableView()
        view.estimatedRowHeight = 44
        view.delegate = self
        view.emptyTitle = "暂时没有观众".localized
        view.emptyImage = nil
        view.register(AgoraVoiceInviteViewCell.self,
                      forCellWithReuseIdentifier: AgoraVoiceInviteViewCell.description())
        return view
    }()
    private var channelName: String = ""
    private var syncName: String = SYNC_MANAGER_AGORA_VOICE_USERS
    init(channelName: String, syncName: String = SYNC_MANAGER_AGORA_VOICE_USERS) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.syncName = syncName
        setupUI()
        fetchAgoraVoiceUserInfoData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func fetchAgoraVoiceUserInfoData() {
        SyncUtil.fetchCollection(id: channelName, className: syncName, success: { results in
            let datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(AgoraVoiceUsersModel.self, value: $0 )})
                .filter({ $0.userId != "\(UserInfo.userId)" && $0.status != .accept })
                .filterDuplicates({ $0.userId })
            self.tableView.dataArray = datas
        })
    }
    
    private func setupUI() {
        backgroundColor = .white
        translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        tableView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(tableView)
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        heightAnchor.constraint(equalToConstant: Screen.height - 100.fit).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        lineView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        tableView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: lineView.bottomAnchor).isActive = true
        tableView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}
extension AgoraVoiceInviteView: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: AgoraVoiceInviteViewCell.description(), for: indexPath) as! AgoraVoiceInviteViewCell
        cell.setUserInfoData(with: self.tableView.dataArray?[indexPath.row],
                             channelName: channelName,
                             syncName: syncName)
        return cell
    }
}

class AgoraVoiceInviteViewCell: UITableViewCell {
    private lazy var avatarImage: UIImageView = {
        let imageView = UIImageView(image: UIImage(systemName: "person.circle")?.withTintColor(.gray, renderingMode: .alwaysOriginal))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.cornerRadius = 20
        imageView.layer.masksToBounds = true
        return imageView
    }()
    private lazy var nameLabel: UILabel = {
        let label = UILabel()
        label.text = "简单点"
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var inviteButton: UIButton = {
        let button = UIButton()
        button.setTitle("Invite".localized, for: .normal)
        button.setTitle("Inviting".localized, for: .disabled)
        button.setTitleColor(.black, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14)
        button.layer.cornerRadius = 15
        button.layer.masksToBounds = true
        button.layer.borderColor = UIColor.blueColor.cgColor
        button.layer.borderWidth = 1
        button.addTarget(self, action: #selector(clickInviteButton(sender:)), for: .touchUpInside)
        return button
    }()
    public var currendModel: AgoraVoiceUsersModel?
    public var channelName: String = ""
    private var syncName: String = SYNC_MANAGER_AGORA_VOICE_USERS
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setUserInfoData(with item: Any?,
                         channelName: String,
                         syncName: String = SYNC_MANAGER_AGORA_VOICE_USERS) {
        guard let model = item as? AgoraVoiceUsersModel else { return }
        self.channelName = channelName
        self.syncName = syncName
        currendModel = model
        nameLabel.text = "User-\(model.userId)"
        avatarImage.image = UIImage(named: model.avatar)
    }
    
    private func setupUI() {
        avatarImage.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        inviteButton.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(avatarImage)
        contentView.addSubview(nameLabel)
        contentView.addSubview(inviteButton)
        
        avatarImage.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        avatarImage.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        avatarImage.heightAnchor.constraint(equalToConstant: 40).isActive = true
        avatarImage.widthAnchor.constraint(equalToConstant: 40).isActive = true
        
        nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        nameLabel.leadingAnchor.constraint(equalTo: avatarImage.trailingAnchor, constant: 15).isActive = true
        
        inviteButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        inviteButton.widthAnchor.constraint(equalToConstant: 60).isActive = true
        inviteButton.heightAnchor.constraint(equalToConstant: 30).isActive = true
        inviteButton.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10).isActive = true
        inviteButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -5).isActive = true
    }
    
    @objc
    private func clickInviteButton(sender: UIButton) {
        guard var model = currendModel else { return }
        sender.isEnabled = !sender.isEnabled
        model.status = .invite
        let params = JSONObject.toJson(model)
        SyncUtil.updateCollection(id: channelName,
                                  className: syncName,
                                  objectId: model.objectId ?? "",
                                  params: params)
        
        AlertManager.hiddenView()
    }
}
