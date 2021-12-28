//
//  OneToOneControlView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/22.
//

import UIKit
import AgoraUIKit

enum OneToOneControlType: Int {
    case switchCamera = 1
    case game = 2
    case mic = 3
    case back = 4
    case exit = 5
    case close = 6
}

class OneToOneControlView: UIView {
    var onClickControlButtonClosure: ((OneToOneControlType, Bool) -> Void)?
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .horizontal
        stackView.distribution = .equalSpacing
        stackView.spacing = 0//37.fit
        return stackView
    }()
    private lazy var switchCameraButton: AGEButton = {
        let button = AGEButton(style: .switchCamera(imageColor: .white))
        button.backgroundColor = .init(hex: "#737374")
        button.imageSize = CGSize(width: 26.fit, height: 22.fit)
        button.cornerRadius = 30.fit
        button.tag = OneToOneControlType.switchCamera.rawValue
        button.addTarget(self, action: #selector(clickSwitchCamera(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var gameButton: AGEButton = {
        let button = AGEButton(style: .systemImage(name: "gamecontroller", imageColor: .white))
        button.backgroundColor = .init(hex: "#737374")
        button.cornerRadius = 30.fit
        button.imageSize = CGSize(width: 30.fit, height: 22.fit)
        button.tag = OneToOneControlType.game.rawValue
        button.addTarget(self, action: #selector(clickGameButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var micButton: AGEButton = {
        let button = AGEButton(style: .mic(imageColor: .white))
        button.backgroundColor = .init(hex: "#737374")
        button.cornerRadius = 30.fit
        button.imageSize = CGSize(width: 20.fit, height: 25.fit)
        button.tag = OneToOneControlType.mic.rawValue
        button.addTarget(self, action: #selector(clickMicButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var backButton: AGEButton = {
        let button = AGEButton(style: .systemImage(name: "phone.down.fill", imageColor: .white))
        button.backgroundColor = .init(hex: "#E23E51")
        button.cornerRadius = 30.fit
        button.imageSize = CGSize(width: 34.fit, height: 10.fit)
        button.tag = OneToOneControlType.back.rawValue
        button.addTarget(self, action: #selector(clickCloseButton), for: .touchUpInside)
        return button
    }()
    private lazy var exitButton: AGEButton = {
        let button = AGEButton(style: .imageName(name: "oneToOne/exit"))
        button.backgroundColor = .init(hex: "#737374")
        button.cornerRadius = 30.fit
        button.tag = OneToOneControlType.exit.rawValue
        button.addTarget(self, action: #selector(clickExitButton), for: .touchUpInside)
        button.isHidden = true
        return button
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateControlUI(types: [OneToOneControlType]) {
        statckView.arrangedSubviews.forEach({ $0.isHidden = true })
        types.forEach({
            let button = statckView.viewWithTag($0.rawValue)
            button?.isHidden = false
        })
    }
    
    private func setupUI() {
        statckView.translatesAutoresizingMaskIntoConstraints = false
        switchCameraButton.translatesAutoresizingMaskIntoConstraints = false
        gameButton.translatesAutoresizingMaskIntoConstraints = false
        micButton.translatesAutoresizingMaskIntoConstraints = false
        backButton.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(statckView)
        statckView.addArrangedSubview(switchCameraButton)
        statckView.addArrangedSubview(gameButton)
        statckView.addArrangedSubview(micButton)
        statckView.addArrangedSubview(exitButton)
        addSubview(backButton)
        
        statckView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 54.fit).isActive = true
        statckView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        statckView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -54).isActive = true
        statckView.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        
        switchCameraButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        switchCameraButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        gameButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        gameButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        micButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        micButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        exitButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        exitButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        
        backButton.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        backButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        backButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        backButton.topAnchor.constraint(equalTo: statckView.bottomAnchor, constant: 30.fit).isActive = true
        backButton.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -48).isActive = true
    }
    
    @objc
    private func clickSwitchCamera(sender: AGEButton) {
        onClickControlButtonClosure?(.switchCamera, sender.isSelected)
    }
    @objc
    private func clickGameButton(sender: AGEButton) {
        onClickControlButtonClosure?(.game, sender.isSelected)
    }
    @objc
    private func clickMicButton(sender: AGEButton) {
        sender.isSelected = !sender.isSelected
        if sender.isSelected {
            sender.buttonStyle = .muteMic(imageColor: .white)
        } else {
            sender.buttonStyle = .mic(imageColor: .white)
        }
        onClickControlButtonClosure?(.mic, sender.isSelected)
    }
    @objc
    private func clickCloseButton() {
        onClickControlButtonClosure?(.back, false)
    }
    @objc
    private func clickExitButton() {
        onClickControlButtonClosure?(.exit, false)
    }
}
