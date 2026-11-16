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

import com.freecrm.common.flow.FlowCallback;
import com.freecrm.crm.entity.CrmInvoice;
import com.freecrm.crm.mapper.CrmInvoiceMapper;
import org.springframework.stereotype.Component;

/**
 * 开票审批回调：通过 -> 发票置已开；驳回 -> 置驳回（金额不累计，可重新申请）
 */
@Component
public class InvoiceFlowCallback implements FlowCallback {

    private final CrmInvoiceMapper invoiceMapper;

    public InvoiceFlowCallback(CrmInvoiceMapper invoiceMapper) {
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    public int bizType() {
        return 5;
    }

    @Override
    public void onPassed(long bizId) {
        CrmInvoice upd = new CrmInvoice();
        upd.setId(bizId);
        upd.setStatus(1);
        invoiceMapper.updateById(upd);
    }

    @Override
    public void onRejected(long bizId) {
        CrmInvoice upd = new CrmInvoice();
        upd.setId(bizId);
        upd.setStatus(2);
        invoiceMapper.updateById(upd);
    }
}
