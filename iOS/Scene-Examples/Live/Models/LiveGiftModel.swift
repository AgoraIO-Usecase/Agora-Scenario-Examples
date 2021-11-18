//
//  LiveGiftModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/12.
//

import UIKit

struct LiveGiftModel: Codable {
    var iconName: String = ""
    var title: String = ""
    var coin: Int = 0
    var gifName: String = ""
    var userId: String = "\(UserInfo.userId)"
    static func createGiftData() -> [LiveGiftModel] {
        var dataArray = [LiveGiftModel]()
        var model = LiveGiftModel(iconName: "gift-dang".localized,
                                  title: "Small_Bell".localized,
                                  coin: 20, gifName: "SuperBell")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-icecream", title: "Ice_Cream".localized, coin: 30, gifName: "SuperIcecream")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-wine", title: "Wine".localized, coin: 40, gifName: "SuperWine")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-cake", title: "Cake".localized, coin: 50, gifName: "SuperCake")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-ring", title: "Ring".localized, coin: 60, gifName: "SuperRing")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-watch", title: "Watch".localized, coin: 70, gifName: "SuperWatch")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-diamond", title: "Crystal".localized, coin: 80, gifName: "SuperDiamond")
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-rocket", title: "Rocket".localized, coin: 90, gifName: "SuperRocket")
        dataArray.append(model)
        return dataArray
    }
}
