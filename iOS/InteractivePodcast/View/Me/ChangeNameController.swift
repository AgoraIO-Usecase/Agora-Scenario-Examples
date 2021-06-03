//
//  ChangeNameController.swift
//  InteractivePodcast
//
//  Created by XC on 2021/4/8.
//

import Foundation
import UIKit
import RxCocoa
import RxSwift
import Core

class ChangeNameController: BaseViewContoller {
    @IBOutlet weak var inputNameView: UITextField!
    @IBOutlet weak var okButton: UIButton!
    @IBOutlet weak var backButton: UIView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        inputNameView.becomeFirstResponder()
        
        let tapBack = UITapGestureRecognizer()
        backButton.addGestureRecognizer(tapBack)
        tapBack.rx.event
            .subscribe(onNext: { _ in
                self.navigationController?.popViewController(animated: true)
            })
            .disposed(by: disposeBag)
        
        let tapOk = UITapGestureRecognizer()
        okButton.addGestureRecognizer(tapOk)
        tapOk.rx.event
            .flatMap { _ -> Observable<Result<Void>> in
                if let name = self.inputNameView.text {
                    if (!name.isEmpty) {
                        self.show(processing: true)
                        return RoomManager.shared().account!.update(name: name.trimmingCharacters(in: [" "]))
                    } else {
                        return Observable.just(Result(success: false, message: "please input profile name".localized))
                    }
                }
                return Observable.just(Result(success: false, message: "please input profile name".localized))
            }
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { result in
                self.show(processing: false)
                if (result.success) {
                    self.navigationController?.popViewController(animated: true)
                } else {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            })
            .disposed(by: disposeBag)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        inputNameView.endEditing(true)
    }
    
    static func instance() -> ChangeNameController {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        let controller = storyBoard.instantiateViewController(withIdentifier: "ChangeNameController") as! ChangeNameController
        return controller
    }
}
