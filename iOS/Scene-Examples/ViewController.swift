//
//  ViewController.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/16.
//

import UIKit
import RxCocoa
import RxSwift
#if LEANCLOUD
import Core_LeanCloud
import InteractivePodcast_LeanCloud
import BlindDate_LeanCloud
#elseif FIREBASE
import Core_Firebase
import InteractivePodcast_Firebase
import BlindDate_Firebase
#endif

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
        #if LEANCLOUD
            setTabBar(items: [
                CustomTabBarItem(icon: UIImage(systemName: "square.grid.3x3")!, title: "All".localized),
                CustomTabBarItem(icon: UIImage(systemName: "music.mic")!, title: "Podcast".localized) {
                    InteractivePodcast_LeanCloud.HomeController.instance()
                },
                CustomTabBarItem(icon: UIImage(systemName: "video")!, title: "Dating".localized) {
                    BlindDate_LeanCloud.BlindDateHomeController.instance()
                },
                CustomTabBarItem(icon: UIImage(systemName: "gearshape")!, title: "Settings".localized),
            ])
        #elseif FIREBASE
            setTabBar(items: [
                CustomTabBarItem(icon: UIImage(systemName: "square.grid.3x3")!, title: "All".localized),
                CustomTabBarItem(icon: UIImage(systemName: "music.mic")!, title: "Podcast".localized) {
                    InteractivePodcast_Firebase.HomeController.instance()
                },
                CustomTabBarItem(icon: UIImage(systemName: "video")!, title: "Dating".localized) {
                    BlindDate_Firebase.BlindDateHomeController.instance()
                },
                CustomTabBarItem(icon: UIImage(systemName: "gearshape")!, title: "Settings".localized),
            ])
        #endif
    }
}

