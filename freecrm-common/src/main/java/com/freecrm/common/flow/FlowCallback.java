/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.common.flow;

/**
 * 业务方接入审批流的回调（各业务模块实现，FlowEngine 自动收集）
 */
public interface FlowCallback {

    /** 支持的业务类型：1合同审批 2回款审批 3请假 */
    int bizType();

    /** 全部节点通过 */
    void onPassed(long bizId);

    /** 任一节点驳回 */
    void onRejected(long bizId);
}
