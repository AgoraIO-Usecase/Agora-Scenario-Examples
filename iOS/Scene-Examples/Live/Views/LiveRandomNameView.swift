//
//  LiveRandomNameView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

class LiveRandomNameView: UIView {
    private lazy var liveNameLabel: UILabel = {
        let label = UILabel()
        label.text = "Create_NameLabel".localized//"直播间的名字: "
        label.textColor = .white
        label.font = .systemFont(ofSize: 12)
        return label
    }()
    private lazy var textField: UITextField = {
        let textField = UITextField()
        textField.font = .systemFont(ofSize: 14)
        textField.textColor = .white
        textField.text = LiveRandomName.randomName()
        return textField
    }()
    private lazy var randomNameButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "icon-random"), for: .normal)
        button.addTarget(self, action: #selector(clickRandomNameButton), for: .touchUpInside)
        return button
    }()
    
    var text: String {
        textField.text ?? ""
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = UIColor.black.withAlphaComponent(0.7)
        layer.cornerRadius = 8
        layer.masksToBounds = true
        liveNameLabel.translatesAutoresizingMaskIntoConstraints = false
        textField.translatesAutoresizingMaskIntoConstraints = false
        randomNameButton.translatesAutoresizingMaskIntoConstraints = false
        addSubview(liveNameLabel)
        addSubview(textField)
        addSubview(randomNameButton)
        liveNameLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        liveNameLabel.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        textField.leadingAnchor.constraint(equalTo: liveNameLabel.trailingAnchor).isActive = true
        textField.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        randomNameButton.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        randomNameButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
    }
    
    @objc
    private func clickRandomNameButton() {
        textField.text = LiveRandomName.randomName()
    }
}
