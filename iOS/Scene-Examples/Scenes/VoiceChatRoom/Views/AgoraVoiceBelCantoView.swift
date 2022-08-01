//
//  AgoraVoiceBelCantoView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/26.
//

import UIKit
import Agora_Scene_Utils

enum AudioEffectType {
    case belCanto, soundEffect
        
    var title: String {
        switch self {
        case .belCanto:    return "bel_canto".localized
        case .soundEffect: return "sound_effect".localized
        }
    }
    
    var segmentTitles: [String] {
        switch self {
        case .belCanto: return AgoraVoiceBelCantoType.allCases.map({ $0.title })
        case .soundEffect: return SoundEffectType.allCases.map({ $0.title })
        }
    }
    
    func edges(belCantoType: AgoraVoiceBelCantoType, soundEffectType: SoundEffectType) -> UIEdgeInsets {
        switch self {
        case .belCanto: return belCantoType.edges
        case .soundEffect: return soundEffectType.edges
        }
    }
    
    func minInteritemSpacing(belCantoType: AgoraVoiceBelCantoType, soundEffectType: SoundEffectType) ->  CGFloat {
        switch self {
        case .belCanto: return belCantoType.minInteritemSpacing
        case .soundEffect: return soundEffectType.minInteritemSpacing
        }
    }
    
    func minLineSpacing(belCantoType: AgoraVoiceBelCantoType, soundEffectType: SoundEffectType) -> CGFloat {
        switch self {
        case .belCanto: return belCantoType.minLineSpacing
        case .soundEffect: return soundEffectType.minLineSpacing
        }
    }
    
    func layout(belCantoType: AgoraVoiceBelCantoType, soundEffectType: SoundEffectType) -> CGSize {
        switch self {
        case .belCanto: return belCantoType.layout
        case .soundEffect: return soundEffectType.layout
        }
    }
}


class AgoraVoiceBelCantoView: UIView {
    var didAgoraVoiceBelCantoItemClosure: ((AgoraVoiceBelCantoModel?) -> Void)?
    var didAgoraVoiceSoundEffectItemClosure: ((AgoraVoiceSoundEffectModel?, Int32) -> Void)?
    var onTapSwitchClosure: ((Bool) -> Void)?
    
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "bel_canto".localized
        label.colorStyle = .white
        return label
    }()
    private lazy var segmentView: SegmentView = {
        let titles = AgoraVoiceBelCantoType.allCases.map({ $0.title })
        let segmentView = SegmentView(frame: CGRect(x: 0, y: 0, width: 300, height: 44), segmentStyle: .init(), titles: titles)
        segmentView.style.indicatorStyle = .line
        segmentView.style.indicatorHeight = 3
        segmentView.style.indicatorColor = .blueColor
        segmentView.style.indicatorWidth = 50
        segmentView.selectedTitleColor = .white
        segmentView.style.titleMargin = 10
        segmentView.style.titlePendingHorizontal = currentType == .belCanto ? 14 : 4
        segmentView.normalTitleColor = .gray
        segmentView.valueChange = { [weak self] index in
            guard let self = self, self.segmentView.titles.count > index else { return }
            let belCantoType = AgoraVoiceBelCantoType(rawValue: index) ?? .voice
            let soundEffectType = SoundEffectType(rawValue: index) ?? .space
            self.currentSoundType = soundEffectType
            self.collectionView.itemSize = self.currentType.layout(belCantoType: belCantoType, soundEffectType: soundEffectType)
            self.collectionView.edge = self.currentType.edges(belCantoType: belCantoType, soundEffectType: soundEffectType)
            self.collectionView.minLineSpacing = self.currentType.minLineSpacing(belCantoType: belCantoType, soundEffectType: soundEffectType)
            self.collectionView.minInteritemSpacing = self.currentType.minInteritemSpacing(belCantoType: belCantoType, soundEffectType: soundEffectType)
            self.collectionView.dataArray = self.currentType == .belCanto ? belCantoType.dataArray : soundEffectType.dataArray
            self.collectionView.reloadData()
            if self.currentType == .soundEffect && soundEffectType == .pitchCorrection {
                self.collectionViewHeightCons?.constant = 310
            } else if self.currentType == .soundEffect {
                self.collectionViewHeightCons?.constant = 240
            } else {
                self.collectionViewHeightCons?.constant = 200
            }
            self.collectionViewHeightCons?.isActive = true
        }
        return segmentView
    }()
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        let belCantoType = AgoraVoiceBelCantoType(rawValue: 0) ?? .voice
        let soundEffectType = SoundEffectType(rawValue: 0) ?? .space
        view.itemSize = currentType.layout(belCantoType: belCantoType, soundEffectType: soundEffectType)
        view.minInteritemSpacing = currentType.minInteritemSpacing(belCantoType: belCantoType, soundEffectType: soundEffectType)
        view.minLineSpacing = currentType.minLineSpacing(belCantoType: belCantoType, soundEffectType: soundEffectType)
        view.edge = currentType.edges(belCantoType: belCantoType, soundEffectType: soundEffectType)
        view.delegate = self
        view.scrollDirection = .vertical
        view.emptyTitle = "upcoming_release".localized
        view.emptyImage = UIImage(named: "pic-coming soon")
        view.emptyTopMargin = 40
        view.register(AgoraVoiceBelCantoViewCell.self,
                      forCellWithReuseIdentifier: AgoraVoiceBelCantoViewCell.description())
        
        view.register(AgoraVoicePitchCorrectionHeaderView.self,
                      forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
                      withReuseIdentifier: "pitchCorrectionHeaderView")
        return view
    }()
    
    private var currentSoundType: SoundEffectType = .space
    private var currentType: AudioEffectType = .belCanto
    private var collectionViewHeightCons: NSLayoutConstraint?
    private var currentPitchValue: Int32 = 1
    
    init(type: AudioEffectType) {
        super.init(frame: .zero)
        currentType = type
        setupUI()
    }
        
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .init(hex: "#4F506A")
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        segmentView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        addSubview(segmentView)
        addSubview(collectionView)
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        segmentView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        segmentView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: currentType == .belCanto ? 40 : 0).isActive = true
        segmentView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        segmentView.heightAnchor.constraint(equalToConstant: 44).isActive = true
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: segmentView.bottomAnchor, constant: 20).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor).isActive = true
        collectionViewHeightCons = collectionView.heightAnchor.constraint(equalToConstant: currentType == .belCanto ? 200 : 240)
        collectionViewHeightCons?.isActive = true
        
        collectionView.dataArray = currentType == .belCanto ? AgoraVoiceBelCantoType.voice.dataArray : SoundEffectType.space.dataArray
        
        titleLabel.text = currentType.title
        segmentView.titles = currentType.segmentTitles
    }
}
extension AgoraVoiceBelCantoView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: AgoraVoiceBelCantoViewCell.description(), for: indexPath) as! AgoraVoiceBelCantoViewCell
        let model = self.collectionView.dataArray?[indexPath.item]
        if currentType == .belCanto {
            cell.setupData(model: model as? AgoraVoiceBelCantoModel)
        } else {
            cell.setupData(model: model as? AgoraVoiceSoundEffectModel)
        }
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        if kind == UICollectionView.elementKindSectionHeader && currentSoundType == .pitchCorrection {
            let view = collectionView.dequeueReusableSupplementaryView(ofKind: UICollectionView.elementKindSectionHeader,
                                                                       withReuseIdentifier: "pitchCorrectionHeaderView",
                                                                       for: indexPath) as! AgoraVoicePitchCorrectionHeaderView
            view.onTapSwitchClosure = onTapSwitchClosure
            view.onTapSegmentViewClosure = { [weak self] index in
                self?.currentPitchValue = Int32(index + 1)
            }
            return view
        }
        return UICollectionReusableView()
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForHeaderInSection section: Int) -> CGSize {
        guard currentType == .soundEffect, currentSoundType == .pitchCorrection else { return .zero }
        return CGSize(width: Screen.width, height: 130)
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let model = self.collectionView.dataArray?[indexPath.item]
        if currentType == .belCanto {
            didAgoraVoiceBelCantoItemClosure?(model as? AgoraVoiceBelCantoModel)
        } else {
            didAgoraVoiceSoundEffectItemClosure?(model as? AgoraVoiceSoundEffectModel, currentPitchValue)
        }
    }
}

