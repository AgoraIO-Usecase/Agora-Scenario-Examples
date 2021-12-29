//
//  LiveGiftView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/12.
//

import UIKit
import AgoraUIKit_iOS

class LiveGiftView: UIView {
    var clickGiftItemClosure: ((LiveGiftModel) -> Void)?
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Gift".localized
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray
        return view
    }()
    private lazy var collectionViewLayout: AGECollectionView = {
        let view = AGECollectionView()
        view.estimatedItemSize = CGSize(width: 60, height: 100)
        view.showsHorizontalScrollIndicator = false
        view.minInteritemSpacing = 20
        view.minLineSpacing = 10
        view.scrollDirection = .vertical
        view.delegate = self
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        view.register(LiveGidtViewCell.self, forCellWithReuseIdentifier: LiveGidtViewCell.description())
        view.dataArray = LiveGiftModel.createGiftData()
        return view
    }()
    private lazy var presentButton: UIButton = {
        let button = UIButton()
        button.setTitle("Present".localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16)
        button.backgroundColor = .blueColor
        button.layer.cornerRadius = 20
        button.layer.masksToBounds = true
        button.addTarget(self, action: #selector(clickPresentButton), for: .touchUpInside)
        return button
    }()
    private var currentModel: LiveGiftModel?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        collectionViewLayout.translatesAutoresizingMaskIntoConstraints = false
        presentButton.translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = .white
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(collectionViewLayout)
        addSubview(presentButton)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        lineView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        collectionViewLayout.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionViewLayout.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionViewLayout.topAnchor.constraint(equalTo: lineView.bottomAnchor,constant: 10).isActive = true
        collectionViewLayout.heightAnchor.constraint(equalToConstant: 235).isActive = true
        
        presentButton.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        presentButton.topAnchor.constraint(equalTo: collectionViewLayout.bottomAnchor, constant: 25).isActive = true
        presentButton.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -20).isActive = true
        presentButton.heightAnchor.constraint(equalToConstant: 40).isActive = true
        presentButton.widthAnchor.constraint(equalToConstant: 80).isActive = true
    }
    
    @objc
    private func clickPresentButton() {
        guard let model = currentModel else { return }
        AlertManager.hiddenView {
            self.clickGiftItemClosure?(model)
        }
    }
}
extension LiveGiftView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveGidtViewCell.description(), for: indexPath) as! LiveGidtViewCell
        let model = LiveGiftModel.createGiftData()[indexPath.item]
        cell.setliveGigtData(item: model)
        if indexPath.item == 0 {
            cell.isSelected = true
            currentModel = model
        }
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        collectionView.subviews.forEach({ ($0 as? UICollectionViewCell)?.isSelected = false })
        currentModel = LiveGiftModel.createGiftData()[indexPath.item]
    }
}

class LiveGidtViewCell: UICollectionViewCell {
    private lazy var imageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "gift-dang"))
        imageView.setContentHuggingPriority(.defaultHigh, for: .vertical)
        imageView.setContentCompressionResistancePriority(.defaultHigh, for: .vertical)
        imageView.layer.masksToBounds = true
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Small_Bell".localized
        label.textColor = .black
        label.font = UIFont.systemFont(ofSize: 14, weight: .medium)
        return label
    }()
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.text = "(20" + "Coin".localized + ")"
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
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
        contentView.layer.cornerRadius = 8
        contentView.layer.masksToBounds = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descLabel.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(imageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(descLabel)
        
        imageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        imageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 1).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: imageView.bottomAnchor, constant: 8).isActive = true
        
        descLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        descLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8).isActive = true
        descLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
    }
    
    func setliveGigtData(item: Any?) {
        guard let model = item as? LiveGiftModel else { return }
        imageView.image = UIImage(named: model.iconName ?? "")
        titleLabel.text = model.title
        descLabel.text = "(\(model.coin)" + "Coin".localized + ")"
    }
    
    override var isSelected: Bool {
        didSet {
            contentView.layer.borderColor = UIColor.blueColor.cgColor
            contentView.layer.borderWidth = isSelected ? 1 : 0
        }
    }
}
