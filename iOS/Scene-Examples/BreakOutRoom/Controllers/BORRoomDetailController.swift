//
//  BORRoomDetailController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit
import AgoraRtcKit

class BORRoomDetailController: BaseViewController {
    private lazy var segmentView: SegmentView = {
        let segmentView = SegmentView(frame: CGRect(x: 0, y: 0, width: 300, height: 44), segmentStyle: .init(), titles: ["agjak", "gsaga", "agqgqwgg", "qweqwe", "gqwgb"])
        segmentView.style.indicatorStyle = .line
        segmentView.style.indicatorHeight = 2
        segmentView.style.indicatorColor = .red
        segmentView.style.indicatorWidth = 50
        segmentView.selectedTitleColor = .red
        segmentView.normalTitleColor = .gray
        segmentView.valueChange = { [weak self] index in
            guard let self = self, self.segmentView.titles.count > index else { return }
            self.leaveChannel()
            self.dataArray.removeAll()
            self.createAgoraVideoCanvas(uid: UserInfo.userId)
            let channelName = self.segmentView.titles[index]
            self.joinChannel(channelName: self.ownerId + channelName)
        }
        return segmentView
    }()
    private lazy var videoView: BaseCollectionViewLayout = {
        let view = BaseCollectionViewLayout()
        let viewW = self.view.frame.width
        view.itemSize = CGSize(width: viewW / 2, height: viewW / 2)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 0
        view.delegate = self
        view.scrollDirection = .vertical
        view.register(BORVideoRoomDetailCell.self,
                      forCellWithReuseIdentifier: BORVideoRoomDetailCell.description())
        return view
    }()
    private lazy var voiceButton: UIButton = {
        let button = UIButton()
        button.setBackgroundImage(UIImage(systemName: "mic.circle"), for: .normal)
        button.setBackgroundImage(UIImage(systemName: "mic.slash.circle")?.withTintColor(.red, renderingMode: .alwaysOriginal), for: .selected)
        button.addTarget(self, action: #selector(clickVoiceButton(sender:)), for: .touchUpInside)
        return button
    }()
    private var isJoined: Bool = false
    private var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
       let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.channelProfile = .liveBroadcasting
        config.areaCode = .global
        config.channelProfile = .liveBroadcasting
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
       let option = AgoraRtcChannelMediaOptions()
        option.publishAudioTrack = .of(true)
        option.clientRoleType = .of((Int32)(AgoraClientRole.broadcaster.rawValue))
        return option
    }()
    private var channleName: String = ""
    private var ownerId: String = ""
    private var id: String = ""
    private var dataArray = [LiveCanvasModel]()
    private var homeDataArray = [BORLiveModel]()
    
    init(channelName: String, ownerId: String) {
        super.init(nibName: nil, bundle: nil)
        self.channleName = channelName
        self.ownerId = ownerId
        self.id = channelName
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupUI()
        setupAgoraKit()
        createAgoraVideoCanvas(uid: UserInfo.userId, isLocal: true)
        // 设置屏幕常亮
        UIApplication.shared.isIdleTimerDisabled = true
        
        segmentView.titles = [channleName]
        
        SyncUtil.fetchCollection(id: id, className: SYNC_COLLECTION_SUB_ROOM, delegate: self)
        
        SyncUtil.subscribeCollection(id: id,
                                     className: SYNC_COLLECTION_SUB_ROOM,
                                     delegate: self)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        joinChannel(channelName: ownerId + channleName)
        var controllers = navigationController?.viewControllers ?? []
        guard let index = controllers.firstIndex(where: { $0 is BORCreateRoomController }) else { return }
        controllers.remove(at: index)
        navigationController?.viewControllers = controllers
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        agoraKit?.stopPreview()
        leaveChannel()
        SyncUtil.unsubscribeCollection(id: id, className: SYNC_COLLECTION_SUB_ROOM)
        SyncUtil.leaveScene(id: id)
    }
    
