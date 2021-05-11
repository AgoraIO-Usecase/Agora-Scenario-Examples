//
//  RoundImageView.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/10.
//

import Foundation
import UIKit
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

class RoundImageView: UIImageView {
    var color: String? = "#AA4E5E76"
    var borderWidth: CGFloat = 1
    
    override func layoutSubviews() {
        super.layoutSubviews()
        clipsToBounds = true
        rounded(color: color, borderWidth: borderWidth)
    }
}
