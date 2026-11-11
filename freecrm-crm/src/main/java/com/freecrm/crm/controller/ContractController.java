/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.entity.CrmContractItem;
import com.freecrm.crm.entity.CrmPayment;
import com.freecrm.crm.entity.CrmPaymentPlan;
import com.freecrm.crm.service.impl.ContractServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/contract")
public class ContractController {

    private final ContractServiceImpl contractService;

    public ContractController(ContractServiceImpl contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:contract:list")
    public R<Page<CrmContract>> page(@RequestParam(defaultValue = "1") long pageNo,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long customerId,
                                     @RequestParam(required = false) Integer auditStatus) {
        return R.ok(contractService.page(pageNo, pageSize, keyword, customerId, auditStatus));
    }

    @GetMapping("/get")
    @SaCheckPermission("crm:contract:list")
    public R<CrmContract> get(@RequestParam Long id) {
        return R.ok(contractService.get(id));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:contract:save")
    public R<Long> save(@RequestBody CrmContract contract) {
        if (contract.getId() == null) {
            return R.ok(contractService.create(contract));
        }
        contractService.update(contract);
        return R.ok(contract.getId());
    }

    @PutMapping("/submit")
    @SaCheckPermission("crm:contract:save")
    public R<Void> submit(@RequestParam Long id) {
        contractService.submit(id);
        return R.ok();
    }

    @PutMapping("/audit")
    @SaCheckPermission("crm:contract:audit")
    public R<Void> audit(@RequestParam Long id, @RequestParam boolean pass) {
        contractService.audit(id, pass);
        return R.ok();
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:contract:delete")
    public R<Void> delete(@RequestParam Long id) {
        contractService.delete(id);
        return R.ok();
    }

    // ---- 回款计划 ----

    @PostMapping("/plan/save")
    @SaCheckPermission("crm:contract:save")
    public R<Long> savePlan(@RequestBody CrmPaymentPlan plan) {
        return R.ok(contractService.createPlan(plan));
    }

    @GetMapping("/plan/list")
    public R<List<CrmPaymentPlan>> listPlans(@RequestParam Long contractId) {
        return R.ok(contractService.listPlans(contractId));
    }

    @DeleteMapping("/plan/delete")
    @SaCheckPermission("crm:contract:save")
    public R<Void> deletePlan(@RequestParam Long id) {
        contractService.deletePlan(id);
        return R.ok();
    }

    // ---- 回款 ----

    @PostMapping("/payment/save")
    @SaCheckPermission("crm:contract:payment")
    public R<Long> savePayment(@RequestBody CrmPayment payment) {
        return R.ok(contractService.createPayment(payment));
    }

    @GetMapping("/payment/page")
    @SaCheckPermission("crm:contract:list")
    public R<Page<CrmPayment>> pagePayments(@RequestParam(defaultValue = "1") long pageNo,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Long contractId) {
        return R.ok(contractService.pagePayments(pageNo, pageSize, contractId));
    }

    @PostMapping("/item/save-batch")
    @SaCheckPermission("crm:contract:save")
    @com.freecrm.common.log.OpLog(module = "合同管理", action = "保存产品明细")
    public R<Void> saveItems(@RequestParam Long contractId,
                             @RequestBody java.util.List<CrmContractItem> items) {
        contractService.saveItems(contractId, items);
        return R.ok();
    }

    @GetMapping("/item/list")
    @SaCheckPermission("crm:contract:list")
    public R<java.util.List<CrmContractItem>> listItems(@RequestParam Long contractId) {
        return R.ok(contractService.listItems(contractId));
    }

    @GetMapping("/payment/get")
    @SaCheckPermission("crm:contract:list")
    public R<CrmPayment> getPayment(@RequestParam Long id) {
        return R.ok(contractService.getPayment(id));
    }
}
