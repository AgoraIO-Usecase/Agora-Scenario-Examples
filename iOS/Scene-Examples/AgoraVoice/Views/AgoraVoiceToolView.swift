//
//  AgoraVoiceToolView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/27.
//

import UIKit
import AgoraUIKit_iOS

class AgoraVoiceToolView: UIView {
    var clickItemClosure: ((LiveToolType, Bool) -> Void)?
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white)
        label.text = "工具".localized
        return label
    }()
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
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
        updateToolType(type: [.mic, .earphone_monitor, .music, .backgroundImage, .real_time_data])
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateToolType(type: [LiveToolType]) {
        var datas = [LiveToolModel]()
        type.forEach({
            let model = LiveToolModel()
            model.imageName = $0.imageName
            model.selectedImageName = $0.selectedImageName
            model.title = $0.title
            model.type = $0
            datas.append(model)
        })
        collectionView.dataArray = datas
    }
    
    private func setupUI() {
        backgroundColor = .init(hex: "#4F506A")
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        addSubview(titleLabel)
        addSubview(collectionView)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor).isActive = true
        collectionView.heightAnchor.constraint(equalToConstant: 140).isActive = true
    }
}
extension AgoraVoiceToolView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveToolViewCell.description(), for: indexPath) as! LiveToolViewCell
        cell.setToolData(item: self.collectionView.dataArray?[indexPath.item])
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let cell = collectionView.cellForItem(at: indexPath) as? LiveToolViewCell,
              let model = self.collectionView.dataArray?[indexPath.item] as? LiveToolModel else { return }
        let isSelected = cell.updateButtonState()
        clickItemClosure?(model.type, isSelected)
    }
}
