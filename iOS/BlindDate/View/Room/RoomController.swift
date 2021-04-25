//
//  RoomController.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import UIKit
import RxSwift
import RxCocoa
import IGListKit
import Core

protocol RoomControlDelegate: class {
    func onTap(view: RoleVideoView)
}

class RoomController: BaseViewContoller, DialogDelegate, RoomDelegate {
    
    @IBOutlet weak var backView: UIButton!
    @IBOutlet weak var showMembersView: UIButton!
    @IBOutlet weak var avatar0: RoundImageView!
    @IBOutlet weak var avatar1: RoundImageView!
    @IBOutlet weak var avatar2: RoundImageView!
    
    @IBOutlet weak var hostRootView: UIView!
    @IBOutlet weak var hostVideoView: UIView! {
        didSet {
            hosterVideoView.videoView = hostVideoView
        }
    }
    @IBOutlet weak var hostNameView: UILabel! {
        didSet {
            hosterVideoView.nameView = hostNameView
        }
    }
    @IBOutlet weak var hostMicView: UIImageView! {
        didSet {
            hosterVideoView.micView = hostMicView
        }
    }
    
    @IBOutlet weak var leftRootView: UIView!
    @IBOutlet weak var leftVideoView: UIView! {
        didSet {
            leftSpeakerVideoView.videoView = leftVideoView
        }
    }
    @IBOutlet weak var leftNameView: UILabel! {
        didSet {
            leftSpeakerVideoView.nameView = leftNameView
        }
    }
    @IBOutlet weak var leftMicView: UIImageView! {
        didSet {
            leftSpeakerVideoView.micView = leftMicView
        }
    }
    @IBOutlet weak var leftAddView: UIButton! {
        didSet {
            leftSpeakerVideoView.addView = leftAddView
        }
    }
    
    @IBOutlet weak var rightRootView: UIView!
    @IBOutlet weak var rightVideoView: UIView! {
       didSet {
            rightSpeakerVideoView.videoView = rightVideoView
       }
   }
    @IBOutlet weak var rightNameView: UILabel! {
        didSet {
            rightSpeakerVideoView.nameView = rightNameView
        }
    }
    @IBOutlet weak var rightMicView: UIImageView! {
        didSet {
            rightSpeakerVideoView.micView = rightMicView
        }
    }
    @IBOutlet weak var rightAddView: UIButton! {
        didSet {
            rightSpeakerVideoView.addView = rightAddView
        }
    }
    
    @IBOutlet weak var chatListView: UITableView!
    @IBOutlet weak var toolBarRootView: UIView!
    @IBOutlet weak var inputMessageRootView: UIView!
    @IBOutlet weak var inputMessageView: UITextField!
    
    private var hosterVideoView: RoleVideoView = RoleVideoView()
    private var leftSpeakerVideoView: RoleVideoView = RoleVideoView()
    private var rightSpeakerVideoView: RoleVideoView = RoleVideoView()
    private var hosterToolbar: HosterToolbar?
    private var speakerToolbar: SpeakerToolbar?
    private var listenerToolbar: ListenerToolbar?
    private var keyboardHeight: CGFloat = -1
    
    var viewModel: RoomViewModel = RoomViewModel()
    
    private func renderToolbar() {
        switch viewModel.role {
        case .manager:
            if (hosterToolbar == nil) {
                hosterToolbar = HosterToolbar()
                if let hosterToolbar = hosterToolbar {
                    toolBarRootView.addSubview(hosterToolbar)
                    hosterToolbar.fill(view: toolBarRootView).active()
                    hosterToolbar.delegate = self
                    
                    if (speakerToolbar != nil || listenerToolbar != nil) {
                        speakerToolbar?.removeFromSuperview()
                        speakerToolbar = nil
                        listenerToolbar?.removeFromSuperview()
                        listenerToolbar = nil
                    }
                    
                    hosterToolbar.subcribeUIEvent()
                    hosterToolbar.subcribeRoomEvent()
                }
            }
        case .leftSpeaker, .rightSpeaker:
            if (speakerToolbar == nil) {
                speakerToolbar = SpeakerToolbar()
                if let speakerToolbar = speakerToolbar {
                    toolBarRootView.addSubview(speakerToolbar)
                    speakerToolbar.fill(view: toolBarRootView).active()
                    speakerToolbar.delegate = self
                    
                    if (hosterToolbar != nil || listenerToolbar != nil) {
                        hosterToolbar?.removeFromSuperview()
                        hosterToolbar = nil
                        listenerToolbar?.removeFromSuperview()
                        listenerToolbar = nil
                    }
                    
                    speakerToolbar.subcribeUIEvent()
                    speakerToolbar.subcribeRoomEvent()
                }
            }
        case .listener:
            if (listenerToolbar == nil) {
                listenerToolbar = ListenerToolbar()
                if let listenerToolbar = listenerToolbar {
                    toolBarRootView.addSubview(listenerToolbar)
                    listenerToolbar.fill(view: toolBarRootView).active()
                    listenerToolbar.delegate = self
                    if (speakerToolbar != nil || hosterToolbar != nil) {
                        speakerToolbar?.removeFromSuperview()
                        speakerToolbar = nil
                        hosterToolbar?.removeFromSuperview()
                        hosterToolbar = nil
                        show(message: "You've been set as audience".localized, type: .error)
                    }
                    
                    listenerToolbar.subcribeUIEvent()
                    listenerToolbar.subcribeRoomEvent()
                }
            }
        }
    }
    
