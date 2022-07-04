//
//  VideoCollectionViewCell.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit

class VideoCollectionViewCell: BaseCollectionViewCell {
    private lazy var bgImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "placeholder"))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        return imageView
    }()
    private lazy var onLineCountLabel: UILabel = {
        let label = UILabel()
        label.backgroundColor = UIColor(hex: "#000000", alpha: 0.3)
        label.textColor = UIColor(hex: "#ffffff")
        label.font = UIFont.systemFont(ofSize: 12)
        label.layer.cornerRadius = 11
        label.layer.masksToBounds = true
        label.textAlignment = .center
        label.isHidden = true
        return label
    }()
    
    override func setupUI() {
        bgImageView.translatesAutoresizingMaskIntoConstraints = false
        onLineCountLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(bgImageView)
        contentView.addSubview(onLineCountLabel)
        super.setupUI()
        
        bgImageView.layer.cornerRadius = 10
        bgImageView.leftAnchor.constraint(equalTo: contentView.leftAnchor).isActive = true
        bgImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        bgImageView.rightAnchor.constraint(equalTo: contentView.rightAnchor).isActive = true
        bgImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        onLineCountLabel.leftAnchor.constraint(equalTo: bgImageView.leftAnchor, constant: 6).isActive = true
        onLineCountLabel.topAnchor.constraint(equalTo: bgImageView.topAnchor, constant: 6).isActive = true
        onLineCountLabel.widthAnchor.constraint(equalToConstant: 48).isActive = true
        onLineCountLabel.heightAnchor.constraint(equalToConstant: 22).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6).isActive = true
    }
    
    override func setupItemData(with item: Any?) {
        guard let model = item as? BORLiveModel else { return }
        bgImageView.image = UIImage(named: model.backgroundId)
        titleLabel.text = model.id
    }
}
