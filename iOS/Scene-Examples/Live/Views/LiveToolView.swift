//
//  LiveToolView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
enum LiveToolType {
    case switch_camera
    case camera
    case mic
    
    var imageName: String {
        switch self {
        case .switch_camera: return "icon-rotate"
        case .camera: return "icon-video on"
        case .mic: return "icon-speaker on"
        }
    }
    
    var selectedImageName: String {
        switch self {
        case .switch_camera: return "icon-rotate"
        case .camera: return "icon-video off"
        case .mic: return "icon-speaker off"
        }
    }
    
    var title: String {
        switch self {
        case .switch_camera: return "Switch_Camera".localized
        case .camera: return "Camera".localized
        case .mic: return "Mic".localized
        }
    }
}
class LiveToolView: UIView {
    var clickItemClosure: ((LiveToolType, Bool) -> Void)?
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Tool".localized
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray
        return view
    }()
    private lazy var collectionViewLayout: BaseCollectionViewLayout = {
        let view = BaseCollectionViewLayout()
        view.itemSize = CGSize(width: 80, height: 100)
        view.showsHorizontalScrollIndicator = false
        view.minInteritemSpacing = 20
        view.scrollDirection = .horizontal
        view.delegate = self
        view.register(LiveToolViewCell.self, forCellWithReuseIdentifier: LiveToolViewCell.description())
        return view
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        updateToolType(type: [.switch_camera, .camera, .mic])
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateToolType(type: [LiveToolType]) {
        let datas = type.map({
            LiveToolModel(imageName: $0.imageName,
                          selectedImageName: $0.selectedImageName,
                          title: $0.title,
                          type: $0)
        })
        collectionViewLayout.dataArray = datas
    }
    
    private func setupUI() {
        translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        collectionViewLayout.translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = .white
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(collectionViewLayout)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        lineView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        collectionViewLayout.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionViewLayout.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -15).isActive = true
        collectionViewLayout.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionViewLayout.topAnchor.constraint(equalTo: lineView.bottomAnchor).isActive = true
        collectionViewLayout.heightAnchor.constraint(equalToConstant: 100).isActive = true
    }
}

extension LiveToolView: BaseCollectionViewLayoutDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveToolViewCell.description(), for: indexPath) as! LiveToolViewCell
        cell.setToolData(item: collectionViewLayout.dataArray?[indexPath.item])
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let cell = collectionView.cellForItem(at: indexPath) as? LiveToolViewCell,
              let model = collectionViewLayout.dataArray?[indexPath.item] as? LiveToolModel else { return }
        let isSelected = cell.updateButtonState()
        clickItemClosure?(model.type, isSelected)
    }
}


class LiveToolViewCell: UICollectionViewCell {
    private lazy var iconButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "icon-rotate"), for: .normal)
        button.isUserInteractionEnabled = false
        return button
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Switch_Camera".localized
        label.textColor = .black
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
        iconButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(iconButton)
        contentView.addSubview(titleLabel)
        
        iconButton.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        iconButton.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: iconButton.bottomAnchor, constant: 10).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
    }
    
    func setToolData(item: Any?) {
        guard let model = item as? LiveToolModel else { return }
        iconButton.setImage(UIImage(named: model.imageName), for: .normal)
        iconButton.setImage(UIImage(named: model.selectedImageName), for: .selected)
        titleLabel.text = model.title
    }
    
    @discardableResult
    func updateButtonState() -> Bool {
        iconButton.isSelected = !iconButton.isSelected
        return iconButton.isSelected
    }
}
