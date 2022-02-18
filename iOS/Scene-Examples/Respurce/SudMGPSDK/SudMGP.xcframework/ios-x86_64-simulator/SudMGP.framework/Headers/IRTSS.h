
//
//  Created by Herbert on 2021/10/21.
//

#import <Foundation/Foundation.h>

@protocol IRTSSEventHandler;

@interface IRTSSStartOption : NSObject
@property (nonatomic, copy) NSArray      * keywordList;
@property (nonatomic, copy) NSString     * wordLanguage;
@property (nonatomic, copy) NSString     * wordType;
@property (nonatomic, assign) BOOL         enableIsHit;
@property (nonatomic, assign) BOOL         enableIsReturnText;
@property (nonatomic, copy) NSDictionary * extra;
@end


/*
 * 接口要确保在UI线程中调用
 */
@protocol IRTSS <NSObject>



/// 初始化SDK
/// @param organization <#organization description#>
/// @param accessKey <#accessKey description#>
/// @param appId <#appId description#>
/// @param wsURL <#wsURL description#>
/// @param httpUrl <#httpUrl description#>
- (void)init:(NSString *)organization accessKey:(NSString *)accessKey appId:(NSString *)appId wsURL:(NSString*)wsURL httpUrl:(NSString*)httpUrl;


- (BOOL)start:(IRTSSStartOption *)option listener:(id<IRTSSEventHandler>)listener;

/// 传入的音频数据必须是：PCM格式，采样率：16000， 采样位数：16， 声道数： MONO
/// @param data PCM数据
- (void)push:(NSData *)data;


/// 断开连接，如果想继续工作，必须重新调用start()新建连接
- (void)stop;


/// 销毁SDK
- (void)destroy;

@end
