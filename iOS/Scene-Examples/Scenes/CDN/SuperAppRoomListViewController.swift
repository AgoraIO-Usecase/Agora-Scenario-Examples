//
//  SuperAppRoomListViewController.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/12.
//

import UIKit

class SuperAppRoomListViewController: BaseViewController {
    private let entryView = SuperAppRoomListView()
    private var syncManager: AgoraSyncManager!
    private var sceneRef: SceneReference?
    private var rooms = [SuperAppRoomInfo]()
    private let appId: String
    private let defaultChannelName = "PKByCDN"
    
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
        commonInit()
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false, isHiddenNavBar: false)
        fetchRooms()
    }
    
    private func setup() {
        view.addSubview(entryView)
        entryView.translatesAutoresizingMaskIntoConstraints = false
        entryView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        entryView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        entryView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        entryView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
    
    private func commonInit() {
        entryView.delegate = self
        let config = AgoraSyncManager.RtmConfig(appId: appId,
                                                channelName: defaultChannelName)
        self.syncManager = AgoraSyncManager(config: config,
                                            complete: { [weak self](code) in
            self?.fetchRooms()
        })
    }
    
    func fetchRooms() {
        syncManager.getScenes { [weak self](objs) in
            let decoder = JSONDecoder()
            let rooms = objs.compactMap({ $0.toJson()?.data(using: .utf8) })
                .compactMap({ try? decoder.decode(SuperAppRoomInfo.self, from: $0) })
            self?.udpateRooms(rooms: rooms)
            self?.entryView.endRefreshing()
        } fail: { [weak self](error) in
            self?.entryView.endRefreshing()
        }
    }
    
    func udpateRooms(rooms: [SuperAppRoomInfo]) {
        let infos = rooms.map({ item -> LiveRoomInfo in
            var roomInfo = LiveRoomInfo()
            roomInfo.roomName = item.roomName
            roomInfo.backgroundId = item.roomId.headImageName
            return roomInfo
        })
        self.rooms = rooms
        entryView.update(infos: infos)
    }
    
    func getRoomInfo(index: Int) -> SuperAppRoomInfo? {
        rooms[index]
    }
}

extension SuperAppRoomListViewController: SuperAppRoomListViewDelegate {
    func entryViewdidPull(_ view: SuperAppRoomListView) {
        fetchRooms()
    }
    
    func entryViewDidTapCreateButton(_ view: SuperAppRoomListView) {
        let vc = SuperAppCreateLiveViewController(appId: appId)
        vc.delegate = self
        vc.modalPresentationStyle = .fullScreen
        present(vc, animated: true, completion: nil)
    }
    
    func entryView(_ view: SuperAppRoomListView,
                   didSelected info: LiveRoomInfo,
                   at index: Int) {
        guard let roomInfo = getRoomInfo(index: index) else {
            return
        }
        /// 作为观众进入
        let config = SuperAppPlayerViewControllerAudience.Config(appId: appId,
                                                           roomInfo: roomInfo)
        let vc = SuperAppPlayerViewControllerAudience(config: config)
        vc.modalPresentationStyle = .fullScreen
        present(vc, animated: true, completion: nil)
    }
}

extension SuperAppRoomListViewController: SuperAppCreateLiveDelegate {
    func createLiveVC(_ vc: SuperAppCreateLiveViewController,
                      didSart roomName: String,
                      sellectedType: SuperAppCreateLiveViewController.SelectedType) {
        /// 作为主播进入
        let createTime = Double(Int(Date().timeIntervalSince1970 * 1000) )
        let roomId = "\(Int(createTime))"
        let liveMode: LiveMode = sellectedType == .value1 ? .push : .byPassPush
        let roomItem = SuperAppRoomInfo(roomId: roomId, roomName: roomName, liveMode: liveMode)
        
        let config = SuperAppPlayerViewControllerHost.Config(appId: appId,
                                                       roomItem: roomItem)
        let vc = SuperAppPlayerViewControllerHost(config: config)
        
        vc.modalPresentationStyle = .fullScreen
        present(vc, animated: true, completion: nil)
    }
}
