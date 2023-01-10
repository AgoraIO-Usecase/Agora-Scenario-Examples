//
//  LargeClassView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/9/30.
//

import UIKit
import Fastboard
import Agora_Scene_Utils

enum LargeClassUserStatus: Int, Codable {
    case none = 0
    /// 请求上麦
    case request = 1
    ///  已接受
    case accept = 2
    /// 已拒绝
    case refuse = 3
    /// 已结束
    case end = 4
}


struct ClassUsersModel: Codable {
    var userName: String = "User-\(UserInfo.uid)"
    var avatar: String = String(format: "portrait%02d", Int.random(in: 1...14))
    var userId: String = UserInfo.uid
    var status: LargeClassUserStatus? = .none
    var timestamp: String = "".timeStamp16
    var isEnableAudio: Bool? = true
    var isEnableVideo: Bool? = true
    var objectId: String?
}


class LargeClassView: UIView {
    var onPublishCameraTrackClosre: ((Bool) -> Void)?
    var onpublishMicrophoneTrackClosure: ((Bool) -> Void)?
    var setupVideoCanvasClosure: ((_ uid: UInt, _ canvasView: UIView) -> Void)?
    var onTapCloseButtonClosure: (() -> Void)?
    
    private lazy var fastRoom: FastRoom = {
        let config = FastRoomConfiguration(appIdentifier: BOARD_APP_ID,
                                           roomUUID: BOARD_ROOM_UUID,
                                           roomToken: BOARD_ROOM_TOKEN,
                                           region: .CN,
                                           userUID: UserInfo.uid)
        let fastRoom = Fastboard.createFastRoom(withFastRoomConfig: config)
        fastRoom.view.layer.cornerRadius = 10
        fastRoom.view.layer.masksToBounds = true
        return fastRoom
    }()
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.itemSize = CGSize(width: 106.fit, height: 83.fit)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 15
        view.delegate = self
        view.scrollDirection = .vertical
        view.register(AgoraUserCollectionViewCell.self,
                      forCellWithReuseIdentifier: AgoraUserCollectionViewCell.description())
        return view
    }()
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .horizontal
        stackView.distribution = .fill
        stackView.spacing = 8
        return stackView
    }()
    private lazy var videoButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "SmallClass/video_default"), for: .normal)
        button.setImage(UIImage(named: "SmallClass/video_selected"), for: .selected)
        button.addTarget(self, action: #selector(onTapVideoButton(sender:)), for: .touchUpInside)
        button.isSelected = true
        button.isHidden = true
        return button
    }()
    private lazy var audioButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "SmallClass/audio_default"), for: .normal)
        button.setImage(UIImage(named: "SmallClass/audio_selected"), for: .selected)
        button.addTarget(self, action: #selector(onTapAudioButton(sender:)), for: .touchUpInside)
        button.isSelected = true
        button.isHidden = true
        return button
    }()
    private lazy var closeButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "LargeClass/close"), for: .normal)
        button.addTarget(self, action: #selector(onTapCloseLive), for: .touchUpInside)
        return button
    }()
    private lazy var endButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "LargeClass/end"), for: .normal)
        button.addTarget(self, action: #selector(onTapEndButton), for: .touchUpInside)
        button.isHidden = true
        return button
    }()
    private lazy var raiseHandButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "LargeClass/raiseHand"), for: .normal)
        button.addTarget(self, action: #selector(onTapRaiseHandButton), for: .touchUpInside)
        button.isHidden = true
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
    private lazy var raiseHandListButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "LargeClass/raiseHandList"), for: .normal)
        button.addTarget(self, action: #selector(onTapRaiseHandListButton), for: .touchUpInside)
        button.isHidden = true
        return button
    }()

    
    private var channelName: String = ""
    private var role: UserRole = .audience
    private var isAccept: Bool = false
    
    private var dataArray: [ClassUsersModel] = [] {
        didSet {
            collectionView.dataArray = dataArray.filter({ $0.status == .accept })
        }
    }
    
    var currentModel: ClassUsersModel? {
         dataArray.first(where: { $0.userId == UserInfo.uid })
    }
        
    init(channelName: String, role: UserRole) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.role = role
        setupUI()
        subscribeUserStatus()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func joinRoom(role: UserRole) {
        fastRoom.joinRoom()
        var model = ClassUsersModel()
        model.status = role == .boradcast ? .accept : LargeClassUserStatus.none
        let params = JSONObject.toJson(model)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).add(data: params, success: { object in
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                self.getUserStatus()
            }
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    func disconnectRoom() {
        fastRoom.disconnectRoom()
    }
    func dismissAllSubPanels() {
        fastRoom.dismissAllSubPanels()
    }
    
    private func getUserStatus() {
         SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).get(success: { results in
             let datas = results.compactMap({ $0.toJson() })
                 .compactMap({ JSONObject.toModel(ClassUsersModel.self, value: $0 )})
                 .sorted(by: { $0.timestamp < $1.timestamp })
             let requestList = datas.filter({ $0.status == .request })
             self.numberLabel.text = "\(requestList.count)"
             self.numberLabel.isHidden = self.role == .audience || requestList.isEmpty
             self.dataArray = datas
         }, fail: { error in
             ToastView.show(text: error.message)
         })
     }
    
    private func subscribeUserStatus() {
        SyncUtil.scene(id: channelName)?.subscribe(key: SYNC_MANAGER_AGORA_VOICE_USERS, onCreated: nil, onUpdated: { object in
            guard let userInfo = JSONObject.toModel(ClassUsersModel.self, value: object.toJson()) else { return }
            let controller = UIViewController.cl_topViewController()
            if self.role == .boradcast && userInfo.status == .request {// 收到学生举手
                self.numberLabel.isHidden = false
                let number = Int(self.numberLabel.text ?? "0") ?? 0
                self.numberLabel.text = "\(number + 1)"
                return
                
            } else if userInfo.status == .refuse && userInfo.userId == UserInfo.uid {// 老师拒绝您上麦
                let alert = UIAlertController(title: "The teacher refuses you to serve the mic".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Confirm".localized, style: .default, handler: nil)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                
            } else if userInfo.status == .accept
                        && userInfo.userId == UserInfo.uid
                        && self.role == .audience
                        && self.isAccept == false { // 老师同意您上麦
                ToastView.show(text: "The teacher agrees you on the mic".localized)
                
            } else if userInfo.status == .end && self.role == .boradcast { // 学生结束了发言
                ToastView.show(text: "\(userInfo.userName) " + "End of speech".localized)
            }
            if userInfo.userId == UserInfo.uid && self.role == .audience {
                self.videoButton.isHidden = userInfo.status != .accept
                self.audioButton.isHidden = userInfo.status != .accept
                self.endButton.isHidden = userInfo.status != .accept
                self.raiseHandButton.isHidden = userInfo.status == .accept
                self.isAccept = userInfo.status == .accept
                
                self.onpublishMicrophoneTrackClosure?(userInfo.status == .accept)
                self.onPublishCameraTrackClosre?(userInfo.status == .accept)
            }
            self.getUserStatus()
            
        }, onDeleted: { object in
            if let index = self.dataArray.firstIndex(where: { $0.objectId == object.getId() }) {
                self.dataArray.remove(at: index)
            }
        }, onSubscribed: nil, fail: { error in
            LogUtils.log(message: error.description, level: .error)
        })
    }
    
    @objc
    private func onTapRaiseHandListButton() {
        let view = LargeClassRaiseListView(channelName: channelName)
        AlertManager.show(view: view, alertPostion: .bottom, didCoverDismiss: true)
    }
    @objc
    private func onTapRaiseHandButton() {
        var model = currentModel
        model?.status = .request
        let params = JSONObject.toJson(model)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .update(id: model?.objectId ?? "", data: params, success: nil, fail: nil)
        ToastView.show(text: "Request received. Please wait ...".localized)
    }
    @objc
    private func onTapEndButton() {
        var model = currentModel
        model?.status = .end
        let params = JSONObject.toJson(model)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .update(id: model?.objectId ?? "", data: params, success: nil, fail: nil)
    }
    
    @objc
    private func onTapVideoButton(sender: AGEButton) {
        guard let index = dataArray.firstIndex(where: { $0.userId == UserInfo.uid }) else { return }
        sender.isSelected = !sender.isSelected
        var model = dataArray[index]
        model.isEnableVideo = sender.isSelected
        dataArray[index] = model
        
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .document(id: model.objectId ?? "")
            .update(key: "", data: JSONObject.toJson(model), success: nil, fail: nil)
        
        onPublishCameraTrackClosre?(sender.isSelected)
    }
    @objc
    private func onTapAudioButton(sender: AGEButton) {
        guard let index = dataArray.firstIndex(where: { $0.userId == UserInfo.uid }) else { return }
        sender.isSelected = !sender.isSelected
        var model = dataArray[index]
        model.isEnableAudio = sender.isSelected
        dataArray[index] = model
        
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS)
            .document(id: model.objectId ?? "")
            .update(key: "", data: JSONObject.toJson(model), success: nil, fail: nil)
        
        onpublishMicrophoneTrackClosure?(sender.isSelected)
    }
    
    @objc
    private func onTapCloseLive() {
        onTapCloseButtonClosure?()
    }
    
    private func setupUI() {
        fastRoom.view.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        statckView.translatesAutoresizingMaskIntoConstraints = false
        numberLabel.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(fastRoom.view)
        addSubview(collectionView)
        addSubview(statckView)
        statckView.addArrangedSubview(raiseHandListButton)
        statckView.addArrangedSubview(raiseHandButton)
        statckView.addArrangedSubview(endButton)
        statckView.addArrangedSubview(videoButton)
        statckView.addArrangedSubview(audioButton)
        statckView.addArrangedSubview(closeButton)
        statckView.addSubview(numberLabel)
        
        raiseHandListButton.isHidden = role == .audience
        raiseHandButton.isHidden = role == .boradcast
        endButton.isHidden = role == .boradcast
        videoButton.isHidden = role == .audience
        audioButton.isHidden = role == .audience
        
        collectionView.topAnchor.constraint(equalTo: topAnchor, constant: 5.fit).isActive = true
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 5.fit).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -5.fit).isActive = true
        collectionView.widthAnchor.constraint(equalToConstant: 106.fit).isActive = true
        
        fastRoom.view.leadingAnchor.constraint(equalTo: collectionView.trailingAnchor, constant: 5.fit).isActive = true
        fastRoom.view.topAnchor.constraint(equalTo: collectionView.topAnchor).isActive = true
        fastRoom.view.bottomAnchor.constraint(equalTo: collectionView.bottomAnchor).isActive = true
        fastRoom.view.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -5.fit).isActive = true
        
        statckView.trailingAnchor.constraint(equalTo: fastRoom.view.trailingAnchor, constant: -5).isActive = true
        statckView.bottomAnchor.constraint(equalTo: fastRoom.view.bottomAnchor, constant: -2).isActive = true
        
        closeButton.widthAnchor.constraint(equalToConstant: 38).isActive = true
        closeButton.heightAnchor.constraint(equalToConstant: 38).isActive = true
        
        numberLabel.centerXAnchor.constraint(equalTo: raiseHandListButton.trailingAnchor).isActive = true
        numberLabel.bottomAnchor.constraint(equalTo: raiseHandListButton.centerYAnchor).isActive = true
    }
}
extension LargeClassView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: AgoraUserCollectionViewCell.description(),
                                                      for: indexPath) as! AgoraUserCollectionViewCell
        cell.avatariImageView.layer.cornerRadius = 10
        cell.canvasView.layer.cornerRadius = 10
        let model = dataArray[indexPath.item]
        
        setupVideoCanvasClosure?(UInt(model.userId) ?? 0, cell.canvasView)

        cell.setupData(model: model)
        return cell
    }

}
