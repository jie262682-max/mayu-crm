/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.handler;

import cn.hutool.crypto.digest.BCrypt;
import com.freecrm.system.entity.*;
import com.freecrm.system.mapper.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 首次启动种子数据：根部门 + 超管角色 + admin 用户 + 菜单
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SeedRunner(SysDeptMapper deptMapper, SysRoleMapper roleMapper, SysUserMapper userMapper,
                      SysMenuMapper menuMapper, SysUserRoleMapper userRoleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        // 用户基础数据：只在空库时建
        SysRole adminRole;
        if (userMapper.selectCount(null) == 0) {
            SysDept root = new SysDept();
            root.setParentId(0L);
            root.setName("总部");
            root.setSort(0);
            root.setStatus(1);
            deptMapper.insert(root);

            adminRole = new SysRole();
            adminRole.setName("超级管理员");
            adminRole.setCode("super_admin");
            adminRole.setSort(0);
            adminRole.setStatus(1);
            adminRole.setRemark("全部权限");
            roleMapper.insert(adminRole);

            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(BCrypt.hashpw("admin123"));
            admin.setNickname("管理员");
            admin.setDeptId(root.getId());
            admin.setStatus(1);
            userMapper.insert(admin);

            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(adminRole.getId());
            userRoleMapper.insert(ur);
        } else {
            adminRole = roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getCode, "super_admin"));
        }
        // 菜单：缺失即补（升级/重置场景）
        if (adminRole == null || menuMapper.selectCount(null) > 0) {
            return;
        }

        // 菜单：目录先落库拿 id，再挂子菜单
        SysMenu sysDir = menu(0L, "系统管理", 1, "/system", "Layout", null, "Setting", 90);
        SysMenu crmDir = menu(0L, "CRM 客户管理", 1, "/crm", "Layout", null, "Bell", 10);
        SysMenu oaDir = menu(0L, "OA 协同办公", 1, "/oa", "Layout", null, "ChatDotRound", 20);
        SysMenu eamDir = menu(0L, "EAM 资产管理", 1, "/eam", "Layout", null, "Box", 15);
        menuMapper.insert(sysDir);
        menuMapper.insert(crmDir);
        menuMapper.insert(oaDir);
        menuMapper.insert(eamDir);

        List<SysMenu> all = new ArrayList<>();
        all.add(sysDir);
        all.add(crmDir);
        all.add(oaDir);
        all.add(eamDir);
        all.add(menu(sysDir.getId(), "用户管理", 2, "user", "system/user/index", "sys:user:list", "User", 1));
        all.add(menu(sysDir.getId(), "角色管理", 2, "role", "system/role/index", "sys:role:list", "Peoples", 2));
        all.add(menu(sysDir.getId(), "菜单管理", 2, "menu", "system/menu/index", "sys:menu:list", "Menu", 3));
        all.add(menu(sysDir.getId(), "部门管理", 2, "dept", "system/dept/index", "sys:dept:list", "OfficeBuilding", 4));
        all.add(menu(crmDir.getId(), "线索管理", 2, "lead", "crm/lead/index", "crm:lead:list", "Guide", 1));
        all.add(menu(crmDir.getId(), "客户管理", 2, "customer", "crm/customer/index", "crm:customer:list", "Suitcase", 2));
        all.add(menu(crmDir.getId(), "公海客户", 2, "pool", "crm/pool/index", "crm:customer:list", "Coin", 3));
        all.add(menu(crmDir.getId(), "联系人管理", 2, "contact", "crm/contact/index", "crm:contact:list", "Avatar", 4));
        all.add(menu(crmDir.getId(), "商机管理", 2, "opportunity", "crm/opportunity/index", "crm:opportunity:list", "Trophy", 5));
        all.add(menu(crmDir.getId(), "合同管理", 2, "contract", "crm/contract/index", "crm:contract:list", "Document", 6));
        all.add(menu(crmDir.getId(), "回款管理", 2, "payment", "crm/payment/index", "crm:contract:list", "Money", 7));
        all.add(menu(crmDir.getId(), "产品管理", 2, "product", "crm/product/index", "crm:product:list", "Goods", 8));
        all.add(menu(crmDir.getId(), "统计分析", 2, "stats", "crm/stats/index", null, "DataAnalysis", 9));
        all.add(menu(oaDir.getId(), "我的待办", 2, "todo", "oa/todo/index", null, "Bell", 1));
        all.add(menu(oaDir.getId(), "我的申请", 2, "mine", "oa/mine/index", null, "Tickets", 2));
        all.add(menu(oaDir.getId(), "流程定义", 2, "flow", "oa/flow/index", "oa:flow:save", "SetUp", 3));
        all.add(menu(oaDir.getId(), "请假申请", 2, "leave", "oa/leave/index", null, "Umbrella", 4));
        all.add(menu(eamDir.getId(), "资产台账", 2, "asset", "eam/asset/index", "eam:asset:list", "Monitor", 1));
        all.add(menu(eamDir.getId(), "资产分类", 2, "category", "eam/category/index", "eam:asset:list", "Grid", 2));
        all.add(menu(eamDir.getId(), "变更记录", 2, "change", "eam/change/index", "eam:asset:list", "Switch", 3));
        all.add(menu(eamDir.getId(), "资产盘点", 2, "check", "eam/check/index", "eam:asset:list", "Search", 4));
        all.add(menu(eamDir.getId(), "折旧记录", 2, "depreciation", "eam/depreciation/index", "eam:asset:list", "Timer", 5));
        all.add(menu(sysDir.getId(), "系统设置", 2, "settings", "system/settings/index", "sys:user:list", "Tools", 5));
        all.add(menu(sysDir.getId(), "操作日志", 2, "operlog", "system/operlog/index", "sys:user:list", "Notebook", 6));
        all.add(menu(sysDir.getId(), "登录日志", 2, "loginlog", "system/loginlog/index", "sys:user:list", "Key", 7));
        all.add(menu(sysDir.getId(), "我的消息", 2, "notice", "system/notice/index", null, "Bell", 8));

        for (SysMenu m : all) {
            if (m.getId() == null) {
                menuMapper.insert(m);
            }
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(adminRole.getId());
            rm.setMenuId(m.getId());
            roleMenuMapper.insert(rm);
        }
    }


    private SysMenu menu(Long parentId, String name, int type, String path, String component,
                         String permission, String icon, int sort) {
        SysMenu m = new SysMenu();
        m.setParentId(parentId);
        m.setName(name);
        m.setType(type);
        m.setPath(path);
        m.setComponent(component);
        m.setPermission(permission);
        m.setIcon(icon);
        m.setSort(sort);
        m.setVisible(1);
        return m;
    }
}
