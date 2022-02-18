//
//  SmRtAsrClient.h
//  SmRtAsr
//
//  Created by wangyankun on 2021/10/28.
//

#import <Foundation/Foundation.h>

/**
                uninit                  idle                                        sending
 create                     去到idle            不允许，回调错误            不允许，回调错误
 startSession           不允许              去到sending                          去到sending
 postAudio              不允许                 去到idle                                  sending
 stopSession           不允许                  去到idle                                  去到idle
 destroy                   unint                    unint                                       unint
 
 */

//SmRtAsrClient的配置文件
@interface SmRtAsrOption : NSObject

@property NSString *organization;
@property NSString *appId;
@property NSString *accessKey;
@property NSString *wsUrl;
@property NSString *httpSessionUrl;

@end


//定义response

@interface SmAsrResponse : NSObject

@property int code;
@property NSString * requestId;
@property NSString * tokenId;
@property NSString * sessionId;
@property int segId;
@property NSInteger type;
@property long startTime;
@property long endTime;
@property NSString * text;
@property NSString * eventId;
@property long wsTime;
@property long weTime;
@property long sessionStartTime;
@property long sessionEndTime;
@property long segStartTime;
@property long segEndTime;
@property NSArray<NSDictionary*> *matchedResults;
@property int requestSegId;
@property int responseSegId;
@property NSDictionary* raw;
//[
//{"hitItemContent":"","positions":[[2,9],[10,13]]},
//{"hitItemContent":"","positions":[[2,9],[10,13]]}
//]
@property NSArray<NSDictionary*> *numbers;

@property NSString *message;
@property NSDictionary *detail;//{"errorCode":1}


-(BOOL)isHit;

@end


//定义delegate
@protocol SmRtDelegate <NSObject>

@required -(void) onRtReceived:(NSString*) sessionId
                     requestId:(NSString*)requestId
                      response:(SmAsrResponse *)response;

@required -(void) onRtError:(NSString*) sessionId
                  requestId:(NSString*)requestId
                  errorCode:(NSInteger)errorCode
                    message:(NSString*)message;

@end


//定义session的config配置
@interface SmRtSessionConfig : NSObject

@property NSArray<NSString*> *keyWords;
@property BOOL returnAll;
@property NSString *voiceType;
@property NSString *voiceEncode;
@property NSString *tokenId;
@property NSString *eventId;
@property int voiceSample;
@property BOOL enableMatch;
@property BOOL returnText;
@property NSString *language;
@property NSString *matchMode;
@property BOOL returnNumbers;
@property NSDictionary *extra;
@property NSString* authToken;

@property __weak id delegate;

-(NSDictionary*) toJson;

@end


@interface SmRtAsrClient : NSObject

- (instancetype)init NS_UNAVAILABLE;

+(instancetype)shareInstance;
-(instancetype)initWithOption:(SmRtAsrOption *)option;

-(void) startSession:(SmRtSessionConfig*) config;
-(BOOL) postAudio:(NSData *)audioClip;
-(void) stopSession;
-(void) destroy;

-(NSString*) getSDKVersion;
-(SmRtAsrOption *) getAsrOption;

@end
