//
//  HomeCardView.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/20.
//

import Foundation
import UIKit
import Core

protocol HomeCard {
    var title: String { get }
    var color: UIColor { get }
    func create() -> UIViewController
}

class HomeCardView: UITableViewCell {
    var item: HomeCard! {
        didSet {
            name.text = item.title
            contentView.backgroundColor = item.color
        }
    }
    
    var name: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 15)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.White)
        return view
    }()
    
    //var background: CALayer? = nil
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        //background = contentView.gradientLayer(colors: [UIColor(hex: "#641BDF"), UIColor(hex: "#D07AF5")])
        //layer.insertSublayer(background!, at: 0)
        render()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        //background?.frame = bounds
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func render() {
        selectionStyle = .none
        backgroundColor = .clear
        contentView.addSubview(name)
        
        name.centerX(anchor: contentView.centerXAnchor)
            .centerY(anchor: contentView.centerYAnchor)
            .active()
    }
}
