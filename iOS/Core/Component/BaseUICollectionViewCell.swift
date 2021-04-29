//
//  BaseUICollectionViewCell.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/11.
//

import Foundation
import UIKit

open class BaseUICollectionViewCell<T>: UICollectionViewCell {
    
    open var model: T!
    
    open override func layoutSubviews() {
        super.layoutSubviews()
        render()
    }
    
    open func render() {}
}
