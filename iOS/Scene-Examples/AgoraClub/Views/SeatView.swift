//
//  SeatView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/3/21.
//

import UIKit
import AgoraUIKit_iOS
import AgoraRtcKit

class SeatView: UIView {
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
        fetchAgoraVoiceUserInfoData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func reloadData() {
        collectionView.reloadData()
    }
    
    private func fetchAgoraVoiceUserInfoData() {
        SyncUtil.fetchCollection(id: channelName, className: SYNC_MANAGER_AGORA_CLUB_USERS, success: { results in
            var tempArray = [Any]()
            let datas = results.compactMap({ $0.toJson() })
                .compactMap({ JSONObject.toModel(AgoraVoiceUsersModel.self, value: $0 )})
                .filter({ $0.status == .accept })
                .sorted(by: { $0.timestamp < $1.timestamp })
            tempArray += datas
            for _ in datas.count..<8 {
                tempArray.append("")
            }
            self.collectionView.dataArray = tempArray
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
        
        SyncUtil.subscribeCollection(id: channelName, className: SYNC_MANAGER_AGORA_CLUB_USERS, onUpdated: { object in
            guard var model = JSONObject.toModel(AgoraVoiceUsersModel.self, value: object.toJson()) else { return }
            let controller = UIApplication.topMostViewController
            if model.userId == UserInfo.uid && model.status == .invite {
                let alert = UIAlertController(title: "host_invites_you_on_the_mic".localized, message: nil, preferredStyle: .alert)
                let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
                    model.status = .refuse
                    let params = JSONObject.toJson(model)
                    SyncUtil.updateCollection(id: self.channelName, className: SYNC_MANAGER_AGORA_CLUB_USERS, objectId: model.objectId ?? "", params: params)
                }
                let invite = UIAlertAction(title: /*上麦*/"Became_A_Host".localized, style: .default) { _ in
                    model.status = .accept
                    
                    guard let option = self.channelMediaOptions else { return }
                    option.publishAudioTrack = .of(true)
                    option.publishCameraTrack = .of(true)
                    option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
                    self.agoraKit?.setClientRole(.broadcaster)
                    self.agoraKit?.updateChannel(with: option)
                    self.agoraKit?.startPreview()
                    self.agoraKit?.setClientRole(.broadcaster)
                    
                    let params = JSONObject.toJson(model)
                    SyncUtil.updateCollection(id: self.channelName, className: SYNC_MANAGER_AGORA_CLUB_USERS, objectId: model.objectId ?? "", params: params)
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
                guard let option = self.channelMediaOptions else { return }
                option.publishAudioTrack = .of(false)
                option.publishCustomVideoTrack = .of(false)
                option.clientRoleType = .of((Int32)(AgoraClientRole.audience.rawValue))
                self.agoraKit?.updateChannel(with: option)
            }
            self.fetchAgoraVoiceUserInfoData()
            

        }, onDeleted: { object in
            guard let model = JSONObject.toModel(AgoraVoiceUsersModel.self, value: object.toJson()) else { return }
            if model.userId == UserInfo.uid {
                guard let option = self.channelMediaOptions else { return }
                option.publishAudioTrack = .of(false)
                option.publishCustomVideoTrack = .of(false)
                option.clientRoleType = .of((Int32)(AgoraClientRole.audience.rawValue))
                self.agoraKit?.updateChannel(with: option)
            }
            self.fetchAgoraVoiceUserInfoData()
            
        }, onSubscribed: {
            print("订阅邀请用户")
        })
    }
}
extension SeatView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: AgoraVoiceUsersViewCell.description(), for: indexPath) as! AgoraVoiceUsersViewCell
        cell.defaultImageName = "zuowei"
        let model = self.collectionView.dataArray?[indexPath.item]
        if let userModel = model as? AgoraVoiceUsersModel {
            let canvas = AgoraRtcVideoCanvas()
            canvas.uid = UInt(userModel.userId) ?? 0
            canvas.renderMode = .hidden
            canvas.view = cell.imageView
            if userModel.userId == UserInfo.uid {
                agoraKit?.setupLocalVideo(canvas)
            } else {
                let connection = AgoraRtcConnection()
                connection.localUid = UserInfo.userId
                connection.channelId = channelName
                agoraKit?.setupRemoteVideoEx(canvas, connection: connection)
            }
        } else {
            cell.setupData(model: model)
        }
        return cell
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard currentRole == .broadcaster else { return }
        let model = self.collectionView.dataArray?[indexPath.item]
        let controller = UIApplication.topMostViewController
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        let close = UIAlertAction(title: /*封麦*/"Seat_Close".localized, style: .destructive) { _ in
            var userModel = model as? AgoraVoiceUsersModel
            userModel?.status = .end
            SyncUtil.updateCollection(id: self.channelName, className: SYNC_MANAGER_AGORA_CLUB_USERS, objectId: userModel?.objectId ?? "", params: JSONObject.toJson(userModel))
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
