#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@protocol ISudFSMStateHandle;

@protocol ISudFSMMG <NSObject>

/// 游戏日志
/// 最低版本：v1.1.30.xx
-(void) onGameLog:(NSString*)dataJson;

/// 游戏开始
/// 最低版本：v1.1.30.xx
-(void) onGameStarted;

/// 游戏销毁
/// 最低版本：v1.1.30.xx
-(void) onGameDestroyed;

/// 短期令牌code过期
/// APP接入方需要调用handle.success或handle.fail
/// @param dataJson {"code":"value"}
-(void) onExpireCode:(id<ISudFSMStateHandle>)handle dataJson:(NSString*)dataJson;

/// 获取游戏View信息
/// APP接入方需要调用handle.success或handle.fail
/// @param handle ISudFSMStateHandle
/// @param dataJson {}
-(void) onGetGameViewInfo:(id<ISudFSMStateHandle>) handle dataJson:(NSString*)dataJson;

/// 获取游戏Config
/// APP接入方需要调用handle.success或handle.fail
/// @param handle ISudFSMStateHandle
/// @param dataJson dataJson
/// 最低版本：v1.1.30.xx
-(void) onGetGameCfg:(id<ISudFSMStateHandle>) handle dataJson:(NSString*)dataJson;

/// 游戏状态变化
/// APP接入方需要调用handle.success或handle.fail
/// @param handle ISudFSMStateHandle
/// @param state state
/// @param dataJson dataJson
-(void) onGameStateChange:(id<ISudFSMStateHandle>) handle state:(NSString*) state dataJson:(NSString*) dataJson;

/// 游戏玩家状态变化
/// APP接入方需要调用handle.success或handle.fail
/// @param handle ISudFSMStateHandle
/// @param userId userId
/// @param state state
/// @param dataJson dataJson
-(void) onPlayerStateChange:(nullable id<ISudFSMStateHandle>) handle userId:(NSString*) userId state:(NSString*) state dataJson:(NSString*) dataJson;
@end

NS_ASSUME_NONNULL_END