    private func setupUI() {
        view.backgroundColor = .white
        navigationItem.titleView = segmentView
        navigationItem.rightBarButtonItem = UIBarButtonItem(image: UIImage(systemName: "plus.circle")?.withTintColor(.red, renderingMode: .alwaysOriginal), style: .plain, target: self, action: #selector(clickAddButton))
        videoView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(videoView)
        
        videoView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        videoView.topAnchor.constraint(equalTo: view.layoutMarginsGuide.topAnchor).isActive = true
        videoView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        videoView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        
        segmentView.titles = [channleName]
        
        view.addSubview(voiceButton)
        voiceButton.translatesAutoresizingMaskIntoConstraints = false
        voiceButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15).isActive = true
        voiceButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -25).isActive = true
        voiceButton.widthAnchor.constraint(equalToConstant: 50).isActive = true
        voiceButton.heightAnchor.constraint(equalToConstant: 50).isActive = true
    }
    
    private func setupAgoraKit() {
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: rtcEngineConfig, delegate: self)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.enableVideo()
        agoraKit?.setVideoEncoderConfiguration(
            AgoraVideoEncoderConfiguration(size: CGSize(width: 320, height: 240),
                                           frameRate: .fps30,
                                           bitrate: AgoraVideoBitrateStandard,
                                           orientationMode: .fixedPortrait,
                                           mirrorMode: .auto))
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String) {
        self.channleName = channelName
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channleName,
                                           uid: UserInfo.userId,
                                           mediaOptions: channelMediaOptions)
        guard result != 0 else { return }
        // Error code description can be found at:
        // en: https://docs.agora.io/en/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        // cn: https://docs.agora.io/cn/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        self.showAlert(title: "Error", message: "joinChannel call failed: \(String(describing: result)), please check your params")
    }
    private func leaveChannel() {
        agoraKit?.leaveChannel({ state in
            LogUtils.log(message: "left channel, duration: \(state.duration)", level: .info)
        })
    }
        
    private func createAgoraVideoCanvas(uid: UInt, isLocal: Bool = false) {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.renderMode = .hidden
        let model = LiveCanvasModel()
        model.canvas = canvas
        dataArray.append(model)
        videoView.dataArray = dataArray
    }
    
    @objc
    private func clickAddButton() {
        showTextFieldAlert(title: "Please enter a subroom name", message: "") { [weak self] text in
            guard text.count < 11 else {
                self?.showHUDError(error: "Over length limit")
                return
            }
            guard !text.isChinese(str: text) else {
                self?.showHUDError(error: "Chinese not supported")
                return
            }
            let roomModel = BORSubRoomModel(subRoom: text)
            let roomParams = JSONObject.toJson(roomModel)
            SyncUtil.addCollection(id: self?.id ?? "",
                                   className: SYNC_COLLECTION_SUB_ROOM,
                                   params: roomParams,
                                   delegate: nil)
        }
    }
    
    @objc
    private func clickVoiceButton(sender: UIButton) {
        sender.isSelected = !sender.isSelected
        agoraKit?.muteLocalAudioStream(sender.isSelected)
    }
}
extension BORRoomDetailController: BaseCollectionViewLayoutDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: BORVideoRoomDetailCell.description(),
                                                      for: indexPath) as! BORVideoRoomDetailCell
        cell.setupItemData(with: videoView.dataArray?[indexPath.item])
        let model = dataArray[indexPath.item]
        if indexPath.item == 0 {
            agoraKit?.setupLocalVideo(model.canvas)
            agoraKit?.startPreview()
        } else {
            agoraKit?.setupRemoteVideo(model.canvas ?? AgoraRtcVideoCanvas())
        }
        cell.clickMuteButtonClosure = { [weak self] isMute in
            guard let self = self else { return }
            if indexPath.item == 0 {
                self.agoraKit?.muteLocalAudioStream(isMute)
            } else {
                self.agoraKit?.muteRemoteAudioStream(model.canvas?.uid ?? 0, mute: isMute)
            }
            collectionView.reloadData()
        }
        return cell
    }
}
extension BORRoomDetailController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        LogUtils.log(message: "warning: \(warningCode.description)", level: .warning)
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        LogUtils.log(message: "error: \(errorCode)", level: .error)
        showAlert(title: "Error", message: "Error \(errorCode.description) occur")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        isJoined = true
        LogUtils.log(message: "Join \(channel) with uid \(uid) elapsed \(elapsed)ms", level: .info)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        LogUtils.log(message: "remote user join: \(uid) \(elapsed)ms", level: .info)
        createAgoraVideoCanvas(uid: uid)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        LogUtils.log(message: "remote user leval: \(uid) reason \(reason)", level: .info)
        guard !dataArray.isEmpty else { return }
        let index = dataArray.firstIndex(where: { $0.canvas?.uid == uid }) ?? 0
        dataArray.remove(at: index)
        videoView.dataArray = dataArray
        // update online number
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, reportRtcStats stats: AgoraChannelStats) {
//        localVideo.statsInfo?.updateChannelStats(stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, localVideoStats stats: AgoraRtcLocalVideoStats) {
//        localVideo.statsInfo?.updateLocalVideoStats(stats)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, localAudioStats stats: AgoraRtcLocalAudioStats) {
//        localVideo.statsInfo?.updateLocalAudioStats(stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteVideoStats stats: AgoraRtcRemoteVideoStats) {
//        remoteVideo.statsInfo?.updateVideoStats(stats)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteAudioStats stats: AgoraRtcRemoteAudioStats) {
//        remoteVideo.statsInfo?.updateAudioStats(stats)
    }
}

extension BORRoomDetailController: IObjectListDelegate {
    func onSuccess(result: [IObject]) {
        SyncUtil.subscribeCollection(id: id,
                                     className: SYNC_COLLECTION_SUB_ROOM,
                                     delegate: self)
        let subRooms = result.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(BORSubRoomModel.self, value: $0) }).sorted { s1, s2 in
                return s1.createTime < s2.createTime
            }
        let titles = subRooms.compactMap({ $0.subRoom })
        guard !titles.isEmpty else { return }
        segmentView.titles = segmentView.titles + titles
    }
    
    func onFailed(code: Int, msg: String) {
        LogUtils.log(message: "onFailed == \(msg)  code === \(code)", level: .error)
    }
}

extension BORRoomDetailController: ISyncManagerEventDelegate {
    func onCreated(object: IObject) {
        var titles = segmentView.titles
        let roomModel = JSONObject.toModel(BORSubRoomModel.self, value: object.toJson())
        guard !titles.contains(roomModel?.subRoom ?? "") else { return }
        titles.append(roomModel?.subRoom ?? "")
        segmentView.titles = titles
        print("onCreated == \(object)")
    }
    
    func onUpdated(object: IObject) {
        print("onUpdated == \(object)")
        var titles = segmentView.titles
        let roomModel = JSONObject.toModel(BORSubRoomModel.self, value: object.toJson())
        guard !titles.contains(roomModel?.subRoom ?? "") else { return }
        titles.append(roomModel?.subRoom ?? "")
        segmentView.titles = titles
    }
    
    func onDeleted(object: IObject?) {
        print("onDeleted == \(String(describing: object?.toJson()))")
        
    }
    func onDeleted(objectId: String) {
    
    }
    
    func onSubscribed() {
        print("onSubscribed")
    }
    
    func onError(code: Int, msg: String) {
        print("onError  code == \(code) msg === \(msg)")
        showAlert(title: "code == \(code) msg == \(msg)", message: "")
    }
}
