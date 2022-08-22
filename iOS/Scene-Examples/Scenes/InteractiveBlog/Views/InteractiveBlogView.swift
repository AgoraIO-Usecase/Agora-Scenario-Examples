//
//  InteractiveBlogView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/8/15.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

enum InteractiveBlogStatus: Int, Codable {
    /// 请求上麦
    case request = 0
    /// 邀请中
    case invite = 1
    ///  已接受
    case accept = 2
    /// 已拒绝
    case refuse = 3
    /// 已结束
    case end = 4
}

struct InteractiveBlogUsersModel: Codable {
    var userName: String = "User-\(UserInfo.uid)"
    var avatar: String = String(format: "portrait%02d", Int.random(in: 1...14))
    var userId: String = UserInfo.uid
    var status: InteractiveBlogStatus? = .end
    var timestamp: String = "".timeStamp16
    var isEnableAudio: Bool? = false
    var objectId: String?
}


class InteractiveBlogView: UIView {
    var onTapDownButtonClosure: ((InteractiveBlogUsersModel?) -> Void)?
    var onTapCloseButtonClosure: (() -> Void)?
    var onTapLeaveButtonClosure: (() -> Void)?
    var enableAudioClosure: ((Bool) -> Void)?
    
    private lazy var downButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "InteractiveBlog/down"), for: .normal)
        button.backgroundColor = UIColor(hex: "#222c3d")
        button.shadowColor = .black
        button.shadowOffset = CGSize(width: 0, height: 0)
        button.shadowRadius = 10
        button.shadowOpacity = 0.9
        button.layer.cornerRadius = 10
        button.addTarget(self, action: #selector(onTapDownButton), for: .touchUpInside)
        return button
    }()
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .large)
        label.text = "1111"
        return label
    }()
    private lazy var closeButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "InteractiveBlog/iconPowerDown"), for: .normal)
        button.addTarget(self, action: #selector(onTapCloseButton), for: .touchUpInside)
        button.isHidden = true
        return button
    }()
    private lazy var leaveButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .white, fontStyle: .middle)
        button.setTitle("Leave quietly".localized, for: .normal)
        button.setImage(UIImage(named: "InteractiveBlog/iconExit"), for: .normal, postion: .left, spacing: 0)
        button.contentEdgeInsets = UIEdgeInsets(top: 0, left: 5, bottom: 0, right: 10)
        button.addTarget(self, action: #selector(onTapLeaveButton), for: .touchUpInside)
        button.cornerRadius = 18
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
    private lazy var raiseHandButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .white, fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconHandsUp"), for: .normal)
        button.addTarget(self, action: #selector(onTapRaiseHandButton), for: .touchUpInside)
        button.cornerRadius = 18
        button.isHidden = role == .broadcaster
        return button
    }()
    private lazy var numberLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.backgroundColor = .red
        label.text = "0"
        label.textInsets = UIEdgeInsets(top: 0, left: 5, bottom: 0, right: 5)
        label.cornerRadius = 8.5
        label.isHidden = true
        return label
    }()
    private lazy var raiseListButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .white, fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconUpNotice"), for: .normal)
        button.addTarget(self, action: #selector(onTapRaiseListButton), for: .touchUpInside)
        button.layer.cornerRadius = 18
        button.isHidden = role == .audience
        return button
    }()
    private lazy var micButton: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray), colorStyle: .white, fontStyle: .middle)
        button.setImage(UIImage(named: "InteractiveBlog/iconMicOn"), for: .normal)
        button.setImage(UIImage(named: "InteractiveBlog/redMic"), for: .selected)
        button.addTarget(self, action: #selector(onTapMicButton(sender:)), for: .touchUpInside)
        button.cornerRadius = 18
        button.isHidden = role == .audience
        return button
    }()
    private lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.minInteritemSpacing = 15
        view.minLineSpacing = 15
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        view.delegate = self
        view.scrollDirection = .vertical
        view.register(InteractiveBlogBoardcastCell.self,
                      forCellWithReuseIdentifier: InteractiveBlogBoardcastCell.description())
        view.register(InteractiveBlogAudienceCell.self, forCellWithReuseIdentifier: InteractiveBlogAudienceCell.description())
        view.register(UICollectionReusableView.self, forSupplementaryViewOfKind: UICollectionView.elementKindSectionFooter, withReuseIdentifier: "headerView")
        view.register(InteractiveBlogAudienceHeaderView.self,
                      forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
                      withReuseIdentifier: InteractiveBlogAudienceHeaderView.description())
        return view
    }()
    
    private var channelName: String = ""
    private var role: AgoraClientRole = .broadcaster
    private var currentUserModel: InteractiveBlogUsersModel?
    
    private var dataArray: [[InteractiveBlogUsersModel]] = [] {
        didSet {
            collectionView.dataArray = dataArray
        }
    }
    
    init(channelName: String, role: AgoraClientRole, isAddUser: Bool) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.role = role
        setupUI()
        subscribeUserEvent()
        if isAddUser {
            addUser()
        } else {
            DispatchQueue.global().asyncAfter(deadline: .now() + 0.25) {
                self.fetchUsers()
            }
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func leave() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).delete(id: currentUserModel?.objectId ?? "", success: nil, fail: nil)
    }
    
    private func updateTool(isBoradcast: Bool) {
        micButton.isHidden = !isBoradcast
        raiseHandButton.isHidden = isBoradcast
    }
    
    private func addUser() {
        var model = InteractiveBlogUsersModel()
        model.status = role == .broadcaster ? .accept : .end
        model.isEnableAudio = true
        let params = JSONObject.toJson(model)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).add(data: params, success: { object in
            let model = JSONObject.toModel(InteractiveBlogUsersModel.self, value: object.toJson())
            self.currentUserModel = model
            DispatchQueue.global().asyncAfter(deadline: .now() + 0.25) {
                self.fetchUsers()
            }
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func fetchUsers() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).get(success: { list in
            let users = list.compactMap({ JSONObject.toModel(InteractiveBlogUsersModel.self, value: $0.toJson()) }).sorted(by: { $0.timestamp < $1.timestamp })
            self.currentUserModel = users.first(where: { $0.userId == UserInfo.uid })
            let boradcastList = users.filter({ $0.status == .accept })
            let audienceList = users.filter({ $0.status != .accept })
            let requestList = users.filter({ $0.status == .request })
            self.numberLabel.text = "\(requestList.count)"
            self.numberLabel.isHidden = self.role == .audience || requestList.isEmpty
            self.dataArray = [boradcastList, audienceList]
        }, fail: { error in
            LogUtils.log(message: error.message, level: .error)
        })
    }
    
    private func subscribeUserEvent() {
        SyncUtil.scene(id: channelName)?.subscribe(key: SYNC_MANAGER_AGORA_VOICE_USERS, onUpdated: { object in
            guard let model = JSONObject.toModel(InteractiveBlogUsersModel.self, value: object.toJson()) else { return }
            let controller = UIApplication.topMostViewController
            if self.role == .broadcaster && model.status == .request {
                self.numberLabel.isHidden = false
                let number = Int(self.numberLabel.text ?? "0") ?? 0
                self.numberLabel.text = "\(number + 1)"
                return
            }
            if self.role == .audience && model.userId == UserInfo.uid && model.status == .invite {
                let alert = UIAlertController(title: "\(model.userName)" + "invite you to speak".localized, message: nil, preferredStyle: .alert)
                let agree = UIAlertAction(title: "Agree".localized, style: .default) { _ in
                    self.currentUserModel?.status = .accept
                    let params = JSONObject.toJson(self.currentUserModel)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
                        .update(id: self.currentUserModel?.objectId ?? "", data: params, success: nil, fail: nil)
                }
                let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
                alert.addAction(agree)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            if model.status == .refuse && model.userId == UserInfo.uid {
                let alert = UIAlertController(title: "\(model.userName)" + "declines your request".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Confirm".localized, style: .default, handler: nil)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            if model.userId == UserInfo.uid {
                self.updateTool(isBoradcast: model.status == .accept)
                self.currentUserModel = model
                self.enableAudioClosure?(model.isEnableAudio == true)
                self.micButton.isSelected = model.isEnableAudio == false
            }
            self.fetchUsers()
            
        }, onDeleted: { object in
            let dataArray = self.dataArray.flatMap({ $0 })
            let models = dataArray.filter({ $0.userId == UserInfo.uid && $0.objectId == object.getId() })
            if models.isEmpty == false {
                self.enableAudioClosure?(false)
            }
            self.fetchUsers()
        }, onSubscribed: {
            
        }, fail: nil)
    }
    
    private func setupUI() {
        backgroundColor = UIColor(hex: "#222c3d")
        layer.cornerRadius = 15
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        addSubview(collectionView)
        addSubview(downButton)
        addSubview(titleLabel)
        addSubview(closeButton)
        addSubview(leaveButton)
        addSubview(statckView)
        statckView.addArrangedSubview(raiseHandButton)
        statckView.addArrangedSubview(raiseListButton)
        statckView.addArrangedSubview(micButton)
        statckView.addSubview(numberLabel)
        
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        downButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        leaveButton.translatesAutoresizingMaskIntoConstraints = false
        statckView.translatesAutoresizingMaskIntoConstraints = false
        numberLabel.translatesAutoresizingMaskIntoConstraints = false
        
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: leaveButton.topAnchor, constant: -10).isActive = true
        
        downButton.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        downButton.topAnchor.constraint(equalTo: topAnchor, constant: -10).isActive = true
        downButton.widthAnchor.constraint(equalToConstant: 80).isActive = true
        downButton.heightAnchor.constraint(equalToConstant: 20).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: downButton.bottomAnchor, constant: 25).isActive = true
        
        closeButton.topAnchor.constraint(equalTo: topAnchor).isActive = true
        closeButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -30).isActive = true
        
        leaveButton.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        leaveButton.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10).isActive = true
        leaveButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        statckView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
        statckView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10).isActive = true
        
        micButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        micButton.widthAnchor.constraint(equalToConstant: 36).isActive = true
        
        raiseListButton.widthAnchor.constraint(equalTo: micButton.widthAnchor).isActive = true
        raiseListButton.heightAnchor.constraint(equalTo: micButton.heightAnchor).isActive = true
        
        raiseHandButton.widthAnchor.constraint(equalTo: micButton.widthAnchor).isActive = true
        raiseHandButton.heightAnchor.constraint(equalTo: micButton.heightAnchor).isActive = true
        
        numberLabel.centerXAnchor.constraint(equalTo: raiseListButton.trailingAnchor).isActive = true
        numberLabel.bottomAnchor.constraint(equalTo: raiseListButton.centerYAnchor).isActive = true
    }
    
    @objc
    private func onTapDownButton() {
        onTapDownButtonClosure?(currentUserModel)
    }
    @objc
    private func onTapCloseButton() {
        onTapCloseButtonClosure?()
    }
    @objc
    private func onTapLeaveButton() {
        onTapLeaveButtonClosure?()
    }
    @objc
    private func onTapRaiseHandButton() {
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
extension InteractiveBlogView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let list = dataArray[indexPath.section]
        if indexPath.section == 0 {
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: InteractiveBlogBoardcastCell.description(), for: indexPath) as! InteractiveBlogBoardcastCell
            let model = list[indexPath.item]
            cell.setupUserInfo(model: model, role: role)
            return cell
        }
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: InteractiveBlogAudienceCell.description(), for: indexPath) as! InteractiveBlogAudienceCell
        let model = list[indexPath.item]
        cell.setupUserInfo(model: model)
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let model = dataArray[indexPath.section][indexPath.item]
        guard role == .broadcaster,  model.userId != UserInfo.uid else { return }
        let view = InteractiveBlogControlView(channelName: channelName,
                                              model: model,
                                              userRole: UserRole(rawValue: indexPath.section) ?? .boradcast)
        AlertManager.show(view: view, alertPostion: .bottom, didCoverDismiss: true)
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        if indexPath.section == 0 {
            let w = (Screen.width - 60) / 3
            return CGSize(width: w, height: 136)
        }
        let w = (Screen.width - 45) / 2
        return CGSize(width: w, height: 60)
    }
    
    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        if indexPath.section == 1 && kind == UICollectionView.elementKindSectionHeader {
            let view = collectionView.dequeueReusableSupplementaryView(ofKind: UICollectionView.elementKindSectionHeader, withReuseIdentifier: InteractiveBlogAudienceHeaderView.description(), for: indexPath)
            
            return view
        }
        let view = collectionView.dequeueReusableSupplementaryView(ofKind: UICollectionView.elementKindSectionFooter, withReuseIdentifier: "headerView", for: indexPath)
        view.backgroundColor = indexPath.section == 0 ?  .black : .clear
        return view
    }
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForHeaderInSection section: Int) -> CGSize {
        section == 1 ? CGSize(width: Screen.width, height: 40) : .zero
    }
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForFooterInSection section: Int) -> CGSize {
        CGSize(width: Screen.width, height: 5)
    }
}

