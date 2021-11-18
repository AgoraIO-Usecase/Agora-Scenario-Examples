//
//  LiveSettingView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

class LiveSettingView: UIView {
    var liveSettingFinishedClosure: ((LiveSettingUseData) -> Void)?
    private lazy var headerView = UIView(frame: CGRect(x: 0, y: 0, width: 0, height: 44))
    private lazy var closeButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(systemName: "chevron.backward")?.withTintColor(.black, renderingMode: .alwaysOriginal), for: .normal)
        button.addTarget(self, action: #selector(clickBackButton), for: .touchUpInside)
        return button
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Live_Room_Settings".localized
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var tableViewLayout: BaseTableViewLayout = {
        let view = BaseTableViewLayout()
        view.estimatedRowHeight = 100
        view.delegate = self
        view.separatorStyle = .singleLine
        view.register(LiveSettingViewCell.self,
                      forCellWithReuseIdentifier: LiveSettingViewCell.description())
        view.register(LiveSettingSliderViewCell.self,
                      forCellWithReuseIdentifier: LiveSettingSliderViewCell.description())
        view.dataArray = LiveSettingModel.settingsData()
        return view
    }()
    private var isDetail: Bool = false
    private var dataArray = LiveSettingModel.settingsData()
    private var currentModel = LiveSettingUseData()
    
    init(title: String, datas: [LiveSettingModel], useModel: LiveSettingUseData?, isDetail: Bool = false) {
        super.init(frame: .zero)
        setupUI()
        titleLabel.text = title
        tableViewLayout.dataArray = datas
        self.isDetail = isDetail
        guard let model = useModel else { return }
        currentModel = model
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .white
        translatesAutoresizingMaskIntoConstraints = false
        tableViewLayout.translatesAutoresizingMaskIntoConstraints = false
        addSubview(tableViewLayout)
        widthAnchor.constraint(equalToConstant: cl_screenWidht).isActive = true
        heightAnchor.constraint(equalToConstant: 250).isActive = true
        layer.cornerRadius = 15
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(closeButton)
        headerView.addSubview(titleLabel)
        closeButton.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 15).isActive = true
        closeButton.centerYAnchor.constraint(equalTo: headerView.centerYAnchor).isActive = true
        titleLabel.centerYAnchor.constraint(equalTo: headerView.centerYAnchor).isActive = true
        titleLabel.centerXAnchor.constraint(equalTo: headerView.centerXAnchor).isActive = true
        tableViewLayout.headerView = headerView
        tableViewLayout.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableViewLayout.topAnchor.constraint(equalTo: topAnchor).isActive = true
        tableViewLayout.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableViewLayout.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    @objc
    private func clickBackButton() {
        AlertManager.hiddenView(all: false)
    }
}
extension LiveSettingView: BaseTableViewLayoutDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let datas = tableViewLayout.dataArray as? [LiveSettingModel] else { return UITableViewCell() }
        var model = datas[indexPath.row]
        if model.settingType == .bitRate {
            let cell = tableView.dequeueReusableCell(withIdentifier: LiveSettingSliderViewCell.description(), for: indexPath) as! LiveSettingSliderViewCell
            if currentModel.sliderValue > 0 {
                model.sliderValue = Float(currentModel.sliderValue) / 2000
            }
            cell.setLiveSettingData(model: model)
            cell.sliderValueChangeClosure = { [weak self] value in
                guard let self = self else { return }
                self.currentModel.sliderValue = value
                self.liveSettingFinishedClosure?(self.currentModel)
            }
            return cell
        }
        let cell = tableView.dequeueReusableCell(withIdentifier: LiveSettingViewCell.description(), for: indexPath) as! LiveSettingViewCell
        if indexPath.row == 0 && currentModel.resolution != .zero && isDetail == false {
            model.desc = "\(Int(currentModel.resolution.height)) X \(Int(currentModel.resolution.width))"
        } else if indexPath.row == 1 && currentModel.resolution != .zero && isDetail == false {
            model.desc = "\(currentModel.framedate.rawValue)"
        }
        cell.setLiveSettingData(model: model)
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        guard let dataArray = tableViewLayout.dataArray as? [LiveSettingModel] else { return }
        let model = dataArray[indexPath.row]
        if model.settingType == .resolution {
            currentModel.resolution = model.resolutionTitle
        } else if model.settingType == .frameRate {
            currentModel.framedate = model.frameRate
        }
        if (model.settingType == .frameRate || model.settingType == .resolution) && isDetail == false {
            let datas = model.title == "Resolution".localized
            ? LiveSettingModel.resolutionData()
            : LiveSettingModel.frameRateData()
            let settintView = LiveSettingView(title: model.title, datas: datas, useModel: currentModel, isDetail: true)
            settintView.liveSettingFinishedClosure = liveSettingFinishedClosure
            AlertManager.show(view: settintView, alertPostion: .bottom)
            return
        }
        liveSettingFinishedClosure?(currentModel)
        AlertManager.hiddenView()
    }
}

