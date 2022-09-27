//
//  AgoraVoiceUsersView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/26.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

enum AgoraVoiceUserType {
    case voiceChat
    case club
}

class AgoraVoiceUsersView: UIView {
    var muteAudioClosure: ((Bool) -> Void)?
    
    private lazy var avatarImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "portrait01")
        imageView.contentMode = .scaleAspectFill
        imageView.cornerRadius = 40.fit
        return imageView
    }()
    
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        let margin = (Screen.width - 4 * 80.fit - 15.fit * 2)
        view.itemSize = CGSize(width: 60.fit, height: 60.fit)
        view.edge = UIEdgeInsets(top: 0, left: 15.fit, bottom: 0, right: 15.fit)
        view.minInteritemSpacing = margin
        view.minLineSpacing = margin
        view.delegate = self
        view.scrollDirection = .vertical
        view.register(AgoraVoiceUsersViewCell.self,
                      forCellWithReuseIdentifier: AgoraVoiceUsersViewCell.description())
        return view
    }()
    private var channelName: String = ""
    private var currentRole: AgoraClientRole = .audience
    private var type: AgoraVoiceUserType = .club
    
    init(channelName: String, role: AgoraClientRole, type: AgoraVoiceUserType = .club) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.currentRole = role
        self.type = type
        setupUI()
        fetchAgoraVoiceUserInfoData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func fetchAgoraVoiceUserInfoData() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).get(success: { results in
            var tempArray = [Any]()
            var datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(AgoraUsersModel.self, value: $0 )})
                .filter({ $0.status == .accept })
                .sorted(by: { $0.timestamp < $1.timestamp })
            datas = datas.map { item in
                var model = item
                model.isEnableVideo = self.type == .voiceChat
                model.isEnableAudio = self.type == .voiceChat
                return model
            }
            tempArray += datas
            for _ in datas.count..<8 {
                tempArray.append("")
            }
            self.collectionView.dataArray = tempArray
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func setupUI() {
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(avatarImageView)
        addSubview(collectionView)
        
        avatarImageView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        avatarImageView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 80.fit).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 80.fit).isActive = true
        
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: avatarImageView.bottomAnchor, constant: 70).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        collectionView.heightAnchor.constraint(equalToConstant: 150.fit).isActive = true
        
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).document().subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            guard var model = JSONObject.toModel(AgoraUsersModel.self, value: object.toJson()) else { return }
            let controller = UIApplication.topMostViewController
            if model.userId == UserInfo.uid && model.status == .invite {
                let alert = UIAlertController(title: "host_invites_you_on_the_mic".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
                    model.status = .refuse
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: model.objectId ?? "", data: params, success: {
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                }
                let invite = UIAlertAction(title: /*上麦*/"Became_A_Host".localized, style: .default) { _ in
                    model.status = .accept
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: model.objectId ?? "", data: params, success: {
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                    self.fetchAgoraVoiceUserInfoData()
                    self.muteAudioClosure?(true)
                }
                alert.addAction(invite)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            if self.currentRole == .broadcaster && model.status == .refuse {
                let alert = UIAlertController(title: "User-\(model.userId)拒绝了您的邀请", message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Confirm".localized, style: .default, handler: nil)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            if model.status == .end && model.userId == UserInfo.uid {
                self.muteAudioClosure?(false)
            }
            self.fetchAgoraVoiceUserInfoData()

        }, onDeleted: { object in
            let dataArray = self.collectionView.dataArray as? [AgoraUsersModel]
            if let models = dataArray?.filter({ $0.userId == UserInfo.uid && $0.objectId == object.getId() }), models.isEmpty == false {
                self.muteAudioClosure?(false)
            }
            self.fetchAgoraVoiceUserInfoData()
        }, onSubscribed: {
            print("订阅邀请用户")
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
}

extension AgoraVoiceUsersView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: AgoraVoiceUsersViewCell.description(), for: indexPath) as! AgoraVoiceUsersViewCell
        let model = self.collectionView.dataArray?[indexPath.item]
        cell.setupData(model: model)
        return cell
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard currentRole == .broadcaster else { return }
        let model = self.collectionView.dataArray?[indexPath.item]
        let controller = UIApplication.topMostViewController
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        let close = UIAlertAction(title: /*封麦*/"Seat_Close".localized, style: .destructive) { _ in
            var userModel = model as? AgoraUsersModel
            userModel?.status = .end
            SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: userModel?.objectId ?? "", data: JSONObject.toJson(userModel), success: {
                
            }, fail: { error in
                ToastView.show(text: error.message)
            })
        }
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        if model is String {
            let invite = UIAlertAction(title: "Invite".localized, style: .default) { _ in
                AlertManager.show(view: AgoraVoiceInviteView(channelName: self.channelName), alertPostion: .bottom)
            }
            alert.addAction(invite)
        }
        alert.addAction(close)
        alert.addAction(cancel)
        controller?.present(alert, animated: true, completion: nil)
    }
}

