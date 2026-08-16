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
package com.jeequan.jeepay.pay.ctrl;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.ctrls.AbstractCtrl;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.JeepayKit;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.AbstractMchAppRQ;
import com.jeequan.jeepay.pay.rqrs.AbstractRQ;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.ValidateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/*
* api 抽象介面， 公共函数
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:28
*/
public abstract class ApiController extends AbstractCtrl {

    @Autowired private ValidateService validateService;
    @Autowired private ConfigContextQueryService configContextQueryService;


    /** 獲取請求參數并转换为对象，通用验证  **/
    protected <T extends AbstractRQ> T getRQ(Class<T> cls){

        T bizRQ = getObject(cls);

        // [1]. 验证通用字段规则
        validateService.validate(bizRQ);

        return bizRQ;
    }


    /** 獲取請求參數并转换为对象，商戶通用验证  **/
    protected <T extends AbstractRQ> T getRQByWithMchSign(Class<T> cls){

        //獲取請求RQ, and 通用验证
        T bizRQ = getRQ(cls);

        AbstractMchAppRQ abstractMchAppRQ = (AbstractMchAppRQ)bizRQ;

        //业务校验， 包括： 簽章驗證， 商戶狀態是否可用， 是否支持该支付方式下单等。
        String mchNo = abstractMchAppRQ.getMchNo();
        String appId = abstractMchAppRQ.getAppId();
        String sign = bizRQ.getSign();

        if(StringUtils.isAnyBlank(mchNo, appId, sign)){
            throw new BizException("參數有誤！");
        }

        MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(mchNo, appId);

        if(mchAppConfigContext == null){
            throw new BizException("商戶或商戶應用不存在");
        }

        if(mchAppConfigContext.getMchInfo() == null || mchAppConfigContext.getMchInfo().getState() != CS.YES){
            throw new BizException("商戶資訊不存在或商戶狀態不可用");
        }

        MchApp mchApp = mchAppConfigContext.getMchApp();
        if(mchApp == null || mchApp.getState() != CS.YES){
            throw new BizException("商戶應用不存在或應用狀態不可用");
        }

        if(!mchApp.getMchNo().equals(mchNo)){
            throw new BizException("參數 appId 與商戶號不匹配");
        }

        // 簽章驗證
        String appSecret = mchApp.getAppSecret();

        // 转换为 JSON
        JSONObject bizReqJSON = (JSONObject)JSONObject.toJSON(bizRQ);
        bizReqJSON.remove("sign");
        if(!sign.equalsIgnoreCase(JeepayKit.getSign(bizReqJSON, appSecret))){
             throw new BizException("簽章驗證失敗");
        }

        // reqTime freshness（防 replay；窗口為 5 分鐘，與系統時間偏差超過即拒絕）
        String reqTimeStr = bizRQ.getReqTime();
        if(StringUtils.isNotEmpty(reqTimeStr) && StringUtils.isNumeric(reqTimeStr)){
            long reqTime = Long.parseLong(reqTimeStr);
            long diff = Math.abs(System.currentTimeMillis() - reqTime);
            if(diff > REQTIME_FRESHNESS_MS){
                throw new BizException("請求時間戳已過期");
            }
        }

        return bizRQ;
    }

    /** reqTime 新鮮度窗口：5 分鐘（ms） **/
    private static final long REQTIME_FRESHNESS_MS = 5 * 60 * 1000L;
}
