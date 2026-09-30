package com.jeequan.jeepay.service.export;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.ExportJob;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.mapper.ExportJobMapper;
import com.jeequan.jeepay.service.mapper.WalletLedgerMapper;
import com.jeequan.jeepay.service.wallet.WithdrawService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 背景匯出（下載中心）。建立工作與產生檔案分開，大量匯出不會卡住畫面或逾時。
 * 商戶平台的工作一律以申請人所屬商戶號限縮資料，不信任前端傳入的商戶號。
 * 檔案為 UTF-8（含 BOM，Excel 可直接開啟）CSV，金額以「元」輸出；保留 7 天後自動清除。
 */
@Slf4j
@Service
public class ExportService extends ServiceImpl<ExportJobMapper, ExportJob> {

    public static final String PAY_ORDER = "PAY_ORDER";
    public static final String WALLET_LEDGER = "WALLET_LEDGER";
    public static final String WITHDRAW = "WITHDRAW";
    public static final String SETTLE_DAILY = "SETTLE_DAILY";

    private static final Set<String> MGR_TYPES = Set.of(PAY_ORDER, WALLET_LEDGER, WITHDRAW, SETTLE_DAILY);
    private static final Set<String> MCH_TYPES = Set.of(PAY_ORDER, WALLET_LEDGER);
    private static final int MAX_ROWS = 200_000;
    private static final int PAGE = 1000;
    private static final Map<String, String> TYPE_NAMES = Map.of(PAY_ORDER, "支付訂單", WALLET_LEDGER, "錢包流水",
            WITHDRAW, "提現單", SETTLE_DAILY, "每日結算彙總");

    @Autowired private PayOrderService payOrderService;
    @Autowired private WalletLedgerMapper walletLedgerMapper;
    @Autowired private WithdrawService withdrawService;

    public ExportJob submit(String sysType, String belongInfoId, Long uid, String name, String jobType, JSONObject params) {
        Set<String> allowed = CS.SYS_TYPE.MCH.equals(sysType) ? MCH_TYPES : MGR_TYPES;
        if (!allowed.contains(jobType)) {
            throw new BizException("不支援的匯出類型");
        }
        long running = count(ExportJob.gw().eq(ExportJob::getOwnerUid, uid).eq(ExportJob::getSysType, sysType)
                .in(ExportJob::getState, ExportJob.STATE_QUEUED, ExportJob.STATE_RUNNING));
        if (running >= 3) {
            throw new BizException("已有 3 個匯出正在處理，請稍後再試");
        }
        ExportJob job = new ExportJob().setSysType(sysType).setBelongInfoId(belongInfoId).setOwnerUid(uid).setOwnerName(name)
                .setJobType(jobType).setParams(params == null ? "{}" : params.toJSONString()).setState(ExportJob.STATE_QUEUED)
                .setFileName(TYPE_NAMES.get(jobType) + "_" + DateUtil.format(new Date(), "yyyyMMdd_HHmmss") + ".csv");
        save(job);
        return job;
    }

    public File fileOf(String rootDir, ExportJob job) {
        return new File(rootDir, "exports/" + job.getJobId() + ".csv");
    }

