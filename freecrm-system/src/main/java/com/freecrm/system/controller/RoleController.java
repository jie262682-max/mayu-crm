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
import com.freecrm.system.entity.SysRole;
import com.freecrm.system.entity.SysRoleMenu;
import com.freecrm.system.mapper.SysRoleMapper;
import com.freecrm.system.mapper.SysRoleMenuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public RoleController(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSort)));
    }

    @PostMapping("/save")
    @SaCheckPermission("sys:role:save")
    @Transactional(rollbackFor = Exception.class)
    public R<Long> save(@RequestBody SysRole role) {
        if (role.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        return R.ok(role.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("sys:role:delete")
    public R<Void> delete(@RequestParam Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        return R.ok();
    }

    /** 角色-菜单授权 */
    @PostMapping("/assign-menus")
    @SaCheckPermission("sys:role:save")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> assignMenus(@RequestParam Long roleId, @RequestBody List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        return R.ok();
    }

    @GetMapping("/menu-ids")
    public R<List<Long>> menuIds(@RequestParam Long roleId) {
        return R.ok(roleMenuMapper.selectMenuIdsByRoleIds(java.util.Collections.singletonList(roleId))
                .stream().distinct().collect(java.util.stream.Collectors.toList()));
    }
}
