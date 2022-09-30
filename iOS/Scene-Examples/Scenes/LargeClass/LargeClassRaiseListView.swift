//
//  LargeClassRaiseListView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/9/30.
//

import UIKit
import Agora_Scene_Utils

class LargeClassRaiseListView: UIView {
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .large)
        label.text = "Raised hands".localized
        return label
    }()
    private lazy var tableView: AGETableView = {
        let view = AGETableView()
        view.rowHeight = 70
        view.delegate = self
        view.register(LargeViewRaiseListViewCell.self,
                      forCellWithReuseIdentifier: LargeViewRaiseListViewCell.description())
        view.emptyTitle = ""
        view.backgroundColor = .clear
        return view
    }()
    private var channelName: String = ""
    private var dataArray: [ClassUsersModel] = [] {
        didSet {
            tableView.dataArray = dataArray
        }
    }
    
    init(channelName: String) {
        super.init(frame: .zero)
        self.channelName = channelName
        setupUI()
        fetchUsers()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func fetchUsers() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).get(success: { list in
            let users = list.compactMap({ JSONObject.toModel(ClassUsersModel.self, value: $0.toJson()) }).filter({ $0.status == .request })
            self.dataArray = users
        }, fail: { error in
            LogUtils.log(message: error.message, level: .error)
        })
    }
    
    private func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 15
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        
        addSubview(titleLabel)
        addSubview(tableView)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        heightAnchor.constraint(equalToConstant: Screen.height * 0.8).isActive = true
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        tableView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        tableView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}
extension LargeClassRaiseListView: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: LargeViewRaiseListViewCell.description(), for: indexPath) as! LargeViewRaiseListViewCell
        cell.setUserInfo(model: dataArray[indexPath.row], channelName: channelName)
        cell.refreshUIClosure = { [weak self] in
            self?.fetchUsers()
        }
        return cell
    }
}

class LargeViewRaiseListViewCell: UITableViewCell {
    var refreshUIClosure: (() -> Void)?
    
    private lazy var avatarImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "portrait04")
        imageView.cornerRadius = 25
        return imageView
    }()
    private lazy var nameLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .middle)
        label.text = "User-123"
        return label
    }()
    private lazy var refuseButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .black, fontStyle: .middle)
        button.setTitle("Reject".localized, for: .normal)
        button.addTarget(self, action: #selector(onTapRefuseButton), for: .touchUpInside)
        button.cornerRadius = 18
        return button
    }()
    private lazy var agreeButton: AGEButton = {
        let button = AGEButton(style: .filled(backgroundColor: UIColor(hex: "#d0a564")), colorStyle: .black, fontStyle: .middle)
        button.setTitle("Agree".localized, for: .normal)
        button.addTarget(self, action: #selector(onTapAgreeButton), for: .touchUpInside)
        button.cornerRadius = 18
        return button
    }()
    
    private var channelName: String = ""
    private var currentUserModel: ClassUsersModel?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setUserInfo(model: ClassUsersModel, channelName: String) {
        currentUserModel = model
        self.channelName = channelName
        avatarImageView.image = UIImage(named: model.avatar)
        nameLabel.text = model.userName
    }
    
    private func setupUI() {
        backgroundColor = .clear
        contentView.addSubview(avatarImageView)
        contentView.addSubview(nameLabel)
        contentView.addSubview(refuseButton)
        contentView.addSubview(agreeButton)
        
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        refuseButton.translatesAutoresizingMaskIntoConstraints = false
        agreeButton.translatesAutoresizingMaskIntoConstraints = false
        
        avatarImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        avatarImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 50).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 50).isActive = true
        
        nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        nameLabel.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 10).isActive = true
        
        agreeButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        agreeButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        agreeButton.widthAnchor.constraint(equalToConstant: 60).isActive = true
        agreeButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        refuseButton.trailingAnchor.constraint(equalTo: agreeButton.leadingAnchor, constant: -10).isActive = true
        refuseButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        refuseButton.widthAnchor.constraint(equalTo: agreeButton.widthAnchor).isActive = true
        refuseButton.heightAnchor.constraint(equalTo: agreeButton.heightAnchor).isActive = true
    }
    
    @objc
    private func onTapRefuseButton() {
        currentUserModel?.status = .refuse
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?
            .collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .update(id: currentUserModel?.objectId ?? "", data: params, success: {
                self.refreshUIClosure?()
            }, fail: nil)
        
    }
    
    @objc
    private func onTapAgreeButton() {
        currentUserModel?.status = .accept
        let params = JSONObject.toJson(currentUserModel)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .update(id: currentUserModel?.objectId ?? "", data: params, success: {
                self.refreshUIClosure?()
            }, fail: nil)
    }
}
