//
//  NoticeCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/20.
//

import UIKit
import AgoraUIKit_iOS

class NoticeCell: UITableViewCell {
    private lazy var containerView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: "#49129A")
        view.layer.cornerRadius = 12
        view.layer.masksToBounds = true
        return view
    }()
    
    private lazy var noticeLabel: AGELabel = {
        let label = AGELabel()
        label.numberOfLines = 0
        label.colorStyle = .white
        let attrs = NSMutableAttributedString()
        let image = UIImage(systemName: "scroll")?.withTintColor(.white, renderingMode: .alwaysOriginal) ?? UIImage()
        let attach = NSTextAttachment(image: image)
        attach.bounds = CGRect(x: 0, y: -7, width: 20, height: 20)
        attrs.append(NSAttributedString(attachment: attach))
        
        let attr = NSAttributedString(string: "公告: ", attributes: [.foregroundColor: UIColor.white,
                                                                    .font: UIFont.systemFont(ofSize: 14, weight: .bold)])
        attrs.append(attr)
        let contentAttr = NSAttributedString(string: "在公屏上打出“主播yyds”有机会和主播一起玩游戏哦！！！",
                                             attributes: [.foregroundColor: UIColor.white])
        attrs.append(contentAttr)
        label.attributedText = attrs
        return label
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .clear
        contentView.backgroundColor = .clear
        containerView.translatesAutoresizingMaskIntoConstraints = false
        noticeLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(containerView)
        containerView.addSubview(noticeLabel)
        
        containerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        containerView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        containerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -15).isActive = true
        containerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        
        noticeLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 19).isActive = true
        noticeLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 15).isActive = true
        noticeLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -15).isActive = true
        noticeLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -19).isActive = true
    }
}
