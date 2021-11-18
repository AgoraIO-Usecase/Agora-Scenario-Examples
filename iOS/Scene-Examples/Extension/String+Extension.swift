//
//  String+Extension.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

extension String {
    func size(font: UIFont, drawRange size: CGSize) -> CGSize {
        let attributes = [NSAttributedString.Key.font: font]
        let option = NSStringDrawingOptions.usesLineFragmentOrigin
        let rect = self.boundingRect(with: size,
                                     options: option,
                                     attributes: attributes,
                                     context: nil)
        return rect.size
    }
}

extension String {
    var localized: String { NSLocalizedString(self, comment: "") }
}
extension String {
    var timeStamp: String {
        let date = Date()
        let timeInterval = date.timeIntervalSince1970
        let millisecond = CLongLong(timeInterval * 1000)
        return "\(millisecond)"
    }
    func isChinese(str: String) -> Bool{
        let match: String = "(^[\\u4e00-\\u9fa5]+$)"
        let predicate = NSPredicate(format: "SELF matches %@", match)
        return predicate.evaluate(with: str)
    }
    
    func timeFormat(secounds: TimeInterval,
                           h: String = ":",
                           m: String = ":",
                           s: String = "",
                           isShowHour: Bool = false) -> String {
        guard !secounds.isNaN else { return "00\(m)00" }
        var minTime = Int(secounds / 60)
        let second = Int(secounds.truncatingRemainder(dividingBy: 60))
        var hour = 0
        if isShowHour || minTime >= 60 {
            hour = Int(minTime / 60)
            minTime -= hour * 60
            return String(format: "%02d%@%02d%@%02d%@", hour, h, minTime, m, second, s)
        }
        return String(format: "%02d%@%02d%@", minTime, m, second, s)
    }
}
