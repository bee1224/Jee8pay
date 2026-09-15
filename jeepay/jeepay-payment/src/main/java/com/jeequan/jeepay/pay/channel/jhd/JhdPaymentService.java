package com.jeequan.jeepay.pay.channel.jhd;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.pay.channel.AbstractPaymentService;
import com.jeequan.jeepay.pay.channel.IPaymentService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import com.jeequan.jeepay.pay.util.PaywayUtil;
import org.springframework.stereotype.Service;

/** JHD payment capability dispatcher。 */
@Service
public class JhdPaymentService extends AbstractPaymentService {

    @Override
    public String getIfCode() {
        return CS.IF_CODE.JHD;
    }

    @Override
    public boolean isSupport(String wayCode) {
        return CS.PAY_WAY_CODE.JHD_IBON.equals(wayCode);
    }

    @Override
    public String preCheck(UnifiedOrderRQ bizRQ, PayOrder payOrder) {
        IPaymentService payway = PaywayUtil.getRealPaywayService(this, payOrder.getWayCode());
        return payway == null ? "JHD 不支援該支付方式" : payway.preCheck(bizRQ, payOrder);
    }

    @Override
    public AbstractRS pay(UnifiedOrderRQ bizRQ, PayOrder payOrder, MchAppConfigContext mchAppConfigContext)
            throws Exception {
        IPaymentService payway = PaywayUtil.getRealPaywayService(this, payOrder.getWayCode());
        if (payway == null) {
            throw new IllegalArgumentException("JHD 支付方式不可用");
        }
        return payway.pay(bizRQ, payOrder, mchAppConfigContext);
    }
}
