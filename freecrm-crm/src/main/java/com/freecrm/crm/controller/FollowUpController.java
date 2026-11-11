/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.entity.CrmFollowUp;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.mapper.CrmFollowUpMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/follow-up")
public class FollowUpController {

    private final CrmFollowUpMapper followUpMapper;
    private final CrmCustomerMapper customerMapper;
    private final TeamService teamService;

    public FollowUpController(CrmFollowUpMapper followUpMapper, CrmCustomerMapper customerMapper,
                              TeamService teamService) {
        this.followUpMapper = followUpMapper;
        this.customerMapper = customerMapper;
        this.teamService = teamService;
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:customer:list")
    public R<Page<CrmFollowUp>> page(@RequestParam int bizType, @RequestParam Long bizId,
                                     @RequestParam(defaultValue = "1") long pageNo,
                                     @RequestParam(defaultValue = "10") long pageSize) {
        teamService.checkPermission(bizType, bizId, TeamServiceImpl.READ);
        return R.ok(followUpMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<CrmFollowUp>()
                        .eq(CrmFollowUp::getBizType, bizType)
                        .eq(CrmFollowUp::getBizId, bizId)
                        .orderByDesc(CrmFollowUp::getId)));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:customer:list")
    public R<Long> save(@RequestBody CrmFollowUp followUp) {
        teamService.checkPermission(followUp.getBizType(), followUp.getBizId(), TeamServiceImpl.WRITE);
        if (followUp.getId() == null) {
            followUpMapper.insert(followUp);
        } else {
            followUpMapper.updateById(followUp);
        }
        // 客户跟进联动：刷新最后联系时间与下次联系时间
        if (followUp.getBizType() == 2) {
            customerMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getId, followUp.getBizId())
                    .set(CrmCustomer::getLastContactTime, java.time.LocalDateTime.now())
                    .set(followUp.getNextTime() != null, CrmCustomer::getNextContactTime, followUp.getNextTime()));
        }
        return R.ok(followUp.getId());
    }
}
