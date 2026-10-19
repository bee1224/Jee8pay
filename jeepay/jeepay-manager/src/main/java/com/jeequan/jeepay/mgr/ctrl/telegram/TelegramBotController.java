package com.jeequan.jeepay.mgr.ctrl.telegram;

import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.TelegramBot;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 上帝的 Telegram 機器人整理：名稱、API token（只回遮罩）、通知對象、用途，可測試連線。 */
@Tag(name = "Telegram 機器人")
@RestController
@RequestMapping("/api/telegramBots")
public class TelegramBotController extends CommonCtrl {

    @Autowired private TelegramBotService telegramBotService;

    @Operation(summary = "機器人列表（token 遮罩）")
    @PreAuthorize("hasAuthority('ENT_TELEGRAM_BOT')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiRes list() {
        return ApiRes.ok(telegramBotService.listMasked());
    }

    @Operation(summary = "新增機器人")
    @PreAuthorize("hasAuthority('ENT_TELEGRAM_BOT_EDIT')")
    @MethodLog(remark = "新增 Telegram 機器人")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes add() {
        return ApiRes.ok(telegramBotService.create(getObject(TelegramBot.class), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "修改機器人（token 留空 = 不更換）")
    @PreAuthorize("hasAuthority('ENT_TELEGRAM_BOT_EDIT')")
    @MethodLog(remark = "修改 Telegram 機器人")
    @RequestMapping(value = "/{botId}", method = RequestMethod.PUT)
    public ApiRes update(@PathVariable("botId") Long botId) {
        telegramBotService.update(botId, getObject(TelegramBot.class));
        return ApiRes.ok();
    }

    @Operation(summary = "刪除機器人")
    @PreAuthorize("hasAuthority('ENT_TELEGRAM_BOT_EDIT')")
    @MethodLog(remark = "刪除 Telegram 機器人")
    @RequestMapping(value = "/{botId}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("botId") Long botId) {
        telegramBotService.removeById(botId);
        return ApiRes.ok();
    }

    @Operation(summary = "測試連線（getMe；有通知對象時另發測試訊息）")
    @PreAuthorize("hasAuthority('ENT_TELEGRAM_BOT_EDIT')")
    @MethodLog(remark = "測試 Telegram 機器人")
    @RequestMapping(value = "/{botId}/check", method = RequestMethod.POST)
    public ApiRes check(@PathVariable("botId") Long botId) {
        return ApiRes.ok(telegramBotService.check(botId, getReqParamJSON().getBooleanValue("sendTest")));
    }
}
