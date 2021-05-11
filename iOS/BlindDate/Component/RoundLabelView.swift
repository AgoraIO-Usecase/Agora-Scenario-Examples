//
//  RoundLabelView.swift
//  BlindDate
//
//  Created by XC on 2021/4/22.
//

import Foundation
import UIKit
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

class RoundLabelView: UILabel {
    
    override func layoutSubviews() {
        super.layoutSubviews()
        widthAnchor.constraint(greaterThanOrEqualToConstant: bounds.height).isActive = text?.isEmpty == false
        clipsToBounds = true
        rounded()
    }
}
