//
//  AgoraClubProgramCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/4/22.
//

import UIKit
import Agora_Scene_Utils

class AgoraClubProgramCell: UITableViewCell {
    private lazy var bgImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "playing")
        return imageView
    }()
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupData(model: AgoraClubProgramModel) {
        bgImageView.image = UIImage(named: model.type.rawValue)
    }
    
    private func setupUI() {
        backgroundColor = .clear
        contentView.addSubview(bgImageView)
        bgImageView.translatesAutoresizingMaskIntoConstraints = false
        bgImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        bgImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14).isActive = true
        bgImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        bgImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
}
