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
import InteractivePodcast
import BlindDate

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
            CustomTabBarItem(icon: UIImage(systemName: "square.grid.3x3")!, title: "全部"),
            CustomTabBarItem(icon: UIImage(systemName: "volume")!, title: "互动播客") {
                InteractivePodcast.HomeController.instance()
            },
            CustomTabBarItem(icon: UIImage(systemName: "video")!, title: "在线相亲") {
                BlindDate.BlindDateHomeController.instance()
            },
            CustomTabBarItem(icon: UIImage(systemName: "gearshape")!, title: "设置"),
        ])
    }
}

