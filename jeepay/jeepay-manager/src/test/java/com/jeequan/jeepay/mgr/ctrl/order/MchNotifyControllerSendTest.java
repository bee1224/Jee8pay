package com.jeequan.jeepay.mgr.ctrl.order;

import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MchNotifyController.send（商户通知手动发送）单元测试。
 *
 * 安全红线：仅终态订单（SUCCESS/FAIL）可发送；不改变订单状态；
 * 不调用 Provider；沿用原生 MchNotifyRecord + PayOrderMchNotifyMQ。
 */
@ExtendWith(MockitoExtension.class)
class MchNotifyControllerSendTest {

    @Mock private PayOrderService payOrderService;
    @Mock private MchNotifyRecordService mchNotifyService;
    @Mock private IMQSender mqSender;
    @InjectMocks private MchNotifyController controller;

    private PayOrder payOrder(byte state) {
        PayOrder order = new PayOrder();
        order.setPayOrderId("P_TEST_0001");
        order.setState(state);
        return order;
    }

    private MchNotifyRecord notifyRecord(byte state) {
        MchNotifyRecord record = new MchNotifyRecord();
        record.setNotifyId(100L);
        record.setState(state);
        return record;
    }

    @Test
    void send_unknownOrder_returnsFail() {
        when(payOrderService.getById("P_NONE")).thenReturn(null);
        ApiRes<MchNotifyRecord> result = controller.send("P_NONE");
        assertEquals(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE.getCode(), result.getCode());
    }

    @Test
    void send_waitingOrder_rejected() {
        when(payOrderService.getById("P_TEST_0001")).thenReturn(payOrder(PayOrder.STATE_ING));
        BizException ex = assertThrows(BizException.class, () -> controller.send("P_TEST_0001"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void send_terminalOrderWithoutRecord_rejected() {
        when(payOrderService.getById("P_TEST_0001")).thenReturn(payOrder(PayOrder.STATE_SUCCESS));
        when(mchNotifyService.findByPayOrder("P_TEST_0001")).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> controller.send("P_TEST_0001"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void send_recordInProgress_rejected() {
        when(payOrderService.getById("P_TEST_0001")).thenReturn(payOrder(PayOrder.STATE_SUCCESS));
        when(mchNotifyService.findByPayOrder("P_TEST_0001")).thenReturn(notifyRecord(MchNotifyRecord.STATE_ING));
        BizException ex = assertThrows(BizException.class, () -> controller.send("P_TEST_0001"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void send_successRecord_requeuesNativeMq() {
        com.jeequan.jeepay.service.mapper.MchNotifyRecordMapper mapper =
                mock(com.jeequan.jeepay.service.mapper.MchNotifyRecordMapper.class);
        when(payOrderService.getById("P_TEST_0001")).thenReturn(payOrder(PayOrder.STATE_SUCCESS));
        when(mchNotifyService.findByPayOrder("P_TEST_0001")).thenReturn(notifyRecord(MchNotifyRecord.STATE_SUCCESS));
        when(mchNotifyService.getBaseMapper()).thenReturn(mapper);

        ApiRes<MchNotifyRecord> result = controller.send("P_TEST_0001");

        assertNotNull(result.getData());
        verify(mapper).updateIngAndAddNotifyCountLimit(100L);
        verify(mqSender).send(any(com.jeequan.jeepay.components.mq.model.PayOrderMchNotifyMQ.class));
    }
}