class AgoraVoiceUsersViewCell: UICollectionViewCell {
    lazy var avatariImageView: AGEButton = {
        let imageView = AGEButton(style: .imageName(name: "icon-invite"))
        imageView.cornerRadius = 30.fit
        imageView.contentMode = .scaleAspectFit
        imageView.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        imageView.isUserInteractionEnabled = false
        return imageView
    }()
    lazy var canvasView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 30.fit
        view.layer.masksToBounds = true
        return view
    }()
    private lazy var muteAudioImageView: AGEImageView = {
        let imageView = AGEImageView(systemName: "mic.slash", imageColor: .white)
        imageView.isHidden = true
        return imageView
    }()
    private lazy var muteVideoImageView: AGEImageView = {
        let imageView = AGEImageView(systemName: "video.slash", imageColor: .white)
        imageView.isHidden = true
        return imageView
    }()
    
    var defaultImageName: String?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupData(model: Any?) {
        if model is AgoraUsersModel {
            let userModel = model as! AgoraUsersModel
            avatariImageView.setImage(UIImage(named: userModel.avatar), for: .normal)
            muteVideoImageView.isHidden = userModel.isEnableVideo ?? false
            muteAudioImageView.isHidden = userModel.isEnableAudio ?? false
            canvasView.isHidden = userModel.isEnableVideo == false
        } else {
            avatariImageView.setImage(UIImage(named: defaultImageName ?? "icon-invite"), for: .normal)
            muteVideoImageView.isHidden = true
            muteAudioImageView.isHidden = true
            canvasView.isHidden = true
        }
    }
    
    private func setupUI() {
        avatariImageView.translatesAutoresizingMaskIntoConstraints = false
        canvasView.translatesAutoresizingMaskIntoConstraints = false
        muteAudioImageView.translatesAutoresizingMaskIntoConstraints = false
        muteVideoImageView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(avatariImageView)
        contentView.addSubview(canvasView)
        contentView.addSubview(muteVideoImageView)
        contentView.addSubview(muteAudioImageView)
        avatariImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        avatariImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        avatariImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        avatariImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        canvasView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        canvasView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        canvasView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        canvasView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        muteVideoImageView.centerXAnchor.constraint(equalTo: avatariImageView.leadingAnchor, constant: 5).isActive = true
        muteVideoImageView.centerYAnchor.constraint(equalTo: avatariImageView.bottomAnchor, constant: -5).isActive = true
        muteAudioImageView.centerXAnchor.constraint(equalTo: avatariImageView.trailingAnchor, constant: -5).isActive = true
        muteAudioImageView.centerYAnchor.constraint(equalTo: avatariImageView.bottomAnchor, constant: -5).isActive = true
    }
    
    override open func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        alpha = 0.65
    }

    override open func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        alpha = 1
    }

    override open func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        alpha = 1
    }
}
