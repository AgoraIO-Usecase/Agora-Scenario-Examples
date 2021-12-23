//
//  LiveView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/23.
//

import UIKit
import AgoraUIKit
import AgoraRtcKit

class LiveBaseView: UIView {
    /// 点击发送消息
    var onClickSendMessageClosure: ((ChatMessageModel) -> Void)?
    /// 点击消息cell
    var onDidMessageCellClosure: ((ChatMessageModel) -> Void)?
    /// 关闭直播
    var onClickCloseLiveClosure: (() -> Void)?
    /// 点击切换摄像头
    var onClickSwitchCameraClosure: ((Bool) -> Void)?
    /// 点击暂停推理
    var onClickIsMuteCameraClosure: ((Bool) -> Void)?
    /// 麦克风是否静音
    var onClickIsMuteMicClosure: ((Bool) -> Void)?
    /// 点击PK邀请
    var onClickPKButtonClosure: (() -> Void)?
    /// 点击游戏
    var onClickGameButtonClosure: (() -> Void)?
    /// 点击退出游戏
    var onClickExitGameButtonClosure: (() -> Void)?
    /// 收到礼物
    var onReceivedGiftClosure: ((LiveGiftModel, PKLiveType) -> Void)?
    /// 设置本地视频画面
    var setupLocalVideoClosure: ((AgoraRtcVideoCanvas?) -> Void)?
    /// 设置远程直播画面
    var setupRemoteVideoClosure: ((AgoraRtcVideoCanvas, AgoraRtcConnection) -> Void)?
    
    enum LiveLayoutPostion {
        case full, center, bottom, signle
    }
    public lazy var liveCanvasView: AGECollectionView = {
        let view = AGECollectionView()
        view.itemSize = CGSize(width: Screen.width, height: Screen.height)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 0
        view.delegate = self
        view.scrollDirection = .vertical
        view.showsVerticalScrollIndicator = false
        view.isUserInteractionEnabled = false
        view.register(LivePlayerCell.self,
                      forCellWithReuseIdentifier: LivePlayerCell.description())
        return view
    }()
    /// 顶部头像昵称
    public lazy var avatarview = LiveAvatarView()
    /// 聊天
    public lazy var chatView = LiveChatView()
    /// 设置直播的工具弹窗
    private lazy var liveToolView = LiveToolView()
    /// 礼物
    private lazy var giftView = LiveGiftView()
    private lazy var onlineView = LiveOnlineView()
    public lazy var playGifView: GIFImageView = {
        let view = GIFImageView()
        view.isHidden = true
        return view
    }()
    /// 底部功能
    public lazy var bottomView: LiveBottomView = {
        let view = LiveBottomView(type: [.gift, .tool, .close])
        return view
    }()
    
    public var canvasDataArray = [LiveCanvasModel]()
    private var channelName: String = ""
    private var currentUserId: String = ""
    private var canvasLeadingConstraint: NSLayoutConstraint?
    private var canvasTopConstraint: NSLayoutConstraint?
    private var canvasTrailingConstraint: NSLayoutConstraint?
    private var canvasBottomConstraint: NSLayoutConstraint?
    private var chatViewTrailingConstraint: NSLayoutConstraint?
    private(set) var liveCanvasViewHeight: CGFloat = 0
    
