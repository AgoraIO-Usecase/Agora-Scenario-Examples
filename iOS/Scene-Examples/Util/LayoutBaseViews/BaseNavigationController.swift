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
        viewControllers.last?.preferredStatusBarStyle ?? .lightContent
    }
    override var prefersStatusBarHidden: Bool {
        viewControllers.last?.prefersStatusBarHidden ?? false
    }
    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation {
        viewControllers.last?.preferredStatusBarUpdateAnimation ?? .slide
    }
    override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation {
        viewControllers.last?.preferredInterfaceOrientationForPresentation ?? .portrait
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
