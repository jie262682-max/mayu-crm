/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.eam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.freecrm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 资产台账：状态 0闲置 1在用 2维修 3报废 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eam_asset")
public class EamAsset extends BaseEntity {
    public static final int IDLE = 0;
    public static final int IN_USE = 1;
    public static final int REPAIR = 2;
    public static final int SCRAPPED = 3;

    private String assetNo;
    private String name;
    private Long categoryId;
    private String model;
    private String serialNo;
    private LocalDate purchaseDate;
    private BigDecimal purchaseAmount;
    private Integer useYears;
    private Long deptId;
    private Long keeperUserId;
    private String location;
    private Integer status;
    private String remark;
}