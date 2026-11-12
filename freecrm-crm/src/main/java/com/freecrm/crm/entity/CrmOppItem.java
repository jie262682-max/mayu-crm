/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.freecrm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 商机产品明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opp_item")
public class CrmOppItem extends BaseEntity {
    private Long opportunityId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String remark;
}
