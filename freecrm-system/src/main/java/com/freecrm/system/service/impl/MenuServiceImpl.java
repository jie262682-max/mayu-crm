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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.exception.BizException;
import com.freecrm.system.entity.SysMenu;
import com.freecrm.system.mapper.SysMenuMapper;
import com.freecrm.system.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuHolder roleMenuHolder;

    public MenuServiceImpl(SysMenuMapper menuMapper, SysRoleMenuHolder roleMenuHolder) {
        this.menuMapper = menuMapper;
        this.roleMenuHolder = roleMenuHolder;
    }

    @Override
    public List<SysMenu> getMenuTree() {
        return buildTree(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)));
    }

    @Override
    public List<SysMenu> getUserMenuTree(Long userId) {
        List<Long> menuIds = roleMenuHolder.getMenuIdsByUser(userId);
        if (menuIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds).stream()
                // 前端导航只渲染 目录/菜单，按钮权限不进树
                .filter(m -> m.getType() == null || m.getType() <= 2)
                .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .collect(Collectors.toList());
        return buildTree(menus);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        menus.forEach(m -> m.setChildren(byParent.getOrDefault(m.getId(), new java.util.ArrayList<>())));
        return byParent.getOrDefault(0L, new java.util.ArrayList<>());
    }

    @Override
    public void save(SysMenu menu) {
        if (menu.getId() == null) {
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
    }

    @Override
    public void delete(Long id) {
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("存在子菜单，先删子级");
        }
        menuMapper.deleteById(id);
    }
}
