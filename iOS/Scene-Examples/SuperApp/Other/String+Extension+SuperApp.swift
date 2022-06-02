//
//  String+Extension.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/15.
//

import Foundation

extension String {
    /// 截取到任意位置
    func subString(to: Int) -> String {
        let index: String.Index = self.index(startIndex, offsetBy: to)
        return String(self[..<index])
    }
    /// 从任意位置开始截取
    func subString(from: Int) -> String {
        let index: String.Index = self.index(startIndex, offsetBy: from)
        return String(self[index ..< endIndex])
    }
    /// 从任意位置开始截取到任意位置
    func subString(from: Int, to: Int) -> String {
        let beginIndex = self.index(self.startIndex, offsetBy: from)
        let endIndex = self.index(self.startIndex, offsetBy: to)
        return String(self[beginIndex...endIndex])
    }
}

extension String {
    var headImageName: String {
        let temp = self
        var result: UInt64 = 0
        Scanner(string: String(temp)).scanHexInt64(&result)
        let i = (result % 12)
        return "pic-" + "\(i)"
    }
}

extension String {
    static var randomUserName: String {
        let list1 = ["富贵",
                     "国强",
                     "建国",
                     "林",
                     "麒麟",
                     "宝强",
                     "强",
                     "相如",
                     "欢喜"]
        let list2 = ["赵",
                     "钱",
                     "孙",
                     "李",
                     "周",
                     "吴",
                     "郑",
                     "王"]
        return (list2.randomElement() ?? list2.first!) +
        (list1.randomElement() ?? list1.first!)
    }
    
    static var randomRoomName: String {
       let list = ["陌上花开等你来", "天天爱你", "我爱你们",
                "有人可以", "风情万种", "强势归来",
                "哈哈哈", "聊聊", "美人舞江山",
                "最美的回忆", "遇见你", "最长情的告白",
                "全力以赴", "简单点", "早上好",
                "春风十里不如你"]
        return list.randomElement() ?? list.first!
    }
}
