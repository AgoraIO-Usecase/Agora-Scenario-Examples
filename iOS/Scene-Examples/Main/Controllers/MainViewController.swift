//
//  ViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import Agora_Scene_Utils

class MainViewController: BaseViewController {
    private lazy var tableView: AGETableView = {
        let view = AGETableView()
        view.estimatedRowHeight = 100
        view.delegate = self
        view.register(MainTableViewCell.self,
                      forCellWithReuseIdentifier: MainTableViewCell.description())
        view.dataArray = MainModel.mainDatas()
        return view
    }()
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "home".localized
        setupUI()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false, isHiddenNavBar: false)
        let image = UIImage().color(.white, height: Screen.kNavHeight)
        navigationController?.navigationBar.isTranslucent = false
        navigationController?.navigationBar.setBackgroundImage(image, for: .any, barMetrics: .default)
        navigationController?.navigationBar.tintColor = .black
        navigationController?.navigationBar.barTintColor = .black
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.black]
        navigationController?.view.backgroundColor = view.backgroundColor
    }
    
    private func setupUI() {
        view.addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
}

extension MainViewController: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: MainTableViewCell.description(),
                                                 for: indexPath) as! MainTableViewCell
        cell.setupData(model: MainModel.mainDatas()[indexPath.row])
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let sceneType = MainModel.mainDatas()[indexPath.row].sceneType
        SyncUtil.initSyncManager(sceneId: sceneType.rawValue)
        if sceneType == .breakoutRoom {
            let breakoutRoomVC = BORHomeViewController()
            breakoutRoomVC.title = MainModel.mainDatas()[indexPath.row].title
            navigationController?.pushViewController(breakoutRoomVC, animated: true)
        } else if sceneType == .agoraClub {
            let clubProgramVC = AgoraClubProgramViewController()
            navigationController?.pushViewController(clubProgramVC, animated: true)
            
        } else if sceneType == .superApp {
            let vc = SuperAppRoomListViewController(appId: KeyCenter.AppId)
            vc.title = MainModel.mainDatas()[indexPath.row].title
            navigationController?.pushViewController(vc, animated: true)
            
        } else {
            let roomListVC = LiveRoomListController(sceneType: sceneType)
            roomListVC.title = MainModel.mainDatas()[indexPath.row].title
            navigationController?.pushViewController(roomListVC, animated: true)
        }
    }
    
    func pullToRefreshHandler() {
        
    }
}
