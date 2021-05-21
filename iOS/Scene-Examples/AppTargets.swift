//
//  Target.swift
//  Scene-Examples
//
//  Created by XC on 2021/5/20.
//

import Foundation
import UIKit
#if LEANCLOUD
import BlindDate_LeanCloud
import InteractivePodcast_LeanCloud
import Database_LeanCloud
#elseif FIREBASE
import BlindDate_Firebase
import InteractivePodcast_Firebase
import Database_Firebase
#endif
import Core

enum SceneApp {
    case InteractivePodcast
    case BlindDate
}

protocol AppTarget {
    func initDatabase() -> Void;
    func getAppMainViewController(app: SceneApp) -> UIViewController
}

#if LEANCLOUD
class LeanCloudAppTarget: AppTarget {
    func initDatabase() {
        Database.initConfig()
    }
    
    func getAppMainViewController(app: SceneApp) -> UIViewController {
        switch app {
        case .InteractivePodcast:
            return InteractivePodcast_LeanCloud.HomeController.instance()
        case .BlindDate:
            return BlindDate_LeanCloud.BlindDateHomeController.instance()
        }
    }
}
#elseif FIREBASE
class FirebaseAppTarget: AppTarget {
    func initDatabase() {
        Database.initConfig()
    }
    
    func getAppMainViewController(app: SceneApp) -> UIViewController {
        switch app {
        case .InteractivePodcast:
            return InteractivePodcast_Firebase.HomeController.instance()
        case .BlindDate:
            return BlindDate_Firebase.BlindDateHomeController.instance()
        }
    }
}
#endif

class AppTargets {
    #if LEANCLOUD
    let target: AppTarget = LeanCloudAppTarget()
    #elseif FIREBASE
    let target: AppTarget = FirebaseAppTarget()
    #endif
}
