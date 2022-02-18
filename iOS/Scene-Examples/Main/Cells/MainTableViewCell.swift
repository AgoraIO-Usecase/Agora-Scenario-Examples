//
//  MainTableViewCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

class MainTableViewCell: UITableViewCell {
    private lazy var bgImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "pic-single"))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Single_Broadcaster".localized
        label.textColor = .white
        label.font = .boldSystemFont(ofSize: 15)
        return label
    }()
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.text = "Single_Broadcaster".localized
        label.textColor = .white
        label.font = .systemFont(ofSize: 12)
        label.numberOfLines = 0
        return label
    }()
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        bgImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descLabel.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(bgImageView)
        bgImageView.addSubview(titleLabel)
        bgImageView.addSubview(descLabel)
        
        bgImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        bgImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        bgImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        bgImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -15).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: bgImageView.centerXAnchor, constant: 35).isActive = true
        titleLabel.centerYAnchor.constraint(equalTo: bgImageView.centerYAnchor, constant: -15).isActive = true
        descLabel.topAnchor.constraint(equalTo: bgImageView.centerYAnchor, constant: 5).isActive = true
        descLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        descLabel.trailingAnchor.constraint(equalTo: bgImageView.trailingAnchor, constant: -15).isActive = true
    }
    
    func setupData(model: MainModel) {
        titleLabel.text = model.title
        descLabel.text = model.desc
        bgImageView.image = UIImage(named: model.imageNmae)
    }
}
