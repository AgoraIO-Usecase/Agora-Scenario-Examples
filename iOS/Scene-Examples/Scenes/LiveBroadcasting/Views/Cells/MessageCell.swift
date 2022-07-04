//
//  MessageCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/20.
//

import UIKit

class MessageCell: UITableViewCell {
    private lazy var containerView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: "#000000", alpha: 0.6)
        view.layer.masksToBounds = true
        return view
    }()
    private lazy var messageLabel: UILabel = {
        let label = UILabel()
        label.text = "ukonw 加入房间"
        label.textColor = .white
        label.font = .systemFont(ofSize: 15)
        label.numberOfLines = 0
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
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(containerView)
        containerView.addSubview(messageLabel)
        
        containerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        containerView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        containerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -15).isActive = true
        
        messageLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 10).isActive = true
        messageLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 10).isActive = true
        messageLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -10).isActive = true
        messageLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -10).isActive = true
        
        containerView.layer.cornerRadius = (contentView.frame.height - 15) / 2
    }
    
    func setChatMessage(message: ChatMessageModel?) {
        guard let message = message?.message else {
            return
        }
        messageLabel.text = message
    }
    override func layoutSubviews() {
        super.layoutSubviews()
        messageLabel.preferredMaxLayoutWidth = contentView.frame.width - 46
    }
}
