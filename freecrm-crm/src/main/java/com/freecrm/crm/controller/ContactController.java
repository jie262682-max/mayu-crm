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
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmContact;
import com.freecrm.crm.mapper.CrmContactMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/contact")
public class ContactController {

    private final CrmContactMapper contactMapper;
    private final TeamService teamService;

    public ContactController(CrmContactMapper contactMapper, TeamService teamService) {
        this.contactMapper = contactMapper;
        this.teamService = teamService;
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:contact:list")
    public R<Page<CrmContact>> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) Long customerId) {
        LambdaQueryWrapper<CrmContact> qw = new LambdaQueryWrapper<>();
        if (customerId != null) {
            qw.eq(CrmContact::getCustomerId, customerId);
        }
        return R.ok(contactMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(CrmContact::getId)));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:contact:save")
    public R<Long> save(@RequestBody CrmContact contact) {
        if (contact.getCustomerId() != null) {
            teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER,
                    contact.getCustomerId(), TeamServiceImpl.WRITE);
        }
        if (contact.getId() == null) {
            if (contact.getOwnerUserId() == null) {
                contact.setOwnerUserId(StpUtil.getLoginIdAsLong());
            }
            contactMapper.insert(contact);
        } else {
            contactMapper.updateById(contact);
        }
        return R.ok(contact.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:contact:delete")
    public R<Void> delete(@RequestParam Long id) {
        contactMapper.deleteById(id);
        return R.ok();
    }
}
