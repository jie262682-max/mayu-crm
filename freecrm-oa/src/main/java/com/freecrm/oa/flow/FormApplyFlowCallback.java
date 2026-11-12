/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.oa.flow;

import com.freecrm.common.flow.FlowCallback;
import com.freecrm.oa.entity.OaFormApply;
import com.freecrm.oa.mapper.OaFormApplyMapper;
import org.springframework.stereotype.Component;

/** 通用申请单审批回调（bizType=100）：结果回写申请状态 */
@Component
public class FormApplyFlowCallback implements FlowCallback {

    private final OaFormApplyMapper applyMapper;

    public FormApplyFlowCallback(OaFormApplyMapper applyMapper) {
        this.applyMapper = applyMapper;
    }

    @Override
    public int bizType() {
        return 100;
    }

    @Override
    public void onPassed(long bizId) {
        upd(bizId, OaFormApply.PASSED);
    }

    @Override
    public void onRejected(long bizId) {
        upd(bizId, OaFormApply.REJECTED);
    }

    private void upd(long bizId, int status) {
        OaFormApply a = new OaFormApply();
        a.setId(bizId);
        a.setAuditStatus(status);
        applyMapper.updateById(a);
    }
}
