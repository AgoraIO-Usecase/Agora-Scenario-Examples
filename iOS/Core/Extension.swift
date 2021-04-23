//
//  Extension.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/3.
//

import UIKit

extension String {
    public var localized: String { NSLocalizedString(self, bundle: Bundle(identifier: "io.agora.Core")!, comment: "") }
}

extension UIColor {
    public convenience init(hex: String, alpha: CGFloat? = nil) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(
            red:   CGFloat(r) / 255,
            green: CGFloat(g) / 255,
            blue:  CGFloat(b) / 255,
            alpha: alpha ?? CGFloat(a) / 255
        )
    }
}

public var screenWidth: CGFloat { return UIScreen.main.bounds.width }

extension UIView {
    
    public enum Relation {
        case equal
        case greaterOrEqual
        case lessOrEqual
    }
    
    public func highlight() {
        UIView.animate(withDuration: 0.1) {
            self.alpha = 0.5
        }
    }
    
    public func unhighlight() {
        UIView.animate(withDuration: 0.1) {
            self.alpha = 1
        }
    }
    
    public func roundCorners(_ corners: UIRectCorner, radius: CGFloat) {
        let path = UIBezierPath(roundedRect: self.bounds, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        let mask = CAShapeLayer()
        mask.path = path.cgPath
        self.layer.mask = mask
        self.clipsToBounds = true
    }
    
    public func rounded(color: String? = nil, borderWidth: CGFloat = 0, radius: CGFloat? = nil) {
        self.layer.cornerRadius = radius ?? min(self.bounds.width, self.bounds.height) / 2
        if let borderColor = color {
            self.layer.borderColor = UIColor(hex: borderColor).cgColor
        }
        self.layer.borderWidth = borderWidth
    }
    
    public func shadow(color: String = "#000", radius: CGFloat = 10, offset: CGSize = .zero, opacity: Float = 0.65) {
        self.layer.shadowRadius = radius
        self.layer.shadowColor = UIColor(hex: color).cgColor
        self.layer.shadowOffset = offset
        self.layer.shadowOpacity = opacity
    }
    
    public func animateTo(frame: CGRect, withDuration duration: TimeInterval, completion: ((Bool) -> Void)? = nil) {
        guard let _ = superview else {
            return
        }
    
        let xScale = frame.size.width / self.frame.size.width
        let yScale = frame.size.height / self.frame.size.height
        let x = frame.origin.x + (self.frame.width * xScale) * self.layer.anchorPoint.x
        let y = frame.origin.y + (self.frame.height * yScale) * self.layer.anchorPoint.y
        
        UIView.animate(withDuration: duration, delay: 0, options: .curveLinear, animations: {
            self.layer.position = CGPoint(x: x, y: y)
            self.transform = self.transform.scaledBy(x: xScale, y: yScale)
        }, completion: completion)
    }
    
    public func width(constant: CGFloat, relation: Relation = .equal) -> UIView {
        switch relation {
        case .equal:
            widthAnchor.constraint(equalToConstant: constant).isActive = true
        case .greaterOrEqual:
            widthAnchor.constraint(greaterThanOrEqualToConstant: constant).isActive = true
        case .lessOrEqual:
            widthAnchor.constraint(lessThanOrEqualToConstant: constant).isActive = true
        }
        return self
    }
    
    public func height(constant: CGFloat, relation: Relation = .equal) -> UIView {
        switch relation {
        case .equal:
            heightAnchor.constraint(equalToConstant: constant).isActive = true
        case .greaterOrEqual:
            heightAnchor.constraint(greaterThanOrEqualToConstant: constant).isActive = true
        case .lessOrEqual:
            heightAnchor.constraint(lessThanOrEqualToConstant: constant).isActive = true
        }
        return self
    }

    public func marginTop(anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = topAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }
    
    public func marginBottom(anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = bottomAnchor
        UIView.anchor(from, anchor, -constant, relation)
        return self
    }
    
    public func marginLeading(anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = leadingAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }
    
    public func marginTrailing(anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = trailingAnchor
        UIView.anchor(from, anchor, -constant, relation)
        return self
    }
    
    public func centerX(anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = centerXAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }
    
    public func centerY(anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = centerYAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }
    
    public func square() -> UIView {
        widthAnchor.constraint(equalTo: heightAnchor).isActive = true
        return self
    }
    
    public func fill(view: UIView, leading: CGFloat = 0, top: CGFloat = 0, trailing: CGFloat = 0, bottom: CGFloat = 0) -> UIView {
        return marginLeading(anchor: view.leadingAnchor, constant: leading)
            .marginTop(anchor: view.topAnchor, constant: top)
            .marginTrailing(anchor: view.trailingAnchor, constant: trailing)
            .marginBottom(anchor: view.bottomAnchor, constant: bottom)
    }
    
    public func active() {
        translatesAutoresizingMaskIntoConstraints = false
    }
    
    public func addConstraints(withFormat format: String, views: UIView...) {
        var viewsDictionary = [String: UIView]()
        for i in 0 ..< views.count {
            let key = "v\(i)"
            views[i].translatesAutoresizingMaskIntoConstraints = false
            viewsDictionary[key] = views[i]
        }
        addConstraints(NSLayoutConstraint.constraints(withVisualFormat: format, options: NSLayoutConstraint.FormatOptions(), metrics: nil, views: viewsDictionary))
    }
    
    public func addConstraints(withFormat format: String, arrayOf views: [UIView]) {
        var viewsDictionary = [String: UIView]()
        for i in 0 ..< views.count {
            let key = "v\(i)"
            views[i].translatesAutoresizingMaskIntoConstraints = false
            viewsDictionary[key] = views[i]
        }
        addConstraints(NSLayoutConstraint.constraints(withVisualFormat: format, options: NSLayoutConstraint.FormatOptions(), metrics: nil, views: viewsDictionary))
    }
    
    public static func anchor(_ from: NSLayoutYAxisAnchor, _ to: NSLayoutYAxisAnchor, _ constant: CGFloat, _ relation: Relation) {
        switch relation {
        case .equal:
            from.constraint(equalTo: to, constant: constant).isActive = true
        case .greaterOrEqual:
            from.constraint(greaterThanOrEqualTo: to, constant: constant).isActive = true
        case .lessOrEqual:
            from.constraint(lessThanOrEqualTo: to, constant: constant).isActive = true
        }
    }
    
    public static func anchor(_ from: NSLayoutXAxisAnchor, _ to: NSLayoutXAxisAnchor, _ constant: CGFloat, _ relation: Relation) {
        switch relation {
        case .equal:
            from.constraint(equalTo: to, constant: constant).isActive = true
        case .greaterOrEqual:
            from.constraint(greaterThanOrEqualTo: to, constant: constant).isActive = true
        case .lessOrEqual:
            from.constraint(lessThanOrEqualTo: to, constant: constant).isActive = true
        }
    }
    
    public func removeAllConstraints() {
        var _superview = self.superview
        while let superview = _superview {
            for constraint in superview.constraints {
                if let first = constraint.firstItem as? UIView, first == self {
                    superview.removeConstraint(constraint)
                }
                if let second = constraint.secondItem as? UIView, second == self {
                    superview.removeConstraint(constraint)
                }
            }
            _superview = superview.superview
        }
        self.removeConstraints(self.constraints)
        self.translatesAutoresizingMaskIntoConstraints = true
    }
}

extension UIViewController {
    public func addViewTop(_ view: UIView) -> UIView {
        if let window: UIWindow = UIApplication.shared.windows.filter({ $0.isKeyWindow }).first {
            window.addSubview(view)
            return window
        } else {
            self.view.addSubview(view)
            return self.view
        }
    }
    
