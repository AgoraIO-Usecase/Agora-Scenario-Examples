//
//  MainHeaderViewCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/6/17.
//

import UIKit
import Agora_Scene_Utils

class MainHeaderViewCell: UICollectionReusableView {
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "社交娱乐"
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
        addSubview(titleLabel)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10).isActive = true
    }
    
    func setTitle(title: String) {
        titleLabel.text = title
    }
}
