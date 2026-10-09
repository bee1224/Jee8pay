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

/**
 * 代理資訊（ADR-0009）：獨立於服務商（ISV）的代理實體，團長 → 隊長 兩層。
 * agentPath 為物化路徑（如 /A001/A002/），查詢轄區用前綴比對，不需遞迴。
 */
@Schema(description = "代理資訊表")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_agent_info")
public class AgentInfo extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<AgentInfo> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    /** 團長 */
    public static final byte LEVEL_SENIOR = 1;
    /** 隊長 */
    public static final byte LEVEL_AGENT = 2;

    @Schema(title = "agentNo", description = "代理號")
    @TableId(value = "agent_no", type = IdType.INPUT)
    private String agentNo;

    @Schema(title = "agentName", description = "代理名稱")
    private String agentName;

    @Schema(title = "agentLevel", description = "代理層級: 1-團長, 2-隊長")
    private Byte agentLevel;

    @Schema(title = "parentAgentNo", description = "上級代理號（團長為空）")
    private String parentAgentNo;

    @Schema(title = "agentPath", description = "物化路徑")
    private String agentPath;

    @Schema(title = "contactName", description = "聯絡人姓名")
    private String contactName;

    @Schema(title = "contactTel", description = "聯絡人手機號")
    private String contactTel;

    @Schema(title = "contactEmail", description = "聯絡人信箱")
    private String contactEmail;

    @Schema(title = "state", description = "狀態: 0-停用, 1-正常")
    private Byte state;

    @Schema(title = "remark", description = "備註")
    private String remark;

    @Schema(title = "createdUid", description = "建立者用戶ID")
    private Long createdUid;

    @Schema(title = "createdBy", description = "建立者姓名")
    private String createdBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;

    @Schema(title = "brandEnabled", description = "白標是否啟用: 0-否, 1-是（僅團長）")
    private Byte brandEnabled;

    @Schema(title = "brandTitle", description = "白標站台名稱")
    private String brandTitle;

    @Schema(title = "brandLogo", description = "白標 Logo 圖片位址")
    private String brandLogo;

    @Schema(title = "isHouse", description = "是否平台直屬: 0-否, 1-是（ADR-0012）")
    private Byte isHouse;
}
