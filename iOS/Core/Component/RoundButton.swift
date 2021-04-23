//
//  RoundButton.swift
//  InteractivePodcast
//
//  Created by XC on 2021/3/12.
//

import Foundation
import UIKit

open class RoundButton: UIButton {
    
    open var borderColor: String?
    
    open override func layoutSubviews() {
        super.layoutSubviews()
        rounded(color: borderColor, borderWidth: 1)
    }
    
    open override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        self.alpha = 0.65
    }
    
    open override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        self.alpha = 1
    }
    
    open override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        self.alpha = 1
    }
}
