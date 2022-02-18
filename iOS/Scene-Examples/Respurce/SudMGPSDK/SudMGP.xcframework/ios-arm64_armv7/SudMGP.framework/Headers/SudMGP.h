#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "ISudListenerInitSDK.h"
#import "ISudListenerGetMGList.h"
#import "ISudListenerUninitSDK.h"

NS_ASSUME_NONNULL_BEGIN

@protocol ISudFSTAPP;
@protocol ISudFSMMG;

@interface SudMGP : NSObject

/**
 * 获取SDK版本
 * @return 示例:"1.1.35.286"
 */
+ (NSString*_Nonnull)getVersion;

/**
 * 初始化SDK
 * @param context Context
 * @param appId 小游戏平台生成
 * @param appKey 小游戏平台生成
 * @param isTestEnv true:测试环境 false:生产环境
 * @param listener ISudListenerInitSDK
 */
+ (void)initSDK:(NSString*_Nonnull)appId
         appKey:(NSString*_Nonnull)appKey
      isTestEnv:(BOOL)isTestEnv
       listener:(ISudListenerInitSDK _Nullable )listener;

/**
 * 反初始化SDK
 * @param listener ISudListenerUninitSDK
 */
+ (void)uninitSDK:(ISudListenerUninitSDK _Nullable )listener;

/**
 * 获取游戏列表
 * @param listener ISudListenerGetMGList
 */
+ (void)getMGList:(ISudListenerGetMGList _Nullable )listener;

/**
 * 加载游戏
 * @param userId 用户ID，业务系统保证每个用户拥有唯一ID
 * @param roomId 房间ID，业务系统保证唯一性，进入同一房间内
 * @param code 短期令牌Code
 * @param mgId 小游戏ID，测试环境和生成环境小游戏ID是一致的
 * @param language 游戏语言 现支持，简体：zh-CN 繁体：zh-TW 英语：en-US 马来语：ms-MY
 * @param fsmMG ISudFSMMG
 * @param rootView 用于显示游戏的根视图
 * @return ISudFSTAPP
 */
+ (id<ISudFSTAPP>_Nonnull)loadMG:(NSString*_Nonnull)userId
                          roomId:(NSString*_Nonnull)roomId
                            code:(NSString*_Nonnull)code
                            mgId:(int64_t)mgId
                        language:(NSString*_Nonnull)language
                           fsmMG:(id<ISudFSMMG>_Nonnull)fsmMG
                        rootView:(UIView*_Nonnull)rootView;

/**
 * 销毁游戏
 * @param fstApp 加载游戏返回的对象ISudFSTAPP
 * @return boolean
 */
+ (bool)destroyMG:(id<ISudFSTAPP>_Nonnull) fstAPP;

/**
 * 设置日志等级
 * @param logLevel 输出log的等级,LogVERBOSE,LogDEBUG,LogINFO 见ISudLogger.h
 */
+ (void)setLogLevel:(int)logLevel;


/// 设置是否由SDK设置AudioSession
/// @param autoSet 是否由SDK自动设置
+ (void)autoSetAudioSession:(BOOL)autoSet;

@end

NS_ASSUME_NONNULL_END
