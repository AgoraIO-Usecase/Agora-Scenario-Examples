//
//  ClubHeaderView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/3/21.
//

import UIKit
import AgoraUIKit_iOS

class ClubHeaderView: UIView {
    var clickVideoViewClosure: ((Bool) -> Void)?
    lazy var localVideoView: AGEView = {
        let view = AGEView()
        return view
    }()
    private lazy var coverButton: AGEButton = {
        let view = AGEButton()
        view.addTarget(self, action: #selector(clickViewHandler(sender:)), for: .touchUpInside)
        return view
    }()
    lazy var fullButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(systemName: "arrow.up.left.and.arrow.down.right")?.withTintColor(.white, renderingMode: .alwaysOriginal), for: .normal)
        button.setImage(UIImage(systemName: "arrow.down.right.and.arrow.up.left")?.withTintColor(.white, renderingMode: .alwaysOriginal), for: .selected)
        button.addTarget(self, action: #selector(clickFullButton(sender:)), for: .touchUpInside)
        return button
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        addSubview(localVideoView)
        addSubview(coverButton)
        addSubview(fullButton)
        localVideoView.translatesAutoresizingMaskIntoConstraints = false
        fullButton.translatesAutoresizingMaskIntoConstraints = false
        coverButton.translatesAutoresizingMaskIntoConstraints = false
        localVideoView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        localVideoView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        localVideoView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        localVideoView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        coverButton.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        coverButton.topAnchor.constraint(equalTo: topAnchor).isActive = true
        coverButton.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        coverButton.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        fullButton.trailingAnchor.constraint(equalTo: localVideoView.trailingAnchor).isActive = true
        fullButton.bottomAnchor.constraint(equalTo: localVideoView.bottomAnchor).isActive = true
        fullButton.widthAnchor.constraint(equalToConstant: 40).isActive = true
        fullButton.heightAnchor.constraint(equalToConstant: 40).isActive = true
    }
    
    @objc
    private func clickFullButton(sender: AGEButton) {
        sender.isSelected = !sender.isSelected
        let appdelegate = UIApplication.shared.delegate as? AppDelegate
        appdelegate?.blockRotation = sender.isSelected ? .landscapeRight : .portrait
    }
    
    @objc
    private func clickViewHandler(sender: AGEButton) {
        guard fullButton.isSelected else { return }
        sender.isSelected = !sender.isSelected
        clickVideoViewClosure?(sender.isSelected)
    }
}
