/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.crm.job;

import com.freecrm.crm.service.PoolRecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 公海自动回收定时触发：每小时整点（规则/开关见 PoolRecycleServiceImpl，可在系统设置页调整）
 */
@Component
public class PoolRecycleJob {

    private static final Logger log = LoggerFactory.getLogger(PoolRecycleJob.class);

    private final PoolRecycleService poolRecycleService;

    public PoolRecycleJob(PoolRecycleService poolRecycleService) {
        this.poolRecycleService = poolRecycleService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void recycle() {
        // recycleNow 内部含 enabled 开关判断
        poolRecycleService.recycleNow();
        log.debug("公海回收定时任务已执行");
    }
}