    private func renderSpeakers() {
        hosterVideoView.member = viewModel.speakers.hoster
        leftSpeakerVideoView.member = viewModel.speakers.leftSpeaker
        rightSpeakerVideoView.member = viewModel.speakers.rightSpeaker
    }
    
    private func subcribeUIEvent() {
        backView.rx.tap
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .concatMap { [unowned self] _ -> Observable<Bool> in
                if (self.viewModel.isManager) {
                    return self.showAlert(title: "Close room".localized, message: "Leaving the room ends the session and removes everyone".localized)
                } else if (self.viewModel.isSpeaker) {
                    return self.showAlert(title: "Leave room".localized, message: "离开直播间将自动停止连麦".localized)
                } else {
                    return Observable.just(true)
                }
            }
            .filter { close in
                return close
            }
            .concatMap { [unowned self] _ in
                return self.viewModel.leaveRoom(action: .closeRoom)
            }
            .filter { [unowned self] result in
                if (!result.success) {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                }
                return result.success
            }
            .concatMap { [unowned self] _ in
                return self.dismiss()
            }
            .subscribe()
            .disposed(by: disposeBag)
        
        hosterVideoView.subcribeUIEvent()
        leftSpeakerVideoView.subcribeUIEvent()
        rightSpeakerVideoView.subcribeUIEvent()
        
        showMembersView.rx.tap
            .debounce(RxTimeInterval.microseconds(300), scheduler: MainScheduler.instance)
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] _ in
                RoomListenerList().show(delegate: self)
            })
            .disposed(by: disposeBag)
        
        keyboardHeight()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] height in
                Logger.log(message: "keyboardHeight: \(height)", level: .info)
                if (height == 0) {
                    if (self.inputMessageRootView.isHidden) {
                        return
                    }
                    self.inputMessageRootView.isHidden = true
                    self.inputMessageRootView.removeAllConstraints()
                    self.inputMessageView.removeAllConstraints()
                    self.inputMessageRootView.marginTrailing(anchor: self.view.trailingAnchor)
                        .centerX(anchor: self.view.centerXAnchor)
                        .marginBottom(anchor: self.view.bottomAnchor, constant: 0)
                        .active()
                    self.inputMessageView.fill(view: self.inputMessageRootView, leading: 12, top: 12, trailing: 12, bottom: 12)
                        .active()
                } else {
                    if (!self.inputMessageRootView.isHidden) {
                        return
                    }
                    if (self.keyboardHeight == -1) {
                        self.keyboardHeight = height
                    }
                    self.inputMessageRootView.removeAllConstraints()
                    self.inputMessageView.removeAllConstraints()
                    self.inputMessageRootView.marginTrailing(anchor: self.view.trailingAnchor)
                        .centerX(anchor: self.view.centerXAnchor)
                        .marginBottom(anchor: self.view.bottomAnchor, constant: self.keyboardHeight)
                        .active()
                    self.inputMessageView.fill(view: self.inputMessageRootView, leading: 12, top: 12, trailing: 12, bottom: 12)
                        .active()
                    self.inputMessageRootView.isHidden = false
                    self.inputMessageRootView.alpha = 0
                    UIView.animate(withDuration: 0.2, delay: 0, options: .curveEaseInOut, animations: {
                        self.inputMessageRootView.alpha = 1
                    })
                }
            })
            .disposed(by: disposeBag)
        
