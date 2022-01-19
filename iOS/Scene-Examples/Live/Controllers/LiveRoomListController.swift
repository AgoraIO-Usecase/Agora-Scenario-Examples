//
//  LiveRoomListController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraSyncManager
import AgoraUIKit_iOS

class LiveRoomListController: BaseViewController {
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
        view.isPagingEnabled = false
        view.addRefresh()
        view.register(LiveRoomListCell.self,
                      forCellWithReuseIdentifier: LiveRoomListCell.description())
        return view
    }()
    private lazy var createLiveButton: UIButton = {
        let button = UIButton()
        button.setBackgroundImage(UIImage(named: "create_room"), for: .normal)
        button.addTarget(self, action: #selector(clickCreateLiveButton), for: .touchUpInside)
        return button
    }()
    private var dataArray = [LiveRoomInfo]()
    private var sceneType: SceneType = .singleLive
    
    init(sceneType: SceneType) {
        super.init(nibName: nil, bundle: nil)
        self.sceneType = sceneType
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        showWaitHUD()
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false)
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        getLiveData()
    }
    
    private func getLiveData() {
        SyncUtil.fetchAll { results in
            self.hideHUD()
            self.roomView.endRefreshing()
            print("result == \(results.compactMap{ $0.toJson() })")
            self.dataArray = results.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(LiveRoomInfo.self, value: $0 )})
            self.roomView.dataArray = self.dataArray
        } fail: { error in
            self.hideHUD()
            self.roomView.endRefreshing()
            LogUtils.log(message: "get live data error == \(error.localizedDescription)", level: .info)
        }
    }
    
    private func setupUI() {
        view.addSubview(roomView)
        view.addSubview(createLiveButton)
        roomView.translatesAutoresizingMaskIntoConstraints = false
        roomView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        roomView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        roomView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        roomView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        createLiveButton.translatesAutoresizingMaskIntoConstraints = false
        createLiveButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -15).isActive = true
        createLiveButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -25).isActive = true
        createLiveButton.widthAnchor.constraint(equalToConstant: 44).isActive = true
        createLiveButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
    }
    
    @objc
    private func clickCreateLiveButton() {
        let model = dataArray.filter({ $0.userId == UserInfo.uid }).first
        if model != nil {
            showAlert(title: "您已创建房间, 将为您跳转进入", message: "") {
                let params = JSONObject.toJson(model)
                SyncUtil.joinScene(id: model?.roomId ?? "",
                                   userId: model?.userId ?? "",
                                   property: params, success: { result in
                    self.joinSceneHandler(result: result)
                })
            }
            return
        }
        let createLiveVC = CreateLiveController(sceneType: sceneType)
        navigationController?.pushViewController(createLiveVC, animated: true)
    }
    
    private func joinSceneHandler(result: IObject) {
        let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
        let ownerId = result.getPropertyWith(key: "userId", type: String.self) as? String
        switch sceneType {
        case .singleLive:
            let livePlayerVC = SignleLiveController(channelName: channelName ?? "", sceneType: sceneType, userId: ownerId ?? "")
            navigationController?.pushViewController(livePlayerVC, animated: true)
        case .pkApply:
            let pkLiveVC = PKLiveController(channelName: channelName ?? "", sceneType: sceneType, userId: ownerId ?? "")
            navigationController?.pushViewController(pkLiveVC, animated: true)
            
        case .breakoutRoom:
            let breakoutRoomVC = BORRoomDetailController(channelName: channelName ?? "", ownerId: ownerId ?? "")
            navigationController?.pushViewController(breakoutRoomVC, animated: true)
            
        case .game:
            let dgLiveVC = GameLiveController(channelName: channelName ?? "", sceneType: sceneType, userId: ownerId ?? "")
            navigationController?.pushViewController(dgLiveVC, animated: true)
            
        case .playTogether:
            let dgLiveVC = PlayTogetherViewController(channelName: channelName ?? "", sceneType: sceneType, userId: ownerId ?? "")
            navigationController?.pushViewController(dgLiveVC, animated: true)
            
        case .oneToOne:
            let oneToOneVC = OneToOneViewController(channelName: channelName ?? "", sceneType: sceneType, userId: ownerId ?? "")
            navigationController?.pushViewController(oneToOneVC, animated: true)
            
        default: break
        }
    }
}

extension LiveRoomListController: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let item = roomView.dataArray?[indexPath.item] as? LiveRoomInfo else { return }
        let params = JSONObject.toJson(item)
        SyncUtil.joinScene(id: item.roomId, userId: item.userId, property: params, success: { result in
            self.joinSceneHandler(result: result)
        })

    }
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveRoomListCell.description(),
                                                      for: indexPath) as! LiveRoomListCell
        cell.setRoomInfo(info: roomView.dataArray?[indexPath.item])
        return cell
    }
    func pullToRefreshHandler() {
        getLiveData()
    }
}
