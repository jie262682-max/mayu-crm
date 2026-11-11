/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.freecrm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单/权限点
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 父菜单 id，0 为根 */
    private Long parentId;

    /** 菜单名 */
    private String name;

    /** 类型：1 目录 2 菜单 3 按钮权限 */
    private Integer type;

    /** 前端路由 path */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识，如 crm:customer:create */
    private String permission;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 0 隐藏 1 显示 */
    private Integer visible;

    /** 供前端树渲染，非库字段 */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
