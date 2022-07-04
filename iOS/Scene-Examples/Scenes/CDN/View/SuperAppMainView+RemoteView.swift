//
//  MainView+RemoteView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/30.
//

import UIKit

extension SuperAppMainView {
    class RemoteView: UIView {
        let renderView = UIView()
        let button = UIButton()
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            setup()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        func setup() {
            isHidden = true
            backgroundColor = .clear
            let image = UIImage(named: "icon-round-close")
            button.setImage(image, for: .normal)
            
            addSubview(renderView)
            addSubview(button)
            renderView.translatesAutoresizingMaskIntoConstraints = false
            button.translatesAutoresizingMaskIntoConstraints = false
            
            button.topAnchor
                .constraint(equalTo: topAnchor)
                .isActive = true
            button.leftAnchor
                .constraint(equalTo: leftAnchor)
                .isActive = true
            button.widthAnchor
                .constraint(equalToConstant: 25)
                .isActive = true
            button.heightAnchor
                .constraint(equalToConstant: 25)
                .isActive = true
            
            renderView.topAnchor
                .constraint(equalTo: button.bottomAnchor, constant: -5)
                .isActive = true
            renderView.leftAnchor
                .constraint(equalTo: button.rightAnchor, constant: -5)
                .isActive = true
            renderView.bottomAnchor
                .constraint(equalTo: bottomAnchor)
                .isActive = true
            renderView.rightAnchor
                .constraint(equalTo: rightAnchor)
                .isActive = true
        }
    }
}
