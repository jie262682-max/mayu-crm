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
import com.freecrm.crm.entity.CrmOpportunity;
import com.freecrm.crm.entity.CrmStage;
import com.freecrm.crm.entity.CrmStageGroup;
import com.freecrm.crm.service.impl.OpportunityServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crm/opportunity")
public class OpportunityController {

    private final OpportunityServiceImpl oppService;

    public OpportunityController(OpportunityServiceImpl oppService) {
        this.oppService = oppService;
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:opportunity:list")
    public R<Page<CrmOpportunity>> page(@RequestParam(defaultValue = "1") long pageNo,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long customerId,
                                        @RequestParam(required = false) Integer endStatus) {
        return R.ok(oppService.page(pageNo, pageSize, keyword, customerId, endStatus));
    }

    @GetMapping("/get")
    @SaCheckPermission("crm:opportunity:list")
    public R<CrmOpportunity> get(@RequestParam Long id) {
        return R.ok(oppService.get(id));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:opportunity:save")
    public R<Long> save(@RequestBody CrmOpportunity opp) {
        if (opp.getId() == null) {
            return R.ok(oppService.create(opp));
        }
        oppService.update(opp);
        return R.ok(opp.getId());
    }

    @PutMapping("/move-stage")
    @SaCheckPermission("crm:opportunity:save")
    public R<Void> moveStage(@RequestParam Long id, @RequestParam Long stageId) {
        oppService.moveStage(id, stageId);
        return R.ok();
    }

    @PutMapping("/end")
    @SaCheckPermission("crm:opportunity:save")
    public R<Void> end(@RequestParam Long id, @RequestParam int endStatus) {
        oppService.end(id, endStatus);
        return R.ok();
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:opportunity:delete")
    public R<Void> delete(@RequestParam Long id) {
        oppService.delete(id);
        return R.ok();
    }

    // ---- 状态组 ----

    @PostMapping("/stage-group/save")
    @SaCheckPermission("crm:opportunity:save")
    public R<Long> saveGroup(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("stages");
        List<CrmStage> stages = new java.util.ArrayList<>();
        for (Map<String, Object> r : raw) {
            CrmStage s = new CrmStage();
            s.setName((String) r.get("name"));
            s.setPercent(new java.math.BigDecimal(String.valueOf(r.get("percent"))));
            stages.add(s);
        }
        return R.ok(oppService.createGroup((String) body.get("name"), stages));
    }

    @GetMapping("/stage-group/list")
    public R<List<CrmStageGroup>> listGroups() {
        return R.ok(oppService.listGroups());
    }

    @GetMapping("/stage/list")
    public R<List<CrmStage>> listStages(@RequestParam Long groupId) {
        return R.ok(oppService.listStages(groupId));
    }

    @DeleteMapping("/stage-group/delete")
    @SaCheckPermission("crm:opportunity:save")
    public R<Void> deleteGroup(@RequestParam Long id) {
        oppService.deleteGroup(id);
        return R.ok();
    }
}
