//
//  PKLiveProgressView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/17.
//

import UIKit

class PKLiveProgressView: UIView {
    private lazy var progressView: UIView = {
        let view = UIView()
        view.backgroundColor = .red
        return view
    }()
    private lazy var targetProgressView: UIView = {
        let view = UIView()
        view.backgroundColor = .blueColor
        return view
    }()
    private var currentValue: CGFloat = 0
    private var currentTargetValue: CGFloat = 0
    private var progressCons: NSLayoutConstraint?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateProgressValue(at coin: Int) {
        let value = CGFloat(coin)
        currentValue += value
        guard frame.width - 20 > progressView.frame.width else { return }
        let progress = (currentValue - currentTargetValue) / frame.width
        let width = progressView.frame.width * progress
        progressCons?.constant = currentValue == value ? width + 40 : width
        self.progressCons?.isActive = true
        UIView.animate(withDuration: 0.25) {
            self.layoutIfNeeded()
        }
    }
    
    func updateTargetProgressValue(at coin: Int) {
        let value = CGFloat(coin)
        currentTargetValue += value
        guard frame.width - 20 > targetProgressView.frame.width else { return }
        let progress = (currentTargetValue - currentValue) / frame.width
        let width = targetProgressView.frame.width * progress
        progressCons?.constant = currentTargetValue == value ? width - 40 : -width
        self.progressCons?.isActive = true
        UIView.animate(withDuration: 0.25) {
            self.layoutIfNeeded()
        }
    }
    
    func reset() {
        currentValue = 0
        currentTargetValue = 0
        progressView.removeConstraints(progressView.constraints)
        targetProgressView.removeConstraints(targetProgressView.constraints)
        addConstraint()
    }
    
    private func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 5
        layer.masksToBounds = true
        progressView.translatesAutoresizingMaskIntoConstraints = false
        targetProgressView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(progressView)
        addSubview(targetProgressView)
        
        addConstraint()
    }
    
    private func addConstraint() {
        progressView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        progressView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        progressView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        progressCons = progressView.widthAnchor.constraint(equalTo: widthAnchor, multiplier: 0.5)
        progressCons?.isActive = true
        
        targetProgressView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        targetProgressView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        targetProgressView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        targetProgressView.leadingAnchor.constraint(equalTo: progressView.trailingAnchor).isActive = true
    }
}
