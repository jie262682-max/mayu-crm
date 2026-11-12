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

/** 通用审批申请单（bizType=100） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_form_apply")
public class OaFormApply extends BaseEntity {
    public static final int AUDITING = 0;
    public static final int PASSED = 1;
    public static final int REJECTED = 2;

    private Long formId;
    private String title;
    /** 填写值 JSON {key:value} */
    private String formData;
    private Long userId;
    private Integer auditStatus;
}