class AgoraVoiceBelCantoViewCell: UICollectionViewCell {
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .vertical
        stackView.distribution = .fill
        stackView.spacing = 5
        return stackView
    }()
    private lazy var imageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "icon-大叔磁性")
        imageView.setContentHuggingPriority(.defaultHigh, for: .vertical)
        return imageView
    }()
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .disabled, fontStyle: .small)
        label.text = "magnetism_male".localized
        label.colorStyle = .disabled
        label.fontStyle = .small
        label.textAlignment = .center
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupData(model: AgoraVoiceBelCantoModel?) {
        guard let model = model else {
            return
        }
        imageView.isHidden = model.imageName.isEmpty
        
        titleLabel.text = model.title
        
        if model.imageName.isEmpty {
            titleLabel.borderColor = .white
            titleLabel.borderWidth = 1
            titleLabel.cornerRadius = 20.fit
            titleLabel.fontStyle = .middle
        } else {
            imageView.image = UIImage(named: model.imageName)
            titleLabel.borderWidth = 0
            titleLabel.fontStyle = .small
        }
    }
    
    func setupData(model: AgoraVoiceSoundEffectModel?) {
        guard let model = model else {
            return
        }
        imageView.isHidden = model.imageName.isEmpty
        
        titleLabel.text = model.title
        
        if model.imageName.isEmpty {
            titleLabel.borderColor = .white
            titleLabel.borderWidth = 1
            titleLabel.cornerRadius = 20.fit
            titleLabel.fontStyle = .middle
        } else {
            imageView.image = UIImage(named: model.imageName)
            titleLabel.borderWidth = 0
            titleLabel.fontStyle = .small
        }
    }
    
    override var isSelected: Bool {
        didSet {
            if imageView.isHidden == false {
                contentView.layer.borderWidth = isSelected ? 1 : 0
                contentView.layer.borderColor = isSelected ? UIColor.blueColor.cgColor : UIColor.clear.cgColor
                contentView.backgroundColor = isSelected ? UIColor.blueColor.withAlphaComponent(0.1) : .clear
            } else {
                titleLabel.borderColor = isSelected ? .blueColor : .white
                titleLabel.backgroundColor = isSelected ? UIColor.blueColor.withAlphaComponent(0.1) : .clear
            }
        }
    }
    
    private func setupUI() {
        statckView.translatesAutoresizingMaskIntoConstraints = false
        imageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(statckView)
        statckView.addArrangedSubview(imageView)
        statckView.addArrangedSubview(titleLabel)
        statckView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        statckView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        statckView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        statckView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
}
