//
//  ViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import AgoraRtcKit
import Agora_Scene_Utils

enum SceneType: String {
    /// 单直播
    case singleLive = "signleLive"
    /// 超级小班课
    case breakoutRoom = "BreakOutRoom"
    /// 视频通话
    case videoCall = "VideoCall"
    /// 音效
    case voiceChatRoom = "agoraVoice"
    /// 夜店
    case agoraClub = "agoraClub"
    /// PKApply
    case pkApply = "pkApplyInfo"
    /// 融合cdn
    case cdn = "PKByCDN"
    /// 多人连麦
    case mutli = "mutli"
    /// 电商
    case shopping = "shopping"
    /// 互动播客
    case interactiveBlog = "interactiveBlog"
    
    var alertTitle: String {
        switch self {
        case .pkApply: return "PK_Recieved_Invite".localized
        default: return ""
        }
    }
}

struct MainModel {
    var title: String = ""
    var imageNmae: String = ""
    var sceneType: SceneType = .singleLive
    
    static func mainDatas() -> [[MainModel]] {
        var dataArray = [[MainModel]]()
        var tempArray = [MainModel]()
        var model = MainModel()
        model.title = "Single_Broadcaster".localized
        model.imageNmae = "LiveSingle"
        model.sceneType = .singleLive
        tempArray.append(model)
        
        model = MainModel()
        model.title = "sound_effect".localized
        model.imageNmae = "VideoCall"
        model.sceneType = .videoCall
        tempArray.append(model)

        model = MainModel()
        model.title = "VoiceChatRoom".localized
        model.imageNmae = "Chatroom"
        model.sceneType = .voiceChatRoom
        tempArray.append(model)
        
        model = MainModel()
        model.title = "PK_Live".localized
        model.imageNmae = "LivePK"
        model.sceneType = .pkApply
        tempArray.append(model)

        model = MainModel()
        model.title = "CDN".localized
        model.imageNmae = "cdn"
        model.sceneType = .cdn
        tempArray.append(model)
        
        model = MainModel()
        model.title = "Multi-person joint broadcasting".localized
        model.imageNmae = "mutli"
        model.sceneType = .mutli
        tempArray.append(model)
        
        model = MainModel()
        model.title = "Interactive blog".localized
        model.imageNmae = "interactiveblog"
        model.sceneType = .interactiveBlog
        tempArray.append(model)
        
        model = MainModel()
        model.title = "Shopping".localized
        model.imageNmae = "interactiveblog"
        model.sceneType = .shopping
        tempArray.append(model)
        
        dataArray.append(tempArray)
        
        tempArray = [MainModel]()
        model = MainModel()
        model.title = "breakoutroom".localized
        model.imageNmae = "BreakoutRoom"
        model.sceneType = .breakoutRoom
        tempArray.append(model)
        
//        model = MainModel()
//        model.title = "agoraClub".localized
//        model.desc = "agoraClub".localized
//        model.imageNmae = "pic-Blind-date"
//        model.sceneType = .agoraClub
//        tempArray.append(model)
        dataArray.append(tempArray)
        
        return dataArray
    }
    
    static func sceneId(type: SceneType) -> String {
        type.rawValue
    }
}


class MainViewController: BaseViewController {
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.channelProfile = .liveBroadcasting
        config.areaCode = .global
        return config
    }()
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        let w = (Screen.width - 40) / 2
        view.itemSize = CGSize(width: w, height: w)
        view.minInteritemSpacing = 10
        view.minLineSpacing = 15
        view.delegate = self
        view.scrollDirection = .vertical
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        view.dataArray = MainModel.mainDatas()
        view.register(MainCollectionViewCell.self,
                      forCellWithReuseIdentifier: MainCollectionViewCell.description())
        view.register(MainHeaderViewCell.self,
                      forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
                      withReuseIdentifier: MainHeaderViewCell.description())
        return view
    }()
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "home".localized
        setupUI()
//        setupAgoraKit()
    }
    
    private func setupAgoraKit() {
        let agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: nil)
        agoraKit.setParameters("{\"rtc.report_app_scenario\":{\"appScenario\":\(APP_SCENARIO),\"serviceType\":\(SERVICE_TYPE),\"appVersion\":\"\(AgoraRtcEngineKit.getSdkVersion())\"}}")
        agoraKit.setLogFile(LogUtils.sdkLogPath())
    }
    
    private func setupUI() {
        view.addSubview(collectionView)
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
}

extension MainViewController: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: MainCollectionViewCell.description(),
                                                      for: indexPath) as! MainCollectionViewCell
        cell.setupData(model: MainModel.mainDatas()[indexPath.section][indexPath.item])
        return cell
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let sceneType = MainModel.mainDatas()[indexPath.section][indexPath.item].sceneType
        SyncUtil.initSyncManager(sceneId: sceneType.rawValue) {
            let model = MainModel.mainDatas()[indexPath.section][indexPath.item]
            if sceneType == .breakoutRoom {
                let breakoutRoomVC = BORHomeViewController()
                breakoutRoomVC.title = model.title
                self.navigationController?.pushViewController(breakoutRoomVC, animated: true)
            } else if sceneType == .agoraClub {
                let clubProgramVC = AgoraClubProgramViewController()
                self.navigationController?.pushViewController(clubProgramVC, animated: true)
                
            } else if sceneType == .cdn {
                let vc = CDNRoomListViewController(appId: KeyCenter.AppId)
                vc.title = model.title
                self.navigationController?.pushViewController(vc, animated: true)
                
            } else {
                let roomListVC = LiveRoomListController(sceneType: sceneType)
                roomListVC.title = model.title
                self.navigationController?.pushViewController(roomListVC, animated: true)
            }
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        let view = collectionView.dequeueReusableSupplementaryView(ofKind: UICollectionView.elementKindSectionHeader,
                                                                   withReuseIdentifier: MainHeaderViewCell.description(),
                                                                   for: indexPath) as! MainHeaderViewCell
        let title = indexPath.section == 0 ? "social_entertainment".localized : "education".localized
        view.setTitle(title: title)
        return view
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForHeaderInSection section: Int) -> CGSize {
        CGSize(width: Screen.width, height: 40)
    }
}
