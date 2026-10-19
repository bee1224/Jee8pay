package com.jeequan.jeepay.mgr.ctrl.apitest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.entity.PayWay;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.utils.MlDsaKit;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.impl.MchInfoService;
import com.jeequan.jeepay.service.impl.MchPayPassageService;
import com.jeequan.jeepay.service.impl.PayWayService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import com.jeequan.jeepay.service.sign.PayApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API 測試（技術人員用）：以商戶身分打支付服務的商戶 API，把完整的請求、回應，
 * 以及支付服務打到上游渠道的 HTTP 往來全部回傳給前端顯示；另提供商戶簽名的離線驗證。
 *
 * 平台不持有商戶私鑰，下單與查單以平台內部金鑰簽名（signType=PLATFORM-ML-DSA-65，ADR-0013），
 * 其餘流程與商戶自己串接完全相同。測試單會真的送到上游，標題固定為「API 測試單」。
 */
@Tag(name = "API 測試")
@RestController
@RequestMapping("/api/apiTest")
public class ApiTestController extends CommonCtrl {

    /** 單筆測試金額上限（分）：與團長渠道測試一致 */
    private static final long TEST_MAX_AMOUNT = 2_000_000L;
    /** reqTime 允許誤差，與支付服務 ApiController 一致 */
    private static final long REQ_TIME_WINDOW_MILLIS = 5 * 60 * 1000L;

    @Autowired private MchInfoService mchInfoService;
    @Autowired private MchAppService mchAppService;
    @Autowired private MchPayPassageService mchPayPassageService;
    @Autowired private PayWayService payWayService;
    @Autowired private SysConfigService sysConfigService;
    @Autowired private PayApiClient payApiClient;

