//
//  UIColor+Extension.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit

extension UIColor {
    static var blueColor: UIColor {
        .init(hex: "#2397FE")
    }
    
    static var textOnAccent: UIColor {
        .init(hex: "#BABCBD")
    }
    static var primaryTint10: UIColor {
        .init(hex: "#3899DC")
    }
    static var primaryTint20: UIColor {
        .init(hex: "#84692")
    }
    static var primaryTint30: UIColor {
        .init(hex: "#667A3")
    }
    static var titleDisabled: UIColor {
        .init(hex: "#9BA4AF")
    }
    static var backgroundFilledDisabled: UIColor {
        .init(hex: "#787F89")
    }
}

public extension UIColor {
    
    /// 便利构造Hex颜色
    ///
    /// - Parameters:
    ///   - string: hex值
    ///   - alpha: alpha值，默认1.0
    convenience init(hex string: String, alpha: CGFloat = 1.0) {
        
        var hex = string.hasPrefix("#") ? String(string.dropFirst()) : string
        guard hex.count == 3 || hex.count == 6  else {
            self.init(white: 1.0, alpha: 0.0)
            return
        }
        
        if hex.count == 3 {
            for (indec, char) in hex.enumerated() {
                hex.insert(char, at: hex.index(hex.startIndex, offsetBy: indec * 2))
            }
        }
        
        self.init(
            red: CGFloat((Int(hex, radix: 16)! >> 16) & 0xFF) / 255.0,
            green: CGFloat((Int(hex, radix: 16)! >> 8) & 0xFF) / 255.0,
            blue: CGFloat((Int(hex, radix: 16)!) & 0xFF) / 255.0,
            alpha: alpha
        )
    }
    
    var randomColor: UIColor {
        UIColor(red: CGFloat(arc4random()%256)/255.0,
                green: CGFloat(arc4random()%256)/255.0,
                blue: CGFloat(arc4random()%256)/255.0,
                alpha: 1)
    }
}

extension UIColor {
    private struct AssociatedKeys {
        static var light: String = "light"
        static var lightHighContrast: String = "lightHighContrast"
    }

    private var light: UIColor? {
        get {
            return objc_getAssociatedObject(self, &AssociatedKeys.light) as? UIColor
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.light, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
    private var lightHighContrast: UIColor? {
        get {
            return objc_getAssociatedObject(self, &AssociatedKeys.lightHighContrast) as? UIColor
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.lightHighContrast, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    convenience init(light: UIColor, lightHighContrast: UIColor? = nil, lightElevated: UIColor? = nil, lightElevatedHighContrast: UIColor? = nil, dark: UIColor? = nil, darkHighContrast: UIColor? = nil, darkElevated: UIColor? = nil, darkElevatedHighContrast: UIColor? = nil) {
        self.init { traits -> UIColor in
            let getColorForContrast = { (_ default: UIColor?, highContrast: UIColor?) -> UIColor? in
                if traits.accessibilityContrast == .high, let color = highContrast {
                    return color
                }
                return `default`
            }

            let getColor = { (_ default: UIColor?, highContrast: UIColor?, elevated: UIColor?, elevatedHighContrast: UIColor?) -> UIColor? in
                if traits.userInterfaceLevel == .elevated,
                    let color = getColorForContrast(elevated, elevatedHighContrast) {
                    return color
                }
                return getColorForContrast(`default`, highContrast)
            }

            if traits.userInterfaceStyle == .dark,
                let color = getColor(dark, darkHighContrast, darkElevated, darkElevatedHighContrast) {
                return color
            }
            return getColor(light, lightHighContrast, lightElevated, lightElevatedHighContrast)!
        }
    }
}
