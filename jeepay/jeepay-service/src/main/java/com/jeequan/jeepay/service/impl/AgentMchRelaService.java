package com.jeequan.jeepay.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.AgentMchRelaMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** 商戶與代理綁定服務（ADR-0009）：每個商戶一個直屬代理。 */
@Service
public class AgentMchRelaService extends ServiceImpl<AgentMchRelaMapper, AgentMchRela> {

    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private MchInfoService mchInfoService;

    public void bind(String mchNo, String agentNo, Long operatorUid, String operatorName) {
        if (mchInfoService.getById(mchNo) == null) {
            throw new BizException("商戶不存在");
        }
        AgentInfo agent = StringUtils.isBlank(agentNo) ? null : agentInfoMapper.selectById(agentNo);
        if (agent == null) {
            throw new BizException("直屬代理不存在");
        }
        if (!Objects.equals(agent.getState(), (byte) 1)) {
            throw new BizException("直屬代理已停用");
        }
        AgentMchRela rela = new AgentMchRela()
                .setMchNo(mchNo)
                .setAgentNo(agentNo)
                .setUpdatedUid(operatorUid)
                .setUpdatedBy(operatorName);
        boolean ok = getById(mchNo) == null ? save(rela) : updateById(rela);
        if (!ok) {
            throw new BizException("綁定代理失敗");
        }
    }

    public void unbind(String mchNo) {
        removeById(mchNo);
    }
}
