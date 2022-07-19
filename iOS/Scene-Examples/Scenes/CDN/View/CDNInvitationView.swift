//
//  InvitationView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/18.
//

import UIKit

protocol CDNInvitationViewDelegate: NSObjectProtocol {
    func invitationView(_ view: CDNInvitationView, didSelected info: CDNInvitationView.Info)
}

class CDNInvitationView: UIView {
    typealias Info = CDNInvitationViewCell.Info
    let tableView = UITableView(frame: .zero,
                                style: .plain)
    let titleLabel = UILabel()
    private var infos = [CDNInvitationViewCell.Info]()
    weak var delegate: CDNInvitationViewDelegate?
    var manager: CDNInvitationSheetManager?
    
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
        tableView.register(CDNInvitationViewCell.self,
                           forCellReuseIdentifier: "InvitationCell")
        tableView.delegate = self
        tableView.dataSource = self
    }
    
    private func update(infos: [CDNInvitationViewCell.Info]) {
        self.infos = infos
        tableView.reloadData()
    }
    
    func startFetch(manager: CDNInvitationSheetManager) {
        self.manager = manager
        manager.delegate = self
        manager.fetchInfos()
    }
}

extension CDNInvitationView: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return infos.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "InvitationCell", for: indexPath) as! CDNInvitationViewCell
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

extension CDNInvitationView: CDNInvitationSheetManagerDelegate {
    func CDNInvitationSheetManagerDidFetch(infos: [CDNInvitationSheetManager.Info]) {
        update(infos: infos)
    }
}

extension CDNInvitationView: CDNInvitationCellDelegate {
    func cell(_ cell: CDNInvitationViewCell, on index: Int) {
        let info = infos[index]
        delegate?.invitationView(self, didSelected: info)
    }
}
