package com.jeequan.jeepay.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.entity.PayOrderFee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.Map;

/**
 * 分潤統計只計入支付成功（state=2）的訂單；已退款（state=5）等狀態不計，
 * 退款後分潤如何追回屬結算規則，另行決定。
 */
public interface PayOrderFeeMapper extends BaseMapper<PayOrderFee> {

    String PROFIT_WHERE = " FROM t_pay_order_fee f JOIN t_pay_order o ON o.pay_order_id = f.pay_order_id"
            + " WHERE o.state = 2 AND (f.sr_agent_no = #{agentNo} OR f.agent_no = #{agentNo})"
            + "<if test='start != null'> AND o.success_time &gt;= #{start}</if>"
            + "<if test='end != null'> AND o.success_time &lt; #{end}</if>";

    /** 單一代理的分潤：身為高級代理取高代費、身為直屬一般代理取代理費。 */
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
}
