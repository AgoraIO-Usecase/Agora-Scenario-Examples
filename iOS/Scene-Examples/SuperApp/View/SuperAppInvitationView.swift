//
//  InvitationView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/18.
//

import UIKit

protocol SuperAppInvitationViewDelegate: NSObjectProtocol {
    func invitationView(_ view: SuperAppInvitationView, didSelected info: SuperAppInvitationView.Info)
}

class SuperAppInvitationView: UIView {
    typealias Info = SuperAppInvitationViewCell.Info
    let tableView = UITableView(frame: .zero,
                                style: .plain)
    let titleLabel = UILabel()
    private var infos = [SuperAppInvitationViewCell.Info]()
    weak var delegate: SuperAppInvitationViewDelegate?
    var manager: SuperAppInvitationSheetManager?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
        commomInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setup() {
        backgroundColor = .white
        translatesAutoresizingMaskIntoConstraints = false
        widthAnchor.constraint(equalToConstant: cl_screenWidht).isActive = true
        heightAnchor.constraint(equalToConstant: 350).isActive = true
        titleLabel.text = "在线用户"
        titleLabel.textColor = .gray
        addSubview(tableView)
        addSubview(titleLabel)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        
        tableView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10).isActive = true
        tableView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    private func commomInit() {
        tableView.register(SuperAppInvitationViewCell.self,
                           forCellReuseIdentifier: "InvitationCell")
        tableView.delegate = self
        tableView.dataSource = self
    }
    
    private func update(infos: [SuperAppInvitationViewCell.Info]) {
        self.infos = infos
        tableView.reloadData()
    }
    
    func startFetch(manager: SuperAppInvitationSheetManager) {
        self.manager = manager
        manager.delegate = self
        manager.fetchInfos()
    }
}

extension SuperAppInvitationView: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return infos.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "InvitationCell", for: indexPath) as! SuperAppInvitationViewCell
        cell.selectionStyle = .none
        cell.delegate = self
        let info = infos[indexPath.row]
        cell.udpate(info: info)
        return cell
    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }
}

extension SuperAppInvitationView: SuperAppInvitationSheetManagerDelegate {
    func superAppInvitationSheetManagerDidFetch(infos: [SuperAppInvitationSheetManager.Info]) {
        update(infos: infos)
    }
}

extension SuperAppInvitationView: SuperAppInvitationCellDelegate {
    func cell(_ cell: SuperAppInvitationViewCell, on index: Int) {
        let info = infos[index]
        delegate?.invitationView(self, didSelected: info)
    }
}
