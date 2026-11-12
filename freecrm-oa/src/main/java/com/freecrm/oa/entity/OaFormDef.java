/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.oa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.freecrm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通用审批表单定义 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_form_def")
public class OaFormDef extends BaseEntity {
    private String name;
    /** 字段定义 JSON：[{key,label,type(text/textarea/number/date/select),required,options[]}] */
    private String fields;
    private String icon;
    private Integer status;
}
