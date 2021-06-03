//
//  CreateRoomDialog.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/5.
//

import Foundation
import UIKit
import RxSwift
import Core

protocol CreateRoomDelegate: BaseViewContoller {
    func createRoom(with: String?) -> Observable<Result<BlindDateRoom>>
    func onCreateSuccess(with: BlindDateRoom)
    func onDismiss()
}

class CreateRoomDialog: UIView {
    
    private let disposeBag = DisposeBag()
    @IBOutlet weak var inputRoomView: UITextField!
    @IBOutlet weak var createButton: UIButton!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var indicatorView: UIActivityIndicatorView!
    @IBOutlet weak var refreshButton: UIButton!
    weak var createRoomDelegate: CreateRoomDelegate!
    private var controller: UIViewController?
    
    private var showing = false
    private var processing = false {
        didSet {
            DispatchQueue.main.async { [unowned self] in
                self.indicatorView.isHidden = !processing
                self.createButton.isEnabled = !processing
                self.inputRoomView.isEnabled = !processing
            }
        }
    }
    
    private func onCreateRoom() -> Observable<Result<BlindDateRoom>> {
        return createButton.rx.tap
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .filter { [unowned self] in
                return !self.processing
            }
            .map { [unowned self] in
                self.processing = true
                return self.inputRoomView.text
            }
            .flatMap { [unowned self] name in
                return self.createRoomDelegate.createRoom(with: name)
            }
            .map { [unowned self] result in
                self.processing = false
                return result
            }
    }
    
    func show() -> Single<Bool> {
        if (showing) {
            return Single.just(true)
        } else {
            showing = true
            processing = false
            createButton.setTitle("", for: .disabled)
            
            inputRoomView.attributedPlaceholder = NSAttributedString(
                string: "Room Name".localized,
                attributes: [NSAttributedString.Key.foregroundColor: UIColor(hex: Colors.Gray)]
            )
            //inputRoomView.superview!.rounded(color: "#373337", borderWidth: 1, radius: 5)
            refreshButton.rx.tap
                .subscribe(onNext: { _ in
                    Logger.log(message: "randomRoomName", level: .info)
                    self.inputRoomView.text = Utils.randomRoomName()
                })
                .disposed(by: disposeBag)
            
            cancelButton.rx.tap
                .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
                .flatMap { [unowned self] _ in
                    return self.dismiss()
                }
                .subscribe()
                .disposed(by: disposeBag)
            
            onCreateRoom()
                .subscribe(onNext: { [unowned self] result in
                    guard let room = result.data else {
                        self.createRoomDelegate.showToast(message: result.message, type: .error)
                        return
                    }
                    self.createRoomDelegate.onCreateSuccess(with: room)
                })
                .disposed(by: disposeBag)
            
            //self.controller = UIViewController()
            if let controller = self.controller, let root = controller.view {
                return Single.create { single in
                    controller.modalPresentationStyle = .overCurrentContext
                    self.alpha = 0
                    root.addSubview(self)
                    self.marginLeading(anchor: root.leadingAnchor, constant: 16, relation: .greaterOrEqual)
                        .centerX(anchor: root.centerXAnchor)
                        .centerY(anchor: root.centerYAnchor, constant: -50)
                        .active()
                    self.createRoomDelegate.present(controller, animated: false) {
                        self.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
                        self.superview?.backgroundColor = UIColor.black.withAlphaComponent(0)
                        UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                            self.alpha = 1
                            self.transform = CGAffineTransform(scaleX: 1, y: 1)
                            self.superview?.backgroundColor = UIColor.black.withAlphaComponent(0.5)
                        }, completion: { finish in
                            single(.success(finish))
                        })
                    }
                    return Disposables.create()
                }.subscribe(on: MainScheduler.instance)
            } else {
                return self.createRoomDelegate.show(dialog: self, padding: 27, relation: .greaterOrEqual).map { finished in
                    self.inputRoomView.becomeFirstResponder()
                    return finished
                }
            }
        }
    }
    
    func dismiss() -> Single<Bool> {
        if let controller = self.controller {
            return Single.create { single in
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    self.alpha = 0
                    self.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
                    self.superview?.backgroundColor = UIColor.black.withAlphaComponent(0)
                }, completion: { finish in
                    controller.dismiss(animated: false) {
                        self.controller = nil
                        self.showing = false
                        self.createRoomDelegate.onDismiss()
                        self.createRoomDelegate = nil
                        single(.success(true))
                    }
                })
                return Disposables.create()
            }.subscribe(on: MainScheduler.instance)
        } else {
            return createRoomDelegate.dismiss(dialog: self)
                .map { [unowned self] finished in
                    self.showing = false
                    return finished
                }
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        inputRoomView.endEditing(true)
    }
    
    deinit {
        Logger.log(message: "createRoomDialog deinit", level: .info)
    }
    
    static func Create() -> CreateRoomDialog {
        let dialog: CreateRoomDialog =  UIView.loadFromNib(name: "CreateRoomDialog", bundle: Utils.bundle)!
        return dialog
    }
}
