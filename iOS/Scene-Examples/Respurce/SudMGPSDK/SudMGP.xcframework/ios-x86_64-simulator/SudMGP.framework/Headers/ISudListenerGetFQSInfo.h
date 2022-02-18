#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@class GameInfo;

typedef void (^ISudListenerGetFQSInfo)(int retCode, const NSString* retMsg, NSString* sdkTokenUrl);

NS_ASSUME_NONNULL_END