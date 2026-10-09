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

/** 費率範本：一組支付方式 × 代理層費率，可批次套用到多個代理或商戶；只含團長費／隊長費，平臺層不走範本。 */
@Schema(description = "費率範本")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_fee_template")
public class FeeTemplate extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<FeeTemplate> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "templateId", description = "範本ID")
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    @Schema(title = "templateName", description = "範本名稱")
    private String templateName;

    @Schema(title = "remark", description = "備註")
    private String remark;

    @Schema(title = "updatedUid", description = "最後修改者用戶ID")
    private Long updatedUid;

    @Schema(title = "updatedBy", description = "最後修改者姓名")
    private String updatedBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;
}
