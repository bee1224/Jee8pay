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
 * Telegram 機器人整理：上帝登記系統用到的每個 bot（名稱、API token、通知對象、用途）。
 * token 以明文存在資料庫（與 t_pay_interface_config.if_params 相同的已知資安債 TD-001），任何 API 都只回遮罩值。
 */
@Schema(description = "Telegram 機器人")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_telegram_bot")
public class TelegramBot extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<TelegramBot> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @TableId(value = "bot_id", type = IdType.AUTO)
    private Long botId;

    @Schema(title = "botName", description = "名稱（給人看的）")
    private String botName;

    @Schema(title = "botUsername", description = "Telegram 上的 @username（測試連線時自動帶入）")
    private String botUsername;

    @Schema(title = "apiToken", description = "Bot API token（只回遮罩值）")
    private String apiToken;

    @Schema(title = "chatId", description = "通知對象：群組或使用者的 chat_id")
    private String chatId;

    @Schema(title = "purpose", description = "用途")
    private String purpose;

    @Schema(title = "state", description = "0-停用, 1-啟用")
    private Byte state;

    @Schema(title = "remark", description = "備註")
    private String remark;

    @Schema(title = "lastCheckAt", description = "最近一次測試連線時間")
    private Date lastCheckAt;

    @Schema(title = "lastCheckResult", description = "最近一次測試結果")
    private String lastCheckResult;

    @Schema(title = "createdBy", description = "建立者")
    private String createdBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;
}
