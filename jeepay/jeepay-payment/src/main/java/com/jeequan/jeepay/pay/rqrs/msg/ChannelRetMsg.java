/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.pay.rqrs.msg;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

/*
* 上游渠道侧响应資訊包装类
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:31
*/
@Slf4j
@Data
public class ChannelRetMsg implements Serializable {

    /** 上游渠道返回狀態 **/
    private ChannelState channelState;

    /** 渠道訂單号 **/
    private String channelOrderId;

    /** 渠道用戶标识 **/
    private String channelUserId;

    /** 渠道錯誤码 **/
    private String channelErrCode;

    /** 渠道錯誤描述 **/
    private String channelErrMsg;

    /** 渠道支付数据包, 一般用于支付訂單的继续支付操作 **/
    private String channelAttach;

    /** 上游渠道返回的原始报文, 一般用于[运营平台的查询上游结果]功能 **/
    private String channelOriginResponse;

    /** 是否需要轮询查单（比如微信条码支付） 默认不查询訂單 **/
    private boolean isNeedQuery = false;

    /** 响应结果（一般用于回調介面返回给上游数据 ） **/
    private ResponseEntity responseEntity;

    //渠道狀態枚举值
    public enum ChannelState {
        CONFIRM_SUCCESS, //介面正確返回： 业务狀態已经明确成功
        CONFIRM_FAIL, //介面正確返回： 业务狀態已经明确失敗
        WAITING, //介面正確返回： 上游處理中， 需通过定时查询/回調进行下一步處理
        UNKNOWN, //介面超时，或网络異常等請求， 或者返回结果的簽名失敗： 狀態不明确 ( 上游介面变更, 暂时無法确定狀態值 )
        API_RET_ERROR, //渠道侧出现異常( 介面返回了異常狀態 )
        SYS_ERROR //本系統出现不可预知的異常
    }

    //静态初始函数
    public ChannelRetMsg(){}
    public ChannelRetMsg(ChannelState channelState, String channelOrderId, String channelErrCode, String channelErrMsg) {
        this.channelState = channelState;
        this.channelOrderId = channelOrderId;
        this.channelErrCode = channelErrCode;
        this.channelErrMsg = channelErrMsg;
    }

    /** 明确成功 **/
    public static ChannelRetMsg confirmSuccess(String channelOrderId){
        return new ChannelRetMsg(ChannelState.CONFIRM_SUCCESS, channelOrderId, null, null);
    }

    /** 明确失敗 **/
    public static ChannelRetMsg confirmFail(String channelErrCode, String channelErrMsg){
        return new ChannelRetMsg(ChannelState.CONFIRM_FAIL, null, channelErrCode, channelErrMsg);
    }

    /** 明确失敗 **/
    public static ChannelRetMsg confirmFail(String channelOrderId, String channelErrCode, String channelErrMsg){
        return new ChannelRetMsg(ChannelState.CONFIRM_FAIL, channelOrderId, channelErrCode, channelErrMsg);
    }

    /** 明确失敗 **/
    public static ChannelRetMsg confirmFail(String channelOrderId){
        return new ChannelRetMsg(ChannelState.CONFIRM_FAIL, channelOrderId, null, null);
    }

    /** 明确失敗 **/
    public static ChannelRetMsg confirmFail(){
        return new ChannelRetMsg(ChannelState.CONFIRM_FAIL, null, null, null);
    }

    /** 處理中 **/
    public static ChannelRetMsg waiting(){
        return new ChannelRetMsg(ChannelState.WAITING, null, null, null);
    }

    /** 異常的情况 **/
    public static ChannelRetMsg sysError(String channelErrMsg){
        return new ChannelRetMsg(ChannelState.SYS_ERROR, null, null, "系統：" + channelErrMsg);
    }

    /** 狀態未知的情况 **/
    public static ChannelRetMsg unknown(){
        return new ChannelRetMsg(ChannelState.UNKNOWN, null, null, null);
    }

    /** 狀態未知的情况 **/
    public static ChannelRetMsg unknown(String channelErrMsg){
        return new ChannelRetMsg(ChannelState.UNKNOWN, null, null, channelErrMsg);
    }

}





