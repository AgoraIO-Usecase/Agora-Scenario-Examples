//
//  MessageView.swift
//  BlindDate
//
//  Created by XC on 2021/4/25.
//

import Foundation
import UIKit

class MessageView: UITableViewCell {
    var message: RoomChat! {
        didSet {
            let name = NSAttributedString(
                string: message.member.user.name,
                attributes: [
                    NSAttributedString.Key.foregroundColor: UIColor(hex: "#F4C11F"),
                    NSAttributedString.Key.font: UIFont.systemFont(ofSize: 13, weight: .bold)
                ])
            let description = NSAttributedString(
                string: " \(message.message)",
                attributes: [NSAttributedString.Key.foregroundColor: UIColor(hex: Colors.White)])
            let message = NSMutableAttributedString(attributedString: name)
            message.append(description)
            label.attributedText = message
        }
    }
    
    var bg: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.black.withAlphaComponent(0.2)
        view.rounded(color: nil, borderWidth: 0, radius: 12)
        return view
    }()
    
    var label: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 13)
        view.backgroundColor = .clear
        view.numberOfLines = 0
        return view
    }()
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        render()
    }
    
    private func render() {
        selectionStyle = .none
        backgroundColor = .clear
        
        contentView.addSubview(bg)
        bg.marginLeading(anchor: contentView.leadingAnchor, constant: 12)
            .width(constant: 235, relation: .lessOrEqual)
            .marginTop(anchor: contentView.topAnchor, constant: 4.5)
            .marginBottom(anchor: contentView.bottomAnchor, constant: 4.5)
            .active()
        
        bg.addSubview(label)
        label.marginTop(anchor: bg.topAnchor, constant: 3)
            .marginTrailing(anchor: bg.trailingAnchor, constant: 6)
            .marginBottom(anchor: bg.bottomAnchor, constant: 3)
            .marginLeading(anchor: bg.leadingAnchor, constant: 6)
            .active()
    }
}
