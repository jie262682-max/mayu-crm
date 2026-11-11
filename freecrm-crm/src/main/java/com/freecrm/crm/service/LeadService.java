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
import com.freecrm.crm.entity.CrmLead;

public interface LeadService {

    Long create(CrmLead lead);

    void update(CrmLead lead);

    void delete(Long id);

    CrmLead get(Long id);

    /** 分页：只看自己团队参与的；pool=true 只看公海 */
    Page<CrmLead> page(long pageNo, long pageSize, String keyword, Boolean pool);

    /** 线索转客户：建客户+联系人+迁移跟进记录+回写转化状态 */
    Long transform(Long id);
}