    public func showToast(message: String?, type: NotificationType = .info, duration: CGFloat = 1.5) {
        guard let _message = message else {
            return
        }
        self.show(message: _message, type: type, duration: duration)
    }
}

extension UIWindow {
    func visibleViewController() -> UIViewController? {
        if let rootViewController: UIViewController = self.rootViewController {
            return UIWindow.getVisibleViewControllerFrom(rootViewController)
        }
        return nil
    }
    
    class func getVisibleViewControllerFrom(_ vc: UIViewController) -> UIViewController {
        if vc.isKind(of: UINavigationController.self) {
            let navigationController = vc as! UINavigationController
            return UIWindow.getVisibleViewControllerFrom(navigationController.visibleViewController!)
        } else if vc.isKind(of: UITabBarController.self) {
            let tabBarController = vc as! UITabBarController
            return UIWindow.getVisibleViewControllerFrom(tabBarController.selectedViewController!)
        } else {
            if let presentedViewController = vc.presentedViewController {
                return UIWindow.getVisibleViewControllerFrom(presentedViewController.presentedViewController!)
            } else {
                return vc;
            }
        }
    }
}

extension UIView {
    public class func loadFromNib<T>(name: String, bundle: Bundle? = nil) -> T? {
        return UINib(
            nibName: name,
            bundle: bundle
        ).instantiate(withOwner: nil, options: nil)[0] as? T
    }
    
