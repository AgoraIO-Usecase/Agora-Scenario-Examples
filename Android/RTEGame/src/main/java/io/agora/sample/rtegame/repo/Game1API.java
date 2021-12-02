package io.agora.sample.rtegame.repo;

import androidx.annotation.NonNull;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface Game1API {
    /**
     * 简要描述
     * <p>
     * 加入游戏请求、H5游戏即跳转游戏请求、由声网引导用户跳转、如开启房间、邀请别人加入游戏
     * 默认为进入第一个为房主
     * 请求URL
     * <p>
     * https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html
     * 请求方式
     * <p>
     * Get
     * 请求参数
     * <p>
     * 参数名	必选	类型	说明
     * user_id	是	number	用户Id
     * app_id	是	number	appId
     * room_id	是	number	房间Id、如邀请加入房间、需为房主的房间Id
     * identity	是	number	用户身份、用于区分主播和观众、1为主播、2为观众
     * token	是	string	用户校验凭证
     * name	    是	string	用户昵称
     * avatar	是	string	用户头像
     */
    @NonNull
    @GET
    Call<ResponseBody> gameStart(@Url @NonNull String url,
                                 @Query("user_id") int user_id,
                                 @Query("app_id") int app_id,
                                 @Query("room_id") int room_id,
                                 @Query("identity") int identity,
                                 @Query("token") int token,
                                 @Query("name") @NonNull String name,
                                 @Query("avatar") @NonNull String avatar);

    /**
     * 简要描述
     * <p>
     * 离开游戏请求、H5游戏即退出房间的请求、由声网请求游戏端
     * 即声网发起http请求——>游戏后端处理——>处理返回声网——>声网通知app端
     * 请求URL
     * 请求方式
     * <p>
     * Post
     * 请求参数
     * <p>
     * Post请求头
     * <p>
     * 参数名	是否必选	类型	说明
     * Content-Type	是	application/x-www-form-urlencoded
     * <p>
     * <p>
     * 参数名	是否必选	类型	说明
     * user_id	    是	number	用户Id
     * app_id	    是	number	appId
     * identity	    是	number	用户身份、用于区分主播和观众、1为主播、2为观众
     * room_id	    是	number	房间Id、当前房间Id
     * token	    是	string	用户校验凭证
     * timestamp	是	string	unix时间戳/秒
     * nonce_str	是	string	随机16位字符串
     * sign	        是	string	签名
     * <p>
     * 返回参数
     * <p>
     * 参数名	是否必选	类型	说明
     * code	是	number	状态码
     * data	是	string	结果信息
     * 更多状态码说明请看首页的状态码描述
     * 返回示例
     * <p>
     * {
     * "code": 10007,
     * "data":"接口请求错误"
     * }
     */
    @FormUrlEncoded
    @POST
    void gameEnd(
            @Field("user_id") int user_id,
            @Field("app_id") int app_id,
            @Field("identity") int identity,
            @Field("room_id") int room_id,
            @Field("token") @NonNull String token,
            @Field("timestamp") @NonNull String timestamp,
            @Field("nonce_str") @NonNull String nonce_str
    );

    /**
     * 简要描述
     * <p>
     * 刷礼物请求、由声网通知游戏端、游戏端变更游戏表现
     * 请求URL
     * <p>
     * testgame.yuanqihuyu.com/guess/gift
     * 请求方式
     * <p>
     * Post
     * 请求参数
     * <p>
     * Post请求头
     * <p>
     * 参数名	是否必选	类型	说明
     * Content-Type	是	application/x-www-form-urlencoded
     * 参数名	是否必选	类型	说明
     * user_id	    是	number	用户Id
     * app_id	    是	number	appId
     * room_id	    是	number	房间Id、当前房间Id
     * name	        是	string	用户昵称
     * token	    是	string	用户校验凭证
     * timestamp	是	string	unix时间戳/秒
     * nonce_str	是	string	随机16位字符串
     * gift	        是	number	礼物的代码
     * count	    是	number	数量
     * player	    是	number	赠送的主播Id
     * sign	        是	string	签名
     * count参数说明
     * <p>
     * 礼物接口统一count为1、即一次请求为观众刷礼物一次
     * gift参数说明
     * <p>
     * 礼物的代码触发对应效果、由平台方判断的观众交互
     * 代码	触发的道具
     * 1	提示卡
     * 2	免答卡
     * 3	延时卡
     * 4	减时卡
     * 5	遮挡卡
     * 返回参数
     * <p>
     * 参数名	是否必选	类型	说明
     * code	是	number	状态码
     * data	是	string	结果信息
     * 更多状态码说明请看首页的状态码描述
     * 返回示例
     * <p>
     * {
     * "code": 10007,
     * "data":"接口请求错误"
     * }
     */
    @POST
    @FormUrlEncoded
    void gameGift(
            @Field("user_id") int user_id,
            @Field("app_id") int app_id,
            @Field("room_id") int room_id,
            @Field("name") @NonNull String name,
            @Field("token") @NonNull String token,
            @Field("timestamp") @NonNull String timestamp,
            @Field("nonce_str") @NonNull String nonce_str,
            @Field("gift") int gift,
            @Field("count") int count,
            @Field("player") int player,
            @Field("sign") @NonNull String sign);

    /**
     * 简要描述
     * <p>
     * 刷弹幕请求、由声网通知游戏端、游戏端变更游戏表现
     * 请求URL
     * <p>
     * testgame.yuanqihuyu.com/guess/barrage
     * 请求方式
     * <p>
     * Post
     * 请求参数
     * <p>
     * Post请求头
     * <p>
     * 参数名	    是否必选	类型	说明
     * Content-Type	是	    application/x-www-form-urlencoded
     * 参数名	    是否必选	类型	        说明
     * user_id	    是	    number	    用户Id
     * app_id	    是	    number	    appId
     * room_id	    是	    number	    房间Id、当前房间Id
     * name	        是	    string	    用户昵称
     * token	    是	    string	    用户校验凭证
     * timestamp	是	    string	    unix时间戳/秒
     * nonce_str	是	    string	    随机16位字符串
     * barrage	    是	    number	    弹幕的代码
     * count	    是	    number	    数量
     * player	    是	    number	    赠送的主播Id
     * sign	        是	    string	    签名
     * count参数说明
     * <p>
     * 多次发请求弹幕会增加服务器压力、所以在count字段、可先做汇总、再统一发起请求
     * 如2秒统计该用户发送固定弹幕的次数、放在count里即可
     * barrage参数说明
     * <p>
     * 代码	触发的效果
     * 1	触发礼炮
     * 2	触发点赞
     * 3	触发丢屎
     * 返回参数
     * <p>
     * 参数名	是否必选	类型	说明
     * code	是	number	状态码
     * data	是	string	结果信息
     * 更多状态码说明请看首页的状态码描述
     * 返回示例
     * <p>
     * {
     * "code": 10007,
     * "data":"接口请求错误"
     * }
     */
    @POST
    @FormUrlEncoded
    void gameComment(
            @Field("user_id") int user_id,
            @Field("app_id") int app_id,
            @Field("room_id") int room_id,
            @Field("name") @NonNull String name,
            @Field("token") @NonNull String token,
            @Field("timestamp") @NonNull String timestamp,
            @Field("nonce_str") @NonNull String nonce_str,
            @Field("barrage") int barrage,
            @Field("count") int count,
            @Field("player") int player,
            @Field("sign") @NonNull String sign);
}
