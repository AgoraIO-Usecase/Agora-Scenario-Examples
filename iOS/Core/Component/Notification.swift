//
//  Notification.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/6.
//

import Foundation
import UIKit

public enum NotificationType {
    case info
    case error
}

extension UIViewController {
    public func show(message: String, type: NotificationType, duration: CGFloat = 1.5) {
        DispatchQueue.main.async {
            let view = NotificationView.create(message: message, type: type)
            view.alpha = 0
            let root = self.addViewTop(view)
            view.marginLeading(anchor: root.leadingAnchor, constant: 16)
                .centerX(anchor: root.centerXAnchor)
                .marginTop(anchor: root.safeAreaLayoutGuide.topAnchor, constant: 16)
                .active()
            let translationY: CGFloat = 40
            view.transform = CGAffineTransform(translationX: 0, y: -translationY)
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                view.alpha = 1
                view.transform = CGAffineTransform(translationX: 0, y: 0)
            }, completion: { success in
                let translationY = view.bounds.height
                UIView.animate(withDuration: 0.3, delay: TimeInterval(duration), options: .curveEaseInOut, animations: {
                    view.alpha = 0
                    view.transform = CGAffineTransform(translationX: 0, y: -translationY)
                }, completion: { _ in
                    view.removeFromSuperview()
                })
            })
        }
    }
    
    public func show(processing: Bool) {
        DispatchQueue.main.async {[unowned self] in
            var oldView = self.view.viewWithTag(233)
            if (processing) {
                if (oldView == nil) {
                    oldView = UIView(frame: self.view.frame)
                }
                guard let backgroundView = oldView else {
                    return
                }
                backgroundView.tag = 233
                let view = ProcessingView.create()
                view.center = backgroundView.center
                
                backgroundView.addSubview(view)
                backgroundView.alpha = 0
                self.view.addSubview(backgroundView)
                
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    backgroundView.alpha = 1
                })
            } else if let backgroundView = oldView {
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    backgroundView.alpha = 0
                }, completion: { _ in
                    backgroundView.removeFromSuperview()
                })
            }
        }
    }
}
