//
//  BaseNavigationController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/2/8.
//

import UIKit

class BaseNavigationController: UINavigationController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        .default
    }
    
    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation {
        .fade
    }

}

extension BaseNavigationController {
    override var childForStatusBarStyle: UIViewController? {
        UIApplication.topMostViewController
    }
    override var childForStatusBarHidden: UIViewController? {
        UIApplication.topMostViewController
    }
}
