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
package com.jeequan.jeepay.pay.rqrs.division;

import com.jeequan.jeepay.core.entity.MchDivisionReceiver;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

/*
* 绑定帳戶 响应參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:34
*/
@Data
public class DivisionReceiverBindRS extends AbstractRS {


    /**
     * 分帳接收者ID
     */
    private Long receiverId;

    /**
     * 接收者帳號别名
     */
    private String receiverAlias;

    /**
     * 组ID（便于商戶介面使用）
     */
    private Long receiverGroupId;

    /**
     * 商戶號
     */
    private String mchNo;

    /**
     * 應用ID
     */
    private String appId;

    /**
     * 支付介面代码
     */
    private String ifCode;

    /**
     * 分帳接收帳號类型: 0-个人(对私) 1-商戶(对公)
     */
    private Byte accType;

    /**
     * 分帳接收帳號
     */
    private String accNo;

    /**
     * 分帳接收帳號名称
     */
    private String accName;

    /**
     * 分帳关系类型（参考微信）， 如： SERVICE_PROVIDER 服務商等
     */
    private String relationType;

    /**
     * 当选择自定义时，需要录入该字段。 否则为对应的名称
     */
    private String relationTypeName;


    /**
     * 渠道特殊資訊
     */
    private String channelExtInfo;

    /**
     * 绑定成功时间
     */
    private Long bindSuccessTime;

    /**
     * 分帳比例
     */
    private BigDecimal divisionProfit;

    /**
     * 分帳狀態 1-绑定成功, 0-绑定異常
     */
    private Byte bindState;

    /**
     * 支付渠道錯誤码
     */
    private String errCode;

    /**
     * 支付渠道錯誤資訊
     */
    private String errMsg;



    public static DivisionReceiverBindRS buildByRecord(MchDivisionReceiver record){

        if(record == null){
            return null;
        }

        DivisionReceiverBindRS result = new DivisionReceiverBindRS();
        BeanUtils.copyProperties(record, result);
        result.setBindSuccessTime(record.getBindSuccessTime() != null ? record.getBindSuccessTime().getTime() : null);

        return result;
    }



}
