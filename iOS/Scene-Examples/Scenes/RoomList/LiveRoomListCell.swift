//
//  LiveRoomListCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import Agora_Scene_Utils

class LiveRoomListCell: UITableViewCell {
    private lazy var containerView: AGEView = {
        let view = AGEView()
        view.backgroundColor = .white
        view.layer.cornerRadius = 5
        view.layer.shadowColor = UIColor.black.withAlphaComponent(0.7).cgColor
        view.layer.shadowOffset = CGSize(width: 0, height: 0)
        view.layer.shadowRadius = 5
        view.layer.shadowOpacity = 0.8
        return view
    }()
    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "placeholder"))
        imageView.contentMode = .scaleAspectFit
        imageView.layer.masksToBounds = true
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .black
        label.font = UIFont.systemFont(ofSize: 16)
        label.numberOfLines = 0
        return label
    }()
    private lazy var descLabel: AGELabel = {
        let label = AGELabel(colorStyle: .disabled, fontStyle: .middle)
        label.font = .systemFont(ofSize: 12)
        label.text = "头脑风暴"
        return label
    }()
    private lazy var arrowImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "arrow_right")
        return imageView
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .clear
        containerView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        descLabel.translatesAutoresizingMaskIntoConstraints = false
        arrowImageView.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(containerView)
        containerView.addSubview(avatarImageView)
        containerView.addSubview(titleLabel)
        containerView.addSubview(descLabel)
        containerView.addSubview(arrowImageView)
        
        containerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        containerView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10).isActive = true
        containerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        containerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        avatarImageView.leftAnchor.constraint(equalTo: containerView.leftAnchor, constant: 15).isActive = true
        avatarImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 20).isActive = true
        titleLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 8).isActive = true
        
        descLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -5).isActive = true
        descLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        descLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4).isActive = true
        
        arrowImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor).isActive = true
        arrowImageView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -15).isActive = true
    }
    
    func setRoomInfo(info: Any?) {
        if let info = info as? LiveRoomInfo {
            titleLabel.text = "\(info.roomName)" + "room_number".localized + ":\(info.roomId)"
            let image = UIImage(named: "portrait0\(Int.random(in: 1...2))")//UIImage(named: info.backgroundId)
            avatarImageView.image = image ?? UIImage(named: "clubBG")
            descLabel.text = info.roomName
        } else if let info = info as? BORLiveModel {
            titleLabel.text = "room_number".localized + ":\(info.id)"
            let image = UIImage(named: info.backgroundId)
            avatarImageView.image = image ?? UIImage(named: "clubBG")
            descLabel.text = info.userId
        }
    }
}
