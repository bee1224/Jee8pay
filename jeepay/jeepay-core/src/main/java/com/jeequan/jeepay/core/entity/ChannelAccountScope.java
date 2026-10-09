package com.jeequan.jeepay.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 渠道帳號使用範圍（ADR-0012）：帳號仍屬於團長，但可限定只給他的某幾位隊長的商戶使用。
 * 某位團長在某個帳號上沒有任何列 = 他這一支全部可用；有列 = 只有列出的隊長的商戶可用（團長直屬商戶也不可用）。
 */
@Schema(description = "渠道帳號使用範圍表")
@Data
@Accessors(chain = true)
@TableName("t_channel_account_scope")
public class ChannelAccountScope implements Serializable {

    public static final LambdaQueryWrapper<ChannelAccountScope> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(title = "accountId", description = "渠道帳號ID")
    private String accountId;

    @Schema(title = "srAgentNo", description = "隊長所屬的團長代理號")
    private String srAgentNo;

    @Schema(title = "agentNo", description = "可使用的隊長代理號")
    private String agentNo;

    @Schema(title = "createdBy", description = "設定者姓名")
    private String createdBy;

    @Schema(title = "createdAt", description = "設定時間")
    private Date createdAt;
}
