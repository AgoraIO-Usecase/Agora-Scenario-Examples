//
//  MainView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/17.
//

import UIKit

protocol SuperAppMainViewDelegate: NSObjectProtocol {
    func mainView(_ view: CDNMainView, didTap action: CDNMainView.Action)
}

class CDNMainView: UIView {
    private let personCountView = CDNRoomListViewCell.IconTextView()
    private let leftView = LiveAvatarView()
    private let moreButton = UIButton()
    private let closeButton = UIButton()
    private let localView = UIView()
    private let remoteView = RemoteView()
    private let personCountButton = UIButton()
    weak var delegate: SuperAppMainViewDelegate?
    private var info = Info.empty
    
    var renderViewLocal: UIView {
        return localView
    }
    
    var renderViewRemote: UIView {
        return remoteView.renderView
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setup() {
        backgroundColor = .white
        localView.backgroundColor = .clear
        personCountButton.backgroundColor = .clear
        moreButton.setImage(.init(named: "icon-more"), for: .normal)
        closeButton.setImage(.init(named: "icon-round-close"), for: .normal)
        personCountView.imageView.image = UIImage(named: "icon-audience")
        personCountView.label.textColor = .white
        
        addSubview(localView)
        addSubview(remoteView)
        addSubview(personCountView)
        addSubview(personCountButton)
        addSubview(leftView)
        addSubview(moreButton)
        addSubview(closeButton)
        
        localView.translatesAutoresizingMaskIntoConstraints = false
        remoteView.translatesAutoresizingMaskIntoConstraints = false
        personCountView.translatesAutoresizingMaskIntoConstraints = false
        leftView.translatesAutoresizingMaskIntoConstraints = false
        moreButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        personCountButton.translatesAutoresizingMaskIntoConstraints = false
        
        localView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        localView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        localView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        localView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        personCountView.rightAnchor.constraint(equalTo: rightAnchor, constant: -10).isActive = true
        personCountView.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor, constant: 5).isActive = true
        personCountView.widthAnchor.constraint(equalToConstant: 55).isActive = true
        personCountView.heightAnchor.constraint(equalToConstant: 22).isActive = true
        
        personCountButton.rightAnchor.constraint(equalTo: rightAnchor, constant: -10).isActive = true
        personCountButton.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor, constant: 5).isActive = true
        personCountButton.widthAnchor.constraint(equalToConstant: 55).isActive = true
        personCountButton.heightAnchor.constraint(equalToConstant: 22).isActive = true
        
        leftView.leftAnchor.constraint(equalTo: leftAnchor, constant: 10).isActive = true
        leftView.centerYAnchor.constraint(equalTo: personCountView.centerYAnchor).isActive = true
        leftView.widthAnchor.constraint(equalToConstant: 160).isActive = true
        leftView.heightAnchor.constraint(equalToConstant: 30).isActive = true
        
        closeButton.rightAnchor.constraint(equalTo: rightAnchor, constant: -15).isActive = true
        closeButton.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -15).isActive = true
        
        moreButton.rightAnchor.constraint(equalTo: closeButton.leftAnchor, constant: -6).isActive = true
        moreButton.centerYAnchor.constraint(equalTo: closeButton.centerYAnchor).isActive = true
        
        remoteView.bottomAnchor.constraint(equalTo: closeButton.topAnchor, constant: -6).isActive = true
        remoteView.rightAnchor.constraint(equalTo: closeButton.rightAnchor).isActive = true
        remoteView.widthAnchor.constraint(equalToConstant: 130).isActive = true
        remoteView.heightAnchor.constraint(equalToConstant: 231).isActive = true
    }
    
    private func commonInit() {
        personCountButton.addTarget(self,
                                    action: #selector(buttonTap(_:)),
                                    for: .touchUpInside)
        moreButton.addTarget(self,
                             action: #selector(buttonTap(_:)),
                             for: .touchUpInside)
        closeButton.addTarget(self,
                              action: #selector(buttonTap(_:)),
                              for: .touchUpInside)
        remoteView.button.addTarget(self,
                                    action: #selector(buttonTap(_:)),
                                    for: .touchUpInside)
    }
    
    @objc func buttonTap(_ sender: UIButton) {
        if sender == moreButton {
            delegate?.mainView(self, didTap: .more)
            return
        }
        
        if sender == closeButton {
            delegate?.mainView(self, didTap: .close)
            return
        }
        
        if sender == personCountButton {
            delegate?.mainView(self, didTap: .member)
            return
        }
        
        if sender == remoteView.button {
            delegate?.mainView(self, didTap: .closeRemote)
            return
        }
    }
    
    func update(info: Info) {
        self.info = info
        leftView.setName(with: info.title)
        personCountView.label.text = ""
    }
    
    func setRemoteViewHidden(hidden: Bool) {
        LogUtils.log(message: "setRemoteViewHidden \(hidden)", level: .info)
        remoteView.isHidden = hidden
    }
    
    func setPersonViewHidden(hidden: Bool) {
        personCountView.isHidden = hidden
        personCountButton.isHidden = hidden
    }
}

extension CDNMainView {
    enum Action {
        case close
        case more
        case member
        case closeRemote
    }
    
    struct Info {
        let title: String
        let imageName: String
        let userCount: Int
        
        static var empty: Info {
            return Info(title: "",
                        imageName: "pic-11",
                        userCount: 0)
        }
    }
}
