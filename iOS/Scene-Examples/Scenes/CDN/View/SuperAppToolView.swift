//
//  ToolView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/18.
//

import UIKit

protocol SuperAppToolViewDelegate: NSObjectProtocol {
    func toolView(_ view: SuperAppToolView, didTap action: SuperAppToolView.Action)
}

class SuperAppToolView: UIView {
    private let titleLabel = UILabel()
    private let cameraButton = UIButton()
    private let micButton = UIButton()
    var micOpen = true
    weak var delegate: SuperAppToolViewDelegate?

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
        translatesAutoresizingMaskIntoConstraints = false
        widthAnchor.constraint(equalToConstant: cl_screenWidht).isActive = true
        heightAnchor.constraint(equalToConstant: 250).isActive = true
        
        titleLabel.text = "工具"
        cameraButton.setImage(.init(named: "icon-rotate-circle"), for: .normal)
        setMicState(open: micOpen)
        addSubview(titleLabel)
        addSubview(cameraButton)
        addSubview(micButton)
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        cameraButton.translatesAutoresizingMaskIntoConstraints = false
        micButton.translatesAutoresizingMaskIntoConstraints = false
        
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        
        cameraButton.leftAnchor.constraint(equalTo: leftAnchor, constant: 15).isActive = true
        cameraButton.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        cameraButton.widthAnchor.constraint(equalToConstant: 60).isActive = true
        cameraButton.heightAnchor.constraint(equalToConstant: 60).isActive = true
        
        micButton.leftAnchor.constraint(equalTo: cameraButton.rightAnchor, constant: 15).isActive = true
        micButton.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        micButton.widthAnchor.constraint(equalToConstant: 60).isActive = true
        micButton.heightAnchor.constraint(equalToConstant: 60).isActive = true
    }
    
    private func commonInit() {
        cameraButton.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
        micButton.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
    }
    
    @objc func buttonTap(_ sender: UIButton) {
        if sender == cameraButton {
            delegate?.toolView(self, didTap: .camera)
            return
        }
        
        if sender == micButton {
            delegate?.toolView(self, didTap: .mic)
            return
        }
    }
    
    func setMicState(open: Bool) {
        micOpen = open
        let image = open ? UIImage(named: "icon-speaker on") : UIImage(named: "icon-speaker off")
        micButton.setImage(image, for: .normal)
    }
}

extension SuperAppToolView {
    enum Action {
        case camera
        case mic
    }
}
