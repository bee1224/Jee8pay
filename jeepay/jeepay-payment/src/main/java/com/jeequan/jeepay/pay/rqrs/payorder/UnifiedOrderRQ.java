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
import com.jeequan.jeepay.pay.rqrs.AbstractMchAppRQ;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
* 创建訂單請求參數对象
* 聚合支付介面（统一下单）
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:33
*/
@Data
public class UnifiedOrderRQ extends AbstractMchAppRQ {

    /** 商戶訂單号 **/
    @NotBlank(message="商戶訂單号不能為空")
    private String mchOrderNo;

    /** 支付方式  如： wxpay_jsapi,alipay_wap等   **/
    @NotBlank(message="支付方式不能為空")
    private String wayCode;

    /** 支付金額， 单位：分 **/
    @NotNull(message="支付金額不能為空")
    @Min(value = 1, message = "支付金額不能為空")
    private Long amount;

    /** 货币代码 **/
    @NotBlank(message="货币代码不能為空")
    private String currency;

    /** 客户端IP地址 **/
    private String clientIp;

    /** 商品標題 **/
    @NotBlank(message="商品標題不能為空")
    private String subject;

    /** 商品描述資訊 **/
    @NotBlank(message="商品描述資訊不能為空")
    private String body;

    /** 異步通知地址 **/
    private String notifyUrl;

    /** 跳转通知地址 **/
    private String returnUrl;

    /** 訂單失效时间, 单位：秒 **/
    private Integer expiredTime;

    /** 特定渠道发起额外參數 **/
    private String channelExtra;

    /** 商戶扩展參數 **/
    private String extParam;

    /** 分帳模式： 0-该笔訂單不允许分帳, 1-支付成功按設定自动完成分帳, 2-商戶手动分帳(解冻商戶金額) **/
    @Range(min = 0, max = 2, message = "分帳模式设置值有误")
    private Byte divisionMode;

    /** 返回真实的bizRQ **/
    public UnifiedOrderRQ buildBizRQ(){

        return this;
    }

    /** 獲取渠道用戶ID **/
    @JSONField(serialize = false)
    public String getChannelUserId(){
        return null;
    }

}
