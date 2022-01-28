//
//  AgoraVoicePitchCorrectionHeaderView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/27.
//

import UIKit
import AgoraUIKit_iOS

class AgoraVoicePitchCorrectionHeaderView: UICollectionReusableView {
    var clickSwitchClosure: ((Bool) -> Void)?
    var clickSegmentViewClosure: ((Int) -> Void)?
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .disabled)
        label.text = "启动电音".localized
        return label
    }()
    private lazy var swicthUI: UISwitch = {
        let switchUI = UISwitch()
        switchUI.tintColor = .init(hex: "#62626F")
        switchUI.onTintColor = .blueColor
        switchUI.thumbTintColor = .blueColor
        switchUI.addTarget(self, action: #selector(clickSwitchHandler(sender:)), for: .valueChanged)
        return switchUI
    }()
    private lazy var segmentView: UISegmentedControl = {
        let segmentView = UISegmentedControl(items: ["大调".localized, "小调".localized, "和风".localized])
        segmentView.selectedSegmentTintColor = .blueColor
        segmentView.backgroundColor = .init(hex: "#62626F")
        segmentView.tintColor = .blueColor
        segmentView.selectedSegmentIndex = 0
        segmentView.addTarget(self, action: #selector(clickSegmentViewHandler(sender:)), for: .valueChanged)
        return segmentView
    }()
    private lazy var selectedLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .large)
        label.text = "选择起始音阶".localized
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .init(hex: "#4F506A")
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        swicthUI.translatesAutoresizingMaskIntoConstraints = false
        segmentView.translatesAutoresizingMaskIntoConstraints = false
        selectedLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        addSubview(swicthUI)
        addSubview(segmentView)
        addSubview(selectedLabel)
        
        titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 7).isActive = true
        
        swicthUI.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor, constant: 10).isActive = true
        swicthUI.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor).isActive = true
        
        segmentView.leadingAnchor.constraint(equalTo: swicthUI.centerXAnchor).isActive = true
        segmentView.topAnchor.constraint(equalTo: swicthUI.bottomAnchor, constant: 15).isActive = true
        segmentView.heightAnchor.constraint(equalToConstant: 30).isActive = true
        
        selectedLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        selectedLabel.topAnchor.constraint(equalTo: segmentView.bottomAnchor, constant: 15).isActive = true
    }
    
    @objc
    private func clickSwitchHandler(sender: UISwitch) {
        clickSwitchClosure?(sender.isOn)
    }
    
    @objc
    private func clickSegmentViewHandler(sender: UISegmentedControl) {
        clickSegmentViewClosure?(sender.selectedSegmentIndex)
    }
}
