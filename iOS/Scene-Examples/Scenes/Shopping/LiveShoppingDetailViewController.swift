//
//  LiveShoppingDetailViewController.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/9/1.
//

import UIKit
import Agora_Scene_Utils

class LiveShoppingDetailViewController: BaseViewController {
    private lazy var coverImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "Shopping/pic-1"))
        return imageView
    }()
    private lazy var descLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .middle)
        label.text = "Pants_Description".localized
        label.numberOfLines = 0
        return label
    }()
    private lazy var bottomView: AGEView = {
        let view = AGEView()
        view.backgroundColor = .white
        return view
    }()
    private lazy var buyButton: AGEButton = {
        let button = AGEButton(style: .none, colorStyle: .white, fontStyle: .middle)
        button.setBackgroundImage(UIImage(named: "Shopping/button-buy"), for: .normal)
        button.setTitle("Buy now".localized, for: .normal)
        button.addTarget(self, action: #selector(onTapBuyButtonHandler), for: .touchUpInside)
        return button
    }()
    
    private var model: LiveShoppingListModel
    
    init(model: LiveShoppingListModel) {
        self.model = model
        super.init(nibName: nil, bundle: nil)
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        setupUI()
        coverImageView.image = UIImage(named: "Shopping/\(model.imageName)")
        descLabel.text = model.desc
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationTransparent(isTransparent: true, isHiddenNavBar: false)
    }
    
    private func setupUI() {
        view.addSubview(coverImageView)
        view.addSubview(descLabel)
        view.addSubview(bottomView)
        bottomView.addSubview(buyButton)
        backButton.backgroundColor = .black.withAlphaComponent(0.6)
        backButton.cornerRadius = 20
        
        coverImageView.translatesAutoresizingMaskIntoConstraints = false
        descLabel.translatesAutoresizingMaskIntoConstraints = false
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        buyButton.translatesAutoresizingMaskIntoConstraints = false
        
        coverImageView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        coverImageView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        coverImageView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        coverImageView.heightAnchor.constraint(equalTo: view.heightAnchor, multiplier: 0.7).isActive = true
        
        descLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15).isActive = true
        descLabel.topAnchor.constraint(equalTo: coverImageView.bottomAnchor).isActive = true
        descLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        
        bottomView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        bottomView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        bottomView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        bottomView.heightAnchor.constraint(equalToConstant: (Screen.safeAreaBottomHeight() + 60)).isActive = true
        
         buyButton.trailingAnchor.constraint(equalTo: bottomView.trailingAnchor, constant: -15).isActive = true
        buyButton.centerYAnchor.constraint(equalTo: bottomView.centerYAnchor).isActive = true
        buyButton.widthAnchor.constraint(equalToConstant: 100).isActive = true
        buyButton.heightAnchor.constraint(equalToConstant: 40).isActive = true
    }
    
    @objc
    private func onTapBuyButtonHandler() {
        showAlert(title: "Buy_This_Product".localized, message: "") { [weak self] in
            ToastView.show(text: "Product_Purchased_Successfully".localized)
            self?.navigationController?.popViewController(animated: true)
        }
    }
}
