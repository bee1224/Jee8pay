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
package com.jeequan.jeepay.pay.rqrs.payorder;

import com.alibaba.fastjson.annotation.JSONField;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import lombok.Data;

/*
* 创建訂單(统一訂單) 响应參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:34
*/
@Data
public class UnifiedOrderRS extends AbstractRS {

    /** 支付訂單号 **/
    private String payOrderId;

    /** 商戶訂單号 **/
    private String mchOrderNo;

    /** 訂單狀態 **/
    private Byte orderState;

    /** 支付參數类型  ( 无參數，  调起支付插件參數， 重定向到指定地址，  用戶扫码   )   **/
    private String payDataType;

    /** 支付參數 **/
    private String payData;

    /** 渠道返回錯誤代码 **/
    private String errCode;

    /** 渠道返回錯誤資訊 **/
    private String errMsg;

    /** 上游渠道返回数据包 (无需JSON序列化) **/
    @JSONField(serialize = false)
    private ChannelRetMsg channelRetMsg;

    /** 生成聚合支付參數 (仅统一下单介面使用) **/
    public String buildPayDataType(){
        return CS.PAY_DATA_TYPE.NONE;
    }

    /** 生成支付參數 **/
    public String buildPayData(){
        return "";
    }


}
