//
//  BORRoomDetailController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit
import AgoraRtcKit
//import AgoraSyncManager
import Agora_Scene_Utils

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
    private lazy var videoView: AGECollectionView = {
        let view = AGECollectionView()
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
        button.addTarget(self, action: #selector(onTapVoiceButton(sender:)), for: .touchUpInside)
        return button
    }()
    private var isJoined: Bool = false
    private var agoraKit: AgoraRtcEngineKit?
    private lazy var rtcEngineConfig: AgoraRtcEngineConfig = {
       let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        return config
    }()
    private lazy var channelMediaOptions: AgoraRtcChannelMediaOptions = {
       let option = AgoraRtcChannelMediaOptions()
        option.publishLocalAudio = true
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
        
        SyncUtil.scene(id: id)?.collection(className: SYNC_COLLECTION_SUB_ROOM).get(success: { results in
            SyncUtil.scene(id: self.id)?.collection(className: SYNC_COLLECTION_SUB_ROOM).document().subscribe(key: "", onCreated: { result in
                self.onCreated(result: result)
            }, onUpdated: { object in
                self.onUpdated(object: object)
            }, onDeleted: { object in
                
            }, onSubscribed: {
                
            }, fail: { error in
                ToastView.show(text: error.message)
            })
            let subRooms = results.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(BORSubRoomModel.self, value: $0) }).sorted { s1, s2 in
                    return s1.createTime < s2.createTime
                }
            let titles = subRooms.compactMap({ $0.subRoom })
            guard !titles.isEmpty else { return }
            self.segmentView.titles = self.segmentView.titles + titles
        }, fail: { error in
            ToastView.show(text: error.message)
        })
        SyncUtil.scene(id: id)?.collection(className: SYNC_COLLECTION_SUB_ROOM).document().subscribe(key: "", onCreated: { result in
            self.onCreated(result: result)
        }, onUpdated: { object in
            self.onUpdated(object: object)
        }, onDeleted: { object in
            
        }, onSubscribed: {
            print("onSubscribed")
        }, fail: { error in
            ToastView.show(text: error.message)
        })
    }
    
    private func onCreated(result: IObject) {
        var titles = segmentView.titles
        let roomModel = JSONObject.toModel(BORSubRoomModel.self, value: result.toJson())
        guard !titles.contains(roomModel?.subRoom ?? "") else { return }
        titles.append(roomModel?.subRoom ?? "")
        segmentView.titles = titles
        print("onCreated == \(result)")
    }
    
    private func onUpdated(object: IObject) {
        print("onUpdated == \(object)")
        var titles = self.segmentView.titles
        let roomModel = JSONObject.toModel(BORSubRoomModel.self, value: object.toJson())
        guard !titles.contains(roomModel?.subRoom ?? "") else { return }
        titles.append(roomModel?.subRoom ?? "")
        segmentView.titles = titles
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
        SyncUtil.scene(id: id)?.collection(className: SYNC_COLLECTION_SUB_ROOM).document().unsubscribe(key: "")
        SyncUtil.leaveScene(id: id)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        guard ownerId == UserInfo.uid else { return }
        SyncUtil.scene(id: id)?.delete(success: nil, fail: nil)
    }
    
    private func setupUI() {
        view.backgroundColor = .white
        navigationItem.titleView = segmentView
        navigationItem.rightBarButtonItem = UIBarButtonItem(image: UIImage(systemName: "plus.circle")?.withTintColor(.red, renderingMode: .alwaysOriginal), style: .plain, target: self, action: #selector(onTapAddButton))
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
        agoraKit?.setChannelProfile(.liveBroadcasting)
        agoraKit?.setLogFile(LogUtils.sdkLogPath())
        agoraKit?.setClientRole(.broadcaster)
        agoraKit?.enableVideo()
        agoraKit?.setVideoEncoderConfiguration(
            AgoraVideoEncoderConfiguration(size: CGSize(width: 320, height: 240),
                                           frameRate: .fps30,
                                           bitrate: AgoraVideoBitrateStandard,
                                           orientationMode: .fixedPortrait))
        /// 开启扬声器
        agoraKit?.setDefaultAudioRouteToSpeakerphone(true)
    }
    
    private func joinChannel(channelName: String) {
        self.channleName = channelName
        let result = agoraKit?.joinChannel(byToken: KeyCenter.Token,
                                           channelId: channleName,
                                           info: nil,
                                           uid: UserInfo.userId,
                                           options: channelMediaOptions)
        guard result != 0 else { return }
        // Error code description can be found at:
        // en: https://docs.agora.io/en/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        // cn: https://docs.agora.io/cn/Voice/API%20Reference/oc/Constants/AgoraErrorCode.html
        self.showAlert(title: "Error".localized, message: "joinChannel call failed: \(String(describing: result)), please check your params")
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
    private func onTapAddButton() {
        showTextFieldAlert(title: "Please_enter_a_subroom_name".localized, message: "") { [weak self] text in
            guard text.count < 11 else {
                ToastView.show(text: "Over_length_limit".localized)
                return
            }
            guard !text.isChinese(str: text) else {
                ToastView.show(text: "Chinese_not_supported".localized)
                return
            }
            let roomModel = BORSubRoomModel(subRoom: text)
            let roomParams = JSONObject.toJson(roomModel)
            SyncUtil.scene(id: self?.id ?? "")?.collection(className: SYNC_COLLECTION_SUB_ROOM).add(data: roomParams, success: { object in
                
            }, fail: { error in
                ToastView.show(text: error.message)
            })
        }
    }
    
    @objc
    private func onTapVoiceButton(sender: UIButton) {
        sender.isSelected = !sender.isSelected
        agoraKit?.muteLocalAudioStream(sender.isSelected)
    }
}
extension BORRoomDetailController: AGECollectionViewDelegate {
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
        cell.onTapMuteButtonClosure = { [weak self] isMute in
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
