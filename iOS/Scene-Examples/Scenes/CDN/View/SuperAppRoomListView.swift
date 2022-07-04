//
//  EntryView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/12.
//

import UIKit
import Agora_Scene_Utils

protocol SuperAppRoomListViewDelegate: NSObjectProtocol {
    func entryViewDidTapCreateButton(_ view: SuperAppRoomListView)
    func entryView(_ view: SuperAppRoomListView,
                   didSelected info: LiveRoomInfo,
                   at index: Int)
    func entryViewdidPull(_ view: SuperAppRoomListView)
}

class SuperAppRoomListView: UIView {
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
    
    weak var delegate: SuperAppRoomListViewDelegate?
    var infos = [LiveRoomInfo]()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setup() {
        backgroundColor = .clear
        
        addSubview(tableView)
        addSubview(createLiveButton)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        
        createLiveButton.translatesAutoresizingMaskIntoConstraints = false
        createLiveButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -25).isActive = true
        createLiveButton.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -35).isActive = true
    }
    
    @objc func buttonTap(_ button: UIButton) {
        delegate?.entryViewDidTapCreateButton(self)
    }
    
    @objc func refreshPull() {
        delegate?.entryViewdidPull(self)
    }
    
    func update(infos: [LiveRoomInfo]) {
        self.infos = infos
        tableView.dataArray = infos
    }
    
    func endRefreshing() {
        tableView.endRefreshing()
    }
    @objc
    private func onTapCreateLiveButton() {
        delegate?.entryViewDidTapCreateButton(self)
    }
}

extension SuperAppRoomListView: AGETableViewDelegate {
    func numberOfSections(in collectionView: UICollectionView) -> Int {
        infos.count
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: LiveRoomListCell.description(), for: indexPath) as! LiveRoomListCell
        cell.setRoomInfo(info: infos[indexPath.item])
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let info = infos[indexPath.row]
        delegate?.entryView(self,
                            didSelected: info,
                            at: indexPath.row)
    }
    
    func pullToRefreshHandler() {
        delegate?.entryViewdidPull(self)
    }
}
