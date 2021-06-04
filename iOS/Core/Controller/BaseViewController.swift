//
//  BaseViewController.swift
//  Core
//
//  Created by XC on 2021/4/19.
//

import Foundation
import UIKit
import RxSwift
import RxCocoa

open class BaseViewContoller: UIViewController {
    
    public let disposeBag = DisposeBag()
    private var dialogBackgroundMaskView: UIView?
    private var onDismiss: (() -> Void)? = nil
    public var enableSwipeGesture: Bool = true
    
    open override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    open override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    open override func viewDidLoad() {
        super.viewDidLoad()
        if (enableSwipeGesture) {
            self.navigationController?.interactivePopGestureRecognizer?.delegate = nil
        }
    }

    private func _showMaskView(dialog: UIView, alpha: CGFloat = 0.3) {
        if (self.dialogBackgroundMaskView == nil) {
            self.dialogBackgroundMaskView = UIView()
            self.dialogBackgroundMaskView!.backgroundColor = UIColor.black
            self.dialogBackgroundMaskView!.alpha = 0
            
            let root = addViewTop(self.dialogBackgroundMaskView!)
            self.dialogBackgroundMaskView!.fill(view: root).active()
        }
        if let mask = dialogBackgroundMaskView {
            mask.onTap().rx.event.flatMap { [unowned self] _ in
                return self.dismiss(dialog: dialog)
            }
            .subscribe()
            .disposed(by: disposeBag)
        }
        if let maskView: UIView = self.dialogBackgroundMaskView {
            maskView.alpha = 0
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                maskView.alpha = alpha
            })
        }
    }
    
    private func _hiddenMaskView() {
        if let maskView = self.dialogBackgroundMaskView {
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                maskView.alpha = 0
            }, completion: { _ in
                maskView.removeFromSuperview()
                self.dialogBackgroundMaskView = nil
            })
        }
    }

    open func show(dialog: UIView,
                   style: DialogStyle = .center,
                   padding: CGFloat = 0,
                   relation: UIView.Relation = .equal,
                   onDismiss: (() -> Void)? = nil) -> Single<Bool> {
        return Single.create { [unowned self] single in
            self.onDismiss = onDismiss
            dialog.tag = style.rawValue
            
            switch style {
            case .bottom:
                _showMaskView(dialog: dialog)
                let root = addViewTop(dialog)
                //self.view.addSubview(dialog)
                dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                    .centerX(anchor: root.centerXAnchor)
                    .marginBottom(anchor: root.bottomAnchor)
                    .active()
                
                dialog.alpha = 0
                let translationY = view.frame.height
                dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    dialog.alpha = 1
                    dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                }, completion: { finish in
                    single(.success(finish))
                })
            case .center:
                _showMaskView(dialog: dialog, alpha: 0.65)
                let root = addViewTop(dialog)
                //self.view.addSubview(dialog)
                dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                    .centerX(anchor: root.centerXAnchor)
                    .centerY(anchor: root.centerYAnchor, constant: -50)
                    .active()

                dialog.alpha = 0
                dialog.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    dialog.alpha = 1
                    dialog.transform = CGAffineTransform(scaleX: 1, y: 1)
                }, completion: { finish in
                    single(.success(finish))
                })
            case .top:
                _showMaskView(dialog: dialog)
                let root = addViewTop(dialog)
                //self.view.addSubview(dialog)
                dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                    .centerX(anchor: root.centerXAnchor)
                    .marginTop(anchor: root.safeAreaLayoutGuide.topAnchor, constant: padding, relation: relation)
                    .active()
                
                dialog.alpha = 0
                let translationY = view.frame.height
                dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    dialog.alpha = 1
                    dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                }, completion: { finish in
                    single(.success(finish))
                })
            case .topNoMask:
                let root = addViewTop(dialog, window: false)
                //self.view.addSubview(dialog)
                dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                    .centerX(anchor: root.centerXAnchor)
                    .marginTop(anchor: root.safeAreaLayoutGuide.topAnchor, constant: padding, relation: relation)
                    .active()
                
                dialog.alpha = 0
                let translationY = view.frame.height
                dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    dialog.alpha = 1
                    dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                }, completion: { finish in
                    single(.success(finish))
                })
            case .bottomNoMask:
                let root = addViewTop(dialog, window: false)
                //self.view.addSubview(dialog)
                if (padding > 0) {
                    dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                        .centerX(anchor: root.centerXAnchor)
                        .marginBottom(anchor: root.safeAreaLayoutGuide.bottomAnchor, constant: padding, relation: relation)
                        .active()
                } else {
                    dialog.marginLeading(anchor: root.leadingAnchor)
                        .centerX(anchor: root.centerXAnchor)
                        .marginBottom(anchor: root.bottomAnchor)
                        .active()
                }
                
                dialog.alpha = 0
                let translationY = view.bounds.height
                dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    dialog.alpha = 1
                    dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                }, completion: { finish in
                    single(.success(finish))
                })
            }
            
            return Disposables.create()
        }
        .subscribe(on: MainScheduler.instance)
    }
    
    open func dismiss(dialog: UIView) -> Single<Bool> {
        return Single.create { [unowned self] single in
            _hiddenMaskView()
            let style = DialogStyle.valueOf(style: dialog.tag)
            switch style {
            case .bottom:
                //dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                //dialog.alpha = 1
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    let translationY = dialog.bounds.height
                    dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
                    dialog.alpha = 0
                }, completion: { finish in
                    dialog.removeFromSuperview()
                    if let onDismiss = self.onDismiss {
                        onDismiss()
                    }
                    self.onDismiss = nil
                    single(.success(finish))
                })
            case .center:
                //dialog.transform = CGAffineTransform(scaleX: 1, y: 1)
                //dialog.alpha = 1
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    dialog.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
                    dialog.alpha = 0
                }, completion: { finish in
                    dialog.removeFromSuperview()
                    if let onDismiss = self.onDismiss {
                        onDismiss()
                    }
                    self.onDismiss = nil
                    single(.success(finish))
                })
            case .top:
                //dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                //dialog.alpha = 1
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    let translationY = dialog.bounds.height
                    dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
                    dialog.alpha = 0
                }, completion: { finish in
                    dialog.removeFromSuperview()
                    if let onDismiss = self.onDismiss {
                        onDismiss()
                    }
                    self.onDismiss = nil
                    single(.success(finish))
                })
            case .bottomNoMask:
                //dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                //dialog.alpha = 1
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    let translationY = dialog.bounds.height
                    dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
                    dialog.alpha = 0
                }, completion: { finish in
                    dialog.removeFromSuperview()
                    if let onDismiss = self.onDismiss {
                        onDismiss()
                    }
                    self.onDismiss = nil
                    single(.success(finish))
                })
            case .topNoMask:
                //dialog.transform = CGAffineTransform(translationX: 0, y: 0)
                //dialog.alpha = 1
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    let translationY = dialog.bounds.height
                    dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
                    dialog.alpha = 0
                }, completion: { finish in
                    dialog.removeFromSuperview()
                    if let onDismiss = self.onDismiss {
                        onDismiss()
                    }
                    self.onDismiss = nil
                    single(.success(finish))
                })
            }
            
            return Disposables.create()
        }
        .subscribe(on: MainScheduler.instance)
    }
    
    open func dismiss() -> Single<Bool> {
        return Single.create { [unowned self] single in
            if let navigationController = self.navigationController {
                navigationController.popViewController(animated: true)
                single(.success(true))
            } else {
                self.dismiss(animated: true, completion: {
                    single(.success(true))
                })
            }
            return Disposables.create()
        }
    }
    
    open func showAlert(title: String, message: String) -> Observable<Bool> {
        return Single.create { single in
            let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
            let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
                single(.success(false))
            }
            alertController.addAction(cancel)
            let ok = UIAlertAction(title: "Ok".localized, style: .default) { _ in
                single(.success(true))
            }
            alertController.addAction(ok)
            self.present(alertController, animated: true, completion: nil)
            
            return Disposables.create()
        }
        .subscribe(on: MainScheduler.instance)
        .asObservable()
    }
    
    open func pop() -> Single<Bool> {
        return Single.create { [unowned self] single in
            if let navigationController = self.navigationController {
                Logger.log(message: "pop with navigationController", level: .info)
                UIView.transition(with: self.navigationController!.view!, duration: 0.3, options: .curveEaseOut) {
                    let transition = CATransition()
                    transition.duration = 0
                    transition.type = CATransitionType.push
                    transition.subtype = CATransitionSubtype.fromBottom
                    transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
                    self.navigationController?.view.layer.add(transition, forKey: kCATransition)
                } completion: { _ in
                    Logger.log(message: "pop with navigationController finish", level: .info)
                    navigationController.popViewController(animated: false)
                    single(.success(true))
                }
            } else {
                Logger.log(message: "pop with dismiss", level: .info)
                self.dismiss(animated: true, completion: {
                    single(.success(true))
                })
            }
            return Disposables.create()
        }
    }
    
    open func push(controller: UIViewController) {
        UIView.transition(with: self.navigationController!.view!, duration: 0.3, options: .curveEaseOut) {
            let transition = CATransition()
            transition.duration = 0
            transition.type = CATransitionType.push
            transition.subtype = CATransitionSubtype.fromTop
            transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
            self.navigationController?.view.layer.add(transition, forKey: kCATransition)
            self.navigationController?.pushViewController(controller, animated: false)
        }
    }
    
    open func keyboardHeight() -> Observable<CGFloat> {
        return Observable.from([
            NotificationCenter.default.rx.notification(UIApplication.keyboardDidShowNotification)
                .map { notification in notification.keyboardHeight },
            NotificationCenter.default.rx.notification(UIApplication.keyboardWillHideNotification)
                .map { _ in 0 }
            ])
            .merge()
    }
}
