//
//  RTCRealTimeDataView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/27.
//

import UIKit
import AgoraUIKit_iOS
import AgoraRtcKit

class RTCRealTimeDataView: UIView {
    private lazy var detailLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .small)
        label.numberOfLines = 0
        return label
    }()
    private lazy var statistics = RTEStatistics()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupData(channelStatus: AgoraChannelStats? = nil, localAudioStatus: AgoraRtcLocalAudioStats? = nil) {
        statistics.sceneStats = channelStatus
        statistics.localAudioStats = localAudioStatus
        detailLabel.text = statistics.description
    }
    
    private func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 15
        layer.masksToBounds = true
        widthAnchor.constraint(equalToConstant: Screen.width - 30).isActive = true
        
        detailLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(detailLabel)
        
        detailLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        detailLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        detailLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -15).isActive = true
        detailLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
    }
}
