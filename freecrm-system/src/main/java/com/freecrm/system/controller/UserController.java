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
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.result.R;
import com.freecrm.system.entity.SysUser;
import com.freecrm.system.entity.SysUserRole;
import com.freecrm.system.mapper.SysUserMapper;
import com.freecrm.system.mapper.SysUserRoleMapper;
import com.freecrm.system.vo.UserSaveVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    public UserController(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @GetMapping("/page")
    @SaCheckPermission("sys:user:list")
    public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNo,
                                 @RequestParam(defaultValue = "10") long pageSize,
                                 @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword);
        }
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNo, pageSize), qw);
        page.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(page);
    }

    /** 简单用户列表（下拉选人用） */
    @GetMapping("/simple-list")
    public R<List<SysUser>> simpleList() {
        StpUtil.checkLogin();
        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1));
        list.forEach(u -> u.setPassword(null));
        return R.ok(list);
    }

    @PostMapping("/save")
    @SaCheckPermission("sys:user:save")
    @Transactional(rollbackFor = Exception.class)
    public R<Long> save(@Valid @RequestBody UserSaveVO vo) {
        SysUser user = new SysUser();
        user.setId(vo.getId());
        user.setUsername(vo.getUsername());
        user.setNickname(vo.getNickname());
        user.setMobile(vo.getMobile());
        user.setEmail(vo.getEmail());
        user.setDeptId(vo.getDeptId());
        user.setStatus(vo.getStatus() == null ? 1 : vo.getStatus());
        if (vo.getPassword() != null && !vo.getPassword().isEmpty()) {
            user.setPassword(BCrypt.hashpw(vo.getPassword()));
        }
        if (user.getId() == null) {
            if (user.getPassword() == null) {
                throw new BizException("新建用户必须设置密码");
            }
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, user.getUsername()));
            if (dup != null && dup > 0) {
                throw new BizException("用户名已存在");
            }
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, user.getId()));
        }
        if (vo.getRoleIds() != null) {
            for (Long roleId : vo.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        return R.ok(user.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("sys:user:delete")
    public R<Void> delete(@RequestParam Long id) {
        if (id.equals(StpUtil.getLoginIdAsLong())) {
            throw new BizException("不能删除自己");
        }
        userMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/role-ids")
    public R<List<Long>> roleIds(@RequestParam Long userId) {
        StpUtil.checkLogin();
        return R.ok(userRoleMapper.selectRoleIdsByUserId(userId).stream()
                .collect(Collectors.toList()));
    }
}
