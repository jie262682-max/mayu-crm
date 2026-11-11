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
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmTeamMember;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/list")
    public R<List<CrmTeamMember>> list(@RequestParam int bizType, @RequestParam Long bizId) {
        teamService.checkPermission(bizType, bizId, TeamServiceImpl.READ);
        return R.ok(teamService.listMembers(bizType, bizId));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:customer:save")
    public R<Long> save(@RequestBody CrmTeamMember member) {
        teamService.checkPermission(member.getBizType(), member.getBizId(), TeamServiceImpl.OWNER);
        teamService.saveMember(member);
        return R.ok(member.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:customer:save")
    public R<Void> delete(@RequestParam Long id) {
        teamService.removeMember(id);
        return R.ok();
    }
}
