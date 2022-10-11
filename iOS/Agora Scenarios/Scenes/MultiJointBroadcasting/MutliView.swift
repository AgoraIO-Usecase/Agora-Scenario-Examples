//
//  MutliView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/8/2.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

class MutliView: UIView {
    var muteAudioClosure: ((Bool) -> Void)?
    var joinTheBroadcasting: ((Bool) -> Void)?
    
    private lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        let w = Screen.width - Screen.width * 0.7
        view.itemSize = CGSize(width: w, height: w)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 0
        view.delegate = self
        view.scrollDirection = .vertical
        view.register(MutliViewCell.self,
                      forCellWithReuseIdentifier: MutliViewCell.description())
        return view
    }()
    
    private var agoraKit: AgoraRtcEngineKit?
    private var channelName: String = ""
    private var currentRole: AgoraClientRole = .audience
    private var currentUserModel: AgoraUsersModel?
    
    init(channelName: String, role: AgoraClientRole, agoraKit: AgoraRtcEngineKit?) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.currentRole = role
        self.agoraKit = agoraKit
        setupUI()
        fetchUserInfoData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        let w = Screen.width - Screen.width * 0.7
        let h = frame.height / 4.0
        collectionView.itemSize = CGSize(width: w, height: h)
    }
    
    func leavl() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).document(id: currentUserModel?.objectId ?? "").delete(success: nil, fail: nil)
    }
    
    private func setupUI() {
        addSubview(collectionView)
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).document().subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            guard var model = JSONObject.toModel(AgoraUsersModel.self, value: object.toJson()) else { return }
            let controller = UIApplication.topMostViewController
            if model.status == .invite && self.currentRole == .broadcaster {
                let alert = UIAlertController(title: "\(model.userName): " + "Apply for joint broadcasting".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
                    model.status = .refuse
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: model.objectId ?? "", data: params, success: {
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                }
                let agree = UIAlertAction(title: "Agree".localized, style: .default) { _ in
                    model.status = .accept
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).update(id: model.objectId ?? "", data: params, success: {
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                    self.fetchUserInfoData()
                }
                alert.addAction(agree)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            if self.currentRole == .audience && model.status == .accept {
                self.joinTheBroadcasting?(true)
            }
            if self.currentRole == .audience && model.status == .refuse {
                let alert = UIAlertController(title: "The anchor rejected your application".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Confirm".localized, style: .default, handler: nil)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            if model.status == .end && model.userId == UserInfo.uid {
                self.muteAudioClosure?(false)
                self.joinTheBroadcasting?(false)
            }
            self.fetchUserInfoData()

        }, onDeleted: { object in
            let dataArray = self.collectionView.dataArray?.filter({ $0 is AgoraUsersModel }) as? [AgoraUsersModel]
            if let models = dataArray?.filter({ $0.userId == UserInfo.uid && $0.objectId == object.getId() }), models.isEmpty == false {
                self.muteAudioClosure?(false)
                self.joinTheBroadcasting?(false)
            }
            self.fetchUserInfoData()
        }, onSubscribed: {
            print("订阅邀请用户")
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func fetchUserInfoData() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).get(success: { results in
            var tempArray = [Any]()
            let datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(AgoraUsersModel.self, value: $0 )})
                .filter({ $0.status == .accept })
                .sorted(by: { $0.timestamp < $1.timestamp })
            tempArray += datas
            for _ in datas.count..<4 {
                tempArray.append("")
            }
            self.collectionView.dataArray = tempArray
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
}
extension MutliView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: MutliViewCell.description(), for: indexPath) as! MutliViewCell
        let model = self.collectionView.dataArray?[indexPath.item]
        if let userModel = model as? AgoraUsersModel {
            let canvas = AgoraRtcVideoCanvas()
            canvas.uid = UInt(userModel.userId) ?? 0
            canvas.view = cell.canvasView
            canvas.renderMode = .hidden
            if userModel.userId == UserInfo.uid {
                agoraKit?.setupLocalVideo(canvas)
                agoraKit?.startPreview()
            } else {
                agoraKit?.setupRemoteVideo(canvas)
            }
        }
        cell.setupData(model: model, role: currentRole)
        return cell
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let controller = UIApplication.topMostViewController
        let model = self.collectionView.dataArray?[indexPath.item]
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        if currentRole == .broadcaster {
            guard model is AgoraUsersModel else { return }
            let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
            let close = UIAlertAction(title: /*封麦*/"Seat_Close".localized, style: .destructive) { _ in
                let userModel = model as? AgoraUsersModel
                SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).document(id: userModel?.objectId ?? "").delete(success: nil, fail: nil)
            }
            alert.addAction(close)
            alert.addAction(cancel)
            controller?.present(alert, animated: true, completion: nil)
        } else {
            let userModels = self.collectionView.dataArray?.compactMap({ $0 as? AgoraUsersModel })
            let isContainer = userModels?.contains(where: { $0.userId == UserInfo.uid }) ?? false
            if isContainer {
                let alert = UIAlertController(title: "Confirm_End_Broadcasting".localized, message: nil, preferredStyle: .alert)
                let end = UIAlertAction(title: "End".localized, style: .destructive) { _ in
                    let userModel = model as? AgoraUsersModel
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).document(id: userModel?.objectId ?? "").delete(success: nil, fail: nil)
                }
                alert.addAction(end)
                alert.addAction(cancel)
                controller?.present(alert, animated: true, completion: nil)
                return
            }
            let alert = UIAlertController(title: "Apply for joint broadcasting".localized, message: nil, preferredStyle: .alert)
            let apply = UIAlertAction(title: "Apply".localized, style: .default) { [weak self] _ in
                guard let self = self else { return }
                var model = AgoraUsersModel()
                model.status = .invite
                let params = JSONObject.toJson(model)
                SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_VOICE_USERS).add(data: params, success: { object in
                    let model = JSONObject.toModel(AgoraUsersModel.self, value: object.toJson())
                    self.currentUserModel = model
                }, fail: { error in
                    ToastView.show(text: error.message)
                })
            }
            alert.addAction(apply)
            alert.addAction(cancel)
            controller?.present(alert, animated: true, completion: nil)
        }
    }
}

