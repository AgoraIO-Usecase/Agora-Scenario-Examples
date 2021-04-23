//
//  Utils.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import Core
import RxSwift
import RxCocoa

extension Utils {
    static let bundle = Bundle.init(identifier: "io.agora.BlindDate")!
    
    static let namesData: [String: [String]] = [
        "cn": [
            "最长的电影",
            "Good voice",
            "Bad day",
            "好故事不容错过",
            "Greatest talk show"
        ],
        "default": [
            "The longest movie",
            "Good voice",
            "Bad day",
            "Good story not to be missed",
            "Greatest talk show"
        ]
    ]
    
    static func randomRoomName() -> String {
        let language = getCurrentLanguage()
        let names = namesData[language] ?? namesData["default"]!
        let index = Int(arc4random_uniform(UInt32(names.count)))
        return names[index]
    }
}

extension String {
    public var localized: String { NSLocalizedString(self, bundle: Utils.bundle, comment: "") }
}

extension BaseViewContoller {
    func showAlert(title: String, message: String) -> Observable<Bool> {
        return Single.create { single in
            let alert = AlertDialog(title: title, message: message)
            alert.cancelAction = {
                self.dismiss(dialog: alert).subscribe().disposed(by: self.disposeBag)
            }
            alert.okAction = {
                single(.success(true))
                self.dismiss(dialog: alert).subscribe().disposed(by: self.disposeBag)
            }
            self.show(dialog: alert, style: .center, padding: 27) {
                single(.success(false))
            }.subscribe().disposed(by: self.disposeBag)
            return Disposables.create {
                Logger.log(message: "showAlert disposed", level: .info)
            }
        }.asObservable()
    }
}