//        inputMessageView.rx.controlEvent(.)
//            .concatMap { [unowned self] _ -> Observable<Result<Void>> in
//                if let message = self.inputMessageView.text {
//                    self.inputMessageView.text = nil
//                    if (!message.isEmpty) {
//                        return self.viewModel.sendMessage(message: message)
//                    } else {
//                        return Observable.just(Result<Void>(success: true))
//                    }
//                }
//                return Observable.just(Result(success: true))
//            }
//            .subscribe(onNext: { [unowned self] result in
//                if (!result.success) {
//                    self.show(message: result.message ?? "unknown error".localized, type: .error)
//                }
//            })
//            .disposed(by: disposeBag)
    }
    
    private func subcribeRoomEvent() {
        viewModel.roomMembersDataSource()
            .observe(on: MainScheduler.instance)
            .flatMap { [unowned self] result -> Observable<Result<Bool>> in
                let roomClosed = result.data
                if (roomClosed == true) {
                    return self.viewModel.leaveRoom(action: .leave).map { _ in result }
                } else {
                    return Observable.just(result)
                }
            }
            .observe(on: MainScheduler.instance)
            .flatMap { [unowned self] result -> Observable<Result<Bool>> in
                if (result.data == true) {
                    Logger.log(message: "subcribeRoomEvent roomClosed", level: .info)
                    return self.dismiss().asObservable().map { _ in result }
                } else {
                    //self.adapter.performUpdates(animated: false)
                    self.renderSpeakers()
                    return Observable.just(result)
                }
            }
            .subscribe(onNext: { [unowned self] result in
                let roomClosed = result.data
                if (!result.success) {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                } else if (roomClosed == true) {
                    //self.leaveAction?(.leave, self.viewModel.room)
                } else {
                    self.renderToolbar()
                }
            })
            .disposed(by: disposeBag)

        viewModel.actionsSource()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                switch self.viewModel.role {
                case .manager:
                    hosterToolbar?.onReceivedAction(result)
                case .leftSpeaker, .rightSpeaker:
                    speakerToolbar?.onReceivedAction(result)
                case .listener:
                    listenerToolbar?.onReceivedAction(result)
                }
            })
            .disposed(by: disposeBag)
        
        viewModel.subscribeMessages()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if (!result.success) {
                    self.show(message: result.message ?? "unknown error".localized, type: .error)
                } else {
                    self.chatListView.reloadData()
                }
            })
            .disposed(by: disposeBag)
        
        viewModel.onTopListenersChange
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] _ in
                self.avatar2.isHidden = self.viewModel.topListeners[2]??.id == nil
                self.avatar1.isHidden = self.viewModel.topListeners[1]??.id == nil
                self.avatar0.isHidden = self.viewModel.topListeners[0]??.id == nil
                let bundle = Bundle(identifier: "io.agora.InteractivePodcast")!
                if (!self.avatar2.isHidden) {
                    if let avatar = self.viewModel.topListeners[0]??.getLocalAvatar() {
                        self.avatar2.image = UIImage(named: avatar, in: bundle, with: nil)
                    }
                    if let avatar = self.viewModel.topListeners[1]??.getLocalAvatar() {
                        self.avatar1.image = UIImage(named: avatar, in: bundle, with: nil)
                    }
                    if let avatar = self.viewModel.topListeners[2]??.getLocalAvatar() {
                        self.avatar0.image = UIImage(named: avatar, in: bundle, with: nil)
                    }
                } else if (!self.avatar1.isHidden) {
                    if let avatar = self.viewModel.topListeners[0]??.getLocalAvatar() {
                        self.avatar1.image = UIImage(named: avatar, in: bundle, with: nil)
                    }
                    if let avatar = self.viewModel.topListeners[1]??.getLocalAvatar() {
                        self.avatar0.image = UIImage(named: avatar, in: bundle, with: nil)
                    }
                } else if (!self.avatar0.isHidden) {
                    if let avatar = self.viewModel.topListeners[0]??.getLocalAvatar() {
                        self.avatar0.image = UIImage(named: avatar, in: bundle, with: nil)
                    }
                }
            })
            .disposed(by: disposeBag)
        
        viewModel.onMemberEnter
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] member in
                if let member = member {
                    self.onMemberEnter(member: member)
                }
            })
            .disposed(by: disposeBag)
    }
    
    private func onMemberEnter(member: Member) {
        Logger.log(message: "\(member.user.name) enter the live room", level: .info)
        let view = MemberEnterView()
        view.member = member
        
        self.view.addSubview(view)
        view.marginLeading(anchor: self.view.leadingAnchor, constant: 12)
            .marginTop(anchor: self.chatListView.topAnchor, constant: 17)
            .width(constant: 257)
            .height(constant: 24, relation: .greaterOrEqual)
            .active()
        
        let translationY: CGFloat = 257
        view.transform = CGAffineTransform(translationX: translationY, y: 0)
        view.alpha = 0
        UIView.animate(withDuration: 0.2, delay: 0, options: .curveEaseInOut, animations: {
            view.alpha = 1
            view.transform = CGAffineTransform(translationX: 0, y: 0)
        }, completion: { success in
            UIView.animate(withDuration: 0.2, delay: 1.5, options: .curveEaseInOut, animations: {
                view.alpha = 0
                view.transform = CGAffineTransform(translationX: -translationY, y: 0)
            }, completion: { _ in
                view.removeFromSuperview()
            })
        })
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        inputMessageView.endEditing(true)
        //inputMessageRootView.isHidden = true
    }
    
    func enableInputMessage() {
        //inputMessageRootView.isHidden = false
        inputMessageView.becomeFirstResponder()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        hostRootView.roundCorners([.topLeft, .topRight], radius: 12)
        leftRootView.roundCorners([.topLeft, .bottomLeft], radius: 12)
        rightRootView.roundCorners([.topRight, .bottomRight], radius: 12)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        hosterVideoView.delegate = self
        hosterVideoView.member = viewModel.speakers.hoster
        leftSpeakerVideoView.delegate = self
        leftSpeakerVideoView.member = viewModel.speakers.leftSpeaker
        rightSpeakerVideoView.delegate = self
        rightSpeakerVideoView.member = viewModel.speakers.rightSpeaker
        
        let id = NSStringFromClass(MessageView.self)
        chatListView.register(MessageView.self, forCellReuseIdentifier: id)
        chatListView.dataSource = self
        chatListView.rowHeight = UITableView.automaticDimension
        chatListView.estimatedRowHeight = 30
        chatListView.separatorStyle = .none
        
        inputMessageView.returnKeyType = .send
        inputMessageView.delegate = self
        
        renderToolbar()
        subcribeUIEvent()
        subcribeRoomEvent()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        self.navigationController?.interactivePopGestureRecognizer?.delegate = self
    }
    
    public static func instance() -> RoomController {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        let controller = storyBoard.instantiateViewController(withIdentifier: "RoomController") as! RoomController
        return controller
    }
}

