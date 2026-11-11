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

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.exception.BizException;
import com.freecrm.system.entity.SysMenu;
import com.freecrm.system.entity.SysRole;
import com.freecrm.system.entity.SysUser;
import com.freecrm.system.entity.SysUserRole;
import com.freecrm.system.mapper.SysMenuMapper;
import com.freecrm.system.mapper.SysRoleMapper;
import com.freecrm.system.mapper.SysUserMapper;
import com.freecrm.system.mapper.SysUserRoleMapper;
import com.freecrm.system.service.AuthService;
import com.freecrm.system.service.MenuService;
import com.freecrm.system.vo.LoginReqVO;
import com.freecrm.system.vo.LoginRespVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuHolder roleMenuHolder;
    private final MenuService menuService;
    private final com.freecrm.system.mapper.SysLoginLogMapper loginLogMapper;

    public AuthServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                           SysUserRoleMapper userRoleMapper, SysRoleMenuHolder roleMenuHolder, MenuService menuService,
                           com.freecrm.system.mapper.SysLoginLogMapper loginLogMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuHolder = roleMenuHolder;
        this.menuService = menuService;
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public LoginRespVO login(LoginReqVO reqVO) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, reqVO.getUsername()));
        if (user == null || !BCrypt.checkpw(reqVO.getPassword(), user.getPassword())) {
            loginLog(reqVO.getUsername(), null, false, "用户名或密码错误");
            throw new BizException(400, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            loginLog(reqVO.getUsername(), user.getId(), false, "账号已禁用");
            throw new BizException(403, "账号已禁用");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        loginLog(user.getUsername(), user.getId(), true, null);

        LoginRespVO resp = new LoginRespVO();
        resp.setToken(StpUtil.getTokenValue());
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setPermissions(getPermissions(user.getId()));
        resp.setMenus(menuService.getUserMenuTree(user.getId()));
        return resp;
    }

    private void loginLog(String username, Long userId, boolean success, String error) {
        try {
            com.freecrm.system.entity.SysLoginLog log = new com.freecrm.system.entity.SysLoginLog();
            log.setUsername(username);
            log.setUserId(userId);
            log.setSuccess(success ? 1 : 0);
            log.setErrorMsg(error);
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String ip = attrs.getRequest().getHeader("X-Real-IP");
                log.setIp(ip != null ? ip : attrs.getRequest().getRemoteAddr());
            }
            loginLogMapper.insert(log);
        } catch (Exception ignore) { /* 日志失败不阻断登录 */ }
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public List<String> getPermissions(Long userId) {
        // 超管通配全部权限
        if (getRoleCodes(userId).contains("super_admin")) {
            return Collections.singletonList("*");
        }
        List<Long> menuIds = roleMenuHolder.getMenuIdsByUser(userId);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());
    }
}
