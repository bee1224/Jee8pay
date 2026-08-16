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

import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/*
*  查询訂單 响应參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:40
*/
@Data
public class QueryPayOrderRS extends AbstractRS {

    /**
     * 支付訂單号
     */
    private String payOrderId;

    /**
     * 商戶號
     */
    private String mchNo;

    /**
     * 商戶應用ID
     */
    private String appId;

    /**
     * 商戶訂單号
     */
    private String mchOrderNo;

    /**
     * 支付介面代码
     */
    private String ifCode;

    /**
     * 支付方式代码
     */
    private String wayCode;

    /**
     * 支付金額,单位分
     */
    private Long amount;

    /**
     * ISO 4217 三位貨幣代碼
     */
    private String currency;

    /**
     * 支付狀態: 0-訂單生成, 1-支付中, 2-支付成功, 3-支付失敗, 4-已撤销, 5-已退款, 6-訂單關閉
     */
    private Byte state;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 商品標題
     */
    private String subject;

    /**
     * 商品描述資訊
     */
    private String body;

    /**
     * 渠道訂單号
     */
    private String channelOrderNo;

    /**
     * 渠道支付錯誤码
     */
    private String errCode;

    /**
     * 渠道支付錯誤描述
     */
    private String errMsg;

    /**
     * 商戶扩展參數
     */
    private String extParam;

    /**
     * 訂單支付成功时间
     */
    private Long successTime;

    /**
     * 创建时间
     */
    private Long createdAt;


    public static QueryPayOrderRS buildByPayOrder(PayOrder payOrder){

        if(payOrder == null){
            return null;
        }

        QueryPayOrderRS result = new QueryPayOrderRS();
        BeanUtils.copyProperties(payOrder, result);
        result.setSuccessTime(payOrder.getSuccessTime() == null ? null : payOrder.getSuccessTime().getTime());
        result.setCreatedAt(payOrder.getCreatedAt() == null ? null : payOrder.getCreatedAt().getTime());

        return result;
    }


}
