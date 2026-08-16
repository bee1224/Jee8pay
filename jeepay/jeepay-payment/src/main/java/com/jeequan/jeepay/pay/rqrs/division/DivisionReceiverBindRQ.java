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
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
* 分帳帳號的绑定 請求參數
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/8/25 09:21
*/
@Data
public class DivisionReceiverBindRQ extends AbstractMchAppRQ {

    /** 支付介面代码   **/
    @NotBlank(message="支付介面代码不能為空")
    private String ifCode;

    /** 接收者帳號别名 **/
    private String receiverAlias;

    /** 组ID  **/
    @NotNull(message="组ID不能為空， 若不存在請先登录商戶平台进行创建操作")
    private Long receiverGroupId;

    /** 分帳接收帳號类型: 0-个人(对私) 1-商戶(对公) **/
    @NotNull(message="分帳接收帳號类型不能為空")
    @Range(min = 0, max = 1, message = "分帳接收帳號类型设置有误")
    private Byte accType;

    /** 分帳接收帳號 **/
    @NotBlank(message="分帳接收帳號不能為空")
    private String accNo;

    /** 分帳接收帳號名称 **/
    private String accName;

    /** 分帳关系类型（参考微信）， 如： SERVICE_PROVIDER 服務商等 **/
    @NotBlank(message="分帳关系类型不能為空")
    private String relationType;

    /** 当选择自定义时，需要录入该字段。 否则为对应的名称 **/
    private String relationTypeName;

    /** 渠道特殊資訊 */
    private String channelExtInfo;

    /** 分帳比例 **/
    @NotBlank(message="分帳比例不能為空")
    private String divisionProfit;

}
