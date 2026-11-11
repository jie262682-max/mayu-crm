/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.freecrm.system.service.AuthService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源：登录用户的权限/角色集合
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final AuthService authService;

    public StpInterfaceImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        List<String> perms = authService.getPermissions(userId);
        return perms == null ? Collections.emptyList() : perms;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        List<String> roles = authService.getRoleCodes(userId);
        return roles == null ? Collections.emptyList() : roles;
    }
}
