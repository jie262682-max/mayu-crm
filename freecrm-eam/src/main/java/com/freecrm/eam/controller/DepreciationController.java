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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.eam.entity.EamDepreciation;
import com.freecrm.eam.job.DepreciationJob;
import com.freecrm.eam.mapper.EamDepreciationMapper;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

/** 资产折旧：记录查询 + 手动按月生成 */
@RestController
@RequestMapping("/api/eam/depreciation")
public class DepreciationController {

    private final EamDepreciationMapper depMapper;
    private final DepreciationJob job;

    public DepreciationController(EamDepreciationMapper depMapper, DepreciationJob job) {
        this.depMapper = depMapper;
        this.job = job;
    }

    @GetMapping("/page")
    @SaCheckPermission("eam:asset:list")
    public R<Page<EamDepreciation>> page(@RequestParam(defaultValue = "1") long pageNo,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) Long assetId,
                                         @RequestParam(required = false) String yearMonth) {
        return R.ok(depMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<EamDepreciation>()
                .eq(assetId != null, EamDepreciation::getAssetId, assetId)
                .eq(yearMonth != null && !yearMonth.isEmpty(), EamDepreciation::getDepMonth, yearMonth)
                .orderByDesc(EamDepreciation::getId)));
    }

    /** 手动生成指定月份折旧（幂等）：body {"yearMonth":"2026-10"}，缺省上月 */
    @PostMapping("/generate")
    @SaCheckPermission("eam:asset:save")
    @OpLog(module = "资产折旧", action = "生成折旧")
    public R<Integer> generate(@RequestBody(required = false) java.util.Map<String, Object> body) {
        YearMonth ym;
        if (body != null && body.get("yearMonth") != null) {
            ym = YearMonth.parse(String.valueOf(body.get("yearMonth")));
        } else {
            ym = YearMonth.now().minusMonths(1);
        }
        return R.ok(job.run(ym));
    }
}
