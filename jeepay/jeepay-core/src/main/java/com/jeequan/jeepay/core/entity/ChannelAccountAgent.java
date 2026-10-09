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

/** 渠道帳號派發（ADR-0012）：哪些團長可以使用這個帳號；擁有者固定有一列。 */
@Schema(description = "渠道帳號派發表")
@Data
@Accessors(chain = true)
@TableName("t_channel_account_agent")
public class ChannelAccountAgent implements Serializable {

    public static final LambdaQueryWrapper<ChannelAccountAgent> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(title = "accountId", description = "渠道帳號ID")
    private String accountId;

    @Schema(title = "srAgentNo", description = "被派發的團長代理號")
    private String srAgentNo;

    @Schema(title = "createdBy", description = "派發者姓名")
    private String createdBy;

    @Schema(title = "createdAt", description = "派發時間")
    private Date createdAt;
}
