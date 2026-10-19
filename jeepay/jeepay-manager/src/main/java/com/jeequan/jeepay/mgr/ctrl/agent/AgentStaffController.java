package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.entity.PayWay;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.model.DBApplicationConfig;
import com.jeequan.jeepay.mgr.service.AuthService;
import com.jeequan.jeepay.service.impl.AgentBranchService;
import com.jeequan.jeepay.service.impl.AgentPortalService;
import com.jeequan.jeepay.service.impl.AgentStaffService;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.sign.PayApiClient;
import com.jeequan.jeepay.service.impl.MchPayPassageService;
import com.jeequan.jeepay.service.impl.PayWayService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代理後台：統計看板、我的子帳號、渠道測試（比照競品高級代理後台的三個流程）。
 * 代理號一律取自登入者；子帳號與商戶都必須在登入者這一支底下，不接受前端指定範圍。
 */
@Tag(name = "代理後台（看板／子帳號／渠道測試）")
@RestController
@RequestMapping("/api/agentPortal")
public class AgentStaffController extends AgentBaseCtrl {

    private static final String ENT_STAFF = "ENT_AGENT_PORTAL_STAFF";
    private static final String ENT_CHANNEL_TEST = "ENT_AGENT_PORTAL_CHANNEL_TEST";
    /** 測試單金額上限（分）：避免誤開大額繳費單 */
    private static final long TEST_MAX_AMOUNT = 2_000_000L;

    @Autowired private AgentPortalService agentPortalService;
    @Autowired private AgentBranchService branch;
    @Autowired private AgentStaffService staff;
    @Autowired private AuthService authService;
    @Autowired private MchAppService mchAppService;
    @Autowired private PayApiClient payApiClient;
    @Autowired private MchPayPassageService mchPayPassageService;
    @Autowired private PayWayService payWayService;
    @Autowired private SysConfigService sysConfigService;

    private AgentInfo me() {
        return agentPortalService.requireAgent(getCurrentUser().getSysUser());
    }

    // ---------------- 統計看板 ----------------

    @Operation(summary = "統計看板：轄下商戶今日／昨日／本月／上月成交與今日成功失敗筆數")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_DASH')")
    @RequestMapping(value = "/branch/dashboard", method = RequestMethod.GET)
    public ApiRes dashboard() {
        requireAuthority("ENT_AGENT_PORTAL_DASH");
        return ApiRes.ok(branch.dashboard(me()));
    }

    // ---------------- 我的子帳號 ----------------

    @Operation(summary = "我的子帳號列表")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @RequestMapping(value = "/staff", method = RequestMethod.GET)
    public ApiRes staffList() {
        requireAuthority(ENT_STAFF);
        return ApiRes.ok(staff.list(me()));
    }

