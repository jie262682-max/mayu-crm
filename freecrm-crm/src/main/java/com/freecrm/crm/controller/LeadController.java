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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmLead;
import com.freecrm.crm.service.LeadService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/lead")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:lead:list")
    public R<Page<CrmLead>> page(@RequestParam(defaultValue = "1") long pageNo,
                                 @RequestParam(defaultValue = "10") long pageSize,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Boolean pool) {
        return R.ok(leadService.page(pageNo, pageSize, keyword, pool));
    }

    @GetMapping("/get")
    @SaCheckPermission("crm:lead:list")
    public R<CrmLead> get(@RequestParam Long id) {
        return R.ok(leadService.get(id));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:lead:save")
    public R<Long> save(@RequestBody CrmLead lead) {
        if (lead.getId() == null) {
            return R.ok(leadService.create(lead));
        }
        leadService.update(lead);
        return R.ok(lead.getId());
    }

    @PutMapping("/transform")
    @SaCheckPermission("crm:lead:transform")
    public R<Long> transform(@RequestParam Long id) {
        return R.ok(leadService.transform(id));
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:lead:delete")
    public R<Void> delete(@RequestParam Long id) {
        leadService.delete(id);
        return R.ok();
    }
}
