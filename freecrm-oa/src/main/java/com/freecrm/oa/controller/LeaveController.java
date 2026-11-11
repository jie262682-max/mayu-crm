/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.oa.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.result.R;
import com.freecrm.oa.entity.OaLeave;
import com.freecrm.oa.mapper.OaLeaveMapper;
import com.freecrm.oa.service.FlowEngine;
import org.springframework.web.bind.annotation.*;

/**
 * 请假申请（示例业务：走审批流引擎）
 */
@RestController
@RequestMapping("/api/oa/leave")
public class LeaveController {

    private final OaLeaveMapper leaveMapper;
    private final FlowEngine flowEngine;

    public LeaveController(OaLeaveMapper leaveMapper, FlowEngine flowEngine) {
        this.leaveMapper = leaveMapper;
        this.flowEngine = flowEngine;
    }

    @PostMapping("/apply")
    public R<Long> apply(@RequestBody OaLeave leave) {
        long userId = StpUtil.getLoginIdAsLong();
        leave.setUserId(userId);
        leave.setAuditStatus(0);
        if (leave.getStartTime() == null || leave.getEndTime() == null
                || !leave.getEndTime().isAfter(leave.getStartTime())) {
            throw new BizException("请假时间不合法");
        }
        leaveMapper.insert(leave);
        java.util.Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("days", leave.getDays() == null ? 0 : leave.getDays());
        flowEngine.start(3, leave.getId(),
                "请假 " + leave.getDays() + " 天：" + (leave.getReason() == null ? "" : leave.getReason()), userId, vars);
        return R.ok(leave.getId());
    }

    @GetMapping("/get")
    public R<OaLeave> get(@RequestParam Long id) {
        return R.ok(leaveMapper.selectById(id));
    }

    @GetMapping("/page")
    public R<Page<OaLeave>> page(@RequestParam(defaultValue = "1") long pageNo,
                                 @RequestParam(defaultValue = "10") long pageSize,
                                 @RequestParam(required = false) Boolean mine) {
        LambdaQueryWrapper<OaLeave> qw = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(mine)) {
            qw.eq(OaLeave::getUserId, StpUtil.getLoginIdAsLong());
        }
        return R.ok(leaveMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(OaLeave::getId)));
    }
}
