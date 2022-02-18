//
//  Created by Herbert on 2021/10/21.
//

#import <Foundation/Foundation.h>

@interface RTSSResult : NSObject

@property (nonatomic, assign) BOOL isHit;

@property (nonatomic, copy) NSString * keyword;

@property (nonatomic, copy) NSString * text;

@property (nonatomic, copy) NSString * wordType;

@property (nonatomic, copy) NSArray * keyWordList;

@property (nonatomic, copy) NSArray * numberList;

@end

