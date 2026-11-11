/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.eam.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.eam.entity.EamCheck;
import com.freecrm.eam.entity.EamCheckItem;
import com.freecrm.eam.service.CheckService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 资产盘点 */
@RestController
@RequestMapping("/api/eam/check")
public class CheckController {

    private final CheckService checkService;

    public CheckController(CheckService checkService) {
        this.checkService = checkService;
    }

    @PostMapping("/create")
    @SaCheckPermission("eam:check:save")
    @OpLog(module = "资产盘点", action = "创建盘点单")
    public R<Long> create(@RequestBody Map<String, Object> body) {
        Integer scopeType = body.get("scopeType") == null ? 1 : Integer.parseInt(String.valueOf(body.get("scopeType")));
        Long deptId = body.get("deptId") == null ? null : Long.parseLong(String.valueOf(body.get("deptId")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(checkService.create(scopeType, deptId, remark));
    }

    @GetMapping("/page")
    @SaCheckPermission("eam:asset:list")
    public R<?> page(@RequestParam(defaultValue = "1") long pageNo,
                     @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(checkService.page(pageNo, pageSize));
    }

    @GetMapping("/items")
    @SaCheckPermission("eam:asset:list")
    public R<List<EamCheckItem>> items(@RequestParam Long checkId,
                                       @RequestParam(required = false) Boolean onlyDiff) {
        return R.ok(checkService.items(checkId, onlyDiff));
    }

    /** 实盘一项（移动端扫码盘点走同一接口） */
    @PutMapping("/item")
    @SaCheckPermission("eam:check:save")
    @OpLog(module = "资产盘点", action = "实盘")
    public R<Void> checkItem(@RequestParam Long itemId,
                             @RequestParam(required = false) Integer actualStatus,
                             @RequestParam(required = false) Long actualKeeperId,
                             @RequestParam(required = false, defaultValue = "false") boolean missing) {
        checkService.checkItem(itemId, actualStatus, actualKeeperId, missing);
        return R.ok();
    }

    @PutMapping("/finish")
    @SaCheckPermission("eam:check:save")
    @OpLog(module = "资产盘点", action = "完成盘点")
    public R<Void> finish(@RequestParam Long checkId) {
        checkService.finish(checkId);
        return R.ok();
    }
}
