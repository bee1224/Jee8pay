package com.jeequan.jeepay.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 背景匯出工作（下載中心）：列表頁按「匯出」只建立工作，背景產生 CSV 後由申請人下載。 */
@Schema(description = "匯出工作")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_export_job")
public class ExportJob extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<ExportJob> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final byte STATE_QUEUED = 0;
    public static final byte STATE_RUNNING = 1;
    public static final byte STATE_DONE = 2;
    public static final byte STATE_FAILED = 3;

    @Schema(title = "jobId", description = "工作ID")
    @TableId(value = "job_id", type = IdType.AUTO)
    private Long jobId;

    @Schema(title = "sysType", description = "所屬系統: MGR/MCH")
    private String sysType;

    @Schema(title = "belongInfoId", description = "所屬（商戶平台為商戶號，營運平台為 0）")
    private String belongInfoId;

    @Schema(title = "ownerUid", description = "申請人用戶ID")
    private Long ownerUid;

    @Schema(title = "ownerName", description = "申請人")
    private String ownerName;

    @Schema(title = "jobType", description = "匯出類型: PAY_ORDER/WALLET_LEDGER/WITHDRAW/SETTLE_DAILY")
    private String jobType;

    @Schema(title = "params", description = "篩選條件（JSON）")
    private String params;

    @Schema(title = "state", description = "狀態: 0-排隊中, 1-產生中, 2-完成, 3-失敗")
    private Byte state;

    @Schema(title = "fileName", description = "下載檔名")
    private String fileName;

    @Schema(title = "rowCount", description = "資料筆數")
    private Long rowCount;

    @Schema(title = "errorMsg", description = "失敗原因")
    private String errorMsg;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "finishedAt", description = "完成時間")
    private Date finishedAt;
}
