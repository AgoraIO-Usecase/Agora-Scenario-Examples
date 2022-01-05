//
//  BORHomeViewController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit
import AgoraUIKit_iOS

class BORHomeViewController: BaseViewController {
    private lazy var roomView: AGECollectionView = {
        let view = AGECollectionView()
        view.delegate = self
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        view.minLineSpacing = 15
        view.minInteritemSpacing = 0
        let viewW = self.view.frame.width
        let w = (viewW - view.minLineSpacing - view.edge.left - view.edge.right) / 2.0
        view.itemSize = CGSize(width: w, height: w)
        view.scrollDirection = .vertical
        view.addRefresh()
        view.register(LiveRoomListCell.self,
                      forCellWithReuseIdentifier: LiveRoomListCell.description())
        return view
    }()
    private lazy var addRoomButton: UIButton = {
        let button = UIButton()
        button.setBackgroundImage(UIImage(systemName: "plus.circle.fill")?.withTintColor(.red, renderingMode: .alwaysOriginal), for: .normal)
        button.addTarget(self, action: #selector(clickAddRoomButton), for: .touchUpInside)
        return button
    }()
    
    private var dataArray = [BORLiveModel]()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "超级小班课"
        setupUI()
        showWaitHUD()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        getData()
    }
    
    private func getData() {
        SyncUtil.fetchAll { objects in
            self.hideHUD()
            self.roomView.endRefreshing()
            print("result == \(objects.compactMap{ $0.toJson() })")
            self.dataArray = objects.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(BORLiveModel.self, value: $0 )})
            self.roomView.dataArray = self.dataArray
        } fail: { error in
            LogUtils.log(message: "get all data error == \(error.localizedDescription)", level: .error)
            self.hideHUD()
            self.roomView.endRefreshing()
        }
    }
    
    private func setupUI() {
        view.addSubview(roomView)
        view.addSubview(addRoomButton)
        roomView.translatesAutoresizingMaskIntoConstraints = false
        roomView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        roomView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        roomView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        roomView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        addRoomButton.translatesAutoresizingMaskIntoConstraints = false
        addRoomButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -15).isActive = true
        addRoomButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -25).isActive = true
        addRoomButton.widthAnchor.constraint(equalToConstant: 44).isActive = true
        addRoomButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
    }
    @objc
    private func clickAddRoomButton() {
        let createRoomVC = BORCreateRoomController()
        navigationController?.pushViewController(createRoomVC, animated: true)
    }
}

extension BORHomeViewController: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let item = roomView.dataArray?[indexPath.item] as? BORLiveModel else { return }
        let params = JSONObject.toJson(item)
        SyncUtil.joinScene(id: item.id, userId: item.userId, property: params) { result in
            let channelName = result.getPropertyWith(key: "id", type: String.self) as? String
            let ownerId = result.getPropertyWith(key: "userId", type: String.self) as? String
            let roomDetailVC = BORRoomDetailController(channelName: channelName ?? "", ownerId: ownerId ?? "")
            self.navigationController?.pushViewController(roomDetailVC, animated: true)
        } fail: { error in
            LogUtils.log(message: "join scene error == \(error.localizedDescription)", level: .error)
        }
    }
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveRoomListCell.description(),
                                                      for: indexPath) as! LiveRoomListCell
        cell.setRoomInfo(info: roomView.dataArray?[indexPath.item])
        return cell
    }
    func pullToRefreshHandler() {
        getData()
    }
}
