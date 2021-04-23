//
//  AlertDialog.swift
//  BlindDate
//
//  Created by XC on 2021/4/23.
//

import Foundation
import UIKit
import Core

class AlertDialog: UIView {
    var cancelAction: (() -> Void)?
    var okAction: (() -> Void)?
    
    private var titleView: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 20, weight: .bold)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Black)
        //view.text = "申请连麦"
        view.textAlignment = .center
        return view
    }()
    
    private var messageView: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 16)
        view.numberOfLines = 0
        view.textColor = UIColor(hex: Colors.Black)
        //view.text = "红娘同意后会自动开始视频连麦"
        view.textAlignment = .center
        return view
    }()
    
    private var cancelButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.Purple
        view.setTitle("Cancel".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Purple), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        return view
    }()
    
    private var okButton: UIButton = {
        let view = RoundButton()
        view.setTitle("Ok".localized, for: .normal)
        view.borderColor = Colors.Purple
        view.setTitleColor(UIColor(hex: Colors.White), for: .normal)
        view.backgroundColor = UIColor(hex: Colors.Purple)
        view.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        return view
    }()
    
    init(title: String, message: String) {
        super.init(frame: CGRect())
        titleView.text = title
        messageView.text = message
        
        addSubview(titleView)
        addSubview(messageView)
        addSubview(cancelButton)
        addSubview(okButton)
        
        titleView.marginTop(anchor: topAnchor, constant: 30)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .centerX(anchor: centerXAnchor)
            .active()
        
        messageView.marginTop(anchor: titleView.bottomAnchor, constant: 27)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .centerX(anchor: centerXAnchor)
            .active()
        
        cancelButton
            .height(constant: 42)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .marginTrailing(anchor: centerXAnchor, constant: 7.5)
            .marginTop(anchor: messageView.bottomAnchor, constant: 27)
            .marginBottom(anchor: bottomAnchor, constant: 30)
            .active()
        
        okButton
            .height(constant: 42)
            .marginLeading(anchor: centerXAnchor, constant: 7.5)
            .marginTrailing(anchor: trailingAnchor, constant: 30)
            .marginTop(anchor: messageView.bottomAnchor, constant: 27)
            .marginBottom(anchor: bottomAnchor, constant: 30)
            .active()
        
        backgroundColor = UIColor(hex: Colors.White)
        okButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(ok(_:))))
        cancelButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(cancel(_:))))
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        rounded(radius: 18)
        shadow()
    }
    
    @objc func cancel(_ sender: UITapGestureRecognizer? = nil) {
        self.cancelAction?()
    }
    
    @objc func ok(_ sender: UITapGestureRecognizer? = nil) {
        self.okAction?()
    }
}
