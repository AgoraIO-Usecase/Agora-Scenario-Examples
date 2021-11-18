//
//  LiveChatView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit

class LiveChatView: UIView {
    private lazy var tableLayoutView: BaseTableViewLayout = {
        let view = BaseTableViewLayout()
        view.estimatedRowHeight = 44
        view.delegate = self
        view.showsVerticalScrollIndicator = false
        view.emptyImage = UIImage()
        view.emptyTitle = ""
        view.register(LiveChatViewCell.self,
                      forCellWithReuseIdentifier: LiveChatViewCell.description())
        view.dataArray = []
        return view
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func sendMessage(message: String) {
        tableLayoutView.insertBottomRow(item: message)
    }
    
    private func setupUI() {
        tableLayoutView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(tableLayoutView)
        
        tableLayoutView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableLayoutView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        tableLayoutView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableLayoutView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        tableLayoutView.contentInset = UIEdgeInsets(top: frame.height - 30, left: 0, bottom: 0, right: 0)
    }
}

extension LiveChatView: BaseTableViewLayoutDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: LiveChatViewCell.description(), for: indexPath) as! LiveChatViewCell
        let message = tableLayoutView.dataArray?[indexPath.row]
        cell.setChatMessage(message: message)
        return cell
    }
}


class LiveChatViewCell: UITableViewCell {
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
    
    func setChatMessage(message: Any?) {
        guard let message = message as? String else {
            return
        }
        messageLabel.text = message
    }
    override func layoutSubviews() {
        super.layoutSubviews()
        messageLabel.preferredMaxLayoutWidth = contentView.frame.width - 46
    }
}


class LiveChatMessageView: UIView {
    var clickKeyboardSendClosure: ((String) -> Void)?
    
    private lazy var textField: UITextField = {
        let textField = UITextField()
        textField.font = .systemFont(ofSize: 14)
        textField.textColor = .black
        textField.placeholder = "Live_Text_Input_Placeholder".localized
        textField.returnKeyType = .send
        textField.delegate = self
        return textField
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        textField.becomeFirstResponder()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .white
        translatesAutoresizingMaskIntoConstraints = false
        textField.translatesAutoresizingMaskIntoConstraints = false
        addSubview(textField)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        textField.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        textField.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        textField.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        textField.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10).isActive = true
        textField.heightAnchor.constraint(equalToConstant: 44).isActive = true
    }
}

extension LiveChatMessageView: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        AlertManager.hiddenView {
            textField.resignFirstResponder()            
        }
        clickKeyboardSendClosure?(textField.text ?? "")
        return true
    }
}
