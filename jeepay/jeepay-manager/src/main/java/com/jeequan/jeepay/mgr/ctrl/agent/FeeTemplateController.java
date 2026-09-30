package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.FeeTemplate;
import com.jeequan.jeepay.core.entity.FeeTemplateItem;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.impl.FeeTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 費率範本（ADR-0009 第四階段）：只含代理層，套用會逐筆寫入費率規則並留下變更紀錄。 */
@Tag(name = "費率範本")
@RestController
@RequestMapping("/api/feeTemplates")
public class FeeTemplateController extends AgentBaseCtrl {

    @Autowired private FeeTemplateService feeTemplateService;

    @Operation(summary = "範本列表")
    @PreAuthorize("hasAuthority('ENT_FEE_TEMPLATE')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<FeeTemplate> list() {
        requireAuthority("ENT_FEE_TEMPLATE");
        return ApiPageRes.pages(feeTemplateService.page(getIPage(true), FeeTemplate.gw().orderByDesc(FeeTemplate::getTemplateId)));
    }

    @Operation(summary = "範本詳情（含明細）")
    @PreAuthorize("hasAuthority('ENT_FEE_TEMPLATE')")
    @RequestMapping(value = "/{templateId}", method = RequestMethod.GET)
    public ApiRes<JSONObject> detail(@PathVariable("templateId") Long templateId) {
        requireAuthority("ENT_FEE_TEMPLATE");
        JSONObject result = (JSONObject) JSONObject.toJSON(feeTemplateService.getById(templateId));
        if (result != null) {
            result.put("items", feeTemplateService.items(templateId));
        }
        return ApiRes.ok(result);
    }

    @Operation(summary = "新增或覆寫範本（items 為明細 JSON 陣列）")
    @PreAuthorize("hasAuthority('ENT_FEE_TEMPLATE_EDIT')")
    @MethodLog(remark = "儲存費率範本")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes save() {
        requireAuthority("ENT_FEE_TEMPLATE_EDIT");
        FeeTemplate template = getObject(FeeTemplate.class);
        template.setTemplateId(getValLong("templateId"));
        return ApiRes.ok(feeTemplateService.saveTemplate(template,
                JSONArray.parseArray(getValStringRequired("items"), FeeTemplateItem.class),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "刪除範本（不影響已套用的規則）")
    @PreAuthorize("hasAuthority('ENT_FEE_TEMPLATE_EDIT')")
    @MethodLog(remark = "刪除費率範本")
    @RequestMapping(value = "/{templateId}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("templateId") Long templateId) {
        requireAuthority("ENT_FEE_TEMPLATE_EDIT");
        feeTemplateService.removeTemplate(templateId);
        return ApiRes.ok();
    }

    @Operation(summary = "套用範本到多個代理或商戶（targetType=AGENT/MCH，targetIds 為 JSON 陣列）")
    @PreAuthorize("hasAuthority('ENT_FEE_TEMPLATE_APPLY') and hasAuthority('ENT_FEE_RULE_EDIT')")
    @MethodLog(remark = "套用費率範本")
    @RequestMapping(value = "/{templateId}/apply", method = RequestMethod.POST)
    public ApiRes apply(@PathVariable("templateId") Long templateId) {
        requireAuthority("ENT_FEE_TEMPLATE_APPLY");
        requireAuthority("ENT_FEE_RULE_EDIT");
        return ApiRes.ok(feeTemplateService.apply(templateId, getValStringRequired("targetType"),
                JSONArray.parseArray(getValStringRequired("targetIds"), String.class),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }
}
