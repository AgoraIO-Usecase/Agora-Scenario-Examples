//
//  HandsupDialog.swift
//  BlindDate
//
//  Created by XC on 2021/4/23.
//

import Foundation
import UIKit
import RxSwift
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

class InvitedDialog: Dialog {
    weak var delegate: RoomDelegate!
    var action: Action! {
        didSet {
            message.text = "\(self.delegate.viewModel.roomManager?.user.name ?? "") \("invite you to speak".localized)"
        }
    }
    
    var title: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 20, weight: .bold)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Black)
        view.text = "邀请连麦"
        view.textAlignment = .center
        return view
    }()
    
    var message: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 16)
        view.numberOfLines = 0
        view.textColor = UIColor(hex: Colors.Black)
        view.text = ""
        view.textAlignment = .center
        return view
    }()
    
    var cancelButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.Purple
        view.setTitle("Cancel".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Purple), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        return view
    }()
    
    var okButton: UIButton = {
        let view = RoundButton()
        view.setTitle("Ok".localized, for: .normal)
        view.borderColor = Colors.Purple
        view.setTitleColor(UIColor(hex: Colors.White), for: .normal)
        view.backgroundColor = UIColor(hex: Colors.Purple)
        view.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        return view
    }()
    
    override func setup() {
        addSubview(title)
        addSubview(message)
        addSubview(cancelButton)
        addSubview(okButton)
        
        title.marginTop(anchor: topAnchor, constant: 30)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .centerX(anchor: centerXAnchor)
            .active()
        
        message.marginTop(anchor: title.bottomAnchor, constant: 27)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .centerX(anchor: centerXAnchor)
            .active()
        
        cancelButton
            .height(constant: 42)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .marginTrailing(anchor: centerXAnchor, constant: 7.5)
            .marginTop(anchor: message.bottomAnchor, constant: 27)
            .marginBottom(anchor: bottomAnchor, constant: 30)
            .active()
        
        okButton
            .height(constant: 42)
            .marginLeading(anchor: centerXAnchor, constant: 7.5)
            .marginTrailing(anchor: trailingAnchor, constant: 30)
            .marginTop(anchor: message.bottomAnchor, constant: 27)
            .marginBottom(anchor: bottomAnchor, constant: 30)
            .active()
        
        backgroundColor = UIColor(hex: Colors.White)
        
        cancelButton.rx.tap
            .flatMap { [unowned self] _ in
                return self.delegate.viewModel.process(action: self.action, agree: false)
            }
            .flatMap { [unowned self] result -> Observable<Result<Bool>> in
                return result.onSuccess {
                    return self.delegate.dismiss(dialog: self).asObservable().map { _ in Result(success: true) }
                }
            }
            .subscribe(onNext: { [unowned self] result in
                if (!result.success) {
                    self.delegate.show(message: result.message ?? "unknown error".localized, type: .error, duration: 1.5)
                }
            })
            .disposed(by: disposeBag)
        
        okButton.rx.tap
            .debounce(RxTimeInterval.microseconds(300), scheduler: MainScheduler.instance)
            .throttle(RxTimeInterval.seconds(1), scheduler: MainScheduler.instance)
            .flatMap { [unowned self] _ in
                return self.delegate.viewModel.process(action: self.action, agree: true)
            }
            .flatMap { [unowned self] result -> Observable<Result<Bool>> in
                return result.onSuccess {
                    return self.delegate.dismiss(dialog: self).asObservable().map { _ in Result(success: true) }
                }
            }
            .subscribe(onNext: { [unowned self] result in
                if (!result.success) {
                    self.delegate.show(message: result.message ?? "unknown error".localized, type: .error, duration: 1.5)
                }
            })
            .disposed(by: disposeBag)
    }
    
    override func render() {
        rounded(radius: 18)
        shadow()
    }
    
    func show(with action: Action, delegate: RoomDelegate) {
        self.delegate = delegate
        self.action = action
        self.show(controller: delegate, style: .center, padding: 27)
    }
}
