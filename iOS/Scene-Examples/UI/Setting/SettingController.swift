//
//  SettingController.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/19.
//

import Foundation
import UIKit

class SettingController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
    }
    
    static func instance() -> SettingController {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let controller = storyBoard.instantiateViewController(withIdentifier: "SettingController") as! SettingController
        return controller
    }
}
