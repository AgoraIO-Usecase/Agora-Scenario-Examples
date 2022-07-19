//
//  LiveOnlineView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/15.
//

import UIKit
import Agora_Scene_Utils

class LiveOnlineView: UIView {
    private lazy var onLineView: UIView = {
        let view = UIView()
        view.backgroundColor = .black.withAlphaComponent(0.4)
        view.layer.cornerRadius = 14
        view.layer.masksToBounds = true
        return view
    }()
    private lazy var personImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(systemName: "person")?.withTintColor(.white, renderingMode: .alwaysOriginal))
        return imageView
    }()
    private lazy var onLineLabel: UILabel = {
        let label = UILabel()
        label.text = "1"
        label.textColor = .white
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.itemSize = CGSize(width: 28, height: 28)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 10
        view.delegate = self
        view.scrollDirection = .horizontal
        view.register(LiveOnLineViewCell.self,
                      forCellWithReuseIdentifier: "LiveOnLineViewCell")
        return view
    }()
    private var collectionViewCons: NSLayoutConstraint?
    
    private var dataArray = [AgoraVoiceUsersModel]() {
        didSet {
            let dats = dataArray//.filterDuplicates({ $0.userId })
            collectionView.dataArray = dats
            onLineLabel.text = "\(dats.count)"
            let w = (dats.count * 28) + (dats.count - 1) * 10
            collectionViewCons?.constant = CGFloat(w > 118 ? 118 : w)
            collectionViewCons?.isActive = true
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func getUserInfo(channelName: String) {
        let group = DispatchGroup()
        group.enter()
        subscribeOnlineUsers(channelName: channelName) {
            group.leave()
        }
        group.enter()
        addUserInfo(channelName: channelName) {
            group.leave()
        }
        group.notify(queue: .main) {
            SyncUtil.scene(id: channelName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).get(success: { list in
                let users = list.compactMap({ JSONObject.toModel(AgoraVoiceUsersModel.self,
                                                                 value: $0.toJson()) })
                guard !users.isEmpty else { return }
                self.dataArray = users
            }, fail: { _ in
                
            })
        }
    }
    
    func delete(channelName: String) {
        let objectId = dataArray.filter({ $0.userId == UserInfo.uid && $0.objectId != nil }).first?.objectId ?? ""
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).delete(id: objectId, success: nil, fail: nil)
    }
    
    private func addUserInfo(channelName: String, finished: @escaping () -> Void) {
        let model = AgoraVoiceUsersModel()
        dataArray.append(model)
        let params = JSONObject.toJson(model)
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_SCENE_ROOM_USER_COLLECTION).add(data: params, success: { object in
            finished()
        }, fail: { error in
            print(error.message)
            finished()
        })
    }
    
    private func subscribeOnlineUsers(channelName: String, finished: @escaping () -> Void) {
        SyncUtil.scene(id: channelName)?.subscribe(key: SYNC_SCENE_ROOM_USER_COLLECTION, onCreated: { _ in
            
        }, onUpdated: { object in
            guard let model = JSONObject.toModel(AgoraVoiceUsersModel.self,
                                                 value: object.toJson()) else { return }
            if self.dataArray.contains(where: { $0.userId == model.userId }) { return }
            self.dataArray.append(model)
        }, onDeleted: { object in
            if let index = self.dataArray.firstIndex(where: { object.getId() == $0.objectId }) {
                self.dataArray.remove(at: index)
            }
        }, onSubscribed: {
            LogUtils.log(message: "subscribe message", level: .info)
            finished()
        }, fail: { error in
            ToastView.show(text: error.message)
            finished()
        })
    }
    
    private func setupUI() {
        onLineView.translatesAutoresizingMaskIntoConstraints = false
        personImageView.translatesAutoresizingMaskIntoConstraints = false
        onLineLabel.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(onLineView)
        onLineView.addSubview(personImageView)
        onLineView.addSubview(onLineLabel)
        addSubview(collectionView)
        
        heightAnchor.constraint(equalToConstant: 28).isActive = true
        onLineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -1).isActive = true
        onLineView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        onLineView.heightAnchor.constraint(equalTo: heightAnchor).isActive = true
        personImageView.leadingAnchor.constraint(equalTo: onLineView.leadingAnchor, constant: 10).isActive = true
        personImageView.centerYAnchor.constraint(equalTo: onLineView.centerYAnchor).isActive = true
        
        onLineLabel.leadingAnchor.constraint(equalTo: personImageView.trailingAnchor, constant: 6).isActive = true
        onLineLabel.centerYAnchor.constraint(equalTo: onLineView.centerYAnchor).isActive = true
        onLineLabel.trailingAnchor.constraint(equalTo: onLineView.trailingAnchor,constant: -10).isActive = true
        
        collectionView.trailingAnchor.constraint(equalTo: onLineView.leadingAnchor, constant: -10).isActive = true
        collectionView.heightAnchor.constraint(equalTo: onLineView.heightAnchor).isActive = true
        collectionViewCons = collectionView.widthAnchor.constraint(equalToConstant: 28)
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionViewCons?.isActive = true
//        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
    }
}
extension LiveOnlineView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "LiveOnLineViewCell", for: indexPath) as! LiveOnLineViewCell
        cell.setUserInfo(info: dataArray[indexPath.item])
        return cell
    }
}

class LiveOnLineViewCell: UICollectionViewCell {
    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "pic-0"))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.cornerRadius = 14
        imageView.layer.masksToBounds = true
        return imageView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setUserInfo(info: AgoraVoiceUsersModel) {
        avatarImageView.image = UIImage(named: info.avatar)
        print("avatar ==== \(info.avatar)")
    }
    
    private func setupUI() {
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(avatarImageView)
        avatarImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        avatarImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        avatarImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        avatarImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
}
