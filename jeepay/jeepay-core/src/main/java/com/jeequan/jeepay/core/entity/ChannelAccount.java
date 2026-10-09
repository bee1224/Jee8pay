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
 * 渠道帳號（ADR-0012）：一組第三方支付金鑰，屬於一位團長，由上帝建立並派發。
 * 金鑰存在 t_pay_interface_config（info_type=4、info_id=accountId），本表不存金鑰。
 */
@Schema(description = "渠道帳號表")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_channel_account")
public class ChannelAccount extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<ChannelAccount> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "accountId", description = "渠道帳號ID")
    @TableId(value = "account_id", type = IdType.INPUT)
    private String accountId;

    @Schema(title = "ifCode", description = "支付接口代碼")
    private String ifCode;

    @Schema(title = "accountName", description = "帳號名稱")
    private String accountName;

    @Schema(title = "ownerSrAgentNo", description = "所屬團長代理號")
    private String ownerSrAgentNo;

    @Schema(title = "shareable", description = "是否可加派給其他團長: 0-否, 1-是")
    private Byte shareable;

    @Schema(title = "state", description = "狀態: 0-停用, 1-啟用")
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
}