class InteractiveBlogBoardcastCell: UICollectionViewCell {
    private lazy var imageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "portrait02")
        imageView.cornerRadius = 33
        return imageView
    }()
    private lazy var micImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "InteractiveBlog/blueMic")
        imageView.backgroundColor = .black
        imageView.layer.borderColor = UIColor.gray.cgColor
        imageView.layer.borderWidth = 0.5
        imageView.cornerRadius = 10
        return imageView
    }()
    private lazy var iconButton: AGEButton = {
        let button = AGEButton(style: .none, colorStyle: .warning, fontStyle: .small)
        button.setTitle("asd", for: .normal)
        button.setImage(UIImage(named: "InteractiveBlog/master"), for: .normal, postion: .left)
        button.isUserInteractionEnabled = false
        return button
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupUserInfo(model: InteractiveBlogUsersModel, role: AgoraClientRole) {
        imageView.image = UIImage(named: model.avatar)
        let imageName = role == .broadcaster ? "InteractiveBlog/master" : "InteractiveBlog/grayBroadcaster"
        iconButton.setImage(UIImage(named: imageName), for: .normal)
        iconButton.setTitle(model.userName, for: .normal)
        micImageView.image = UIImage(named: model.isEnableAudio == false ? "InteractiveBlog/redMic" : "InteractiveBlog/blueMic")
    }
    
    private func setupUI() {
        contentView.addSubview(imageView)
        contentView.addSubview(micImageView)
        contentView.addSubview(iconButton)
        
        imageView.translatesAutoresizingMaskIntoConstraints = false
        micImageView.translatesAutoresizingMaskIntoConstraints = false
        iconButton.translatesAutoresizingMaskIntoConstraints = false
        
        imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        imageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        imageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        imageView.heightAnchor.constraint(equalTo: contentView.widthAnchor).isActive = true
        
        micImageView.bottomAnchor.constraint(equalTo: imageView.bottomAnchor).isActive = true
        micImageView.trailingAnchor.constraint(equalTo: imageView.trailingAnchor, constant: -5).isActive = true
        micImageView.widthAnchor.constraint(equalToConstant: 20).isActive = true
        micImageView.heightAnchor.constraint(equalToConstant: 20).isActive = true
        
        iconButton.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        iconButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
        iconButton.topAnchor.constraint(equalTo: imageView.bottomAnchor, constant: 10).isActive = true
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        imageView.cornerRadius = contentView.frame.width * 0.5
    }
}
class InteractiveBlogAudienceCell: UICollectionViewCell {
    private lazy var imageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "portrait03")
        imageView.cornerRadius = 30
        return imageView
    }()
    private lazy var nameLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "User-123"
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupUserInfo(model: InteractiveBlogUsersModel) {
        imageView.image = UIImage(named: model.avatar)
        nameLabel.text = model.userName
    }
    
    private func setupUI() {
        contentView.addSubview(imageView)
        contentView.addSubview(nameLabel)
        
        imageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        
        imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        imageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        imageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        imageView.widthAnchor.constraint(equalToConstant: 60).isActive = true
        
        nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        nameLabel.leadingAnchor.constraint(equalTo: imageView.trailingAnchor, constant: 10).isActive = true
        nameLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -5).isActive = true
    }
}

class InteractiveBlogAudienceHeaderView: UICollectionReusableView {
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "Audience".localized
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        addSubview(titleLabel)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
    }
}
