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
    
    private lazy var bannerView: InteractiveBlogBannerView = {
        let view = InteractiveBlogBannerView()
        view.isHidden = true
        view.onTapInteractiveBlogExitClosure = { [weak self] in
            self?.createLiveButton.isHidden = false
            view.isHidden = true
            self?.getLiveData()
        }
        view.onTapInteractiveBlogBannerViewClosure = { [weak self] channelName in
            guard let self = self else { return }
            self.createLiveButton.isHidden = false
            let roomInfo = self.dataArray.first(where: { $0.roomId == channelName })
            let params = JSONObject.toJson(roomInfo)
            SyncUtil.joinScene(id: roomInfo?.roomId ?? "",
                               userId: roomInfo?.userId ?? "",
                               property: params,
                               success: { result in
                let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
                let ownerId = result.getPropertyWith(key: "userId", type: String.self) as? String
                NetworkManager.shared.generateToken(channelName: channelName ?? "", uid: UserInfo.userId) {
                    let interactiveBlogVC = InteractiveBlogController(channelName: channelName ?? "", userId: ownerId ?? "", isAddUser: false)
                    self.navigationController?.pushViewController(interactiveBlogVC, animated: true)
                }
            })
        }
        interactiveBlogDownPullClosure = { [weak self] channelName, userModel, agoraKit, role in
            guard let self = self else { return }
            self.bannerView.isHidden = false
            self.createLiveButton.isHidden = true
            self.bannerView.setupParams(channelName: channelName, model: userModel, agoraKit: agoraKit, role: role)
        }
        return view
    }()
    
    private var dataArray = [LiveRoomInfo]()
    private var sceneType: SceneType = .singleLive
    
    var clubProgramType: AgoraClubProgramType?
    
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
        if sceneType == .agoraClub {
            navigationController?.navigationBar.setBackgroundImage(UIImage().color(view.backgroundColor, height: Screen.kNavHeight), for: .any, barMetrics: .default)
        }
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
            if let clubType = self.clubProgramType {
                self.dataArray = self.dataArray.filter({ $0.videoUrl == clubType.videoUrl})
            }
            self.tableView.dataArray = self.dataArray
        } fail: { error in
            ToastView.hidden()
            self.tableView.endRefreshing()
            LogUtils.log(message: "get live data error == \(error.localizedDescription)", level: .info)
        }
    }
    
    private func setupUI() {
        view.backgroundColor = sceneType == .agoraClub ? UIColor(hex: "#0E141D") : view.backgroundColor
        tableView.backgroundColor = view.backgroundColor
        if sceneType == .agoraClub {
            backButton.setImage(UIImage(systemName: "chevron.backward")?
                                    .withTintColor(.white, renderingMode: .alwaysOriginal),
                                for: .normal)
            navigationItem.leftBarButtonItem = UIBarButtonItem(customView: backButton)
        }
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
        
        view.addSubview(bannerView)
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        bannerView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        bannerView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -(Screen.safeAreaBottomHeight() + 30)).isActive = true
        bannerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
    }
    
    override func onTapBackButton() {
        super.onTapBackButton()
        guard sceneType == .interactiveBlog else { return }
        bannerView.leave()
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
            
        case .agoraClub:
            let createLiveVC = ClubCreateController()
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
        
        case .shopping:
            let createLiveVC = LiveShoppingCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .education1v1:
            let createLiveVC = EducationCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .smallClass:
            let createLiveVC = SmallClassCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .largeClass:
            let createLiveVC = LargeClassCreateController()
            navigationController?.pushViewController(createLiveVC, animated: true)
            
        case .cdn:
            break
        }
        
    }
    
    private func joinSceneHandler(result: IObject) {
        let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
        let ownerId = result.getPropertyWith(key: "userId", type: String.self) as? String
        switch sceneType {
        case .singleLive:
            let livePlayerVC = LiveBroadcastingController(channelName: channelName ?? "",
                                                          userId: ownerId ?? "")
            navigationController?.pushViewController(livePlayerVC, animated: true)
            
        case .pkApply:
            let pkLiveVC = LivePKController(channelName: channelName ?? "",
                                            userId: ownerId ?? "")
            navigationController?.pushViewController(pkLiveVC, animated: true)
            
        case .breakoutRoom:
            let breakoutRoomVC = BORRoomDetailController(channelName: channelName ?? "",
                                                         ownerId: ownerId ?? "")
            navigationController?.pushViewController(breakoutRoomVC, animated: true)
            
        case .voiceChatRoom:
            let roomInfo = JSONObject.toModel(LiveRoomInfo.self, value: result.toJson())
            let agoraVoiceVC = VoiceChatRoomController(roomInfo: roomInfo)
            navigationController?.pushViewController(agoraVoiceVC, animated: true)
            
        case .agoraClub:
            let videoUrl = result.getPropertyWith(key: "videoUrl", type: String.self) as? String ?? clubProgramType?.videoUrl
            let clubVC = AgoraClubController(userId: ownerId ?? "",
                                             channelName: channelName,
                                             videoUrl: videoUrl)
            navigationController?.pushViewController(clubVC, animated: true)
            
        case .videoCall:
            let livePlayerVC = VideoCallViewController(channelName: channelName ?? "",
                                                       userId: ownerId ?? "")
            navigationController?.pushViewController(livePlayerVC, animated: true)
            
        case .mutli:
            let mutliVC = MutliBroadcastingController(channelName: channelName ?? "",
                                                      userId: ownerId ?? "")
            navigationController?.pushViewController(mutliVC, animated: true)
            
        case .interactiveBlog:
            let interactiveBlogVC = InteractiveBlogController(channelName: channelName ?? "",
                                                              userId: ownerId ?? "",
                                                              isAddUser: bannerView.isHidden)
            navigationController?.pushViewController(interactiveBlogVC, animated: true)
            
        case .shopping:
            let shoppingVC = LiveShoppingViewController(channelName: channelName ?? "",
                                                               userId: ownerId ?? "")
            navigationController?.pushViewController(shoppingVC, animated: true)
            
        case .education1v1:
            let vc = EducationController(channelName: channelName ?? "",
                                         userId: ownerId ?? "")
            navigationController?.pushViewController(vc, animated: true)
            
        case .smallClass:
            let vc = SmallClassController(channelName: channelName ?? "",
                                          userId: ownerId ?? "")
            navigationController?.pushViewController(vc, animated: true)
            
        case .largeClass:
            let vc = LargeClassController(channelName: channelName ?? "",
                                          userId: ownerId ?? "")
            navigationController?.pushViewController(vc, animated: true)
        
        default: break
        }
    }
}

extension LiveRoomListController: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let item = dataArray[indexPath.item]
        if sceneType == .interactiveBlog {
            bannerView.checkRoom(channelName: item.roomId)
        }
        let params = JSONObject.toJson(item)
        SyncUtil.joinScene(id: item.roomId, userId: item.userId, property: params, success: { result in
            let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
            NetworkManager.shared.generateToken(channelName: channelName ?? "", uid: UserInfo.userId) {
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
