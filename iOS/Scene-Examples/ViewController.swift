//
//  ViewController.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/16.
//

import UIKit
import RxCocoa
import RxSwift
import Core

class ViewController: CustomTabBarController {
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: false)
    }
    
    override func setupView() {
        viewControllers = [
            HomeController.instance(),
            UIViewController(),
            UIViewController(),
            SettingController.instance()
        ]
        setTabBar(items: [
            CustomTabBarItem(icon: UIImage(systemName: "square.grid.3x3")!, title: "All".localized),
            CustomTabBarItem(icon: UIImage(systemName: "music.mic")!, title: "Podcast".localized) {
                AppTargets.getAppMainViewController(app: .InteractivePodcast)
            },
            CustomTabBarItem(icon: UIImage(systemName: "video")!, title: "Dating".localized) {
                AppTargets.getAppMainViewController(app: .BlindDate)
            },
            CustomTabBarItem(icon: UIImage(systemName: "gearshape")!, title: "Settings".localized),
        ])
    }
}

