//
//  GameCenterView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit
import AgoraUIKit_iOS

class GameCenterView: UIView {
    var didGameCenterItemClosure: ((GameCenterModel) -> Void)?
    private lazy var backButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(systemName: "chevron.backward")?.withTintColor(.black, renderingMode: .alwaysOriginal), for: .normal)
        button.addTarget(self, action: #selector(clickBackButton), for: .touchUpInside)
        return button
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "游戏中心"
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray
        return view
    }()
    public lazy var collectionLayout: AGECollectionView = {
        let view = AGECollectionView()
        let w = (Screen.width - 15 * 5) / 4
        view.itemSize = CGSize(width: w, height: 123)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 15
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 0)
        view.delegate = self
        view.emptyTitle = "没有游戏"
        view.scrollDirection = .horizontal
        view.showsHorizontalScrollIndicator = false
        view.register(GameModeViewCell.self,
                      forCellWithReuseIdentifier: GameModeViewCell.description())
        return view
    }()
    private var sceneType: SceneType = .game
    private var dataArray: [GameCenterModel]? {
        didSet {
            collectionLayout.dataArray = dataArray
        }
    }
    
    init(sceneType: SceneType) {
        super.init(frame: .zero)
        self.sceneType = sceneType
        setupUI()
        getGameList()
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        getGameList()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func getGameList() {
//        GameViewModel.shared.getGameList(sceneType: sceneType) { [weak self] list in
//            guard let list = list else { return }
//            self?.dataArray = list
//        }
//        return
//        SudMGP.getMGList { code, error, value in
//            let dict = JSONObject.toDictionary(jsonString: value)
//            let data = dict["data"] as? [String: Any]
//            let list = data?["mg_info_list"] as? [[String: Any]]
//            print(list)
//        }
        var tempArray = [GameCenterModel]()
        for i in 0..<16 {
            var model = GameCenterModel()
            switch i {
            case 0: model.gameId = .bumper
            case 1: model.gameId = .knife
            case 2: model.gameId = .draw_and_guess
            case 3: model.gameId = .gobang
            case 4: model.gameId = .ludo
            case 5: model.gameId = .reversi
            case 6: model.gameId = .skating
            case 7: model.gameId = .roll
            case 8: model.gameId = .rsp
            case 9: model.gameId = .number_bomb
            case 10: model.gameId = .mine
            case 11: model.gameId = .sayGuess
            case 12: model.gameId = .teenPatti
            case 13: model.gameId = .UMO
            case 14: model.gameId = .deminers
            case 15: model.gameId = .TWMahjong
            default: break
            }
            model.gameName = model.gameId.title
            model.sources = .sud
            tempArray.append(model)
        }
        self.dataArray = tempArray
    }
    
    private func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 15
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMinXMaxYCorner]
        layer.masksToBounds = true
        translatesAutoresizingMaskIntoConstraints = false
        backButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        collectionLayout.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(backButton)
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(collectionLayout)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        backButton.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        backButton.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.centerYAnchor.constraint(equalTo: backButton.centerYAnchor).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        lineView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        collectionLayout.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionLayout.topAnchor.constraint(equalTo: lineView.bottomAnchor, constant: 15).isActive = true
        collectionLayout.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionLayout.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor).isActive = true
        collectionLayout.heightAnchor.constraint(equalToConstant: 123).isActive = true
    }
    
    @objc
    private func clickBackButton() {
        AlertManager.hiddenView()
    }
}
extension GameCenterView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: GameModeViewCell.description(),
                                                      for: indexPath) as! GameModeViewCell
        guard let model = collectionLayout.dataArray?[indexPath.item] as? GameCenterModel else { return cell }
        cell.setupGameCenterData(model: model)
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let model = collectionLayout.dataArray?[indexPath.item] as? GameCenterModel else { return }
        didGameCenterItemClosure?(model)
    }
}
