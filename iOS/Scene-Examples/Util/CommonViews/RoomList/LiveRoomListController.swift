//
//  LiveRoomListController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
//import AgoraSyncManager
import Agora_Scene_Utils

class LiveRoomListController: BaseViewController {
    private lazy var tableView: AGETableView = {
        let view = AGETableView()
        view.rowHeight = 58
        view.delegate = self
        view.emptyTitleColor = .white
        view.addRefresh()
        view.register(LiveRoomListCell.self,
                      forCellWithReuseIdentifier: LiveRoomListCell.description())
        return view
    }()
    private lazy var createLiveButton: UIButton = {
        let button = UIButton()
        button.setBackgroundImage(UIImage(named: "create_room"), for: .normal)
        button.addTarget(self, action: #selector(onTapCreateLiveButton), for: .touchUpInside)
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
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false)
        let appdelegate = UIApplication.shared.delegate as? AppDelegate
        appdelegate?.blockRotation = .portrait
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        getLiveData()
    }
    
    private func getLiveData() {
        ToastView.showWait(text: "loading".localized, view: view)
        SyncUtil.fetchAll { results in
            ToastView.hidden()
            self.tableView.endRefreshing()
            print("result == \(results.compactMap{ $0.toJson() })")
            self.dataArray = results.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(LiveRoomInfo.self, value: $0 )})
            self.tableView.dataArray = self.dataArray
        } fail: { error in
            ToastView.hidden()
            self.tableView.endRefreshing()
            LogUtils.log(message: "get live data error == \(error.localizedDescription)", level: .info)
        }
    }
    
    private func setupUI() {
        tableView.backgroundColor = view.backgroundColor
        view.addSubview(tableView)
        view.addSubview(createLiveButton)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        createLiveButton.translatesAutoresizingMaskIntoConstraints = false
        createLiveButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -25).isActive = true
        createLiveButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -35).isActive = true
    }
    
    @objc
    private func onTapCreateLiveButton() {
//        let model = dataArray.filter({ $0.userId == UserInfo.uid }).first
//        if model != nil {
//            showAlert(title: "you_have_created_the_room_will_jump_into_you".localized, message: "") {
//                let params = JSONObject.toJson(model)
//                SyncUtil.joinScene(id: model?.roomId ?? "",
//                                   userId: model?.userId ?? "",
//                                   property: params, success: { result in
//                    self.joinSceneHandler(result: result)
//                })
//            }
//            return
//        }
        switch sceneType {
        case .singleLive:
            let createLiveVC = LiveBroadcastingCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .breakoutRoom:
            break
            
        case .videoCall:
            let createLiveVC = VideoCallCreateViewController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .voiceChatRoom:
            let createLiveVC = VoiceChatRoomCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .pkApply:
            let createLiveVC = LivePKCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .mutli:
            let createLiveVC = MutliBroadcastingCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .interactiveBlog:
            let createLiveVC = InteractiveBlogCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .Education1v1:
            let createLiveVC = EducationCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
        }
        
    }
    
    private func joinSceneHandler(result: IObject) {
        let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
        let ownerId = result.getPropertyWith(key: "userId", type: String.self) as? String
        switch sceneType {
        case .singleLive:
            let livePlayerVC = LiveBroadcastingController(channelName: channelName ?? "", userId: ownerId ?? "")
            navigationController?.pushViewController(livePlayerVC, animated: true)
        case .pkApply:
            let pkLiveVC = LivePKController(channelName: channelName ?? "", userId: ownerId ?? "")
            navigationController?.pushViewController(pkLiveVC, animated: true)
            
        case .breakoutRoom:
            let breakoutRoomVC = BORRoomDetailController(channelName: channelName ?? "", ownerId: ownerId ?? "")
            navigationController?.pushViewController(breakoutRoomVC, animated: true)
            
        case .voiceChatRoom:
            let roomInfo = JSONObject.toModel(LiveRoomInfo.self, value: result.toJson())
            let agoraVoiceVC = VoiceChatRoomController(roomInfo: roomInfo)
            navigationController?.pushViewController(agoraVoiceVC, animated: true)
            
        case .videoCall:
            let livePlayerVC = VideoCallViewController(channelName: channelName ?? "", userId: ownerId ?? "")
            navigationController?.pushViewController(livePlayerVC, animated: true)
            
        case .mutli:
            let livePlayerVC = MutliBroadcastingController(channelName: channelName ?? "", userId: ownerId ?? "")
            navigationController?.pushViewController(livePlayerVC, animated: true)
            
        case .Education1v1:
            let shoppingVC = EducationController(channelName: channelName ?? "",
                                                 userId: ownerId ?? "")
            navigationController?.pushViewController(shoppingVC, animated: true)
            
        default: break
        }
    }
}

extension LiveRoomListController: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let item = dataArray[indexPath.item]
        let params = JSONObject.toJson(item)
        SyncUtil.joinScene(id: item.roomId, userId: item.userId, property: params, success: { result in
            let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
            NetworkManager.shared.generateToken(channelName: channelName ?? "") {
                self.joinSceneHandler(result: result)                
            }
        })
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: LiveRoomListCell.description(), for: indexPath) as! LiveRoomListCell
        
        cell.setRoomInfo(info: dataArray[indexPath.item])
        return cell
    }
    func pullToRefreshHandler() {
        getLiveData()
    }
}
