//
//  Target.swift
//  Scene-Examples
//
//  Created by XC on 2021/5/20.
//

import Foundation
import UIKit
import BlindDate
import InteractivePodcast
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
        SyncManager.shared.setProxy(LeanCloudSyncProxy())
        UserManager.shared.setProxy(LeanCloudUserProxy())
        PodcastModelManager.shared.setProxy(LeanCloudPodcastModelProxy())
        BlindDateModelManager.shared.setProxy(LeanCloudBlindDateModelProxy())
    }
    
    func getAppMainViewController(app: SceneApp) -> UIViewController {
        switch app {
        case .InteractivePodcast:
            return InteractivePodcast.HomeController.instance()
        case .BlindDate:
            return BlindDate.BlindDateHomeController.instance()
        }
    }
}
#elseif FIREBASE
class FirebaseAppTarget: AppTarget {
    func initDatabase() {
        Database.initConfig()
        SyncManager.shared.setProxy(FirebaseSyncProxy())
        UserManager.shared.setProxy(FirebaseUserProxy())
        PodcastModelManager.shared.setProxy(FirebasePodcastModelProxy())
        BlindDateModelManager.shared.setProxy(FirebaseBlindDateModelProxy())
    }
    
    func getAppMainViewController(app: SceneApp) -> UIViewController {
        switch app {
        case .InteractivePodcast:
            return InteractivePodcast.HomeController.instance()
        case .BlindDate:
            return BlindDate.BlindDateHomeController.instance()
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
