package com.jeequan.jeepay.mch.ctrl.export;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.ExportJob;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mch.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.export.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** 下載中心：建立匯出工作、列出自己的工作、下載完成的檔案。只能看到與下載自己建立的工作。 */
@Tag(name = "下載中心")
@RestController
@RequestMapping("/api/exports")
public class ExportController extends CommonCtrl {

    /** 各匯出類型需要的列表權限（與對應列表頁一致） */
    private static final Map<String, String> TYPE_ENT = Map.of(ExportService.PAY_ORDER, "ENT_ORDER_LIST", ExportService.WALLET_LEDGER, "ENT_MCH_WALLET");

    @Autowired private ExportService exportService;

    @Value("${isys.oss.file-root-path:/tmp}")
    private String fileRootPath;

    @Operation(summary = "我的匯出工作")
    @PreAuthorize("hasAuthority('ENT_MCH_EXPORT_CENTER')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<ExportJob> list() {
        return ApiPageRes.pages(exportService.page(getIPage(), ExportJob.gw()
                .eq(ExportJob::getSysType, CS.SYS_TYPE.MCH)
                .eq(ExportJob::getOwnerUid, getCurrentUser().getSysUser().getSysUserId())
                .orderByDesc(ExportJob::getJobId)));
    }

    @Operation(summary = "建立匯出工作（jobType＋與列表頁相同的篩選條件）")
    @PreAuthorize("hasAuthority('ENT_MCH_EXPORT_CENTER')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes<ExportJob> submit() {
        JSONObject params = getReqParamJSON();
        String jobType = params.getString("jobType");
        String ent = TYPE_ENT.get(jobType);
        if (ent == null || getCurrentUser().getAuthorities().stream().map(GrantedAuthority::getAuthority).noneMatch(ent::equals)) {
            throw new BizException("沒有匯出這項資料的權限");
        }
        params.remove("jobType");
        params.remove("pageNumber");
        params.remove("pageSize");
        return ApiRes.ok(exportService.submit(CS.SYS_TYPE.MCH, getCurrentMchNo(),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname(), jobType, params));
    }

    @Operation(summary = "下載匯出檔")
    @PreAuthorize("hasAuthority('ENT_MCH_EXPORT_CENTER')")
    @RequestMapping(value = "/{jobId}/file", method = RequestMethod.GET)
    public void download(@PathVariable("jobId") Long jobId) throws Exception {
        ExportJob job = exportService.getById(jobId);
        if (job == null || !CS.SYS_TYPE.MCH.equals(job.getSysType())
                || !job.getOwnerUid().equals(getCurrentUser().getSysUser().getSysUserId())) {
            throw new BizException("匯出工作不存在");
        }
        File file = exportService.fileOf(fileRootPath + "/private", job);
        if (job.getState() != ExportJob.STATE_DONE || !file.exists()) {
            throw new BizException("檔案尚未產生或已過期");
        }
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(job.getFileName(), StandardCharsets.UTF_8).replace("+", "%20"));
        response.setContentLengthLong(file.length());
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
        }
    }
}
