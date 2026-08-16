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
package com.jeequan.jeepay.pay.rqrs.transfer;

import com.jeequan.jeepay.pay.rqrs.AbstractMchAppRQ;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
* 申請轉帳 請求參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/8/10 11:31
*/
@Data
public class TransferOrderRQ extends AbstractMchAppRQ {

    /** 商戶訂單号 **/
    @NotBlank(message="商戶訂單号不能為空")
    private String mchOrderNo;

    /** 支付介面代码   **/
    @NotBlank(message="支付介面代码不能為空")
    private String ifCode;

    /** 入账方式  **/
    @NotBlank(message="入账方式不能為空")
    private String entryType;

    /** 支付金額， 单位：分 **/
    @NotNull(message="轉帳金額不能為空")
    @Min(value = 1, message = "轉帳金額不能小于1分")
    private Long amount;

    /** 货币代码 **/
    @NotBlank(message="货币代码不能為空")
    private String currency;

    /** 收款帳號 **/
    @NotBlank(message="收款帳號不能為空")
    private String accountNo;

    /** 收款人姓名 **/
    private String accountName;

    /** 收款人开户行名称 **/
    private String bankName;

    /** 客户端IP地址 **/
    private String clientIp;

    /** 轉帳备注資訊 **/
    @NotBlank(message="轉帳备注資訊不能為空")
    private String transferDesc;

    /** 異步通知地址 **/
    private String notifyUrl;

    /** 特定渠道发起额外參數 **/
    private String channelExtra;

    /** 商戶扩展參數 **/
    private String extParam;

}
