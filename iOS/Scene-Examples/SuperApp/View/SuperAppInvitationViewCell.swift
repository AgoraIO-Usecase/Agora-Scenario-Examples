//
//  InvitationCell.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/17.
//

import UIKit

protocol SuperAppInvitationCellDelegate: NSObjectProtocol {
    func cell(_ cell: SuperAppInvitationViewCell, on index: Int)
}

class SuperAppInvitationViewCell: UITableViewCell {
    private let headImageView = UIImageView()
    private let nameLabel = UILabel()
    private let inviteButton = UIButton()
    weak var delegate: SuperAppInvitationCellDelegate?
    private var info = Info.empty
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setup() {
        inviteButton.backgroundColor = .white
        inviteButton.layer.borderWidth = 2
        inviteButton.layer.borderColor = UIColor.systemBlue.cgColor
        inviteButton.layer.cornerRadius = 16
        inviteButton.setTitleColor(.systemBlue, for: .normal)
        inviteButton.setTitle("邀请", for: .normal)
        headImageView.layer.cornerRadius = 20
        headImageView.layer.masksToBounds = true
        
        contentView.addSubview(headImageView)
        contentView.addSubview(nameLabel)
        contentView.addSubview(inviteButton)
        
        headImageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        inviteButton.translatesAutoresizingMaskIntoConstraints = false
        
        headImageView.leftAnchor.constraint(equalTo: contentView.leftAnchor, constant: 16).isActive = true
        headImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        headImageView.widthAnchor.constraint(equalToConstant: 40).isActive = true
        headImageView.heightAnchor.constraint(equalToConstant: 40).isActive = true
        
        nameLabel.leftAnchor.constraint(equalTo: headImageView.rightAnchor, constant: 10).isActive = true
        nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        inviteButton.rightAnchor.constraint(equalTo: contentView.rightAnchor, constant: -15).isActive = true
        inviteButton.widthAnchor.constraint(equalToConstant: 80).isActive = true
        inviteButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
    }
    
    func commonInit() {
        inviteButton.addTarget(self,
                               action: #selector(buttonTap(_:)),
                               for: .touchUpInside)
    }
    
    @objc func buttonTap(_ sender: UIButton) {
        delegate?.cell(self, on: info.idnex)
    }
    
    func udpate(info: Info) {
        self.info = info
        headImageView.image = UIImage(named: info.imageName)
        nameLabel.text = info.title
        inviteButton.isHidden = info.isInvited
    }
}

extension SuperAppInvitationViewCell {
    enum InviteButtonState {
        case none, inviting, availableInvite
    }
    
    struct Info {
        let idnex: Int
        let title: String
        let imageName: String
        let isInvited: Bool
        let userId: String
        
        static var empty: Info {
            return Info(idnex: 0,
                        title: "",
                        imageName: "",
                        isInvited: false,
                        userId: "")
        }
    }
}
