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

import com.jeequan.jeepay.core.entity.TransferOrder;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/*
* 查询轉帳訂單 响应參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/17 14:08
*/
@Data
public class QueryTransferOrderRS extends AbstractRS {

    /**
     * 轉帳訂單号
     */
    private String transferId;

    /**
     * 商戶號
     */
    private String mchNo;

    /**
     * 應用ID
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
     * 入账方式： WX_CASH-微信零钱; ALIPAY_CASH-支付宝轉帳; BANK_CARD-银行卡
     */
    private String entryType;

    /**
     * 轉帳金額,单位分
     */
    private Long amount;

    /**
     * ISO 4217 三位貨幣代碼
     */
    private String currency;

    /**
     * 收款帳號
     */
    private String accountNo;

    /**
     * 收款人姓名
     */
    private String accountName;

    /**
     * 收款人开户行名称
     */
    private String bankName;

    /**
     * 轉帳备注資訊
     */
    private String transferDesc;

    /**
     * 支付狀態: 0-訂單生成, 1-轉帳中, 2-轉帳成功, 3-轉帳失敗, 4-訂單關閉
     */
    private Byte state;

    /**
     * 特定渠道发起额外參數
     */
    private String channelExtra;

    /**
     * 渠道訂單号
     */
    private String channelOrderNo;

    /** 渠道响应数据（如微信确认数据包）   **/
    private String channelResData;

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
     * 轉帳成功时间
     */
    private Long successTime;

    /**
     * 创建时间
     */
    private Long createdAt;


    public static QueryTransferOrderRS buildByRecord(TransferOrder record){

        if(record == null){
            return null;
        }

        QueryTransferOrderRS result = new QueryTransferOrderRS();
        BeanUtils.copyProperties(record, result);
        result.setSuccessTime(record.getSuccessTime() == null ? null : record.getSuccessTime().getTime());
        result.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().getTime());
        return result;
    }


}
