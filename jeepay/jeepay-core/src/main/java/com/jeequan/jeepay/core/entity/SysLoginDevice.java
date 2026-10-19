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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 登入裝置紀錄：登入成功後由前端回報裝置資訊，IP 與 User-Agent 由後端取自請求。
 * 裝置識別碼是瀏覽器端產生並保存的隨機 ID（網頁無法讀取 IMEI）；型號、GPS 皆為用戶端提供，只作參考，不作為驗證依據。
 * 代理只看得到自己的時間、IP、裝置；GPS 與裝置識別碼只給平台查看。
 */
@Schema(description = "登入裝置紀錄")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_sys_login_device")
public class SysLoginDevice extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<SysLoginDevice> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final String GEO_GRANTED = "GRANTED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(title = "sysUserId", description = "登入的用戶ID")
    private Long sysUserId;

    @Schema(title = "sysType", description = "所屬系統：MGR／MCH")
    private String sysType;

    @Schema(title = "belongInfoId", description = "所屬代理號（平台帳號為 0）")
    private String belongInfoId;

    @Schema(title = "userName", description = "用戶姓名")
    private String userName;

    @Schema(title = "userIp", description = "登入 IP")
    private String userIp;

    @Schema(title = "userAgent", description = "瀏覽器 User-Agent")
    private String userAgent;

    @Schema(title = "deviceId", description = "裝置識別碼（瀏覽器端隨機產生）")
    private String deviceId;

    @Schema(title = "deviceModel", description = "裝置型號")
    private String deviceModel;

    @Schema(title = "deviceOs", description = "作業系統")
    private String deviceOs;

    @Schema(title = "latitude", description = "緯度")
    private BigDecimal latitude;

    @Schema(title = "longitude", description = "經度")
    private BigDecimal longitude;

    @Schema(title = "accuracy", description = "定位精確度（公尺）")
    private Integer accuracy;

    @Schema(title = "geoState", description = "定位狀態：GRANTED／DENIED／UNAVAILABLE／TIMEOUT／UNSUPPORTED")
    private String geoState;

    @Schema(title = "createdAt", description = "登入時間")
    private Date createdAt;
}
