//
//  SeatView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/3/21.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

class SeatView: UIView {
    var updateBottomTypeClosure: ((Bool) -> Void)?
    var updateUserStatusClosure: ((AgoraUsersModel) -> Void)?
    
    private lazy var collectionView: AGECollectionView = {
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
    private var agoraKit: AgoraRtcEngineKit?
    private var channelMediaOptions: AgoraRtcChannelMediaOptions?
    private var connection: AgoraRtcConnection?
        
    init(channelName: String,
         role: AgoraClientRole,
         agoraKit: AgoraRtcEngineKit?,
         mediaOptions: AgoraRtcChannelMediaOptions?, connection: AgoraRtcConnection) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.currentRole = role
        self.agoraKit = agoraKit
        self.channelMediaOptions = mediaOptions
        self.connection = connection
        setupUI()
        eventHandler()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func reloadData() {
        collectionView.reloadData()
    }
    
    func updateParams(channelName: String,
                      role: AgoraClientRole,
                      agoraKit: AgoraRtcEngineKit?,
                      mediaOptions: AgoraRtcChannelMediaOptions?,
                      connection: AgoraRtcConnection) {
        SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).document().unsubscribe(key: "")
        self.channelName = channelName
        self.currentRole = role
        self.agoraKit = agoraKit
        self.channelMediaOptions = mediaOptions
        self.connection = connection
        eventHandler()
    }
    
    func fetchAgoraVoiceUserInfoData() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).get(success: { results in
            var tempArray = [Any]()
            let datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(AgoraUsersModel.self, value: $0 )})
                .filter({ $0.status == .accept })
                .sorted(by: { $0.timestamp < $1.timestamp })
            tempArray += datas
            let isContainer = datas.contains(where: { $0.userId == UserInfo.uid })
            self.updateBottomTypeClosure?(isContainer)
            if datas.count < 8 {
                for _ in datas.count..<8 {
                    tempArray.append("")
                }
            }
            self.collectionView.dataArray = tempArray
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func eventHandler() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).document().subscribe(key: "", onCreated: { object in
            
        }, onUpdated: { object in
            guard var model = JSONObject.toModel(AgoraUsersModel.self, value: object.toJson()) else { return }
            let controller = UIApplication.topMostViewController
            if model.userId == UserInfo.uid && model.status == .invite {
                let alert = UIAlertController(title: "host_invites_you_on_the_mic".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
                    model.status = .refuse
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).update(id: model.objectId ?? "", data: params, success: {
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                }
                let invite = UIAlertAction(title: /*上麦*/"Became_A_Host".localized, style: .default) { _ in
                    model.status = .accept
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).update(id: model.objectId ?? "", data: params, success: {
                        
                    }, fail: { error in
                        ToastView.show(text: error.message)
                    })
                    self.updateUserStatusClosure?(model)
                    self.fetchAgoraVoiceUserInfoData()
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
                guard let option = self.channelMediaOptions, let connection = self.connection else { return }
                option.publishMicrophoneTrack = .of(false)
                option.publishCustomVideoTrack = .of(false)
                self.agoraKit?.updateChannelEx(with: option, connection: connection)
            }
            self.fetchAgoraVoiceUserInfoData()
        }, onDeleted: { object in
            let dataArray = self.collectionView.dataArray as? [AgoraUsersModel]
            if let model = dataArray?.filter({ $0.userId == UserInfo.uid }).first, model.objectId == object.getId() {
                guard let option = self.channelMediaOptions, let connection = self.connection else { return }
                option.publishMicrophoneTrack = .of(false)
                option.publishCustomVideoTrack = .of(false)
                self.agoraKit?.updateChannelEx(with: option, connection: connection)
            }
            self.fetchAgoraVoiceUserInfoData()
        }, onSubscribed: {
            print("订阅邀请用户")
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func setupUI() {
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(collectionView)
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        collectionView.heightAnchor.constraint(equalToConstant: 150.fit).isActive = true
    }
}
extension SeatView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: AgoraVoiceUsersViewCell.description(), for: indexPath) as! AgoraVoiceUsersViewCell
        cell.defaultImageName = "zuowei"
        let model = self.collectionView.dataArray?[indexPath.item]
        if let userModel = model as? AgoraUsersModel {
            let canvas = AgoraRtcVideoCanvas()
            canvas.uid = UInt(userModel.userId) ?? 0
            canvas.renderMode = .hidden
            canvas.view = cell.canvasView
            if userModel.userId == UserInfo.uid && userModel.isEnableVideo == true {
                agoraKit?.setupLocalVideo(canvas)
                agoraKit?.startPreview()
                
            } else if userModel.userId != UserInfo.uid && userModel.isEnableVideo == true {
                agoraKit?.setupRemoteVideoEx(canvas, connection: connection!)
            }
            cell.setupData(model: model)
        } else {
            cell.setupData(model: model)
            cell.canvasView.isHidden = true
        }
        return cell
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard currentRole == .broadcaster, indexPath.item > 0 else { return }
        let model = self.collectionView.dataArray?[indexPath.item]
        let controller = UIApplication.topMostViewController
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        let close = UIAlertAction(title: /*封麦*/"Seat_Close".localized, style: .destructive) { _ in
            guard var userModel = model as? AgoraUsersModel else { return }
            userModel.status = .end
            SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_MANAGER_AGORA_CLUB_USERS).update(id: userModel.objectId ?? "", data: JSONObject.toJson(userModel), success: {
                
            }, fail: { error in
                ToastView.show(text: error.message)
            })
            self.updateUserStatusClosure?(userModel)
        }
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        if model is String {
            let invite = UIAlertAction(title: "Invite".localized, style: .default) { _ in
                AlertManager.show(view: AgoraVoiceInviteView(channelName: self.channelName,
                                                             syncName: SYNC_MANAGER_AGORA_CLUB_USERS),
                                  alertPostion: .bottom)
            }
            alert.addAction(invite)
        }
        alert.addAction(close)
        alert.addAction(cancel)
        controller?.present(alert, animated: true, completion: nil)
    }
}