    /** 取出一個排隊中的工作並產生檔案；沒有工作時回傳 false。 */
    public boolean runNext(String sysType, String rootDir) {
        ExportJob job = getOne(ExportJob.gw().eq(ExportJob::getSysType, sysType).eq(ExportJob::getState, ExportJob.STATE_QUEUED)
                .orderByAsc(ExportJob::getJobId).last("LIMIT 1"));
        if (job == null) {
            return false;
        }
        boolean claimed = update(new LambdaUpdateWrapper<ExportJob>().eq(ExportJob::getJobId, job.getJobId())
                .eq(ExportJob::getState, ExportJob.STATE_QUEUED).set(ExportJob::getState, ExportJob.STATE_RUNNING));
        if (!claimed) {
            return true;
        }
        File file = fileOf(rootDir, job);
        try {
            Files.createDirectories(file.getParentFile().toPath());
            long rows = write(job, file);
            update(new LambdaUpdateWrapper<ExportJob>().eq(ExportJob::getJobId, job.getJobId())
                    .set(ExportJob::getState, ExportJob.STATE_DONE).set(ExportJob::getRowCount, rows)
                    .set(ExportJob::getFinishedAt, new Date()));
        } catch (Exception e) {
            log.error("匯出失敗 jobId={}", job.getJobId(), e);
            file.delete();
            update(new LambdaUpdateWrapper<ExportJob>().eq(ExportJob::getJobId, job.getJobId())
                    .set(ExportJob::getState, ExportJob.STATE_FAILED)
                    .set(ExportJob::getErrorMsg, StringUtils.abbreviate(e.getMessage(), 250))
                    .set(ExportJob::getFinishedAt, new Date()));
        }
        return true;
    }

    /** 清除 7 天前的工作與檔案。 */
    public void purge(String sysType, String rootDir) {
        Date before = DateUtil.offsetDay(new Date(), -7);
        for (ExportJob job : list(ExportJob.gw().eq(ExportJob::getSysType, sysType).lt(ExportJob::getCreatedAt, before))) {
            fileOf(rootDir, job).delete();
            removeById(job.getJobId());
        }
    }

