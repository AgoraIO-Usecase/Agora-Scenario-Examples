//
//  CreateLiveView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/16.
//

import UIKit

protocol CreateLiveViewDelegate: NSObjectProtocol {
    func createLiveViewDidTapCloseButton(_ view: CDNCreateLiveView)
    func createLiveViewDidTapCameraButton(_ view: CDNCreateLiveView)
    func createLiveViewDidTapStartButton(_ view: CDNCreateLiveView)
    func createLiveViewDidTapRandomButton(_ view: CDNCreateLiveView)
}

class CDNCreateLiveView: UIView {
    typealias SelectedType = CDNCreateLiveView.CenterView.SelectedType
    
    let cameraPreview = UIView()
    private let backButton = UIButton()
    private let switchCameraButton = UIButton()
    private let nameLabel = UILabel()
    private let nameTextField = UITextField()
    private let nameBgView = UIView()
    private let randomButton = UIButton()
    private let startButton = UIButton()
    private let centerView = CenterView()
    
    weak var delegate: CreateLiveViewDelegate?
    
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
        cameraPreview.backgroundColor = .black
        backButton.setImage(.init(named: "icon-close-gray"), for: .normal)
        switchCameraButton.setImage(.init(named: "icon-rotate"), for: .normal)
        randomButton.setImage(.init(named: "icon-random"), for: .normal)
        nameBgView.backgroundColor = UIColor.black.withAlphaComponent(0.3)
        nameBgView.layer.cornerRadius = 8
        nameBgView.layer.masksToBounds = true
        startButton.setTitle("开始直播", for: .normal)
        startButton.backgroundColor = UIColor(hex: "#0088EB")
        startButton.setTitleColor(.white, for: .normal)
        startButton.layer.cornerRadius = 22
        startButton.layer.masksToBounds = true
        nameLabel.text = "直播间的名称:"
        nameLabel.textColor = .white
        nameLabel.font = UIFont.systemFont(ofSize: 12)
        nameTextField.font = UIFont.systemFont(ofSize: 12)
        nameTextField.textColor = .white
        nameTextField.clearButtonMode = .whileEditing
        
        addSubview(cameraPreview)
        addSubview(nameBgView)
        addSubview(backButton)
        addSubview(switchCameraButton)
        addSubview(nameLabel)
        addSubview(nameTextField)
        addSubview(randomButton)
        addSubview(startButton)
        addSubview(centerView)
        
        cameraPreview.translatesAutoresizingMaskIntoConstraints = false
        nameBgView.translatesAutoresizingMaskIntoConstraints = false
        backButton.translatesAutoresizingMaskIntoConstraints = false
        switchCameraButton.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        nameTextField.translatesAutoresizingMaskIntoConstraints = false
        randomButton.translatesAutoresizingMaskIntoConstraints = false
        startButton.translatesAutoresizingMaskIntoConstraints = false
        centerView.translatesAutoresizingMaskIntoConstraints = false
        
        cameraPreview.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        cameraPreview.topAnchor.constraint(equalTo: topAnchor).isActive = true
        cameraPreview.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        cameraPreview.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        backButton.leftAnchor.constraint(equalTo: safeAreaLayoutGuide.leftAnchor, constant: 15).isActive = true
        backButton.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor, constant: 10).isActive = true
        
        switchCameraButton.rightAnchor.constraint(equalTo: safeAreaLayoutGuide.rightAnchor, constant: -15).isActive = true
        switchCameraButton.centerYAnchor.constraint(equalTo: backButton.centerYAnchor).isActive = true
        switchCameraButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
        switchCameraButton.widthAnchor.constraint(equalToConstant: 44).isActive = true
        
        nameBgView.topAnchor.constraint(equalTo: backButton.bottomAnchor, constant: 9).isActive = true
        nameBgView.leftAnchor.constraint(equalTo: safeAreaLayoutGuide.leftAnchor, constant: 15).isActive = true
        nameBgView.rightAnchor.constraint(equalTo: safeAreaLayoutGuide.rightAnchor, constant: -15).isActive = true
        nameBgView.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        nameLabel.leftAnchor.constraint(equalTo: nameBgView.leftAnchor, constant: 10).isActive = true
        nameLabel.centerYAnchor.constraint(equalTo: nameBgView.centerYAnchor).isActive = true
        nameLabel.widthAnchor.constraint(equalToConstant: 80).isActive = true
        
        randomButton.rightAnchor.constraint(equalTo: nameBgView.rightAnchor, constant: -5).isActive = true
        randomButton.centerYAnchor.constraint(equalTo: nameBgView.centerYAnchor).isActive = true
        randomButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
        randomButton.widthAnchor.constraint(equalToConstant: 44).isActive = true
        
        nameTextField.leftAnchor.constraint(equalTo: nameLabel.rightAnchor, constant: 2).isActive = true
        nameTextField.centerYAnchor.constraint(equalTo: nameBgView.centerYAnchor).isActive = true
        nameTextField.rightAnchor.constraint(equalTo: randomButton.leftAnchor, constant: -5).isActive = true
        
        centerView.leftAnchor.constraint(equalTo: nameBgView.leftAnchor).isActive = true
        centerView.rightAnchor.constraint(equalTo: nameBgView.rightAnchor).isActive = true
        centerView.topAnchor.constraint(equalTo: nameBgView.bottomAnchor, constant: 20).isActive = true
        centerView.heightAnchor.constraint(equalToConstant: centerView.itemWidth * 1.3).isActive = true
        
        startButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
        startButton.widthAnchor.constraint(equalToConstant: 130).isActive = true
        startButton.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -40).isActive = true
        startButton.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
    }
    
    private func commonInit() {
        backButton.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
        randomButton.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
        startButton.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
        switchCameraButton.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
    }
    
    var text: String {
        return nameTextField.text ?? ""
    }
    
    var currentSelectedType: SelectedType {
        return centerView.currentSelectedType
    }
    
    func set(text: String) {
        nameTextField.text = text
    }
    
    @objc func buttonTap(_ sender: UIButton) {
        if sender == backButton {
            delegate?.createLiveViewDidTapCloseButton(self)
            return
        }
        
        if sender == switchCameraButton {
            delegate?.createLiveViewDidTapCameraButton(self)
            return
        }
        
        if sender == randomButton {
            delegate?.createLiveViewDidTapRandomButton(self)
            return
        }
        
        if sender == startButton, nameTextField.text?.count ?? 0 > 0 {
            delegate?.createLiveViewDidTapStartButton(self)
            return
        }
    }
}