    init(channelName: String, currentUserId: String) {
        super.init(frame: .zero)
        self.channelName = channelName
        self.currentUserId = currentUserId
        setupUI()
        eventHandler()
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        eventHandler()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    /// 设置主播昵称
    func setAvatarName(name: String) {
        avatarview.setName(with: name)
    }
    
    /// 更新底部功能按钮
    func updateBottomButtonType(type: [LiveBottomView.LiveBottomType]) {
        bottomView.updateButtonType(type: type)
    }
    
    func setupCanvasData(data: LiveCanvasModel) {
        canvasDataArray.append(data)
        liveCanvasView.dataArray = canvasDataArray
    }
    
    func sendMessage(message: String, messageType: ChatMessageType) {
        let model = ChatMessageModel(message: message, messageType: messageType)
        chatView.sendMessage(messageModel: model)
    }
    
    func reloadData() {
        liveCanvasView.reloadData()
    }
    
    func removeData(index: Int) {
        canvasDataArray.remove(at: index)
        liveCanvasView.dataArray = canvasDataArray
    }
    
    /// 监听礼物
    func subscribeGift(channelName: String, type: PKLiveType) {
        SyncUtil.subscribe(id: channelName, key: SYNC_MANAGER_GIFT_INFO, delegate: LiveGiftDelegate(view: self, type: type))
    }
    
    /// 更新直播布局
    func updateLiveLayout(postion: LiveLayoutPostion) {
        var leading: CGFloat = 0
        var top: CGFloat = -Screen.statusHeight()
        var bottom: CGFloat = Screen.safeAreaBottomHeight()
        var trailing: CGFloat = 0
        var itemWidth: CGFloat = Screen.width
        var itemHeight: CGFloat = Screen.height
        switch postion {
        case .bottom:
            let viewW = Screen.width
            itemWidth = ((viewW * 0.5) - 15) * 0.5
            itemHeight = viewW / 2 * 0.7
            let topMargin = frame.height - itemHeight - 78
            leading = viewW * 0.5
            top = topMargin
            bottom = -70
            trailing = -15
            chatViewTrailingConstraint?.constant = -(leading - 10)
            
        case .center:
            let viewW = Screen.width
            itemWidth = viewW / 2
            itemHeight = viewW / 2 * 1.2
            top = Screen.kNavHeight + 40
            guard let cons = chatViewTrailingConstraint else { return }
            chatView.removeConstraint(cons)
            
        case .signle:
            let viewW = Screen.width
            itemWidth = 90
            itemHeight = viewW / 2 * 0.7
            let topMargin = frame.height - itemHeight - 78
            leading = viewW - (itemWidth + 15)
            top = topMargin
            bottom = -70
            trailing = -15
            chatViewTrailingConstraint?.constant = -(itemWidth + 10)
            
        default:
            let chatViewW = Screen.width / 2 * 0.9
            chatViewTrailingConstraint?.constant = -chatViewW
        }
        liveCanvasViewHeight = top + itemHeight
        canvasLeadingConstraint?.constant = leading
        canvasTopConstraint?.constant = top
        canvasBottomConstraint?.constant = bottom
        canvasTrailingConstraint?.constant = trailing
        UIView.animate(withDuration: 0.5) {
            self.canvasTopConstraint?.isActive = true
            self.canvasBottomConstraint?.isActive = true
            self.canvasTrailingConstraint?.isActive = true
            self.canvasLeadingConstraint?.isActive = true
            self.liveCanvasView.itemSize = CGSize(width: itemWidth,
                                                  height: itemHeight)
            self.chatViewTrailingConstraint?.isActive = true
        }
    }
    
    private func eventHandler() {
        // gif播放完成回调
        playGifView.gifAnimationFinishedClosure = { [weak self] in
            guard let self = self else { return }
            self.playGifView.isHidden = true
        }
        // 聊天发送
        bottomView.clickChatButtonClosure = { [weak self] message in
            guard let self = self else { return }
            let model = ChatMessageModel(message: message, messageType: .message)
            self.chatView.sendMessage(messageModel: model)
            self.onClickSendMessageClosure?(model)
        }
        // 点击聊天消息
        chatView.didSelectRowAt = { [weak self] messageModel in
            self?.onDidMessageCellClosure?(messageModel)
        }
        // 底部功能回调
        bottomView.clickBottomButtonTypeClosure = { [weak self] type in
            guard let self = self else { return }
            switch type {
            case .close:
                self.onClickCloseLiveClosure?()
                
            case .tool:
                self.liveToolView.clickItemClosure = { itemType, isSelected in
                    switch itemType {
                    case .switch_camera:
                        self.onClickSwitchCameraClosure?(isSelected)
                        
                    case .camera:
                        self.onClickIsMuteCameraClosure?(isSelected)
                        self.liveCanvasView.isHidden = isSelected
                    
                    case .mic:
                        self.onClickIsMuteMicClosure?(isSelected)
                    }
                }
                AlertManager.show(view: self.liveToolView, alertPostion: .bottom)
            
            case .gift:
                self.giftView.clickGiftItemClosure = { giftModel in
                    LogUtils.log(message: "gif == \(giftModel.gifName)", level: .info)
                    let params = JSONObject.toJson(giftModel)
                    /// 发送礼物
                    SyncUtil.update(id: self.channelName,
                                    key: SYNC_MANAGER_GIFT_INFO,
                                    params: params,
                                    delegate: nil)
                }
                AlertManager.show(view: self.giftView, alertPostion: .bottom)
            case .pk:
                self.onClickPKButtonClosure?()
                
            case .game:
                self.onClickGameButtonClosure?()
                
            case .exitgame:
                self.onClickExitGameButtonClosure?()
            }
        }
        
        subscribeGift(channelName: channelName, type: .me)
        
        // 收到礼物
        LiveReceivedGiftClosure = onReceivedGiftClosure
    }
    
    private func setupUI() {
        liveCanvasView.translatesAutoresizingMaskIntoConstraints = false
        avatarview.translatesAutoresizingMaskIntoConstraints = false
        chatView.translatesAutoresizingMaskIntoConstraints = false
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        playGifView.translatesAutoresizingMaskIntoConstraints = false
        onlineView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(liveCanvasView)
        addSubview(avatarview)
        addSubview(chatView)
        addSubview(bottomView)
        addSubview(playGifView)
        addSubview(onlineView)
        
        canvasLeadingConstraint = liveCanvasView.leadingAnchor.constraint(equalTo: leadingAnchor)
        canvasTopConstraint = liveCanvasView.topAnchor.constraint(equalTo: topAnchor, constant: -Screen.statusHeight())
        canvasBottomConstraint = liveCanvasView.bottomAnchor.constraint(equalTo: bottomAnchor)
        canvasTrailingConstraint = liveCanvasView.trailingAnchor.constraint(equalTo: trailingAnchor)
        canvasTopConstraint?.isActive = true
        canvasBottomConstraint?.isActive = true
        canvasLeadingConstraint?.isActive = true
        canvasTrailingConstraint?.isActive = true
        
        avatarview.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        avatarview.topAnchor.constraint(equalTo: topAnchor, constant: Screen.statusHeight() + 15).isActive = true
        
        chatView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        chatView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -60).isActive = true
        let chatViewW = Screen.width / 2 * 0.9
        chatViewTrailingConstraint = chatView.trailingAnchor.constraint(equalTo: liveCanvasView.trailingAnchor, constant: -chatViewW)
        chatViewTrailingConstraint?.isActive = true
        chatView.heightAnchor.constraint(equalToConstant: chatViewW).isActive = true
        
        bottomView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        bottomView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        bottomView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        
        playGifView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        playGifView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        playGifView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        playGifView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        onlineView.topAnchor.constraint(equalTo: avatarview.topAnchor).isActive = true
        onlineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        
        setAvatarName(name: currentUserId)
        
        let bottomType: [LiveBottomView.LiveBottomType] = currentUserId == UserInfo.uid ? [.tool, .close] : [.gift, .close]
        bottomView.updateButtonType(type: bottomType)
    }
}

extension LiveBaseView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LivePlayerCell.description(),
                                                      for: indexPath) as! LivePlayerCell
        if indexPath.item >= canvasDataArray.count {
            return cell
        }
        let model = canvasDataArray[indexPath.item]
        cell.setupPlayerCanvas(with: model)
        
        if indexPath.item == 0 && currentUserId == UserInfo.uid {// 本房间主播
            setupLocalVideoClosure?(model.canvas)
            
        } else { // 观众
            if let connection = model.connection, let canvas = model.canvas {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    self.setupRemoteVideoClosure?(canvas, connection)
                }
            }
        }
        return cell
    }
}
