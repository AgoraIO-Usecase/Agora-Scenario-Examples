//
//  PKLiveInviteView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit
import AgoraUIKit_iOS

class PKLiveInviteView: UIView {
    var pkInviteSubscribe: ((String) -> Void)?
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
    private lazy var tableViewLayout: AGETableView = {
        let view = AGETableView()
        view.estimatedRowHeight = 44
        view.delegate = self
        view.emptyTitle = "暂时没有主播"
        view.emptyImage = nil
        view.register(PKLiveInviteViewCell.self,
                      forCellWithReuseIdentifier: PKLiveInviteViewCell.description())
        return view
    }()
    private var channelName: String = ""
    private var sceneType: SceneType = .singleLive
    
    init(channelName: String, sceneType: SceneType) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.sceneType = sceneType
        setupUI()
        fetchPKInfoData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func fetchPKInfoData() {
        SyncUtil.fetchAll(success: { results in
            let datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(LiveRoomInfo.self, value: $0 )})
                .filter({ $0.userId != "\(UserInfo.userId)"})
            self.tableViewLayout.dataArray = datas
        })
    }
    
    private func setupUI() {
        backgroundColor = .white
        translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        tableViewLayout.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(tableViewLayout)
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
        
        tableViewLayout.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableViewLayout.topAnchor.constraint(equalTo: lineView.bottomAnchor).isActive = true
        tableViewLayout.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableViewLayout.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}

extension PKLiveInviteView: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: PKLiveInviteViewCell.description(), for: indexPath) as! PKLiveInviteViewCell
        cell.setPKInfoData(with: tableViewLayout.dataArray?[indexPath.row],
                           channelName: channelName,
                           sceneType: sceneType)
        cell.pkInviteSubscribe = pkInviteSubscribe
        return cell
    }
}

class PKLiveInviteViewCell: UITableViewCell {
    var pkInviteSubscribe: ((String) -> Void)?
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
    public var currendModel: LiveRoomInfo?
    public var channelName: String = ""
    public var sceneType: SceneType = .singleLive
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setPKInfoData(with item: Any?, channelName: String, sceneType: SceneType) {
        guard let model = item as? LiveRoomInfo else { return }
        self.channelName = channelName
        self.sceneType = sceneType
        currendModel = model
        nameLabel.text = "User-\(model.userId)"
        avatarImage.image = UIImage(named: model.backgroundId)
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
        guard let model = currendModel else { return }
        sender.isEnabled = !sender.isEnabled
        
        // 加入要pk主播的channel
        SyncUtil.joinScene(id: model.roomId, userId: model.userId, property: JSONObject.toJson(model), success: { result in
            let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
            SyncUtil.fetch(id: channelName ?? "", key: SceneType.pkApply.rawValue) { object in
                let pkApplyInfo = JSONObject.toModel(PKApplyInfoModel.self, value: object?.toJson())
                if pkApplyInfo == nil {
                    self.pkApplyInfoHandler(channelName: self.currendModel?.roomId ?? "")
                } else {
                    if pkApplyInfo?.status == .accept {
                        ToastView.show(text: "PK_Invite_Fail".localized, duration: 3)
                    } else {
                        self.pkApplyInfoHandler(channelName: self.currendModel?.roomId ?? "")
                    }
                }
            } fail: { error in
                if error.code == -1 {
                    self.pkApplyInfoHandler(channelName: self.currendModel?.roomId ?? "")
                }
            }

        })
        AlertManager.hiddenView()
    }
    
    private func pkApplyInfoHandler(channelName: String) {
        pkInviteSubscribe?(channelName)
        var pkModel = PKApplyInfoModel()
        pkModel.roomId = self.channelName
        pkModel.targetRoomId = channelName
        pkModel.targetUserId = currendModel?.userId ?? ""
        pkModel.status = .invite
        let params = JSONObject.toJson(pkModel)
        SyncUtil.update(id: channelName, key: sceneType.rawValue, params: params)
    }
}
