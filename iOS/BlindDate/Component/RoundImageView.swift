//
//  RoundImageView.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import UIKit
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

class RoundImageView: UIImageView {
    var color: String? = "#FFFFFF"
    var borderWidth: CGFloat = 1
    var radius: CGFloat? = nil
    
    override func layoutSubviews() {
        super.layoutSubviews()
        clipsToBounds = true
        rounded(color: !isHidden ? color : nil, borderWidth: borderWidth, radius: radius)
    }
}

