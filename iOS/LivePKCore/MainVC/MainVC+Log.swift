//
//  MainVC+Log.swift
//  LivePKCore
//
//  Created by ZYP on 2021/9/27.
//

import Foundation
import Toast_Swift

extension MainVC {
    func show(_ text: String) {
        if Thread.current.isMainThread {
            showNoQueue(text: text)
            return
        }
        DispatchQueue.main.sync { [unowned self] in
            self.showNoQueue(text: text)
        }
    }
    
    private func showNoQueue(text: String) {
        self.view.makeToast(text, position: .center)
    }
}
