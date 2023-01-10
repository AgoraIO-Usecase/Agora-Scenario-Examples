//
//  SuperAppRoomListViewController.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/12.
//

import UIKit

class CDNRoomListViewController: BaseViewController {
    private let entryView = CDNRoomListView()
    private var rooms = [CDNRoomInfo]()
    private let appId: String
    
    public init(appId: String) {
        self.appId = appId
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        setup()
        fetchRooms()
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false, isHiddenNavBar: false)
        fetchRooms()
    }
    
    private func setup() {
        view.addSubview(entryView)
        entryView.delegate = self
        entryView.translatesAutoresizingMaskIntoConstraints = false
        entryView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        entryView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        entryView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        entryView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
        
    func fetchRooms() {
        SyncUtil.fetchAll { [weak self] objs in
            let rooms = objs.compactMap({ JSONObject.toModel(CDNRoomInfo.self, value: $0.toJson()) })
            self?.udpateRooms(rooms: rooms)
            self?.entryView.endRefreshing()
        } fail: { [weak self] error in
            self?.entryView.endRefreshing()
        }
    }
    
    func udpateRooms(rooms: [CDNRoomInfo]) {
        let infos = rooms.map({ item -> LiveRoomInfo in
            var roomInfo = LiveRoomInfo()
            roomInfo.roomName = item.roomName
            roomInfo.backgroundId = item.roomId.headImageName
            return roomInfo
        })
        self.rooms = rooms
        entryView.update(infos: infos)
    }
    
    func getRoomInfo(index: Int) -> CDNRoomInfo? {
        rooms[index]
    }
}

extension CDNRoomListViewController: SuperAppRoomListViewDelegate {
    func entryViewdidPull(_ view: CDNRoomListView) {
        fetchRooms()
    }
    
    func entryViewDidTapCreateButton(_ view: CDNRoomListView) {
        let vc = CDNCreateLiveViewController(appId: appId)
        vc.delegate = self
        vc.modalPresentationStyle = .fullScreen
        present(vc, animated: true, completion: nil)
    }
    
    func entryView(_ view: CDNRoomListView,
                   didSelected info: LiveRoomInfo,
                   at index: Int) {
        guard let roomInfo = getRoomInfo(index: index) else {
            return
        }
        /// 作为观众进入
        let config = CDNPlayerViewControllerAudience.Config(appId: appId,
                                                           roomInfo: roomInfo)
        NetworkManager.shared.generateToken(channelName: config.sceneId, uid: UserInfo.userId) {
            let vc = CDNPlayerViewControllerAudience(config: config)
            vc.modalPresentationStyle = .fullScreen
            self.present(vc, animated: true, completion: nil)
        }
    }
}

extension CDNRoomListViewController: CDNCreateLiveDelegate {
    func createLiveVC(_ vc: CDNCreateLiveViewController,
                      didSart roomName: String,
                      sellectedType: CDNCreateLiveViewController.SelectedType) {
        /// 作为主播进入
        let createTime = Double(Int(Date().timeIntervalSince1970 * 1000) )
        let roomId = "\(Int(createTime))"
        let liveMode: LiveMode = sellectedType == .value1 ? .push : .byPassPush
        let roomItem = CDNRoomInfo(roomId: roomId, roomName: roomName, liveMode: liveMode)
        
        let config = CDNPlayerViewControllerHost.Config(appId: appId,
                                                       roomItem: roomItem)
        NetworkManager.shared.generateToken(channelName: config.sceneId, uid: UserInfo.userId) {
            let vc = CDNPlayerViewControllerHost(config: config)
            vc.modalPresentationStyle = .fullScreen
            self.present(vc, animated: true, completion: nil)
        }
    }
}
