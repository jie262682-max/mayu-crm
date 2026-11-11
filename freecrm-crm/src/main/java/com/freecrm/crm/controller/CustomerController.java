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
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:customer:list")
    public R<Page<CrmCustomer>> page(@RequestParam(defaultValue = "1") long pageNo,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Boolean pool) {
        return R.ok(customerService.page(pageNo, pageSize, keyword, pool));
    }

    @GetMapping("/get")
    @SaCheckPermission("crm:customer:list")
    public R<CrmCustomer> get(@RequestParam Long id) {
        return R.ok(customerService.get(id));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:customer:save")
    public R<Long> save(@RequestBody CrmCustomer customer) {
        if (customer.getId() == null) {
            return R.ok(customerService.create(customer));
        }
        customerService.update(customer);
        return R.ok(customer.getId());
    }

    @PutMapping("/transfer")
    @SaCheckPermission("crm:customer:save")
    public R<Void> transfer(@RequestParam Long id, @RequestParam Long newOwnerUserId) {
        customerService.transfer(id, newOwnerUserId);
        return R.ok();
    }

    @PutMapping("/put-pool")
    @SaCheckPermission("crm:customer:save")
    public R<Void> putPool(@RequestParam Long id) {
        customerService.putPool(id);
        return R.ok();
    }

    @PutMapping("/receive")
    @SaCheckPermission("crm:customer:receive")
    public R<Void> receive(@RequestParam Long id) {
        customerService.receive(id);
        return R.ok();
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:customer:delete")
    public R<Void> delete(@RequestParam Long id) {
        customerService.delete(id);
        return R.ok();
    }

    /** 查重：名称全等或手机号全等 */
    @GetMapping("/check-duplicate")
    @SaCheckPermission("crm:customer:list")
    public R<List<CrmCustomer>> checkDuplicate(@RequestParam(required = false) String name,
                                               @RequestParam(required = false) String mobile,
                                               @RequestParam(required = false) Long excludeId) {
        return R.ok(customerService.checkDuplicate(name, mobile, excludeId));
    }

    /** 合并：被合并客户（dupId）的联系人/跟进/商机/合同全部迁入 mainId 后删除 */
    @PostMapping("/merge")
    @SaCheckPermission("crm:customer:save")
    @com.freecrm.common.log.OpLog(module = "客户管理", action = "合并客户")
    public R<java.util.Map<String, Object>> merge(@RequestParam Long mainId, @RequestParam Long dupId) {
        return R.ok(customerService.merge(mainId, dupId));
    }
}
