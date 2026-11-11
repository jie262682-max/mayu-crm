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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.eam.entity.EamAsset;
import com.freecrm.eam.entity.EamAssetChange;
import com.freecrm.eam.service.AssetService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** EAM 资产台账 + 变更操作 */
@RestController
@RequestMapping("/api/eam/asset")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping("/page")
    @SaCheckPermission("eam:asset:list")
    public R<Page<EamAsset>> page(@RequestParam(defaultValue = "1") long pageNo,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) Long deptId,
                                  @RequestParam(required = false) Long keeperUserId) {
        return R.ok(assetService.page(pageNo, pageSize, keyword, categoryId, status, deptId, keeperUserId));
    }

    @GetMapping("/get")
    @SaCheckPermission("eam:asset:list")
    public R<EamAsset> get(@RequestParam Long id) {
        return R.ok(assetService.get(id));
    }

    @PostMapping("/save")
    @SaCheckPermission("eam:asset:save")
    @OpLog(module = "资产管理", action = "保存资产")
    public R<Long> save(@RequestBody EamAsset asset) {
        if (asset.getId() == null) {
            return R.ok(assetService.create(asset));
        }
        assetService.update(asset);
        return R.ok(asset.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("eam:asset:delete")
    @OpLog(module = "资产管理", action = "删除资产")
    public R<Void> delete(@RequestParam Long id) {
        assetService.delete(id);
        return R.ok();
    }

    // ---- 变更操作 ----

    @PutMapping("/receive")
    @SaCheckPermission("eam:asset:change")
    @OpLog(module = "资产管理", action = "资产领用")
    public R<Void> receive(@RequestParam Long id,
                           @RequestParam(required = false) Long toUserId,
                           @RequestParam(required = false) String reason) {
        long receiver = toUserId != null ? toUserId : cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        assetService.receive(id, receiver, reason);
        return R.ok();
    }

    @PutMapping("/give-back")
    @SaCheckPermission("eam:asset:change")
    @OpLog(module = "资产管理", action = "资产退回")
    public R<Void> giveBack(@RequestParam Long id, @RequestParam(required = false) String reason) {
        assetService.giveBack(id, reason);
        return R.ok();
    }

    @PutMapping("/lend")
    @SaCheckPermission("eam:asset:change")
    @OpLog(module = "资产管理", action = "资产借出")
    public R<Void> lend(@RequestParam Long id, @RequestParam Long toUserId,
                        @RequestParam(required = false) String reason) {
        assetService.lend(id, toUserId, reason);
        return R.ok();
    }

    @PutMapping("/repair")
    @SaCheckPermission("eam:asset:change")
    @OpLog(module = "资产管理", action = "资产报修")
    public R<Void> repair(@RequestParam Long id, @RequestParam(required = false) String reason) {
        assetService.repair(id, reason);
        return R.ok();
    }

    @PutMapping("/repair-done")
    @SaCheckPermission("eam:asset:change")
    @OpLog(module = "资产管理", action = "维修完成")
    public R<Void> repairDone(@RequestParam Long id, @RequestParam(required = false) String reason) {
        assetService.repairDone(id, reason);
        return R.ok();
    }

    /** 报废：有流程走审批（返回流程实例），无流程直接报废 */
    @PutMapping("/scrap")
    @SaCheckPermission("eam:asset:change")
    @OpLog(module = "资产管理", action = "资产报废")
    public R<Map<String, Object>> scrap(@RequestParam Long id, @RequestParam(required = false) String reason) {
        return R.ok(assetService.scrap(id, reason));
    }

    @GetMapping("/change/page")
    @SaCheckPermission("eam:asset:list")
    public R<Page<EamAssetChange>> changePage(@RequestParam(defaultValue = "1") long pageNo,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) Long assetId,
                                              @RequestParam(required = false) Integer type) {
        return R.ok(assetService.pageChanges(pageNo, pageSize, assetId, type));
    }
}
