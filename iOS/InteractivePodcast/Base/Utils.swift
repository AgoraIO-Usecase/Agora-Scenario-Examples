//
//  Utils.swift
//  InteractivePodcast
//
//  Created by XC on 2021/4/20.
//

import Foundation
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

extension Utils {
    static let bundle = Bundle(identifier: "io.agora.InteractivePodcast")!
    
    static let namesData: [String: [String]] = [
        "cn": [
            "最长的电影",
            "Good voice",
            "Bad day",
            "好故事不容错过",
            "Greatest talk show"
        ],
        "default": [
            "The longest movie",
            "Good voice",
            "Bad day",
            "Good story not to be missed",
            "Greatest talk show"
        ]
    ]
    
    static func randomRoomName() -> String {
        let language = getCurrentLanguage()
        let names = namesData[language] ?? namesData["default"]!
        let index = Int(arc4random_uniform(UInt32(names.count)))
        return names[index]
    }
}

extension String {
    public var localized: String { NSLocalizedString(self, bundle: Utils.bundle, comment: "") }
}
