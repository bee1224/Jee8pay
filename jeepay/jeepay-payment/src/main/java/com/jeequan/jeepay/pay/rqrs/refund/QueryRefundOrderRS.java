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
package com.jeequan.jeepay.pay.rqrs.refund;

import com.jeequan.jeepay.core.entity.RefundOrder;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/*
* 查询退款单 响应參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/17 14:08
*/
@Data
public class QueryRefundOrderRS extends AbstractRS {

    /**
     * 退款訂單号（支付系統生成訂單号）
     */
    private String refundOrderId;

    /**
     * 支付訂單号（与t_pay_order对应）
     */
    private String payOrderId;

    /**
     * 商戶號
     */
    private String mchNo;

    /**
     * 應用ID
     */
    private String appId;

    /**
     * 商戶退款單號（商戶系統的訂單号）
     */
    private String mchRefundNo;

    /**
     * 支付金額,单位分
     */
    private Long payAmount;

    /**
     * 退款金額,单位分
     */
    private Long refundAmount;

    /**
     * ISO 4217 三位貨幣代碼
     */
    private String currency;

    /**
     * 退款狀態:0-訂單生成,1-退款中,2-退款成功,3-退款失敗
     */
    private Byte state;

    /**
     * 渠道訂單号
     */
    private String channelOrderNo;

    /**
     * 渠道錯誤码
     */
    private String errCode;

    /**
     * 渠道錯誤描述
     */
    private String errMsg;

    /**
     * 扩展參數
     */
    private String extParam;

    /**
     * 訂單退款成功时间
     */
    private Long successTime;

    /**
     * 创建时间
     */
    private Long createdAt;


    public static QueryRefundOrderRS buildByRefundOrder(RefundOrder refundOrder){

        if(refundOrder == null){
            return null;
        }

        QueryRefundOrderRS result = new QueryRefundOrderRS();
        BeanUtils.copyProperties(refundOrder, result);
        result.setSuccessTime(refundOrder.getSuccessTime() == null ? null : refundOrder.getSuccessTime().getTime());
        result.setCreatedAt(refundOrder.getCreatedAt() == null ? null : refundOrder.getCreatedAt().getTime());
        return result;
    }


}
