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
import com.freecrm.system.service.SmsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 短信配置 / 测试发送 / 发送记录（管理后台配置阿里云） */
@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private final SmsService smsService;
    private final com.freecrm.system.service.ConfigService configService;

    public SmsController(SmsService smsService, com.freecrm.system.service.ConfigService configService) {
        this.smsService = smsService;
        this.configService = configService;
    }

    @GetMapping("/config")
    @SaCheckPermission("sys:user:list")
    public R<Map<String, String>> config() {
        return R.ok(smsService.configView());
    }

    /** 保存配置（secret 传 ****** 表示不变） */
    @PostMapping("/config")
    @SaCheckPermission("sys:user:save")
    @OpLog(module = "短信配置", action = "修改短信配置")
    public R<Void> save(@RequestBody Map<String, String> kv) {
        kv.forEach((k, v) -> {
            if ("sms.accessSecret".equals(k) && "******".equals(v)) {
                return;
            }
            configService.set(k, v);
        });
        smsService.resetClient();
        return R.ok();
    }

    /** 测试发送：templateParam 可传 {"title":"测试"} */
    @PostMapping("/test")
    @SaCheckPermission("sys:user:save")
    @OpLog(module = "短信配置", action = "测试发送")
    public R<String> test(@RequestParam String mobile,
                          @RequestParam(required = false) String paramJson) {
        Map<String, String> param = new java.util.HashMap<>();
        if (paramJson != null && !paramJson.isEmpty()) {
            for (String kv : paramJson.split(",")) {
                String[] p = kv.split("=", 2);
                if (p.length == 2) {
                    param.put(p[0].trim(), p[1].trim());
                }
            }
        }
        boolean ok = smsService.send(mobile, param);
        return ok ? R.ok("发送成功") : R.fail(500, "发送失败，请查看发送记录中的错误信息");
    }

    @GetMapping("/log/page")
    @SaCheckPermission("sys:user:list")
    public R<?> logs(@RequestParam(defaultValue = "1") long pageNo,
                     @RequestParam(defaultValue = "15") long pageSize,
                     @RequestParam(required = false) String mobile) {
        return R.ok(smsService.page(pageNo, pageSize, mobile));
    }
}