class LiveSettingViewCell: UITableViewCell {
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "分辨率"
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.text = "240 X 240"
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var arrowImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(systemName: "chevron.right")?.withTintColor(.gray, renderingMode: .alwaysOriginal))
        imageView.isHidden = true
        return imageView
    }()
    private var descConstraint: NSLayoutConstraint?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setLiveSettingData(model: LiveSettingModel) {
        titleLabel.text = model.title
        descLabel.text = model.desc
        arrowImageView.isHidden = model.settingType == .bitRate || model.title.isEmpty
        if model.title.isEmpty {
            descConstraint = descLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor)
            descConstraint?.isActive = true
        }
    }
    
    private func setupUI() {
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descLabel.translatesAutoresizingMaskIntoConstraints = false
        arrowImageView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleLabel)
        contentView.addSubview(descLabel)
        contentView.addSubview(arrowImageView)
        
        titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        titleLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        arrowImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        arrowImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        descConstraint = descLabel.trailingAnchor.constraint(equalTo: arrowImageView.leadingAnchor, constant: -15)
        descLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 15).isActive = true
        descLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -15).isActive = true
        descConstraint?.isActive = true
    }
}

class LiveSettingSliderViewCell: UITableViewCell {
    var sliderValueChangeClosure:((Int) -> Void)?
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "分辨率"
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.text = "240 X 240"
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var slideView: UISlider = {
        let slider = UISlider()
        slider.maximumTrackTintColor = .gray
        slider.minimumTrackTintColor = .blueColor
        slider.minimumValue = 0
        slider.maximumValue = 1.0
        slider.addTarget(self, action: #selector(sliderValueChangeHandler(sender:)), for: .valueChanged)
        slider.addTarget(self, action: #selector(sliderValueTouchUpInside(sender:)), for: .touchUpInside)
        return slider
    }()
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    @objc
    private func sliderValueChangeHandler(sender: UISlider) {
        let value = Int(sender.value * 2000)
        descLabel.text = "\(value)kbps"
    }
    @objc
    private func sliderValueTouchUpInside(sender: UISlider) {
        let value = Int(sender.value * 2000)
        sliderValueChangeClosure?(value)
    }
    
    func setLiveSettingData(model: LiveSettingModel) {
        titleLabel.text = model.title
        descLabel.text = "\(Int(model.sliderValue * 2000))kbps"
        slideView.setValue(model.sliderValue, animated: true)
    }
    
    private func setupUI() {
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descLabel.translatesAutoresizingMaskIntoConstraints = false
        slideView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleLabel)
        contentView.addSubview(descLabel)
        contentView.addSubview(slideView)
        
        titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 15).isActive = true
        
        descLabel.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor, constant: 10).isActive = true
        descLabel.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor).isActive = true
        
        slideView.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        slideView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10).isActive = true
        slideView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -15).isActive = true
        slideView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
    }
}
