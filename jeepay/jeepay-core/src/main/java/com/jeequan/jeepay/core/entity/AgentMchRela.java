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
 * 商戶與代理綁定（ADR-0009）：直屬代理決定費率瀑布的代理層。
 */
@Schema(description = "商戶與代理綁定表")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_agent_mch_rela")
public class AgentMchRela extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<AgentMchRela> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "mchNo", description = "商戶號")
    @TableId(value = "mch_no", type = IdType.INPUT)
    private String mchNo;

    @Schema(title = "agentNo", description = "直屬代理號")
    private String agentNo;


    @Schema(title = "updatedUid", description = "最後修改者用戶ID")
    private Long updatedUid;

    @Schema(title = "updatedBy", description = "最後修改者姓名")
    private String updatedBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;
}
