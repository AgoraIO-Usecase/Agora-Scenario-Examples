//
//  CreateLiveViewController.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/16.
//

import AgoraRtcKit

protocol CDNCreateLiveDelegate: NSObjectProtocol {
    func createLiveVC(_ vc: CDNCreateLiveViewController,
                      didSart roomName: String,
                      sellectedType: CDNCreateLiveViewController.SelectedType)
}

class CDNCreateLiveViewController: BaseViewController {
    let createLiveView = CDNCreateLiveView(frame: .zero)
    private var appId: String!
    private var rtcKit: AgoraRtcEngineKit!
    weak var delegate: CDNCreateLiveDelegate?
    
    public init(appId: String) {
        self.appId = appId
        self.rtcKit = AgoraRtcEngineKit.sharedEngine(withAppId: appId,
                                                     delegate: nil)
        rtcKit.enableVideo()
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setup()
        commonInit()
    }
    
    private func setup() {
        view.addSubview(createLiveView)
        createLiveView.frame = view.bounds
    }
    
    private func commonInit() {
        renderLocalVideo(view: createLiveView.cameraPreview)
        createLiveView.delegate = self
        genRandomName()
    }
    
    func genRandomName() {
        let text: String = .randomRoomName
        createLiveView.set(text: text)
    }
    
    func renderLocalVideo(view: UIView) {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = 0
        canvas.view = view
        rtcKit.setupLocalVideo(canvas)
        rtcKit.startPreview()
    }
    
    func switchCamera() {
        rtcKit.switchCamera()
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}

extension CDNCreateLiveViewController: CreateLiveViewDelegate {
    func createLiveViewDidTapCloseButton(_ view: CDNCreateLiveView) {
        dismiss(animated: true, completion: nil)
    }
    
    func createLiveViewDidTapCameraButton(_ view: CDNCreateLiveView) {
        switchCamera()
    }
    
    func createLiveViewDidTapStartButton(_ view: CDNCreateLiveView) {
        let text = createLiveView.text
        let sellectedType: SelectedType = createLiveView.currentSelectedType == .value1 ? .value1 : .value2
        dismiss(animated: true) { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.createLiveVC(self,
                                        didSart: text,
                                        sellectedType: sellectedType)
        }
    }
    
    func createLiveViewDidTapRandomButton(_ view: CDNCreateLiveView) {
        genRandomName()
    }
}

extension CDNCreateLiveViewController {
    enum SelectedType {
        case value1
        case value2
    }
}
