/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.freecrm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** crm_opportunity */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class CrmOpportunity extends BaseEntity {

    /** 商机名 */
    private String name;
    private Long customerId;
    private Long stageGroupId;
    /** 当前阶段 */
    private Long stageId;
    private BigDecimal amount;
    /** 预计成交日 */
    private LocalDate expectedDealDate;
    /** NULL进行中 1赢单 2输单 */
    private Integer endStatus;
    private Long ownerUserId;
    private String remark;
}
