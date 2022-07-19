//
//  BORHomeViewController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit
import Agora_Scene_Utils

class BORHomeViewController: BaseViewController {
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
    private lazy var addRoomButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "create_room"), for: .normal)
        button.addTarget(self, action: #selector(onTapAddRoomButton), for: .touchUpInside)
        return button
    }()
    
    private var dataArray = [BORLiveModel]()

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        ToastView.showWait(text: "loading".localized, view: view)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        getData()
    }
    
    private func getData() {
        SyncUtil.fetchAll { objects in
            ToastView.hidden()
            self.tableView.endRefreshing()
            print("result == \(objects.compactMap{ $0.toJson() })")
            self.dataArray = objects.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(BORLiveModel.self, value: $0 )})
            self.tableView.dataArray = self.dataArray
        } fail: { error in
            LogUtils.log(message: "get all data error == \(error.localizedDescription)", level: .error)
            ToastView.hidden()
            self.tableView.endRefreshing()
        }
    }
    
    private func setupUI() {
        view.addSubview(tableView)
        view.addSubview(addRoomButton)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        addRoomButton.translatesAutoresizingMaskIntoConstraints = false
        addRoomButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -25).isActive = true
        addRoomButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -35).isActive = true
    }
    @objc
    private func onTapAddRoomButton() {
        let createRoomVC = BORCreateRoomController()
        navigationController?.pushViewController(createRoomVC, animated: true)
    }
}

extension BORHomeViewController: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let item = dataArray[indexPath.item]
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
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: LiveRoomListCell.description(), for: indexPath) as! LiveRoomListCell
        cell.setRoomInfo(info: dataArray[indexPath.item])
        return cell
    }
    func pullToRefreshHandler() {
        getData()
    }
}
