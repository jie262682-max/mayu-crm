/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.job;

import com.freecrm.crm.service.impl.LeadPoolRecycleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 线索池自动回收定时触发：每小时第 10 分（与客户公海错开，规则/开关见系统设置）
 */
@Component
public class LeadPoolRecycleJob {

    private static final Logger log = LoggerFactory.getLogger(LeadPoolRecycleJob.class);

    private final LeadPoolRecycleServiceImpl leadPoolRecycleService;

    public LeadPoolRecycleJob(LeadPoolRecycleServiceImpl leadPoolRecycleService) {
        this.leadPoolRecycleService = leadPoolRecycleService;
    }

    @Scheduled(cron = "0 10 * * * ?")
    public void run() {
        try {
            leadPoolRecycleService.recycleNow(false);
        } catch (Exception e) {
            log.error("线索池回收任务异常", e);
        }
    }
}