    private long write(ExportJob job, File file) throws IOException {
        JSONObject p = JSONObject.parseObject(StringUtils.defaultIfBlank(job.getParams(), "{}"));
        boolean mch = CS.SYS_TYPE.MCH.equals(job.getSysType());
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write('﻿');
            switch (job.getJobType()) {
                case PAY_ORDER: return payOrders(w, p, mch ? job.getBelongInfoId() : p.getString("mchNo"), mch);
                case WALLET_LEDGER: return ledger(w, p, mch ? job.getBelongInfoId() : null);
                case WITHDRAW: return withdraws(w, p);
                case SETTLE_DAILY: return settleDaily(w, p);
                default: throw new BizException("不支援的匯出類型");
            }
        }
    }

    private long payOrders(BufferedWriter w, JSONObject p, String mchNo, boolean forMerchant) throws IOException {
        row(w, "支付訂單號", "商戶號", "商戶名稱", "商戶訂單號", "支付方式", "金額（元）", "手續費（元）", "狀態", "建立時間", "成功時間");
        long n = 0;
        for (int page = 1; n < MAX_ROWS; page++) {
            var wr = PayOrder.gw()
                    .eq(StringUtils.isNotBlank(mchNo), PayOrder::getMchNo, mchNo)
                    .eq(StringUtils.isNotBlank(p.getString("wayCode")), PayOrder::getWayCode, p.getString("wayCode"))
                    .eq(p.getByte("state") != null, PayOrder::getState, p.getByte("state"))
                    .ge(StringUtils.isNotBlank(p.getString("createdStart")), PayOrder::getCreatedAt, date(p.getString("createdStart")))
                    .le(StringUtils.isNotBlank(p.getString("createdEnd")), PayOrder::getCreatedAt, date(p.getString("createdEnd")))
                    .orderByDesc(PayOrder::getCreatedAt);
            // 商戶平台強制限縮為自己的訂單（mchNo 必定有值）
            if (forMerchant && StringUtils.isBlank(mchNo)) {
                throw new BizException("商戶號缺失");
            }
            IPage<PayOrder> pg = payOrderService.page(new Page<>(page, PAGE, false), wr);
            for (PayOrder o : pg.getRecords()) {
                row(w, o.getPayOrderId(), o.getMchNo(), o.getMchName(), o.getMchOrderNo(), o.getWayCode(), yuan(o.getAmount()),
                        yuan(o.getMchFeeAmount()), orderState(o.getState()), fmt(o.getCreatedAt()), fmt(o.getSuccessTime()));
                n++;
            }
            if (pg.getRecords().size() < PAGE) {
                break;
            }
        }
        return n;
    }

    private long ledger(BufferedWriter w, JSONObject p, String forcedMchNo) throws IOException {
        row(w, "時間", "帳戶類型", "帳戶", "類型", "單號", "可用變動（元）", "凍結變動（元）", "變動後可用（元）", "說明", "操作者");
        long n = 0;
        String ownerType = forcedMchNo != null ? WalletAccount.OWNER_MCH : p.getString("ownerType");
        String ownerId = forcedMchNo != null ? forcedMchNo : p.getString("ownerId");
        for (int page = 1; n < MAX_ROWS; page++) {
            var wr = WalletLedger.gw()
                    .eq(StringUtils.isNotBlank(ownerType), WalletLedger::getOwnerType, ownerType)
                    .eq(StringUtils.isNotBlank(ownerId), WalletLedger::getOwnerId, ownerId)
                    .eq(StringUtils.isNotBlank(p.getString("bizType")), WalletLedger::getBizType, p.getString("bizType"))
                    .ge(StringUtils.isNotBlank(p.getString("startDate")), WalletLedger::getCreatedAt, dayStart(p.getString("startDate")))
                    .lt(StringUtils.isNotBlank(p.getString("endDate")), WalletLedger::getCreatedAt, dayEnd(p.getString("endDate")))
                    .orderByDesc(WalletLedger::getLedgerId);
            IPage<WalletLedger> pg = walletLedgerMapper.selectPage(new Page<>(page, PAGE, false), wr);
            for (WalletLedger l : pg.getRecords()) {
                row(w, fmt(l.getCreatedAt()), l.getOwnerType(), l.getOwnerId(), l.getBizType(), l.getBizId(), yuan(l.getAmount()),
                        yuan(l.getFrozenChange()), yuan(l.getBalanceAfter()), l.getRemark(), l.getOperatorName());
                n++;
            }
            if (pg.getRecords().size() < PAGE) {
                break;
            }
        }
        return n;
    }

    private long withdraws(BufferedWriter w, JSONObject p) throws IOException {
        row(w, "提現單號", "申請人類型", "申請人", "申請（元）", "手續費（元）", "實付（元）", "銀行", "銀行代碼", "帳號", "戶名",
                "風控提示", "狀態", "匯款單號", "審核人", "審核說明", "申請時間", "審核時間");
        long n = 0;
        for (int page = 1; n < MAX_ROWS; page++) {
            var wr = WithdrawOrder.gw()
                    .eq(p.getByte("state") != null, WithdrawOrder::getState, p.getByte("state"))
                    .eq(StringUtils.isNotBlank(p.getString("ownerId")), WithdrawOrder::getOwnerId, p.getString("ownerId"))
                    .ge(StringUtils.isNotBlank(p.getString("startDate")), WithdrawOrder::getCreatedAt, dayStart(p.getString("startDate")))
                    .lt(StringUtils.isNotBlank(p.getString("endDate")), WithdrawOrder::getCreatedAt, dayEnd(p.getString("endDate")))
                    .orderByDesc(WithdrawOrder::getCreatedAt);
            IPage<WithdrawOrder> pg = withdrawService.page(new Page<>(page, PAGE, false), wr);
            for (WithdrawOrder o : pg.getRecords()) {
                row(w, o.getWithdrawId(), o.getOwnerType(), o.getOwnerId(), yuan(o.getAmount()), yuan(o.getFee()), yuan(o.getActualAmount()),
                        o.getBankName(), o.getBankCode(), o.getAccountNo(), o.getAccountName(), o.getRiskFlags(),
                        withdrawState(o.getState()), o.getPaidRef(), o.getReviewerName(), o.getReviewRemark(),
                        fmt(o.getCreatedAt()), fmt(o.getReviewedAt()));
                n++;
            }
            if (pg.getRecords().size() < PAGE) {
                break;
            }
        }
        return n;
    }

    private long settleDaily(BufferedWriter w, JSONObject p) throws IOException {
        Date start = StringUtils.isNotBlank(p.getString("startDate")) ? dayStart(p.getString("startDate")) : DateUtil.offsetDay(DateUtil.beginOfDay(new Date()), -30);
        Date end = StringUtils.isNotBlank(p.getString("endDate")) ? dayEnd(p.getString("endDate")) : DateUtil.offsetDay(DateUtil.beginOfDay(new Date()), 1);
        row(w, "入帳日", "結算訂單數", "沖回訂單數", "商戶實收（元）", "代理分潤（元）", "上游渠道（元）", "平台收入（元）", "合計（元）");
        Map<String, Map<String, Long>> days = new TreeMap<>();
        for (Map<String, Object> r : walletLedgerMapper.settleDaily(start, end)) {
            String day = String.valueOf(r.get("day"));
            Map<String, Long> d = days.computeIfAbsent(day, k -> new LinkedHashMap<>());
            String bizType = String.valueOf(r.get("bizType"));
            long orders = ((Number) r.get("orders")).longValue();
            // 同一天同類型的訂單數在每個帳戶類型都出現一次，取最大值即為訂單數
            String countKey = WalletLedger.BIZ_ORDER_SETTLE.equals(bizType) ? "settled" : "reversed";
            d.merge(countKey, orders, Math::max);
            d.merge(String.valueOf(r.get("ownerType")), ((Number) r.get("amount")).longValue(), Long::sum);
        }
        long n = 0;
        for (Map.Entry<String, Map<String, Long>> e : days.entrySet()) {
            Map<String, Long> d = e.getValue();
            long mchAmt = d.getOrDefault(WalletAccount.OWNER_MCH, 0L);
            long agent = d.getOrDefault(WalletAccount.OWNER_AGENT, 0L);
            long channel = d.getOrDefault(WalletAccount.OWNER_CHANNEL, 0L);
            long platform = d.getOrDefault(WalletAccount.OWNER_PLATFORM, 0L);
            row(w, e.getKey(), String.valueOf(d.getOrDefault("settled", 0L)), String.valueOf(d.getOrDefault("reversed", 0L)),
                    yuan(mchAmt), yuan(agent), yuan(channel), yuan(platform), yuan(mchAmt + agent + channel + platform));
            n++;
        }
        return n;
    }

    static void row(BufferedWriter w, String... cols) throws IOException {
        List<String> out = new ArrayList<>(cols.length);
        for (String c : cols) {
            String v = c == null ? "" : c;
            // 以 = + - @ 開頭的內容加上單引號，避免在試算表中被當成公式執行
            if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?[\\d,.]+")) {
                v = "'" + v;
            }
            out.add(v.contains(",") || v.contains("\"") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v);
        }
        w.write(String.join(",", out));
        w.write("\r\n");
    }

    private static String yuan(Long fen) {
        return fen == null ? "" : BigDecimal.valueOf(fen).movePointLeft(2).toPlainString();
    }

    private static String fmt(Date d) {
        return d == null ? "" : DateUtil.formatDateTime(d);
    }

    private static Date date(String v) {
        return StringUtils.isBlank(v) ? null : DateUtil.parse(v);
    }

    private static Date dayStart(String v) {
        return StringUtils.isBlank(v) ? null : DateUtil.beginOfDay(DateUtil.parseDate(v));
    }

    private static Date dayEnd(String v) {
        return StringUtils.isBlank(v) ? null : DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parseDate(v)), 1);
    }

    private static String orderState(Byte s) {
        String[] names = {"訂單生成", "支付中", "支付成功", "支付失敗", "已撤銷", "已退款", "訂單關閉"};
        return s == null || s < 0 || s >= names.length ? String.valueOf(s) : names[s];
    }

    private static String withdrawState(Byte s) {
        String[] names = {"待審核", "已撥款", "已駁回", "已取消"};
        return s == null || s < 0 || s >= names.length ? String.valueOf(s) : names[s];
    }
}
