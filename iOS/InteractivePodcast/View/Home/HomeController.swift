//
//  GroupController.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/3.
//

import Foundation
import UIKit
import RxSwift
import RxCocoa
import Core

public class HomeController: BaseViewContoller, DialogDelegate {
    @IBOutlet weak var emptyView: UIView!
    @IBOutlet weak var avatarView: RoundImageView!
    @IBOutlet weak var backView: UIView! {
        didSet {
            backView.isHidden = navigationController?.viewControllers.count == 1
        }
    }
    @IBOutlet weak var listView: UICollectionView! {
        didSet {
            let layout = WaterfallLayout()
            layout.delegate = self
            layout.sectionInset = UIEdgeInsets(top: 16, left: 16, bottom: 0, right: 16)
            layout.minimumLineSpacing = 10.0
            layout.minimumInteritemSpacing = 10.0
            
            listView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0)
            listView.collectionViewLayout = layout
            listView.register(HomeCardView.self, forCellWithReuseIdentifier: NSStringFromClass(HomeCardView.self))
            listView.dataSource = self
        }
    }
    @IBOutlet var handsupTap: UITapGestureRecognizer!
    @IBOutlet weak var createRoomButton: UIButton!
    @IBOutlet var meTap: UITapGestureRecognizer!
    @IBOutlet weak var reloadButton: UIButton!
    
    private var createRoomDialog: CreateRoomDialog? = nil
    private let refreshControl: UIRefreshControl = {
        let view = UIRefreshControl()
        view.tintColor = UIColor(hex: Colors.White)
        return view
    }()
    private var viewModel: HomeViewModel!
    private var miniRoomView: MiniRoomView? = nil
    
    private func subcribeUIEvent() {
        meTap.rx.event
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .filter { [unowned self] _ in
                self.viewModel.account() != nil
            }
            .subscribe(onNext: { [unowned self] _ in
                self.navigationController?.pushViewController(
                    MeController.instance(),
                    animated: true
                )
            })
            .disposed(by: disposeBag)
        
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
        
        viewModel
            .showMiniRoom
            .startWith(false)
            .distinctUntilChanged()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] showMiniRoom in
                self.showCreatRoomView(!showMiniRoom)
                self.showMiniRoom(showMiniRoom)
            })
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
    
    private func initAppData() {
        viewModel.setup()
            .do(onSubscribe: {
                self.show(processing: true)
            }, onDispose: {
                self.show(processing: false)
            })
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if let user = self.viewModel.account() {
                    self.avatarView.image = UIImage(named: user.getLocalAvatar(), in: Utils.bundle, with: nil)
                }
                if (result.success) {
                    // UIRefreshControl bug?
                    self.refreshControl.tintColor = UIColor(hex: Colors.White)
                    self.refreshControl.refreshManually()
                } else {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            })
            .disposed(by: disposeBag)
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        backView.onTap().rx.event
            .subscribe(onNext: { [unowned self] _ in
                self.navigationController?.popViewController(animated: true)
            })
            .disposed(by: disposeBag)
        
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
    
    public override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        showMiniRoom(false)
    }
    
    private func showCreatRoomView(_ show: Bool) {
        if (show) {
            createRoomButton.alpha = 0
            UIView.animate(withDuration: TimeInterval(0.3), delay: TimeInterval(0.3), options: .curveEaseInOut, animations: {
                self.createRoomButton.alpha = 1
            })
        } else {
            createRoomButton.alpha = 1
            UIView.animate(withDuration: TimeInterval(0.3), delay: 0, options: .curveEaseInOut, animations: {
                self.createRoomButton.alpha = 0
            })
        }
    }
        
    private func showMiniRoom(_ show: Bool) {
        Logger.log(message: "showMiniRoom \(show)", level: .info)
        if (show) {
            if (miniRoomView == nil) {
                miniRoomView = MiniRoomView()
                miniRoomView!.leaveAction = self.onLeaveRoomController
                miniRoomView!.show(with: self)
            }
        } else {
            miniRoomView?.disconnect()
            miniRoomView?.dismiss(controller: self)
            miniRoomView = nil
        }
    }
    
    private func refresh() {
        refreshControl.sendActions(for: .valueChanged)
    }
    
    func onLeaveRoomController(action: LeaveRoomAction, room: Room?) {
        //self.refresh()
        switch action {
        case .closeRoom:
            show(message: "Room closed".localized, type: .error)
            viewModel.showMiniRoom.accept(false)
            refresh()
        case .leave:
            if (room != nil) {
                show(message: "Room closed".localized, type: .error)
                refresh()
            }
            viewModel.showMiniRoom.accept(false)
        case .mini:
            viewModel.showMiniRoom.accept(true)
        }
    }
    
    deinit {
        Logger.log(message: "HomeController deinit", level: .info)
        let _ = Server.shared().leave().subscribe()
    }
}

extension HomeController: UICollectionViewDataSource {
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

extension HomeController: WaterfallLayoutDelegate {
    public func collectionView(_ collectionView: UICollectionView, layout: WaterfallLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = (layout.collectionViewContentSize.width - 16 * 2 - 10) / 2
        return HomeCardView.sizeForItem(room: viewModel.roomList[indexPath.item], width: width)
    }
    
    public func collectionViewLayout(for section: Int) -> WaterfallLayout.Layout {
        return .waterfall(column: 2, distributionMethod: .balanced)
    }
    
    func checkMiniRoom(with room: Room) -> Observable<Bool> {
        if let miniRoom = miniRoomView {
            return miniRoom.onChange(room: room).map { [unowned self] close in
                if (close) { self.viewModel.showMiniRoom.accept(false) }
                return close
            }
        }
        return Observable.just(true)
    }
}

extension HomeController: HomeCardDelegate {
    func onTapCard(with room: Room) {
        checkMiniRoom(with: room)
            .concatMap { [unowned self] _ -> Observable<Result<Room>> in
                self.show(processing: true)
                return self.viewModel.join(room: room)
            }
            .observe(on: MainScheduler.instance)
            .subscribe() { [unowned self] result in
                if (result.success) {
                    let roomController = RoomController.instance(leaveAction: self.onLeaveRoomController)
                    //roomController.navigationController = self.navigationController
                    self.push(controller: roomController)
                } else {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
            } onDisposed: {
                self.show(processing: false)
            }
            .disposed(by: disposeBag)
    }
}

extension HomeController: CreateRoomDelegate {
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
    
    public static func instance() -> HomeController {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        let controller = storyBoard.instantiateViewController(withIdentifier: "HomeController") as! HomeController
        return controller
    }
}
