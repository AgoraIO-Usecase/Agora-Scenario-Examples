//
//  BORCreateRoomController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit

class BORCreateRoomController: BaseViewController {
    var createRoomFinished: (() -> Void)?
    private lazy var textField: UITextField = {
        let textField = UITextField()
        textField.layer.borderColor = UIColor.systemPink.cgColor
        textField.layer.borderWidth = 1
        textField.layer.cornerRadius = 10
        textField.layer.masksToBounds = true
        textField.textColor = .black
        textField.leftView = UIView(frame: CGRect(x: 10, y: 0, width: 10, height: 0))
        textField.leftViewMode = .always
        textField.font = .systemFont(ofSize: 14)
        textField.placeholder = "Please enter a room name"
        return textField
    }()
    private lazy var createRoomButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "create_room"), for: .normal)
        button.addTarget(self, action: #selector(clickCreateRoomButton(_:)), for: .touchUpInside)
        return button
    }()
    private var channelName: String?
    
    init(channelName: String? = nil) {
        super.init(nibName: nil, bundle: nil)
        self.channelName = channelName
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "create room"
        setupUI()
    }
    
    private func setupUI() {
        view.backgroundColor = .white
        textField.translatesAutoresizingMaskIntoConstraints = false
        createRoomButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(textField)
        view.addSubview(createRoomButton)
        textField.topAnchor.constraint(equalTo: view.layoutMarginsGuide.topAnchor, constant: 80).isActive = true
        textField.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 40).isActive = true
        textField.heightAnchor.constraint(equalToConstant: 40).isActive = true
        textField.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -40).isActive = true
        
        createRoomButton.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        createRoomButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -40).isActive = true
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        textField.resignFirstResponder()
    }
    
    @objc
    private func clickCreateRoomButton(_ sender: UIButton) {
        let text = textField.text ?? ""
        guard !text.isEmpty else {
            showHUDError(error: "Can't be empty")
            return
        }
        guard text.count < 11 else {
            showHUDError(error: "Over length limit")
            return
        }
        guard !text.isChinese(str: text) else {
            showHUDError(error: "Chinese not supported")
            return
        }
        showWaitHUD(title: "")
        var itemModel = BORLiveModel()
        itemModel.id = text.trimmingCharacters(in: .whitespacesAndNewlines)
        itemModel.backgroundId = String(format: "portrait%02d", arc4random_uniform(13) + 1)
        let params = JSONObject.toJson(itemModel)
        LogUtils.log(message: "params == \(params)", level: .info)
        SyncUtil.joinScene(id: itemModel.id,
                           userId: "\(UserInfo.userId)",
                           property: params,
                           delegate: self)
        let roomModel = BORSubRoomModel(subRoom: text)
        let roomParams = JSONObject.toJson(roomModel)
//        SyncUtil.instance.addCollection(id: itemModel.id, className: SYNC_COLLECTION_SUB_ROOM,
//                                        params: roomParams,
//                                        delegate: self)
        createRoomFinished?()
    }
}

extension BORCreateRoomController: IObjectDelegate {
    func onSuccess(result: IObject) {
        let roomDetailVC = BORRoomDetailController(channelName: textField.text ?? "",
                                                   ownerId: "\(UserInfo.userId)")
        navigationController?.pushViewController(roomDetailVC, animated: true)
        hideHUD()
    }
    
    func onFailed(code: Int, msg: String) {
        hideHUD()
        LogUtils.log(message: "code == \(code) msg == \(msg)", level: .error)
    }
}
