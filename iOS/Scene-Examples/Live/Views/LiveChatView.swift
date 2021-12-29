//
//  LiveChatView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraUIKit_iOS

enum ChatMessageType {
    case message
    case notice
}

struct ChatMessageModel {
    var message: String = ""
    var messageType: ChatMessageType = .message
}

class LiveChatView: UIView {
    var didSelectRowAt: ((ChatMessageModel) -> Void)?
    
    private lazy var tableLayoutView: AGETableView = {
        let view = AGETableView()
        view.estimatedRowHeight = 44
        view.delegate = self
        view.showsVerticalScrollIndicator = false
        view.emptyImage = UIImage()
        view.emptyTitle = ""
        view.register(MessageCell.self,
                      forCellWithReuseIdentifier: MessageCell.description())
        view.register(NoticeCell.self, forCellWithReuseIdentifier: NoticeCell.description())
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
    
    func sendMessage(messageModel: ChatMessageModel) {
        tableLayoutView.insertBottomRow(item: messageModel)
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

extension LiveChatView: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let message = tableLayoutView.dataArray?[indexPath.row] as? ChatMessageModel else { return UITableViewCell() }
        switch message.messageType {
        case .message:
            let cell = tableView.dequeueReusableCell(withIdentifier: MessageCell.description(), for: indexPath) as! MessageCell
            cell.setChatMessage(message: message)
            return cell
            
        case .notice:
            let cell = tableView.dequeueReusableCell(withIdentifier: NoticeCell.description(), for: indexPath) as! NoticeCell
            return cell
        }
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        guard let message = tableLayoutView.dataArray?[indexPath.row] as? ChatMessageModel else { return }
        didSelectRowAt?(message)
    }
}

class LiveChatMessageView: UIView {
    var clickKeyboardSendClosure: ((String) -> Void)?
    
    private lazy var textField: UITextField = {
        let textField = UITextField()
        textField.font = .systemFont(ofSize: 14.fit)
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
