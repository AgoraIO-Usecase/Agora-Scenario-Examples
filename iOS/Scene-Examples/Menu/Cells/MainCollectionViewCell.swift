//
//  MainTableViewCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

class MainCollectionViewCell: UICollectionViewCell {
    private lazy var bgImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "LiveSingle"))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        imageView.layer.cornerRadius = 5
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Single_Broadcaster".localized
        label.textColor = .white
        label.font = .boldSystemFont(ofSize: 14)
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        bgImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(bgImageView)
        bgImageView.addSubview(titleLabel)
        
        bgImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        bgImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        bgImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        bgImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: bgImageView.leadingAnchor, constant: 12).isActive = true
        titleLabel.topAnchor.constraint(equalTo: bgImageView.topAnchor, constant: 10).isActive = true
    }
    
    func setupData(model: MainModel) {
        titleLabel.text = model.title
        bgImageView.image = UIImage(named: model.imageNmae)
    }
}
