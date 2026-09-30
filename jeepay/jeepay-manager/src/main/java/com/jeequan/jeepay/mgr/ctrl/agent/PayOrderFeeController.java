package com.jeequan.jeepay.mgr.ctrl.agent;

import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.impl.PayOrderFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 訂單四層手續費快照查詢（ADR-0009）；無快照時回傳 null，由畫面顯示「無快照」。 */
@Tag(name = "訂單手續費快照")
@RestController
@RequestMapping("/api/payOrderFee")
public class PayOrderFeeController extends AgentBaseCtrl {

    @Autowired private PayOrderFeeService payOrderFeeService;

    @Operation(summary = "訂單手續費快照")
    @PreAuthorize("hasAuthority('ENT_PAY_ORDER_VIEW')")
    @RequestMapping(value = "/{payOrderId}", method = RequestMethod.GET)
    public ApiRes detail(@PathVariable("payOrderId") String payOrderId) {
        requireAuthority("ENT_PAY_ORDER_VIEW");
        return ApiRes.ok(payOrderFeeService.getById(payOrderId));
    }
}