    @Operation(summary = "可測試的商戶、應用與已開通的支付方式")
    @PreAuthorize("hasAuthority('ENT_API_TEST')")
    @RequestMapping(value = "/options", method = RequestMethod.GET)
    public ApiRes options() {
        Map<String, String> wayNames = payWayService.list().stream()
                .collect(Collectors.toMap(PayWay::getWayCode, PayWay::getWayName, (a, b) -> a));
        Map<String, List<MchPayPassage>> passages = mchPayPassageService.list(MchPayPassage.gw()
                        .eq(MchPayPassage::getState, CS.PUB_USABLE))
                .stream().collect(Collectors.groupingBy(MchPayPassage::getAppId));
        Map<String, List<MchApp>> apps = mchAppService.list().stream().collect(Collectors.groupingBy(MchApp::getMchNo));

        List<JSONObject> result = mchInfoService.list(MchInfo.gw().orderByDesc(MchInfo::getCreatedAt)).stream().map(mch -> {
            JSONObject m = new JSONObject(true);
            m.put("mchNo", mch.getMchNo());
            m.put("mchName", mch.getMchName());
            m.put("state", mch.getState());
            m.put("apps", apps.getOrDefault(mch.getMchNo(), List.of()).stream().map(app -> {
                JSONObject a = new JSONObject(true);
                a.put("appId", app.getAppId());
                a.put("appName", app.getAppName());
                a.put("state", app.getState());
                a.put("hasPublicKey", StringUtils.isNotBlank(app.getMchPublicKey()));
                a.put("ways", passages.getOrDefault(app.getAppId(), List.of()).stream()
                        .map(MchPayPassage::getWayCode).distinct()
                        .map(w -> Map.of("wayCode", w, "wayName", wayNames.getOrDefault(w, w)))
                        .collect(Collectors.toList()));
                return a;
            }).collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
        return ApiRes.ok(result);
    }

    @Operation(summary = "下單測試：走一次統一下單，回傳完整請求、回應與上游渠道往來")
    @PreAuthorize("hasAuthority('ENT_API_TEST')")
    @MethodLog(remark = "API 測試：統一下單")
    @RequestMapping(value = "/unifiedOrder", method = RequestMethod.POST)
    public ApiRes unifiedOrder() {
        MchApp app = requireApp();
        String wayCode = getValStringRequired("wayCode");
        Long amount = getRequiredAmountL("amount");
        if (amount <= 0 || amount > TEST_MAX_AMOUNT) {
            throw new BizException("測試金額須大於 0 且不超過 " + TEST_MAX_AMOUNT / 100 + " 元");
        }

        // 繳款人資料：ibon 上游必填，內容交給 Provider 驗證（沒填的欄位不送，讓技術人員看到上游的錯誤回應）
        JSONObject extra = new JSONObject(true);
        for (String key : new String[]{"payerName", "payerMobile", "payerEmail", "payerPostcode", "payerAddress"}) {
            String value = getValString(key);
            if (StringUtils.isNotBlank(value)) {
                extra.put(key, value.trim());
            }
        }

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("mchNo", app.getMchNo());
        req.put("appId", app.getAppId());
        req.put("mchOrderNo", "APITEST" + System.currentTimeMillis() + RandomStringUtils.randomNumeric(4));
        req.put("wayCode", wayCode);
        req.put("amount", amount);
        req.put("currency", "TWD");
        req.put("clientIp", getClientIp());
        req.put("subject", "API 測試單");
        req.put("body", "API 測試單[" + app.getMchNo() + "]");
        req.put("notifyUrl", sysConfigService.getDBApplicationConfig().getMchSiteUrl() + "/api/anon/paytestNotify/payOrder");
        if (!extra.isEmpty()) {
            req.put("channelExtra", extra.toJSONString());
        }
        return ApiRes.ok(payApiClient.callTraced("/api/pay/unifiedOrder", req));
    }

    @Operation(summary = "查單測試：以支付單號或商戶單號查詢（查平台本地訂單）")
    @PreAuthorize("hasAuthority('ENT_API_TEST')")
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public ApiRes query() {
        MchApp app = requireApp();
        String payOrderId = StringUtils.trimToNull(getValString("payOrderId"));
        String mchOrderNo = StringUtils.trimToNull(getValString("mchOrderNo"));
        if (payOrderId == null && mchOrderNo == null) {
            throw new BizException("請填寫支付單號或商戶單號");
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("mchNo", app.getMchNo());
        req.put("appId", app.getAppId());
        if (payOrderId != null) {
            req.put("payOrderId", payOrderId);
        }
        if (mchOrderNo != null) {
            req.put("mchOrderNo", mchOrderNo);
        }
        return ApiRes.ok(payApiClient.callTraced("/api/pay/query", req));
    }

    /**
     * 驗證商戶貼上的請求 JSON：逐項檢查 signType、reqTime、商戶與應用、公鑰，並以商戶公鑰驗簽，
     * 回傳平台組出的待簽字串，方便和商戶自己組的逐字比對。不會送出任何請求。
     */
    @Operation(summary = "簽名驗證：檢查商戶請求的簽名")
    @PreAuthorize("hasAuthority('ENT_API_TEST')")
    @RequestMapping(value = "/verifySign", method = RequestMethod.POST)
    public ApiRes verifySign() {
        String raw = getValStringRequired("requestJson");
        JSONObject req;
        try {
            req = JSON.parseObject(raw);
        } catch (Exception e) {
            throw new BizException("不是合法的 JSON：" + e.getMessage());
        }
        if (req == null) {
            throw new BizException("請貼上商戶送出的請求 JSON");
        }

        JSONObject result = new JSONObject(true);
        List<JSONObject> checks = new java.util.ArrayList<>();
        result.put("canonical", MlDsaKit.canonical(req));
        result.put("checks", checks);

        String signType = req.getString("signType");
        boolean signTypeOk = MlDsaKit.SIGN_TYPE.equals(signType);
        checks.add(check("signType", signTypeOk, signTypeOk ? signType : "必須是 " + MlDsaKit.SIGN_TYPE + "，收到 " + signType));

        String reqTime = req.getString("reqTime");
        boolean reqTimeOk = false;
        String reqTimeMsg;
        if (StringUtils.isBlank(reqTime) || !StringUtils.isNumeric(reqTime)) {
            reqTimeMsg = "必填，13 位毫秒時間戳";
        } else {
            long diff = System.currentTimeMillis() - Long.parseLong(reqTime);
            reqTimeOk = Math.abs(diff) <= REQ_TIME_WINDOW_MILLIS;
            reqTimeMsg = "與平台時間相差 " + diff / 1000 + " 秒" + (reqTimeOk ? "" : "（超過 5 分鐘會被拒絕；若是舊請求可忽略）");
        }
        checks.add(check("reqTime", reqTimeOk, reqTimeMsg));

        MchApp app = StringUtils.isBlank(req.getString("appId")) ? null : mchAppService.getById(req.getString("appId"));
        boolean appOk = app != null && app.getMchNo().equals(req.getString("mchNo"));
        checks.add(check("mchNo / appId", appOk, appOk ? app.getMchNo() + " / " + app.getAppId()
                + (app.getState() == CS.PUB_USABLE ? "" : "（應用已停用）") : "找不到這個商戶應用，或 appId 不屬於這個 mchNo"));

        String sign = req.getString("sign");
        if (!appOk || StringUtils.isBlank(app.getMchPublicKey())) {
            checks.add(check("sign", false, appOk ? "商戶應用尚未設定公鑰" : "無法取得商戶公鑰"));
        } else if (StringUtils.isBlank(sign)) {
            checks.add(check("sign", false, "缺少 sign"));
        } else {
            PublicKey key = MlDsaKit.parsePublicKey(app.getMchPublicKey());
            boolean ok = MlDsaKit.verify(req, sign, key);
            checks.add(check("sign", ok, ok ? "通過（以商戶公鑰驗證）"
                    : "失敗：請比對上方待簽字串與商戶組出的字串，並確認使用的是這個應用的私鑰"));
        }
        result.put("note", "平台實際只對該 API 定義的欄位驗簽；商戶若多送了文件沒有的欄位，可能這裡通過但正式呼叫失敗。");
        return ApiRes.ok(result);
    }

    private MchApp requireApp() {
        String mchNo = getValStringRequired("mchNo");
        MchApp app = mchAppService.getById(getValStringRequired("appId"));
        if (app == null || !mchNo.equals(app.getMchNo())) {
            throw new BizException("商戶應用不存在");
        }
        return app;
    }

    private static JSONObject check(String item, boolean ok, String message) {
        JSONObject c = new JSONObject(true);
        c.put("item", item);
        c.put("ok", ok);
        c.put("message", message);
        return c;
    }
}
