/*
 * Jee8pay 專屬：UAT Edge 白名單自助管理（非 upstream JeePay 功能）。
 *
 * 設計：
 * - 白名單唯一來源是 JSON 檔案（預設 /edge-allowlist/allowlist.json，compose 由 host 目錄 bind 進 manager 容器）。
 * - host 端 cron 每分鐘比對 allowlist.json 與 nginx allow 檔（uat.conf），有變更就重新產生並 reload edge，
 *   因此「新增/刪除 IP」儲存後約 1 分鐘內自動套用，不需要人工部署 edge。
 * - 系統保留 IP（Talend 兩台測試機）不可刪除，避免誤刪造成外部 UAT 全斷。
 */
package com.jeequan.jeepay.mgr.ctrl.uatedge;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * UAT Edge 白名單管理
 *
 * @author Jee8pay
 * @date 2026-08-17
 */
@Tag(name = "UAT Edge 白名單")
@RestController
@RequestMapping("api/uatEdge/allowlist")
public class UatEdgeAllowlistController extends CommonCtrl {

    private static final Logger log = LoggerFactory.getLogger(UatEdgeAllowlistController.class);

    /** 系統保留 IP：Talend 測試機（不可刪除） */
    private static final Set<String> PROTECTED_IPS = new HashSet<>();

    static {
        PROTECTED_IPS.add("34.92.245.74");
        PROTECTED_IPS.add("34.92.52.162");
    }

    @Value("${isys.uat-allowlist-path:/edge-allowlist/allowlist.json}")
    private String allowlistPath;

    private Path allowlistFile() {
        return Paths.get(allowlistPath);
    }

    private JSONArray readAllowlist() {
        Path f = allowlistFile();
        if (!Files.exists(f)) {
            return new JSONArray();
        }
        try {
            String text = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            JSONArray arr = JSON.parseArray(text);
            return arr == null ? new JSONArray() : arr;
        } catch (Exception e) {
            log.error("讀取 allowlist 失敗: {}", e.getMessage());
            return new JSONArray();
        }
    }

    private synchronized void writeAllowlist(JSONArray list) throws IOException {
        Path f = allowlistFile();
        Path dir = f.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
        Files.write(tmp, JSON.toJSONString(list).getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** 驗證 IP 或 CIDR（支援 IPv4/IPv6） */
    private boolean isValidIpOrCidr(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        String ipPart = value;
        if (value.contains("/")) {
            String[] parts = value.split("/", 2);
            ipPart = parts[0];
            if (!parts[1].matches("\\d{1,3}")) {
                return false;
            }
            int p = Integer.parseInt(parts[1]);
            if (value.contains(":") && p > 128) {
                return false;
            }
            if (!value.contains(":") && p > 32) {
                return false;
            }
        }
        try {
            InetAddress.getByName(ipPart);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Operation(summary = "查詢白名單與套用狀態")
    @PreAuthorize("hasAuthority('ENT_UAT_EDGE_ALLOWLIST')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiRes list() {
        JSONObject result = new JSONObject();
        result.put("records", readAllowlist());
        // 套用狀態：host cron 寫入的同目錄 status.txt
        Path status = allowlistFile().resolveSibling("status.txt");
        String statusText = "";
        if (Files.exists(status)) {
            try {
                statusText = new String(Files.readAllBytes(status), StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                // 讀取失敗視為無狀態
            }
        }
        result.put("applyStatus", statusText);
        return ApiRes.ok(result);
    }

    @Operation(summary = "新增白名單 IP")
    @MethodLog(remark = "新增 UAT Edge 白名單 IP")
    @PreAuthorize("hasAuthority('ENT_UAT_EDGE_ALLOWLIST')")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ApiRes add() {
        String ip = getValStringRequired("ip").trim();
        String remark = getValString("remark");
        if (!isValidIpOrCidr(ip)) {
            throw new BizException("IP 格式不正確（支援 IPv4 / IPv6 / CIDR）");
        }
        JSONArray list = readAllowlist();
        for (int i = 0; i < list.size(); i++) {
            if (list.getJSONObject(i).getString("ip").equals(ip)) {
                throw new BizException("該 IP 已存在於白名單");
            }
        }
        JSONObject item = new JSONObject();
        item.put("ip", ip);
        item.put("remark", StringUtils.isBlank(remark) ? "" : remark.trim());
        list.add(item);
        try {
            writeAllowlist(list);
        } catch (IOException e) {
            log.error("寫入 allowlist 失敗", e);
            throw new BizException("寫入失敗，請檢查 server 設定");
        }
        return ApiRes.ok();
    }

    @Operation(summary = "刪除白名單 IP")
    @MethodLog(remark = "刪除 UAT Edge 白名單 IP")
    @PreAuthorize("hasAuthority('ENT_UAT_EDGE_ALLOWLIST')")
    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    public ApiRes remove() {
        String ip = getValStringRequired("ip").trim();
        if (PROTECTED_IPS.contains(ip)) {
            throw new BizException("系統保留 IP 不可刪除");
        }
        JSONArray list = readAllowlist();
        JSONArray after = new JSONArray();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (item.getString("ip").equals(ip)) {
                found = true;
                continue;
            }
            after.add(item);
        }
        if (!found) {
            throw new BizException("該 IP 不存在於白名單");
        }
        try {
            writeAllowlist(after);
        } catch (IOException e) {
            log.error("寫入 allowlist 失敗", e);
            throw new BizException("寫入失敗，請檢查 server 設定");
        }
        return ApiRes.ok();
    }
}
