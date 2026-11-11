/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.flow;

import com.freecrm.common.exception.BizException;
import com.freecrm.common.flow.FlowCallback;
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.entity.CrmPayment;
import com.freecrm.crm.mapper.CrmContractMapper;
import com.freecrm.crm.mapper.CrmPaymentMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 回款审批回调：通过 -> 回款置通过并累计合同已回款；驳回 -> 回款置驳回（不累计）
 */
@Component
public class PaymentFlowCallback implements FlowCallback {

    private final CrmPaymentMapper paymentMapper;
    private final CrmContractMapper contractMapper;

    public PaymentFlowCallback(CrmPaymentMapper paymentMapper, CrmContractMapper contractMapper) {
        this.paymentMapper = paymentMapper;
        this.contractMapper = contractMapper;
    }

    @Override
    public int bizType() {
        return 2;
    }

    @Override
    public void onPassed(long bizId) {
        CrmPayment p = paymentMapper.selectById(bizId);
        if (p == null) {
            throw new BizException("回款单不存在：" + bizId);
        }
        CrmContract db = contractMapper.selectById(p.getContractId());
        BigDecimal received = db.getReceivedAmount() == null ? BigDecimal.ZERO : db.getReceivedAmount();
        BigDecimal amount = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
        if (received.add(amount).compareTo(db.getTotalAmount()) > 0) {
            throw new BizException("回款累计超过合同总额"); // 异常回滚流程完成，保证不超收
        }
        CrmPayment pUpd = new CrmPayment();
        pUpd.setId(bizId);
        pUpd.setAuditStatus(1);
        paymentMapper.updateById(pUpd);
        CrmContract upd = new CrmContract();
        upd.setId(db.getId());
        upd.setReceivedAmount(received.add(amount));
        contractMapper.updateById(upd);
    }

    @Override
    public void onRejected(long bizId) {
        CrmPayment pUpd = new CrmPayment();
        pUpd.setId(bizId);
        pUpd.setAuditStatus(2);
        paymentMapper.updateById(pUpd);
    }
}
