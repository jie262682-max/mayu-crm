/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.result.R;
import com.freecrm.system.entity.SysDept;
import com.freecrm.system.mapper.SysDeptMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/api/dept")
public class DeptController {

    private final SysDeptMapper deptMapper;

    public DeptController(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort));
        Map<Long, List<SysDept>> byParent = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        all.forEach(d -> d.setChildren(byParent.getOrDefault(d.getId(), new java.util.ArrayList<>())));
        return R.ok(byParent.getOrDefault(0L, new java.util.ArrayList<>()));
    }

    @PostMapping("/save")
    @SaCheckPermission("sys:dept:save")
    public R<Long> save(@RequestBody SysDept dept) {
        if (dept.getId() == null) {
            deptMapper.insert(dept);
        } else {
            deptMapper.updateById(dept);
        }
        return R.ok(dept.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("sys:dept:delete")
    public R<Void> delete(@RequestParam Long id) {
        deptMapper.deleteById(id);
        return R.ok();
    }
}