    public func onTap() -> UITapGestureRecognizer {
        let tap = UITapGestureRecognizer()
        addGestureRecognizer(tap)
        return tap
    }
}

extension UIView {
    public func gradientLayer(colors: [UIColor], degree: CGFloat = 0) -> CAGradientLayer {
        let layer = CAGradientLayer()
        layer.frame = bounds
        layer.colors = colors.map({ (color: UIColor) -> CGColor in
            color.cgColor
        })

        let a = tan((degree + 45) * CGFloat.pi / 180) - 1
        let b = 0.5 - a / 2
        
        layer.startPoint = CGPoint(x: 0, y: b)
        layer.endPoint = CGPoint(x: 1, y: a + b)
        return layer
    }
}

extension UIImage {
    public static func gradient(colors: [UIColor], with frame: CGRect, degree: CGFloat = 0) -> UIImage? {
        let layer = CAGradientLayer()
        layer.frame = frame
        layer.colors = colors.map({ (color: UIColor) -> CGColor in
            color.cgColor
        })

        let a = tan((degree + 45) * CGFloat.pi / 180) - 1
        let b = 0.5 - a / 2
        
        layer.startPoint = CGPoint(x: 0, y: b)
        layer.endPoint = CGPoint(x: 1, y: a + b)
        
        UIGraphicsBeginImageContext(CGSize(width: frame.width, height: frame.height))
        layer.render(in: UIGraphicsGetCurrentContext()!)
        guard let image = UIGraphicsGetImageFromCurrentImageContext() else { return nil }
        UIGraphicsEndImageContext()
        return image
    }
}

extension UIRefreshControl {
    public func refreshManually() {
        if let scrollView = superview as? UIScrollView {
            scrollView.setContentOffset(CGPoint(x: 0, y: scrollView.contentOffset.y - frame.height), animated: false)
        }
        //beginRefreshing()
        sendActions(for: .valueChanged)
    }
}

extension UIColor {

    public class var background: UIColor {
        if #available(iOS 13.0, *) {
            return .systemBackground
        } else {
            return .white
        }
    }

    public class var secondaryBackground: UIColor {
        if #available(iOS 13.0, *) {
            return .secondarySystemBackground
        } else {
            return .lightGray
        }
    }

    public class var defaultSeparator: UIColor {
        if #available(iOS 13.0, *) {
            return UIColor.separator
        } else {
            return UIColor(red: 200 / 255.0,
                           green: 199 / 255.0,
                           blue: 204 / 255.0,
                           alpha: 1)
        }
    }

    public class var titleLabel: UIColor {
        if #available(iOS 13.0, *) {
            return .label
        } else {
            return .darkText
        }
    }

    public class var detailLabel: UIColor {
        if #available(iOS 13.0, *) {
            return .secondaryLabel
        } else {
            return .lightGray
        }
    }
}
