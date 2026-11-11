/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.eam.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.eam.entity.EamAssetCategory;
import com.freecrm.eam.mapper.EamAssetCategoryMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 资产分类（树） */
@RestController
@RequestMapping("/api/eam/category")
public class CategoryController {

    private final EamAssetCategoryMapper categoryMapper;

    public CategoryController(EamAssetCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/tree")
    @SaCheckPermission("eam:asset:list")
    public R<List<Map<String, Object>>> tree() {
        List<EamAssetCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<EamAssetCategory>().orderByAsc(EamAssetCategory::getSort));
        return R.ok(build(all, 0L));
    }

    private List<Map<String, Object>> build(List<EamAssetCategory> all, Long parentId) {
        return all.stream().filter(c -> parentId.equals(c.getParentId()))
                .map(c -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("parentId", c.getParentId());
                    m.put("name", c.getName());
                    m.put("sort", c.getSort());
                    m.put("children", build(all, c.getId()));
                    return m;
                }).collect(Collectors.toList());
    }

    @PostMapping("/save")
    @SaCheckPermission("eam:asset:save")
    @OpLog(module = "资产分类", action = "保存分类")
    public R<Long> save(@RequestBody EamAssetCategory category) {
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return R.ok(category.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("eam:asset:save")
    @OpLog(module = "资产分类", action = "删除分类")
    public R<Void> delete(@RequestParam Long id) {
        Long children = categoryMapper.selectCount(new LambdaQueryWrapper<EamAssetCategory>()
                .eq(EamAssetCategory::getParentId, id));
        if (children != null && children > 0) {
            return R.fail(400, "请先删除子分类");
        }
        categoryMapper.deleteById(id);
        return R.ok();
    }
}
