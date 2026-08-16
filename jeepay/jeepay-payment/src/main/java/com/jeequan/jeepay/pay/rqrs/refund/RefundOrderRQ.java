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

import com.jeequan.jeepay.pay.rqrs.AbstractMchAppRQ;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
* 创建退款訂單請求參數对象
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/16 15:41
*/
@Data
public class RefundOrderRQ extends AbstractMchAppRQ {

    /** 商戶訂單号 **/
    private String mchOrderNo;

    /** 支付系統訂單号 **/
    private String payOrderId;

    /** 商戶系統生成的退款單號   **/
    @NotBlank(message="商戶退款單號不能為空")
    private String mchRefundNo;

    /** 退款金額， 单位：分 **/
    @NotNull(message="退款金額不能為空")
    @Min(value = 1, message = "退款金額請大于1分")
    private Long refundAmount;

    /** 货币代码 **/
    @NotBlank(message="货币代码不能為空")
    private String currency;

    /** 退款原因 **/
    @NotBlank(message="退款原因不能為空")
    private String refundReason;

    /** 客户端IP地址 **/
    private String clientIp;

    /** 異步通知地址 **/
    private String notifyUrl;

    /** 特定渠道发起额外參數 **/
    private String channelExtra;

    /** 商戶扩展參數 **/
    private String extParam;

}
