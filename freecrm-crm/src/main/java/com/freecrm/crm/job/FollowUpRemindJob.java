/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.crm.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.system.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 跟进到期提醒：每天 08:05 扫描「下次联系日期 = 今天」且有负责人的客户，
 * 向负责人发站内信（提前于公海回收，给销售留处理窗口）
 */
@Component
public class FollowUpRemindJob {

    private static final Logger log = LoggerFactory.getLogger(FollowUpRemindJob.class);

    private final CrmCustomerMapper customerMapper;
    private final NoticeService noticeService;

    public FollowUpRemindJob(CrmCustomerMapper customerMapper, NoticeService noticeService) {
        this.customerMapper = customerMapper;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 5 8 * * ?")
    public void remind() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<CrmCustomer> list = customerMapper.selectList(
                new LambdaQueryWrapper<CrmCustomer>()
                        .isNotNull(CrmCustomer::getOwnerUserId)
                        .isNotNull(CrmCustomer::getNextContactTime)
                        .ge(CrmCustomer::getNextContactTime, start)
                        .lt(CrmCustomer::getNextContactTime, end));
        for (CrmCustomer c : list) {
            try {
                noticeService.send(c.getOwnerUserId(), "今日跟进提醒",
                        "客户「" + c.getName() + "」今天到了约定跟进时间，请及时联系。",
                        3, null, c.getId());
            } catch (Exception ignore) { }
        }
        if (!list.isEmpty()) {
            log.info("跟进到期提醒已发送 {} 条", list.size());
        }
    }
}
