//
//  LiveRoomListCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit

class LiveRoomListCell: UICollectionViewCell {
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor(hex: "#ffffff")
        label.font = UIFont.systemFont(ofSize: 14)
        return label
    }()
    private lazy var bgImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "placeholder"))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        return imageView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        bgImageView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(bgImageView)
        contentView.addSubview(titleLabel)
        bgImageView.layer.cornerRadius = 10
        bgImageView.leftAnchor.constraint(equalTo: contentView.leftAnchor).isActive = true
        bgImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        bgImageView.rightAnchor.constraint(equalTo: contentView.rightAnchor).isActive = true
        bgImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6).isActive = true
    }
    
    func setRoomInfo(info: Any?) {
        if let info = info as? BORLiveModel {
            titleLabel.text = info.id
            bgImageView.image = UIImage(named: info.backgroundId)
        }
        if let info = info as? LiveRoomInfo {
            titleLabel.text = info.roomName
            bgImageView.image = UIImage(named: info.backgroundId)
        }
    }
}
