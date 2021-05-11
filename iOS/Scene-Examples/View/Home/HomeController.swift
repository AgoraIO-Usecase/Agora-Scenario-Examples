//
//  HomeController.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/19.
//

import Foundation
import UIKit
#if LEANCLOUD
import InteractivePodcast_LeanCloud
import BlindDate_LeanCloud
#elseif FIREBASE
import InteractivePodcast_Firebase
import BlindDate_Firebase
#endif

class InteractivePodcastCard: HomeCard {
    var title: String = "Interactive Podcast".localized
    var color: UIColor = UIColor(hex: Colors.Blue)
    
    func create() -> UIViewController {
        #if LEANCLOUD
            InteractivePodcast_LeanCloud.HomeController.instance()
        #elseif FIREBASE
            InteractivePodcast_Firebase.HomeController.instance()
        #else
            UIViewController()
        #endif
    }
}

class InteractiveLiveDatingCard: HomeCard {
    var title: String = "Interactive Live Dating".localized
    var color: UIColor = UIColor(hex: Colors.LightBLue)

    func create() -> UIViewController {
        #if LEANCLOUD
            BlindDate_LeanCloud.BlindDateHomeController.instance()
        #elseif FIREBASE
            BlindDate_Firebase.BlindDateHomeController.instance()
        #else
            UIViewController()
        #endif
    }
}

class HomeController: UITableViewController {
    
    var listData: Array<HomeCard> = [
        InteractivePodcastCard(),
        InteractiveLiveDatingCard()
    ]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.backgroundColor = .white
        tableView.rowHeight = 200
        tableView.separatorStyle = .none
        tableView.register(HomeCardView.self, forCellReuseIdentifier: NSStringFromClass(HomeCardView.self))
    }
    
    static func instance() -> HomeController {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let controller = storyBoard.instantiateViewController(withIdentifier: "HomeController") as! HomeController
        return controller
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return listData.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let identifier = NSStringFromClass(HomeCardView.self)
        let cell = tableView.dequeueReusableCell(withIdentifier: identifier, for: indexPath) as! HomeCardView
        cell.item = listData[indexPath.row]
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let item = listData[indexPath.row]
        navigationController?.pushViewController(item.create(), animated: true)
    }
}
