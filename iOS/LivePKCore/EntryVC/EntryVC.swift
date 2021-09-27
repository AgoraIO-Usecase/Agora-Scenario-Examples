//
//  LoginVC.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import UIKit

public class EntryVC: UIViewController {
    let roomNameTextField = UITextField()
    let roleSwitch = UISwitch()
    let button = UIButton()
    public var appId: String!
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        title = "LivePK Entry"
        setup()
        commonInit()
    }
    
    func setup() {
        let tipsLabel = UILabel()
        tipsLabel.text = "is Host"
        tipsLabel.textColor = .gray
        button.backgroundColor = .systemBlue
        button.setTitle("Join", for: .normal)
        button.setTitleColor(.white, for: .normal)
        roomNameTextField.placeholder = "room name"
        roomNameTextField.clearButtonMode = .whileEditing
        roomNameTextField.keyboardType = .numberPad
        roomNameTextField.borderStyle = .roundedRect
        roleSwitch.isOn = true
        
        view.addSubview(roomNameTextField)
        view.addSubview(roleSwitch)
        view.addSubview(button)
        view.addSubview(tipsLabel)
        
        roomNameTextField.translatesAutoresizingMaskIntoConstraints = false
        roleSwitch.translatesAutoresizingMaskIntoConstraints = false
        button.translatesAutoresizingMaskIntoConstraints = false
        tipsLabel.translatesAutoresizingMaskIntoConstraints = false
        
        roomNameTextField.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 30).isActive = true
        roomNameTextField.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -30).isActive = true
        roomNameTextField.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 10).isActive = true
        
        roleSwitch.leftAnchor.constraint(equalTo: roomNameTextField.leftAnchor).isActive = true
        roleSwitch.topAnchor.constraint(equalTo: roomNameTextField.bottomAnchor, constant: 10).isActive = true
        
        tipsLabel.leftAnchor.constraint(equalTo: roleSwitch.rightAnchor, constant: 10).isActive = true
        tipsLabel.centerYAnchor.constraint(equalTo: roleSwitch.centerYAnchor).isActive = true
        
        button.leftAnchor.constraint(equalTo: roomNameTextField.leftAnchor).isActive = true
        button.rightAnchor.constraint(equalTo: roomNameTextField.rightAnchor).isActive = true
        button.topAnchor.constraint(equalTo: roleSwitch.bottomAnchor, constant: 10).isActive = true
        button.heightAnchor.constraint(equalToConstant: 45).isActive = true
    }
    
    func commonInit() {
        button.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
    }
    
    public override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
    
    @objc func buttonTap(_ sender: Any) {
        view.endEditing(true)
        guard let roomName = roomNameTextField.text, !roomName.isEmpty else {
            return
        }
        
        let role: Role = roleSwitch.isOn ? .broadcaster : .audience
        let info = LoginInfo(role: role, roomName: roomName)
        let vc = MainVC(loginInfo: info, appId: appId)
        let nvc = UINavigationController(rootViewController: vc)
        nvc.modalPresentationStyle = .fullScreen
        present(nvc, animated: true, completion: nil)
    }
}


