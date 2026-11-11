/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.oa.service;

import com.freecrm.common.flow.FlowCallback;
import com.freecrm.oa.entity.OaLeave;
import com.freecrm.oa.mapper.OaLeaveMapper;
import org.springframework.stereotype.Component;

/** 请假审批结果回写 */
@Component
public class LeaveFlowCallback implements FlowCallback {

    private final OaLeaveMapper leaveMapper;

    public LeaveFlowCallback(OaLeaveMapper leaveMapper) {
        this.leaveMapper = leaveMapper;
    }

    @Override
    public int bizType() {
        return 3;
    }

    @Override
    public void onPassed(long bizId) {
        upd(bizId, 1);
    }

    @Override
    public void onRejected(long bizId) {
        upd(bizId, 2);
    }

    private void upd(long bizId, int status) {
        OaLeave upd = new OaLeave();
        upd.setId(bizId);
        upd.setAuditStatus(status);
        leaveMapper.updateById(upd);
    }
}
