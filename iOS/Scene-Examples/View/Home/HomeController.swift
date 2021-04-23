//
//  HomeController.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/19.
//

import Foundation
import UIKit
import InteractivePodcast
import BlindDate

class InteractivePodcastCard: HomeCard {
    var title: String = "互动播客"
    var color: UIColor = UIColor(hex: Colors.Blue)
    
    func create() -> UIViewController {
        InteractivePodcast.HomeController.instance()
    }
}

class DateFaceTime: HomeCard {
    var title: String = "相亲"
    var color: UIColor = UIColor(hex: Colors.LightBLue)
    
    func create() -> UIViewController {
        BlindDate.BlindDateHomeController.instance()
    }
}

class HomeController: UITableViewController {
    
    var listData: Array<HomeCard> = [
        InteractivePodcastCard(),
        DateFaceTime(),
        InteractivePodcastCard(),
        DateFaceTime(),
        InteractivePodcastCard(),
        DateFaceTime(),
        InteractivePodcastCard(),
        DateFaceTime()
    ]
    
    override func viewDidLoad() {
        super.viewDidLoad()
//        tableView.contentInsetAdjustmentBehavior = .automatic
//        tableView.automaticallyAdjustsScrollIndicatorInsets = true
        tableView.backgroundColor = .white
        tableView.rowHeight = 200
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
