//
//  RoundImageButton.swift
//  InteractivePodcast
//
//  Created by XC on 2021/3/15.
//

import Foundation
import UIKit

class RoundImageButton: UIButton {
    var color: String? = "#AA4E5E76"
    var borderWidth: CGFloat = 1
    
    override func layoutSubviews() {
        super.layoutSubviews()
        clipsToBounds = true
        rounded(color: color, borderWidth: borderWidth)
    }
}
