package com.jeequan.jeepay.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.TelegramBot;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.TelegramBotMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/** Telegram 機器人整理：登記、遮罩 token、測試連線（getMe，有 chat_id 時另發一則測試訊息）。 */
@Service
public class TelegramBotService extends ServiceImpl<TelegramBotMapper, TelegramBot> {

    private static final String TOKEN_PATTERN = "\\d{5,15}:[A-Za-z0-9_-]{30,64}";

    /** 列表與詳情一律遮罩：只留前 6 碼與後 4 碼 */
    public static String mask(String token) {
        if (StringUtils.isBlank(token) || token.length() < 12) {
            return StringUtils.isBlank(token) ? "" : "****";
        }
        return token.substring(0, 6) + "****" + token.substring(token.length() - 4);
    }

    public List<TelegramBot> listMasked() {
        List<TelegramBot> list = list(TelegramBot.gw().orderByAsc(TelegramBot::getBotId));
        list.forEach(b -> b.setApiToken(mask(b.getApiToken())));
        return list;
    }

    public TelegramBot create(TelegramBot in, String operator) {
        requireToken(in.getApiToken());
        TelegramBot b = new TelegramBot().setBotName(required(in.getBotName(), "名稱")).setApiToken(in.getApiToken().trim())
                .setChatId(StringUtils.trimToNull(in.getChatId())).setPurpose(required(in.getPurpose(), "用途"))
                .setState(in.getState() == null ? 1 : in.getState()).setRemark(in.getRemark()).setCreatedBy(operator);
        save(b);
        b.setApiToken(mask(b.getApiToken()));
        return b;
    }

    /** token 留空表示不更換 */
    public void update(Long botId, TelegramBot in) {
        if (getById(botId) == null) {
            throw new BizException("機器人不存在");
        }
        TelegramBot u = new TelegramBot().setBotId(botId).setBotName(StringUtils.trimToNull(in.getBotName())).setChatId(in.getChatId())
                .setPurpose(StringUtils.trimToNull(in.getPurpose())).setState(in.getState()).setRemark(in.getRemark());
        if (StringUtils.isNotBlank(in.getApiToken())) {
            requireToken(in.getApiToken());
            u.setApiToken(in.getApiToken().trim()).setBotUsername("");
        }
        updateById(u);
    }

    /** 呼叫 getMe 驗證 token；有 chat_id 時再發一則測試訊息。結果記錄在 lastCheck* */
    public JSONObject check(Long botId, boolean sendTest) {
        TelegramBot b = getById(botId);
        if (b == null) {
            throw new BizException("機器人不存在");
        }
        JSONObject out = new JSONObject();
        String result;
        try {
            JSONObject me = call(b.getApiToken(), "getMe", null);
            String username = me.getJSONObject("result").getString("username");
            out.put("username", username);
            result = "連線正常 @" + username;
            if (sendTest && StringUtils.isNotBlank(b.getChatId())) {
                call(b.getApiToken(), "sendMessage", "chat_id=" + enc(b.getChatId()) + "&text=" + enc("【測試】" + b.getBotName() + "：連線正常"));
                result += "，已發送測試訊息";
            }
            updateById(new TelegramBot().setBotId(botId).setBotUsername(username).setLastCheckAt(new Date()).setLastCheckResult(result));
        } catch (BizException e) {
            result = e.getMessage();
            updateById(new TelegramBot().setBotId(botId).setLastCheckAt(new Date()).setLastCheckResult(result));
        }
        out.put("result", result);
        return out;
    }

    private JSONObject call(String token, String method, String form) {
        String body;
        try {
            HttpRequest req = HttpRequest.post("https://api.telegram.org/bot" + token + "/" + method).timeout(15000);
            if (form != null) {
                req.body(form, "application/x-www-form-urlencoded");
            }
            body = req.execute().body();
        } catch (Exception e) {
            throw new BizException("無法連線到 Telegram");
        }
        JSONObject res = JSON.parseObject(body);
        if (res == null || !res.getBooleanValue("ok")) {
            // 不回傳 Telegram 原始錯誤以外的內容，也不把 token 帶進訊息
            throw new BizException("Telegram 拒絕：" + (res == null ? "回應格式錯誤" : res.getString("description")));
        }
        return res;
    }

    private static void requireToken(String token) {
        if (token == null || !token.trim().matches(TOKEN_PATTERN)) {
            throw new BizException("API token 格式不正確（應為「數字:英數字串」，可向 @BotFather 取得）");
        }
    }

    private static String required(String v, String name) {
        if (StringUtils.isBlank(v)) {
            throw new BizException("請填寫" + name);
        }
        return v.trim();
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
