//
//  LoginVC.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import UIKit

class LoginVC: UIViewController {
    @IBOutlet weak var roomNameTextField: UITextField!
    @IBOutlet weak var roleSwitch: UISwitch!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Login"
        roomNameTextField.text = "123"
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
    
    @IBAction func buttonTap(_ sender: Any) {
        view.endEditing(true)
        guard let roomName = roomNameTextField.text, !roomName.isEmpty else {
            return
        }
        
        let role: Role = roleSwitch.isOn ? .broadcaster : .audience
        let info = LoginInfo(role: role, roomName: roomName)
        let vc = MainVC(loginInfo: info)
        let nvc = UINavigationController(rootViewController: vc)
        nvc.modalPresentationStyle = .fullScreen
        present(nvc, animated: true, completion: nil)
    }
}


