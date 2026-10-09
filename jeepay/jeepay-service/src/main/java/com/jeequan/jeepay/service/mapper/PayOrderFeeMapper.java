package com.jeequan.jeepay.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.entity.PayOrderFee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 分潤統計只計入支付成功（state=2）的訂單；已退款（state=5）不計，
 * 已結算入帳的分潤在退款後由結算任務反向沖回（ADR-0010）。
 */
public interface PayOrderFeeMapper extends BaseMapper<PayOrderFee> {

    String PROFIT_WHERE = " FROM t_pay_order_fee f JOIN t_pay_order o ON o.pay_order_id = f.pay_order_id"
            + " WHERE o.state = 2 AND (f.sr_agent_no = #{agentNo} OR f.agent_no = #{agentNo})"
            + "<if test='start != null'> AND o.success_time &gt;= #{start}</if>"
            + "<if test='end != null'> AND o.success_time &lt; #{end}</if>";

    /** 單一代理的分潤：身為團長取團長費、身為直屬隊長取隊長費。 */
    String PROFIT_EXPR = "(CASE WHEN f.sr_agent_no = #{agentNo} THEN f.sr_agent_fee ELSE 0 END"
            + " + CASE WHEN f.agent_no = #{agentNo} THEN f.agent_fee ELSE 0 END)";

    @Select("<script>SELECT COUNT(*) AS orderCount, IFNULL(SUM(f.amount), 0) AS amount, IFNULL(SUM(" + PROFIT_EXPR + "), 0) AS profit"
            + PROFIT_WHERE + "</script>")
    Map<String, Object> sumAgentProfit(@Param("agentNo") String agentNo, @Param("start") Date start, @Param("end") Date end);

    @Select("<script>SELECT f.pay_order_id AS payOrderId, f.mch_no AS mchNo, o.mch_name AS mchName, f.way_code AS wayCode,"
            + " f.amount AS amount, " + PROFIT_EXPR + " AS profit, o.success_time AS successTime"
            + PROFIT_WHERE + " ORDER BY o.success_time DESC</script>")
    IPage<Map<String, Object>> pageAgentProfit(IPage<?> page, @Param("agentNo") String agentNo,
                                               @Param("start") Date start, @Param("end") Date end);

    /** 待結算：快照未結算、訂單支付成功且成功時間早於結算截止點（T+N）。回傳訂單號與支付接口。 */
    @Select("SELECT f.pay_order_id AS payOrderId, o.if_code AS ifCode FROM t_pay_order_fee f"
            + " JOIN t_pay_order o ON o.pay_order_id = f.pay_order_id"
            + " WHERE f.settle_state = 0 AND o.state = 2 AND o.success_time < #{cutoff} ORDER BY o.success_time LIMIT #{limit}")
    List<Map<String, Object>> listSettleDue(@Param("cutoff") Date cutoff, @Param("limit") int limit);

    /** 待沖回：已結算但訂單已全額退款。 */
    @Select("SELECT f.pay_order_id AS payOrderId, o.if_code AS ifCode FROM t_pay_order_fee f"
            + " JOIN t_pay_order o ON o.pay_order_id = f.pay_order_id"
            + " WHERE f.settle_state = 1 AND o.state = 5 LIMIT #{limit}")
    List<Map<String, Object>> listReverseDue(@Param("limit") int limit);

    /** 結算前就已退款的快照標記為不結算。 */
    @Update("UPDATE t_pay_order_fee f JOIN t_pay_order o ON o.pay_order_id = f.pay_order_id"
            + " SET f.settle_state = 3, f.settled_at = NOW() WHERE f.settle_state = 0 AND o.state = 5")
    int skipRefundedBeforeSettle();

    /** 以狀態條件更新，確保同一筆快照只被結算或沖回一次。 */
    @Update("UPDATE t_pay_order_fee SET settle_state = #{to}, settled_at = NOW() WHERE pay_order_id = #{payOrderId} AND settle_state = #{from}")
    int transitSettleState(@Param("payOrderId") String payOrderId, @Param("from") byte from, @Param("to") byte to);
}
