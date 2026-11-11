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
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.system.service.ConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 系统配置（公海回收规则等），仅系统管理员可改 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/list")
    @SaCheckPermission("sys:user:list")
    public R<List<com.freecrm.system.entity.SysConfig>> list() {
        return R.ok(configService.listAll());
    }

    @PostMapping("/save")
    @SaCheckPermission("sys:user:save")
    @OpLog(module = "系统设置", action = "修改系统配置")
    public R<Void> save(@RequestBody Map<String, String> kv) {
        kv.forEach(configService::set);
        return R.ok();
    }
}
