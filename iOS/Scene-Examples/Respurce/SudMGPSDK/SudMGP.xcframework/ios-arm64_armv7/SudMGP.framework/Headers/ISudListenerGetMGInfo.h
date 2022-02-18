#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@class GameInfo;

typedef void (^ISudListenerGetMGInfo)(int retCode, const NSString* retMsg, GameInfo* gameInfo);

NS_ASSUME_NONNULL_END