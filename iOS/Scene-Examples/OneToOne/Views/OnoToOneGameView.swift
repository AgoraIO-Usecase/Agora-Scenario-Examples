//
//  OnoToOneGameView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/28.
//

import UIKit
import AgoraUIKit_iOS

class OnoToOneGameView: UIView {
    var onClickControlButtonClosure: ((OneToOneControlType, Bool) -> Void)?
    var onLeaveGameClosure: (() -> Void)?
    
    private lazy var micButton: AGEButton = {
        let button = AGEButton(style: .mic(imageColor: .white))
        button.backgroundColor = .init(hex: "#737374")
        button.cornerRadius = 30.fit
        button.imageSize = CGSize(width: 20.fit, height: 25.fit)
        button.tag = OneToOneControlType.mic.rawValue
        button.addTarget(self, action: #selector(clickMicButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var exitButton: AGEButton = {
        let button = AGEButton(style: .imageName(name: "oneToOne/exit"))
        button.backgroundColor = .init(hex: "#737374")
        button.cornerRadius = 30.fit
        button.tag = OneToOneControlType.exit.rawValue
        button.addTarget(self, action: #selector(clickExitButton), for: .touchUpInside)
        return button
    }()
    private lazy var webView: GameWebView = {
        let webView = GameWebView()
        webView.backgroundColor = .init(hex: "#B8C4D6")
        return webView
    }()
    private lazy var closeButton: AGEButton = {
        let button = AGEButton(style: .systemImage(name: "chevron.down", imageColor: .white))
        button.tag = OneToOneControlType.close.rawValue
        button.addTarget(self, action: #selector(clickCloseButton), for: .touchUpInside)
        return button
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setLoadUrl(gameId: String, roomId: String, roleType: GameRoleType) {
        webView.loadUrl(gameId: gameId, roomId: roomId, roleType: roleType)
    }
    
    func reset() {
        webView.reset()
    }
    
    private func setupUI() {
        micButton.translatesAutoresizingMaskIntoConstraints = false
        exitButton.translatesAutoresizingMaskIntoConstraints = false
        webView.translatesAutoresizingMaskIntoConstraints = false
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        addSubview(micButton)
        addSubview(exitButton)
        addSubview(webView)
        webView.addSubview(closeButton)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        heightAnchor.constraint(equalToConstant: Screen.height).isActive = true
        
        micButton.topAnchor.constraint(equalTo: topAnchor, constant: 52.fit).isActive = true
        micButton.trailingAnchor.constraint(equalTo: centerXAnchor, constant: -17).isActive = true
        micButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        micButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        
        exitButton.topAnchor.constraint(equalTo: micButton.topAnchor).isActive = true
        exitButton.leadingAnchor.constraint(equalTo: centerXAnchor, constant: 17).isActive = true
        exitButton.widthAnchor.constraint(equalToConstant: 60.fit).isActive = true
        exitButton.heightAnchor.constraint(equalToConstant: 60.fit).isActive = true
        
        webView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        webView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: micButton.bottomAnchor, constant: 41.fit).isActive = true
        
        closeButton.centerXAnchor.constraint(equalTo: webView.centerXAnchor).isActive = true
        closeButton.topAnchor.constraint(equalTo: webView.topAnchor, constant: 13).isActive = true
        closeButton.widthAnchor.constraint(equalToConstant: 40).isActive = true
        
        webView.onLeaveGameClosure = onLeaveGameClosure
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
    private func clickExitButton() {
        let controller = UIApplication.topMostViewController
        let alertVC = UIAlertController(title: "quit_the_game".localized, message: "confirm_exit_game".localized, preferredStyle: .alert)
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel)
        let ok = UIAlertAction(title: "Confirm".localized, style: .default) { _ in
            AlertManager.hiddenView(all: true) {
                self.onClickControlButtonClosure?(.exit, false)
            }
        }
        alertVC.addAction(cancel)
        alertVC.addAction(ok)
        controller?.present(alertVC, animated: true, completion: nil)
    }
    
    @objc
    private func clickCloseButton() {
        onClickControlButtonClosure?(.close, false)
    }
}