extension RoomController: RoomControlDelegate {
    func onTap(view: RoleVideoView) {
        if (viewModel.isManager) {
            if (view === leftSpeakerVideoView) {
                if let member = view.member {
                    if (member.id != viewModel.member.id) {
                        ManageSpeakerDialog().show(with: member, delegate: self)
                    }
                } else {
                    //InviteLeftSpeakerDialog
                }
            } else if (view === rightSpeakerVideoView) {
                if let member = view.member {
                    if (member.id != viewModel.member.id) {
                        ManageSpeakerDialog().show(with: member, delegate: self)
                    }
                } else {
                    //InviteRightSpeakerDialog
                }
            }
        } else if (viewModel.role == .listener) {
            var left: Bool? = nil
            if (view === leftSpeakerVideoView && view.member == nil) {
                left = true
            } else if (view === rightSpeakerVideoView && view.member == nil) {
                left = false
            }
            if let left = left {
                self.showAlert(title: "申请连麦", message: "红娘同意后会自动开始视频连麦")
                    .filter { ok in
                        ok
                    }
                    .flatMap { [unowned self] _ -> Observable<Result<Void>> in
                        return self.viewModel.handsup(left: left)
                    }
                    .subscribe(onNext: { [unowned self] result in
                        if (!result.success) {
                            self.show(message: result.message ?? "unknown error".localized, type: .error, duration: 1.5)
                        }
                    })
                    .disposed(by: disposeBag)
            }
        }
    }
}

extension RoomController: UIGestureRecognizerDelegate {
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        false
    }
}

extension RoomController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        self.viewModel.messageList.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let identifier = NSStringFromClass(MessageView.self)
        let cell = tableView.dequeueReusableCell(withIdentifier: identifier, for: indexPath) as! MessageView
        cell.message = self.viewModel.messageList[indexPath.row]
        return cell
    }
}

extension RoomController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if let message = textField.text {
            textField.text = nil
            if (!message.isEmpty) {
                self.viewModel
                    .sendMessage(message: message)
                    .subscribe(onNext: { [unowned self] result in
                        if (!result.success) {
                            self.show(message: result.message ?? "unknown error".localized, type: .error)
                        }
                    })
                    .disposed(by: disposeBag)
            }
        }
        textField.endEditing(true)
        return true
    }
}
