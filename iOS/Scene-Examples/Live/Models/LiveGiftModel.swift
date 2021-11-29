//
//  LiveGiftModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/12.
//

import UIKit

struct LiveGiftModel: Codable {
    enum GiftType: Int, Codable {
        /// 提示卡
        case tips = 1
        /// 免答卡
        case avoid = 2
        /// 延时卡
        case delay = 3
        /// 减时卡
        case reduction = 4
        /// 遮挡卡
        case shelter = 5
    }
    
    var iconName: String = ""
    var title: String = ""
    var coin: Int = 0
    var gifName: String = ""
    var userId: String = "\(UserInfo.userId)"
    var giftType: GiftType = .delay
    
    
    static func createGiftData() -> [LiveGiftModel] {
        var dataArray = [LiveGiftModel]()
        var model = LiveGiftModel(iconName: "gift-dang".localized,
                                  title: "Small_Bell".localized,
                                  coin: 20,
                                  gifName: "SuperBell",
                                  giftType: .tips)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-icecream",
                              title: "Ice_Cream".localized,
                              coin: 30,
                              gifName: "SuperIcecream",
                              giftType: .avoid)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-wine",
                              title: "Wine".localized,
                              coin: 40,
                              gifName: "SuperWine",
                              giftType: .delay)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-cake",
                              title: "Cake".localized,
                              coin: 50,
                              gifName: "SuperCake",
                              giftType: .reduction)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-ring",
                              title: "Ring".localized,
                              coin: 60,
                              gifName: "SuperRing",
                              giftType: .shelter)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-watch",
                              title: "Watch".localized,
                              coin: 70,
                              gifName: "SuperWatch",
                              giftType: .delay)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-diamond",
                              title: "Crystal".localized,
                              coin: 80,
                              gifName: "SuperDiamond",
                              giftType: .shelter)
        dataArray.append(model)
        model = LiveGiftModel(iconName: "gift-rocket",
                              title: "Rocket".localized,
                              coin: 90,
                              gifName: "SuperRocket",
                              giftType: .tips)
        dataArray.append(model)
        return dataArray
    }
}