    @Operation(summary = "建立子帳號（隨機密碼只顯示一次；建立後需另設權限）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @MethodLog(remark = "代理建立子帳號")
    @RequestMapping(value = "/staff", method = RequestMethod.POST)
    public ApiRes staffAdd() {
        requireAuthority(ENT_STAFF);
        return ApiRes.ok(staff.create(me(), getValString("loginUsername"), getValString("realname"), getValString("telphone")));
    }

    @Operation(summary = "可下放給子帳號的權限")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @RequestMapping(value = "/staff/grantable", method = RequestMethod.GET)
    public ApiRes staffGrantable() {
        requireAuthority(ENT_STAFF);
        return ApiRes.ok(staff.grantable(me()));
    }

    @Operation(summary = "子帳號目前的權限")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @RequestMapping(value = "/staff/{sysUserId}/ents", method = RequestMethod.GET)
    public ApiRes staffEnts(@PathVariable("sysUserId") Long sysUserId) {
        requireAuthority(ENT_STAFF);
        return ApiRes.ok(staff.entIds(me(), sysUserId));
    }

    @Operation(summary = "設定子帳號權限")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @MethodLog(remark = "代理設定子帳號權限")
    @RequestMapping(value = "/staff/{sysUserId}/ents", method = RequestMethod.PUT)
    public ApiRes staffEntsSave(@PathVariable("sysUserId") Long sysUserId) {
        requireAuthority(ENT_STAFF);
        JSONArray array = getReqParamJSON().getJSONArray("entIds");
        List<String> entIds = array == null ? new ArrayList<>() : array.toJavaList(String.class);
        List<String> saved = staff.saveEntIds(me(), sysUserId, entIds);
        // 權限快取在登入憑證裡，改完立即刷新，不必等對方重新登入
        authService.refAuthentication(List.of(sysUserId));
        return ApiRes.ok(saved);
    }

    @Operation(summary = "啟用／停用子帳號")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @MethodLog(remark = "代理啟用或停用子帳號")
    @RequestMapping(value = "/staff/{sysUserId}/state", method = RequestMethod.PUT)
    public ApiRes staffState(@PathVariable("sysUserId") Long sysUserId) {
        requireAuthority(ENT_STAFF);
        byte state = getValByteRequired("state");
        staff.setState(me(), sysUserId, state);
        if (state == CS.YES) {
            authService.refAuthentication(List.of(sysUserId));
        } else {
            authService.delAuthentication(List.of(sysUserId));
        }
        return ApiRes.ok();
    }

    @Operation(summary = "重設子帳號密碼（新密碼只顯示一次）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_STAFF')")
    @MethodLog(remark = "代理重設子帳號密碼")
    @RequestMapping(value = "/staff/{sysUserId}/resetPassword", method = RequestMethod.POST)
    public ApiRes staffResetPassword(@PathVariable("sysUserId") Long sysUserId) {
        requireAuthority(ENT_STAFF);
        JSONObject result = staff.resetPassword(me(), sysUserId);
        authService.delAuthentication(List.of(sysUserId));
        return ApiRes.ok(result);
    }

    // ---------------- 渠道測試 ----------------

    /** 商戶必須在登入者這一支底下。 */
    private void requireMyMch(String mchNo) {
        if (!agentPortalService.directMchNos(me()).contains(mchNo)) {
            throw new BizException("商戶不在你的轄區內");
        }
    }

    @Operation(summary = "渠道測試：商戶已開通的支付方式")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_CHANNEL_TEST')")
    @RequestMapping(value = "/branch/merchants/{mchNo}/testWays", method = RequestMethod.GET)
    public ApiRes testWays(@PathVariable("mchNo") String mchNo) {
        requireAuthority(ENT_CHANNEL_TEST);
        requireMyMch(mchNo);
        Map<String, String> appNames = mchAppService.list(MchApp.gw().select(MchApp::getAppId, MchApp::getAppName)
                        .eq(MchApp::getMchNo, mchNo).eq(MchApp::getState, CS.PUB_USABLE))
                .stream().collect(Collectors.toMap(MchApp::getAppId, MchApp::getAppName));
        Map<String, String> wayNames = payWayService.list(PayWay.gw().select(PayWay::getWayCode, PayWay::getWayName))
                .stream().collect(Collectors.toMap(PayWay::getWayCode, PayWay::getWayName));
        List<JSONObject> result = new ArrayList<>();
        mchPayPassageService.list(MchPayPassage.gw()
                        .select(MchPayPassage::getAppId, MchPayPassage::getWayCode, MchPayPassage::getIfCode)
                        .eq(MchPayPassage::getMchNo, mchNo).eq(MchPayPassage::getState, CS.PUB_USABLE))
                .stream().filter(p -> appNames.containsKey(p.getAppId()))
                .forEach(p -> {
                    JSONObject row = new JSONObject(true);
                    row.put("appId", p.getAppId());
                    row.put("appName", appNames.get(p.getAppId()));
                    row.put("wayCode", p.getWayCode());
                    row.put("wayName", wayNames.getOrDefault(p.getWayCode(), p.getWayCode()));
                    row.put("ifCode", p.getIfCode());
                    result.add(row);
                });
        return ApiRes.ok(result);
    }

    /**
     * 以商戶身分走一次正式的統一下單（與商戶自己串接 API 完全相同的路徑），回傳繳費資訊。
     * 訂單會真的送到第三方並留在訂單列表，標題固定帶「管理員測試單」以便辨識；不繳費即逾期關閉。
     */
    @Operation(summary = "渠道測試：替商戶開一筆測試單")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_CHANNEL_TEST')")
    @MethodLog(remark = "代理渠道測試下單")
    @RequestMapping(value = "/branch/merchants/{mchNo}/channelTest", method = RequestMethod.POST)
    public ApiRes channelTest(@PathVariable("mchNo") String mchNo) {
        requireAuthority(ENT_CHANNEL_TEST);
        requireMyMch(mchNo);
        String appId = getValStringRequired("appId");
        String wayCode = getValStringRequired("wayCode");
        Long amount = getRequiredAmountL("amount");
        if (amount <= 0 || amount > TEST_MAX_AMOUNT) {
            throw new BizException("測試金額須大於 0 且不超過 " + TEST_MAX_AMOUNT / 100 + " 元");
        }
        MchApp mchApp = mchAppService.getById(appId);
        if (mchApp == null || mchApp.getState() != CS.PUB_USABLE || !mchNo.equals(mchApp.getMchNo())) {
            throw new BizException("商戶應用不存在或不可用");
        }
        if (mchPayPassageService.count(MchPayPassage.gw().eq(MchPayPassage::getMchNo, mchNo).eq(MchPayPassage::getAppId, appId)
                .eq(MchPayPassage::getWayCode, wayCode).eq(MchPayPassage::getState, CS.PUB_USABLE)) == 0) {
            throw new BizException("商戶尚未開通這個支付方式");
        }

        // 繳款人資料：ibon 上游必填，交給 Provider 驗證內容
        JSONObject extra = new JSONObject(true);
        for (String key : new String[]{"payerName", "payerMobile", "payerEmail", "payerPostcode", "payerAddress"}) {
            String value = getValString(key);
            if (StringUtils.isBlank(value)) {
                throw new BizException("請填寫完整的繳款人資料");
            }
            extra.put(key, value.trim());
        }

        DBApplicationConfig config = sysConfigService.getDBApplicationConfig();
        // 平台不持有商戶私鑰，以平台內部金鑰代商戶下單（ADR-0013）
        Map<String, Object> req = new HashMap<>();
        req.put("mchNo", mchNo);
        req.put("appId", appId);
        req.put("mchOrderNo", "TEST" + System.currentTimeMillis() + RandomStringUtils.randomNumeric(4));
        req.put("wayCode", wayCode);
        req.put("amount", amount);
        req.put("currency", "TWD");
        req.put("clientIp", getClientIp());
        req.put("subject", "管理員測試單");
        req.put("body", "管理員測試單[" + mchNo + "]");
        req.put("notifyUrl", config.getMchSiteUrl() + "/api/anon/paytestNotify/payOrder");
        req.put("channelExtra", extra.toString());
        return ApiRes.ok(payApiClient.call("/api/pay/unifiedOrder", req));
    }
}
