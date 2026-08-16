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

import com.jeequan.jeepay.pay.rqrs.AbstractMchAppRQ;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/*
* 发起訂單分帳 請求參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/8/26 17:21
*/
@Data
public class PayOrderDivisionExecRQ extends AbstractMchAppRQ {

    /** 商戶訂單号 **/
    private String mchOrderNo;

    /** 支付系統訂單号 **/
    private String payOrderId;

    /**
     * 是否使用系統設定的自动分帳组： 0-否 1-是
     **/
    @NotNull(message = "是否使用系統設定的自动分帳组不能為空")
    private Byte useSysAutoDivisionReceivers;

    /** 接收者帳號列表（JSONArray 转换为字符串类型）
     * 仅当useSysAutoDivisionReceivers=0 时有效。
     *
     * 参考：
     *
     * 方式1： 按帳號纬度
     * [{
     *     receiverId: 800001,
     *     divisionProfit: 0.1 (若不填入则使用系統默认設定值)
     * }]
     *
     * 方式2： 按组纬度
     * [{
     *     receiverGroupId: 100001, (该组所有 當前訂單的渠道帳號并且可用狀態的全部参与分帳)
     *     divisionProfit: 0.1 (每个帳號的分帳比例， 若不填入则使用系統默认設定值， 建议不填写)
     * }]
     *
     * **/
    private String receivers;

}
