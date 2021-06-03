//
//  MemberEnterView.swift
//  BlindDate
//
//  Created by XC on 2021/4/25.
//

import Foundation
import UIKit
import Core

class MemberEnterView: UIView {
    
    var member: BlindDateMember! {
        didSet {
            let name = NSAttributedString(
                string: member.user.name,
                attributes: [
                    NSAttributedString.Key.foregroundColor: UIColor(hex: "#9B44FD"),
                    NSAttributedString.Key.font: UIFont.systemFont(ofSize: 13, weight: .bold)
                ])
            let description = NSAttributedString(
                string: " enter the live room",
                attributes: [NSAttributedString.Key.foregroundColor: UIColor(hex: Colors.White)])
            let message = NSMutableAttributedString(attributedString: name)
            message.append(description)
            label.attributedText = message
        }
    }
    
    var bg: CALayer!
    
    var label: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 13)
        view.backgroundColor = .clear
        view.numberOfLines = 1
        return view
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(label)
        label.marginTop(anchor: topAnchor, constant: 3)
            .centerY(anchor: centerYAnchor)
            .marginLeading(anchor: leadingAnchor, constant: 12)
            .marginTrailing(anchor: trailingAnchor, constant: 6)
            .active()
        
        bg = gradientLayer(colors: [UIColor(hex: "#ACB0FF"), UIColor(hex: "#C8CBFF"), UIColor(hex: "#C8CBFF").withAlphaComponent(0)])
        bg.frame = frame
        layer.insertSublayer(bg, at: 0)
        backgroundColor = .clear
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        roundCorners([.topLeft, .bottomLeft], radius: 12)
        bg.frame = bounds
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
