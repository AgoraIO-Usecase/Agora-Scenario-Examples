//
//  AgoraClubProgramViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/4/22.
//

import UIKit
import Agora_Scene_Utils

class AgoraClubProgramViewController: BaseViewController {
    private lazy var tableView: AGETableView = {
        let view = AGETableView()
        view.estimatedRowHeight = 100
        view.delegate = self
        view.register(AgoraClubProgramCell.self,
                      forCellWithReuseIdentifier: "AgoraClubProgramCell")
        return view
    }()
    private let dataArray = AgoraClubProgramModel.createData()
        
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "agoraProgram".localized
        setupUI()
        tableView.dataArray = dataArray
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: false, isHiddenNavBar: false)
        let image = UIImage().color(.black, height: Screen.kNavHeight)
        navigationController?.navigationBar.isTranslucent = false
        navigationController?.navigationBar.setBackgroundImage(image, for: .any, barMetrics: .default)
        navigationController?.navigationBar.tintColor = .white
        navigationController?.navigationBar.barTintColor = .white
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.view.backgroundColor = view.backgroundColor
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        navigationTransparent(isTransparent: false)
    }
    private func setupUI() {
        backButton.setImage(UIImage(systemName: "chevron.backward")?
                                .withTintColor(.white, renderingMode: .alwaysOriginal),
                            for: .normal)
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: backButton)
        view.backgroundColor = .init(hex: "#0E141D")
        view.addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor).isActive = true
        tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor).isActive = true
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }
}
extension AgoraClubProgramViewController: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "AgoraClubProgramCell", for: indexPath) as! AgoraClubProgramCell
        cell.setupData(model: dataArray[indexPath.row])
        return cell
    }
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let model = dataArray[indexPath.row]
        let roomListVC = LiveRoomListController(sceneType: .agoraClub)
        roomListVC.clubProgramType = model.type
        roomListVC.title = "agoraClub".localized
        navigationController?.pushViewController(roomListVC, animated: true)
    }
}
