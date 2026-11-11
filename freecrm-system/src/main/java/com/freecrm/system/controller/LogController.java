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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.system.entity.SysLoginLog;
import com.freecrm.system.entity.SysOperLog;
import com.freecrm.system.mapper.SysLoginLogMapper;
import com.freecrm.system.mapper.SysOperLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志 / 登录日志查询 */
@RestController
@RequestMapping("/api/log")
public class LogController {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    public LogController(SysOperLogMapper operLogMapper, SysLoginLogMapper loginLogMapper) {
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
    }

    @GetMapping("/oper/page")
    @SaCheckPermission("sys:user:list")
    public R<Page<SysOperLog>> operPage(@RequestParam(defaultValue = "1") long pageNo,
                                        @RequestParam(defaultValue = "15") long pageSize,
                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(SysOperLog::getModule, keyword)
                    .or().like(SysOperLog::getAction, keyword)
                    .or().like(SysOperLog::getUsername, keyword));
        }
        return R.ok(operLogMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(SysOperLog::getId)));
    }

    @GetMapping("/login/page")
    @SaCheckPermission("sys:user:list")
    public R<Page<SysLoginLog>> loginPage(@RequestParam(defaultValue = "1") long pageNo,
                                          @RequestParam(defaultValue = "15") long pageSize) {
        return R.ok(loginLogMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<SysLoginLog>().orderByDesc(SysLoginLog::getId)));
    }
}