class MutliViewCell: UICollectionViewCell {
    private lazy var imageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "icon-invite")
        return imageView
    }()
    private lazy var label: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "Apply for connection".localized
        return label
    }()
    lazy var canvasView: UIView = {
        let view = UIView()
        return view
    }()
    private lazy var nameLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
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
        contentView.backgroundColor = UIColor(hex: "#0D0D0D", alpha: 0.56)
        contentView.addSubview(imageView)
        contentView.addSubview(label)
        contentView.addSubview(canvasView)
        contentView.addSubview(nameLabel)
        
        imageView.translatesAutoresizingMaskIntoConstraints = false
        label.translatesAutoresizingMaskIntoConstraints = false
        
        imageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        imageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor, constant: -10).isActive = true
        
        label.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        label.topAnchor.constraint(equalTo: imageView.bottomAnchor, constant: 10).isActive = true
        
        canvasView.translatesAutoresizingMaskIntoConstraints = false
        
        canvasView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        canvasView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        canvasView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        canvasView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.leadingAnchor.constraint(equalTo: canvasView.leadingAnchor, constant: 5).isActive = true
        nameLabel.bottomAnchor.constraint(equalTo: canvasView.bottomAnchor, constant: -5).isActive = true
    }
    
    func setupData(model: Any?, role: AgoraClientRole) {
        label.text = role == .broadcaster ? "Waiting for joint broadcasting".localized : "Apply for connection".localized
        if model is AgoraUsersModel {
            let userModel = model as! AgoraUsersModel
            nameLabel.text = "\(userModel.userName)"
            label.isHidden = true
            nameLabel.isHidden = false
            canvasView.isHidden = false
        } else {
            label.isHidden = false
            nameLabel.isHidden = true
            imageView.image = UIImage(named: "icon-invite")
            canvasView.isHidden = true
        }
    }
}
