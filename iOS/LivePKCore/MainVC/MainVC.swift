//
//  MainVC.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import UIKit

class MainVC: UIViewController {
    var vm: MainVM!
    let mainView = MainView()
    var loginInfo: LoginInfo!
    var renderInfos = [RenderInfo]()
    var closeItem: UIBarButtonItem!
    var pkItem: UIBarButtonItem!
    var exitPkItem: UIBarButtonItem!
    
    init(loginInfo: LoginInfo,
         appId: String) {
        super.init(nibName: nil, bundle: nil)
        self.vm = MainVM(loginInfo: loginInfo, appId: appId)
        self.loginInfo = loginInfo
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setup() {
        title = loginInfo.roomName
        closeItem = UIBarButtonItem(title: "Close", style: .plain, target: self, action: #selector(closeOnClick))
        pkItem = UIBarButtonItem(title: "PK", style: .plain, target: self, action: #selector(pkOnClick))
        exitPkItem = UIBarButtonItem(title: "Exit PK", style: .plain, target: self, action: #selector(exitPkOnClick))
        view.addSubview(mainView)
        mainView.translatesAutoresizingMaskIntoConstraints = false
        mainView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        mainView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        mainView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        mainView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
    }
    
    func commonInit() {
        mainView.collectionView.dataSource = self
        mainView.collectionView.delegate = self
        mainView.collectionView.register(VideoCell.self, forCellWithReuseIdentifier: "cell")
        vm.delegate = self
        vm.start()
    }
    
    @objc func closeOnClick() {
        dismiss(animated: true, completion: nil)
    }
    
    @objc func pkOnClick() {
        let alertVC = UIAlertController(title: "RoomName", message: "enter room name", preferredStyle: .alert)
        alertVC.addTextField(configurationHandler: nil)
        let action1 = UIAlertAction(title: "cancle", style: .cancel, handler: nil)
        let action2 = UIAlertAction(title: "ok", style: .default, handler: { [weak self](_) in
            guard let text = alertVC.textFields?.first?.text else {
                return
            }
            self?.vm.joinChannelRemote(channelName: text)
        })
        alertVC.addAction(action1)
        alertVC.addAction(action2)
        present(alertVC, animated: true, completion: nil)
    }
    
    @objc func exitPkOnClick() {
        vm.exitPk()
    }
}

extension MainVC: UICollectionViewDataSource, UICollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return renderInfos.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "cell", for: indexPath) as! VideoCell
        let info = renderInfos[indexPath.row]
        if info.isLocal { vm.subscribeVideoLocal(view: cell.videoView) }
        else {
            if info.type == .rtc {
                vm.subscribeVideoRemote(view: cell.videoView, uid: info.uid)
            }
            else {
                vm.subscribeMedia(view: cell.videoView, roomName: info.roomName)
            }
        }
        return cell
    }
}

extension MainVC: MainVMDelegate {
    func mainVMShouldShowTips(tips: String) {
        show(tips)
    }
    
    func mainVMDidUpdateRenderInfos(renders: [RenderInfo]) {
        if loginInfo.role == .audience {
            navigationItem.rightBarButtonItems =  [closeItem]
        }
        else {
            navigationItem.rightBarButtonItems = renders.count == 1 ? [closeItem, pkItem] : [closeItem, exitPkItem]
        }
        
        self.renderInfos = renders
        mainView.collectionView.reloadData()
    }
}
