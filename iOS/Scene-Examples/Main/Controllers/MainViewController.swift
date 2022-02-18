//
//  ViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit
import AgoraUIKit_iOS

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
        title = "首页"
        setupUI()
        signIn()
    }
    
    private func signIn() {
        SudMGP.initSDK("1461564080052506636", appKey: "03pNxK2lEXsKiiwrBQ9GbH541Fk2Sfnc", isTestEnv: true) { code, _ in
            guard code == 0 else { return }
            print("初始化成功")
        }
        
        NetworkManager.shared.postRequest(urlString: "https://fat-mgp-hello.sudden.ltd/login/v2", params: ["user_id": UserInfo.uid]) { reponse in
            print("signIn response == \(reponse)")
            let data = reponse["data"] as? [String: Any]
            let token = data?["code"] as? String
            NetworkManager.shared.gameToken = token ?? ""
        } failure: { error in
            print("sign in error == \(error)")
        }
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
        } else {
            let roomListVC = LiveRoomListController(sceneType: sceneType)
            roomListVC.title = MainModel.mainDatas()[indexPath.row].title
            navigationController?.pushViewController(roomListVC, animated: true)
        }
    }
    
    func pullToRefreshHandler() {
        
    }
}
