//
//  ManageSpeakerDialog.swift
//  BlindDate
//
//  Created by XC on 2021/4/22.
//

import Foundation
import UIKit
import RxSwift
import RxCocoa
import Core

class ManageSpeakerDialog: Dialog {
    weak var delegate: RoomController!
    var model: Member! {
        didSet {
            name.text = model.user.name
            avatar.image = UIImage(named: model.user.getLocalAvatar(), in: Bundle(identifier: "io.agora.InteractivePodcast")!, with: nil)
            if (model.isMuted) {
                closeMicButton.setTitle(model.isMuted ? "开麦".localized : "禁麦".localized, for: .normal)
            }
        }
    }
    
    var backView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: Colors.White)
        return view
    }()
    
    var avatar: UIImageView = {
        let view = RoundImageView()
        view.borderWidth = 2
        return view
    }()
    
    var name: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 20, weight: .bold)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Black)
        return view
    }()
    
    var vline: UIView = {
        let view = UIView()
         view.backgroundColor = UIColor(hex: Colors.LightGray)
         return view
    }()
    
    var hline: UIView = {
        let view = UIView()
         view.backgroundColor = UIColor(hex: Colors.LightGray)
         return view
    }()
    
    var kickButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.White
        view.setTitle("下台".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Black), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        return view
    }()
    
    var closeMicButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.White
        view.setTitle("禁麦".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Black), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        return view
    }()
    
    override func setup() {
        backgroundColor = .clear
        
        addSubview(backView)
        addSubview(avatar)
        addSubview(name)
        addSubview(hline)
        addSubview(vline)
        addSubview(closeMicButton)
        addSubview(kickButton)
        
        backView.fill(view: self, leading: 0, top: 40, trailing: 0, bottom: 0)
            .active()
        
        avatar.width(constant: 80)
            .height(constant: 80)
            .marginTop(anchor: topAnchor, constant: 0)
            .centerX(anchor: centerXAnchor)
            .active()
        
        name.marginTop(anchor: avatar.bottomAnchor, constant: 17)
            .marginLeading(anchor: leadingAnchor, constant: 20, relation: .greaterOrEqual)
            .centerX(anchor: centerXAnchor)
            .active()
        
        hline.marginTop(anchor: name.bottomAnchor, constant: 30)
            .height(constant: 1)
            .marginLeading(anchor: leadingAnchor, constant: 22)
            .centerX(anchor: centerXAnchor)
            .active()
        
        vline.height(constant: 27)
            .width(constant: 1)
            .centerX(anchor: centerXAnchor)
            .marginTop(anchor: hline.bottomAnchor, constant: 17)
            .marginBottom(anchor: safeAreaLayoutGuide.bottomAnchor, constant: 17)
            .active()
        
        closeMicButton.height(constant: 36)
            .marginLeading(anchor: leadingAnchor)
            .marginTrailing(anchor: vline.leadingAnchor)
            .centerY(anchor: vline.centerYAnchor)
            .active()
        
        kickButton.height(constant: 36)
            .marginLeading(anchor: vline.trailingAnchor)
            .marginTrailing(anchor: trailingAnchor)
            .centerY(anchor: vline.centerYAnchor)
            .active()
        
        kickButton.rx.tap
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .flatMap { [unowned self] _ in
                return self.delegate.viewModel.kickSpeaker(member: self.model)
            }
            .flatMap { [unowned self] result -> Observable<Result<Bool>> in
                return result.onSuccess {
                    return self.delegate.dismiss(dialog: self).asObservable().map { _ in Result(success: true) }
                }
            }
            .subscribe(onNext: { [unowned self] result in
                if (!result.success) {
                    self.delegate.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            })
            .disposed(by: disposeBag)
        
        closeMicButton.rx.tap
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .flatMap { [unowned self] _ -> Observable<Result<Void>> in
                if (self.model.isMuted) {
                    return self.delegate.viewModel.unMuteSpeaker(member: self.model)
                } else {
                    return self.delegate.viewModel.muteSpeaker(member: self.model)
                }
            }
            .flatMap { [unowned self] result -> Observable<Result<Void>> in
                return result.onSuccess {
                    return self.delegate.dismiss(dialog: self).asObservable().map { _ in result }
                }
            }
            .subscribe(onNext: { [unowned self] result in
                if (!result.success) {
                    self.delegate.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            })
            .disposed(by: disposeBag)
    }
    
    override func render() {
        backView.roundCorners([.topLeft, .topRight], radius: 18)
        shadow()
        clipsToBounds = false
    }
    
    func show(with member: Member, delegate: RoomController) {
        self.delegate = delegate
        self.model = member
        self.show(controller: delegate, padding: 6)
    }
}
