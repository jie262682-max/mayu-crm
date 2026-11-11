/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.service.impl;

import com.freecrm.system.mapper.SysRoleMenuMapper;
import com.freecrm.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户 -> 角色 -> 菜单 id 集合 桥接
 */
@Component
public class SysRoleMenuHolder {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysRoleMenuHolder(SysUserRoleMapper userRoleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public List<Long> getMenuIdsByUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return menuIds.stream().distinct().collect(java.util.stream.Collectors.toList());
    }
}
