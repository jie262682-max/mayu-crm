/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.crm.service;

import java.util.List;
import java.util.Map;

/**
 * 公海自动回收（Job 与手动触发共用）：
 * 1. 掉保回收 —— 超 N 天未跟进（lastContactTime 为空则看创建时间）自动入池
 * 2. 超期回收 —— 创建超 N 天未成交自动入池
 * 已成交 / 已锁定客户不回收；领取/转移后 pool.protect.days 天内不回收
 */
public interface PoolRecycleService {

    /** 按当前规则预览将被回收的客户（不执行回收） */
    List<Map<String, Object>> preview();

    /** 立即执行一轮回收，返回 {noContact: n, expire: n} */
    Map<String, Integer> recycleNow();
}
