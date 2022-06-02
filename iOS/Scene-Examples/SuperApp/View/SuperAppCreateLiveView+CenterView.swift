//
//  CreateLiveView+CenterView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/17.
//

import UIKit

extension SuperAppCreateLiveView {
    
    class CenterView: UIView {
        let itemWidth: CGFloat = (UIScreen.main.bounds.size.width - 70)/2
        private let selectedView1 = SelectedView()
        private let selectedView2 = SelectedView()
        private var selectedType = SelectedType.value1
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            setup()
            commonInit()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        private func setup() {
            backgroundColor = .clear
            addSubview(selectedView1)
            addSubview(selectedView2)
            
            selectedView1.translatesAutoresizingMaskIntoConstraints = false
            selectedView2.translatesAutoresizingMaskIntoConstraints = false
            
            selectedView1.topAnchor.constraint(equalTo: topAnchor).isActive = true
            selectedView1.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
            selectedView1.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
            selectedView1.widthAnchor.constraint(equalToConstant: itemWidth).isActive = true
            
            selectedView2.topAnchor.constraint(equalTo: topAnchor).isActive = true
            selectedView2.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
            selectedView2.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
            selectedView2.widthAnchor.constraint(equalToConstant: itemWidth).isActive = true
        }
        
        private func commonInit() {
            selectedView1.update(text: "客户端\n直推模式")
            selectedView2.update(text: "服务端\n旁推模式")
            selectedView1.setSelected(selected: true)
            selectedView2.setSelected(selected: false)
            
            selectedView1.button.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
            selectedView2.button.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
        }
        
        @objc func buttonTap(_ sender: UIButton) {
            if sender == selectedView1.button {
                selectedType = .value1
                selectedView1.setSelected(selected: true)
                selectedView2.setSelected(selected: false)
                return
            }
            
            if sender == selectedView2.button {
                selectedType = .value2
                selectedView1.setSelected(selected: false)
                selectedView2.setSelected(selected: true)
                return
            }
        }
        
        var currentSelectedType: SelectedType {
            return selectedType
        }
        
        enum SelectedType {
            case value1
            case value2
        }
    }
    
    class SelectedView: UIView {
        private var isSelected = false
        private let textLabel = UILabel()
        private let checkImageView = UIImageView()
        private let indicatedView = UIView()
        fileprivate let button = UIButton()
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            setup()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        func setup() {
            layer.cornerRadius = 8
            layer.masksToBounds = true
            backgroundColor = UIColor.black.withAlphaComponent(0.3)
            
            indicatedView.isHidden = true
            indicatedView.backgroundColor = .clear
            indicatedView.layer.cornerRadius = 8
            indicatedView.layer.masksToBounds = true
            indicatedView.layer.borderColor = UIColor.white.cgColor
            indicatedView.layer.borderWidth = 3
            checkImageView.image = UIImage(named: "check-circle")
            button.backgroundColor = .clear
            textLabel.numberOfLines = 0
            textLabel.textColor = .white
            textLabel.font = UIFont.systemFont(ofSize: 17, weight: .medium)
            textLabel.textAlignment = .center
            
            addSubview(indicatedView)
            addSubview(textLabel)
            addSubview(checkImageView)
            addSubview(button)
            
            indicatedView.translatesAutoresizingMaskIntoConstraints = false
            textLabel.translatesAutoresizingMaskIntoConstraints = false
            checkImageView.translatesAutoresizingMaskIntoConstraints = false
            button.translatesAutoresizingMaskIntoConstraints = false
            
            indicatedView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
            indicatedView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
            indicatedView.topAnchor.constraint(equalTo: topAnchor).isActive = true
            indicatedView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
            
            textLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
            textLabel.centerYAnchor.constraint(equalTo: centerYAnchor, constant: -10).isActive = true
            
            checkImageView.topAnchor.constraint(equalTo: textLabel.bottomAnchor, constant: 15).isActive = true
            checkImageView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
            checkImageView.heightAnchor.constraint(equalToConstant: 20).isActive = true
            checkImageView.widthAnchor.constraint(equalToConstant: 20).isActive = true
            
            button.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
            button.topAnchor.constraint(equalTo: topAnchor).isActive = true
            button.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
            button.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        }
        
        func update(text: String) {
            textLabel.text = text
        }
        
        func setSelected(selected: Bool) {
            isSelected = selected
            indicatedView.isHidden = !selected
            checkImageView.isHidden = !selected
        }
    }
}
