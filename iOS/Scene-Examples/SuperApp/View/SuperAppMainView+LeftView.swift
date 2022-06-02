//
//  MainView+LeftView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/17.
//

import UIKit

extension SuperAppMainView {
    class LeftView: UIView {
        let imageView = UIImageView()
        let titleLabel = UILabel()
        let bgView = UIView()
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            setup()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        private func setup(){
            backgroundColor = .clear
            bgView.backgroundColor = UIColor.black.withAlphaComponent(0.3)
            bgView.layer.cornerRadius = 15
            bgView.layer.masksToBounds = true
            titleLabel.textColor = .white
            titleLabel.font = UIFont.systemFont(ofSize: 14)
            
            addSubview(bgView)
            addSubview(imageView)
            addSubview(titleLabel)
            
            bgView.translatesAutoresizingMaskIntoConstraints = false
            imageView.translatesAutoresizingMaskIntoConstraints = false
            titleLabel.translatesAutoresizingMaskIntoConstraints = false
            
            bgView.leftAnchor.constraint(equalTo: leftAnchor, constant: -4).isActive = true
            bgView.topAnchor.constraint(equalTo: topAnchor).isActive = true
            bgView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
            bgView.rightAnchor.constraint(equalTo: titleLabel.rightAnchor, constant: 10).isActive = true
            
            imageView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
            imageView.widthAnchor.constraint(equalToConstant: 22).isActive = true
            imageView.heightAnchor.constraint(equalToConstant: 22).isActive = true
            imageView.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
            
            titleLabel.leftAnchor.constraint(equalTo: imageView.rightAnchor, constant: 5).isActive = true
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        }
    }
}
