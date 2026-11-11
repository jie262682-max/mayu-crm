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
import com.freecrm.common.result.R;
import com.freecrm.system.entity.SysMenu;
import com.freecrm.system.service.MenuService;
import org.springframework.web.bind.annotation.*;

/**
 * 菜单管理
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    public R<?> tree() {
        return R.ok(menuService.getMenuTree());
    }

    @PostMapping("/save")
    @SaCheckPermission("sys:menu:save")
    public R<Long> save(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return R.ok(menu.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("sys:menu:delete")
    public R<Void> delete(@RequestParam Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
