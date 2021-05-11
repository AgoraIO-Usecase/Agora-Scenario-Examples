//
//  RoundLableView.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/11.
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
