//
//  AgoraVoiceMusicView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/27.
//

import UIKit
import AgoraUIKit_iOS

class AgoraVoiceMusicView: UIView {
    var clickPlayButtonClosure: ((AgoraVoiceMusicModel?, Bool) -> Void)?
    var clickSliderValueChangeClosure: ((Float) -> Void)?
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white)
        label.text = "背景音乐".localized
        return label
    }()
    private lazy var loudspeakerImageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "icon-volume")
        return imageView
    }()
    private lazy var slider: AGESlider = {
        let slider = AGESlider()
        slider.tintColor = .init(hex: "#62626F")
        slider.thumbTintColor = .blueColor
        slider.maximumTrackTintColor = .init(hex: "#62626F")
        slider.minimumTrackTintColor = .blueColor
        slider.setThumbImage(UIImage(named: "icon-volume handle"), for: .normal)
        slider.setThumbImage(UIImage(named: "icon-volume handle"), for: .highlighted)
        slider.minimumValue = 0
        slider.maximumValue = 1.0
        slider.value = 0.5
        slider.addTarget(self, action: #selector(clickSliderChangeHandler(sender:)), for: .valueChanged)
        return slider
    }()
    private lazy var tableView: AGETableView = {
        let view = AGETableView()
        view.estimatedRowHeight = 44
        view.delegate = self
        view.register(AgoraVoiceMusicViewCell.self,
                      forCellWithReuseIdentifier: AgoraVoiceMusicViewCell.description())
        return view
    }()
    private var preCell: AgoraVoiceMusicViewCell?
    private var preModel: AgoraVoiceMusicModel?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        getMusicData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func getMusicData() {
        NetworkManager.shared.getRequest(urlString: "http://api.agora.io/ent/v1/musics") { response in
            let datas = response["data"] as? [[String: Any]]
            let models = datas?.compactMap({ JSONObject.toModel(AgoraVoiceMusicModel.self, value: $0) })
            self.tableView.dataArray = models
        } failure: { error in
            AGEToastView.show(text: error, view: self)
        }
    }
    
    private func setupUI() {
        backgroundColor = .init(hex: "#4F506A")
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        loudspeakerImageView.translatesAutoresizingMaskIntoConstraints = false
        slider.translatesAutoresizingMaskIntoConstraints = false
        tableView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        addSubview(loudspeakerImageView)
        addSubview(slider)
        addSubview(tableView)
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        loudspeakerImageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        loudspeakerImageView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 20).isActive = true
        
        slider.leadingAnchor.constraint(equalTo: loudspeakerImageView.trailingAnchor, constant: 10).isActive = true
        slider.centerYAnchor.constraint(equalTo: loudspeakerImageView.centerYAnchor).isActive = true
        slider.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        
        tableView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: loudspeakerImageView.bottomAnchor, constant: 20).isActive = true
        tableView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor).isActive = true
        tableView.heightAnchor.constraint(equalToConstant: 300).isActive = true
    }
    @objc
    private func clickSliderChangeHandler(sender: AGESlider) {
        clickSliderValueChangeClosure?(sender.value)
    }
}

extension AgoraVoiceMusicView: AGETableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: AgoraVoiceMusicViewCell.description(), for: indexPath) as! AgoraVoiceMusicViewCell
        let model = self.tableView.dataArray?[indexPath.row] as? AgoraVoiceMusicModel
        cell.setupMusicData(model: model)
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let cell = tableView.cellForRow(at: indexPath) as? AgoraVoiceMusicViewCell
        if preCell != cell {
            preCell?.resetPlayButtonStatus()
        }
        guard let model = self.tableView.dataArray?[indexPath.row] as? AgoraVoiceMusicModel else { return }        
        let isSelected = cell?.updatePlayButtonStatus() ?? true
        clickPlayButtonClosure?(model, isSelected)
        preCell = cell
    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        let view = AGEView()
        view.backgroundColor = .clear
        let containerView = AGELabel(colorStyle: .white)
        containerView.textAlignment = .center
        containerView.backgroundColor = .black
        containerView.layer.cornerRadius = 10
        containerView.layer.masksToBounds = true
        let string = "Music:http://api.agora.io/ent/v1/musics"
        let attr = NSMutableAttributedString(string: string)
        attr.addAttribute(.foregroundColor, value: UIColor.blueColor, range: NSRange(location: "Music:".count, length: string.count - "Music:".count))
        containerView.attributedText = attr
        view.addSubview(containerView)
        containerView.translatesAutoresizingMaskIntoConstraints = false
        containerView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        containerView.topAnchor.constraint(equalTo: view.topAnchor, constant: 15).isActive = true
        containerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        containerView.heightAnchor.constraint(equalToConstant: 50).isActive = true
        return view
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        (self.tableView.dataArray?.isEmpty ?? true) ? 0 : 80
    }
}

class AgoraVoiceMusicViewCell: UITableViewCell {
    private lazy var playButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(named: "icon-play"), for: .normal)
        button.setImage(UIImage(named: "icon-pause"), for: .selected)
        button.isUserInteractionEnabled = false
        return button
    }()
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .large)
        label.text = "asasfasfs"
        return label
    }()
    private lazy var detailLabel: AGELabel = {
        let label = AGELabel(colorStyle: .disabled)
        label.text = "asfagas"
        return label
    }()
    private var currentModel: AgoraVoiceMusicModel?
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupMusicData(model: AgoraVoiceMusicModel?) {
        currentModel = model
        titleLabel.text = model?.singer
        detailLabel.text = model?.musicName
    }
    
    func updatePlayButtonStatus() -> Bool {
        playButton.isSelected = !playButton.isSelected
        return playButton.isSelected
    }
    
    func resetPlayButtonStatus() {
        playButton.isSelected = false
    }
    
    private func setupUI() {
        playButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        detailLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.backgroundColor = .clear
        backgroundColor = .clear
        
        contentView.addSubview(playButton)
        contentView.addSubview(titleLabel)
        contentView.addSubview(detailLabel)
        playButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        playButton.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 15).isActive = true
        playButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -15).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: playButton.trailingAnchor, constant: 10).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: playButton.topAnchor, constant: 10).isActive = true
        
        detailLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        detailLabel.bottomAnchor.constraint(equalTo: playButton.bottomAnchor, constant: 5).isActive = true
    }
}
