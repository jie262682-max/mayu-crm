/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.crm.entity.CrmCustomer;

public interface CustomerService {

    Long create(CrmCustomer customer);

    void update(CrmCustomer customer);

    void delete(Long id);

    CrmCustomer get(Long id);

    Page<CrmCustomer> page(long pageNo, long pageSize, String keyword, Boolean pool);

    /** 转移负责人 */
    void transfer(Long id, Long newOwnerUserId);

    /** 放入公海（负责人清空，团队解散） */
    void putPool(Long id);

    /** 从公海领取 */
    void receive(Long id);

    /** 按名称/手机号查重（排除自身，限 10 条） */
    java.util.List<CrmCustomer> checkDuplicate(String name, String mobile, Long excludeId);

    /** 被合并客户业务数据迁入目标客户后删除：联系人/跟进/商机/合同 */
    java.util.Map<String, Object> merge(Long mainId, Long dupId);
}
