//
//  LiveRandomNameView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

class LiveRandomNameView: UIView {
    private lazy var textField: UITextField = {
        let textField = UITextField()
        textField.font = .systemFont(ofSize: 16)
        textField.textColor = .white
        textField.placeholder = "pleaseInputRoomName".localized
        textField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 20, height: 20))
        textField.leftViewMode = .always
        textField.attributedPlaceholder = NSAttributedString(string: textField.placeholder ?? "",
                                                             attributes: [.foregroundColor: UIColor.white.withAlphaComponent(0.41)])
        return textField
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
        backgroundColor = UIColor.black.withAlphaComponent(0.3)
        layer.cornerRadius = 8
        layer.masksToBounds = true
        textField.translatesAutoresizingMaskIntoConstraints = false
        addSubview(textField)

        textField.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        textField.topAnchor.constraint(equalTo: topAnchor).isActive = true
        textField.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        textField.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    @objc
    private func onTapRandomNameButton() {
        textField.text = LiveRandomName.randomName()
    }
}
