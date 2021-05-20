//
//  HomeController.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import UIKit
import RxSwift
import RxCocoa
import Core

public class BlindDateHomeController: BaseViewContoller {
    @IBOutlet weak var reloadButton: RoundButton!
    @IBOutlet weak var emptyView: UILabel!
    @IBOutlet weak var listView: UICollectionView! {
        didSet {
            let layout = WaterfallLayout()
            layout.delegate = self
            layout.sectionInset = UIEdgeInsets(top: 12, left: 12, bottom: 0, right: 12)
            layout.minimumLineSpacing = 7
            layout.minimumInteritemSpacing = 7
            
            listView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0)
            listView.collectionViewLayout = layout
            listView.register(HomeCardView.self, forCellWithReuseIdentifier: NSStringFromClass(HomeCardView.self))
            listView.dataSource = self
        }
    }
    @IBOutlet weak var createRoomButton: UIButton!
    private var createRoomDialog: CreateRoomDialog? = nil
    private let refreshControl: UIRefreshControl = {
        let view = UIRefreshControl()
        view.tintColor = UIColor(hex: Colors.Gray)
        return view
    }()
    private var viewModel: HomeViewModel!
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return [.portrait]
    }
    
    public override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    func configNavbar() {
        self.title = "Room List".localized
        if let navBar = navigationController?.navigationBar {
            navBar.tintColor = .white
            navBar.backItem?.backButtonTitle = ""
            
            let appearance = UINavigationBarAppearance()
            appearance.configureWithOpaqueBackground()
            //appearance.backgroundColor = UIColor(hex: "#641BDF")
            appearance.backgroundImage = UIImage.gradient(
                colors: [UIColor(hex: "#641BDF"), UIColor(hex: "#D07AF5")],
                with: navBar.superview!.bounds
            )
            appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
            navigationItem.standardAppearance = appearance
            navigationItem.scrollEdgeAppearance = appearance
        }
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: false)
        configNavbar()
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        listView.refreshControl = refreshControl
        viewModel = HomeViewModel()
        
        if (Utils.checkNetworkPermission()) {
            initAppData()
        } else {
            reloadButton.isHidden = false
            reloadButton.rx.tap
                .subscribe(onNext: {
                    if (Utils.checkNetworkPermission()) {
                        self.reloadButton.isHidden = true
                        self.initAppData()
                    } else {
                        self.show(message: "Needs Network permission".localized, type: .error)
                    }
                })
                .disposed(by: disposeBag)
        }
        
        subcribeUIEvent()
    }
    
    private func initAppData() {
        viewModel.setup()
            .do(onSubscribe: {
                self.show(processing: true)
            }, onDispose: {
                self.show(processing: false)
            })
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if (result.success) {
                    // UIRefreshControl bug?
                    self.refreshControl.tintColor = UIColor(hex: Colors.Gray)
                    self.refreshControl.refreshManually()
                } else {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            })
            .disposed(by: disposeBag)
    }
    
    private func subcribeUIEvent() {
        createRoomButton.rx.tap
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .filter { [unowned self] _ in
                self.viewModel.account() != nil
            }
            .concatMap { [unowned self] _ -> Single<Bool> in
                self.createRoomDialog = CreateRoomDialog.Create()
                self.createRoomDialog?.createRoomDelegate = self
                return self.createRoomDialog!.show()
            }
            .subscribe()
            .disposed(by: disposeBag)
        
        refreshControl
            .rx.controlEvent(.valueChanged)
            .flatMapLatest { [unowned self] _ in
                return self.viewModel.dataSource()
            }
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if (result.success) {
                    self.emptyView.isHidden = self.viewModel.roomList.count != 0
                    self.listView.reloadData()
                } else {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            })
            .disposed(by: disposeBag)
        
        viewModel.activityIndicator
            .drive(self.refreshControl.rx.isRefreshing)
            .disposed(by: disposeBag)
    }
    
    @IBAction func onTapSetting(_ sender: Any) {
        Logger.log(message: "onTapSetting", level: .info)
    }
    
    private func refresh() {
        refreshControl.sendActions(for: .valueChanged)
    }
    
    public static func instance() -> BlindDateHomeController {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        let controller = storyBoard.instantiateViewController(withIdentifier: "HomeController") as! BlindDateHomeController
        return controller
    }
}

extension BlindDateHomeController: UICollectionViewDataSource {
    public func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }
    
    public func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let card: HomeCardView = collectionView.dequeueReusableCell(withReuseIdentifier: NSStringFromClass(HomeCardView.self), for: indexPath) as! HomeCardView
        card.delegate = self
        card.room = viewModel.roomList[indexPath.item]
        return card
    }
    
    public func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return viewModel.roomList.count
    }
}

extension BlindDateHomeController: WaterfallLayoutDelegate {
    public func collectionView(_ collectionView: UICollectionView, layout: WaterfallLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = (layout.collectionViewContentSize.width - 16 * 2 - 10) / 2
        return HomeCardView.sizeForItem(room: viewModel.roomList[indexPath.item], width: width)
    }
    
    public func collectionViewLayout(for section: Int) -> WaterfallLayout.Layout {
        return .waterfall(column: 2, distributionMethod: .balanced)
    }
}

extension BlindDateHomeController: HomeCardDelegate {
    func onTapCard(with room: Room) {
        viewModel.join(room: room)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                self.show(processing: true)
            }, onDispose: {
                self.show(processing: false)
            })
            .subscribe() { [unowned self] result in
                if (result.success) {
                    let controller = RoomController.instance()
                    //roomController.navigationController = self.navigationController
                    self.navigationController?.pushViewController(controller, animated: true)
                    //self.push(controller: controller)
                } else {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            } onDisposed: {
                self.show(processing: false)
            }
            .disposed(by: disposeBag)
    }
}

extension BlindDateHomeController: CreateRoomDelegate {
    func onDismiss() {
        createRoomDialog = nil
    }
    
    func onCreateSuccess(with room: Room) {
        if let dialog = createRoomDialog {
            dialog.dismiss()
                .subscribe(onSuccess: { [unowned self] _ in
                    self.refresh()
                    self.onTapCard(with: room)
                })
                .disposed(by: disposeBag)
        }
    }
    
    func createRoom(with name: String?) -> Observable<Result<Room>> {
        if (name?.isEmpty == false) {
            return viewModel.createRoom(with: name!)
        } else {
            return Observable.just(Result<Room>(success: false, message: "Enter a room name".localized))
        }
    }
}
