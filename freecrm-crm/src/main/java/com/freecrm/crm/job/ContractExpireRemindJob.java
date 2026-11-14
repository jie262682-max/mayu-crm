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
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.mapper.CrmContractMapper;
import com.freecrm.system.service.ConfigService;
import com.freecrm.system.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 合同到期提醒：每天 08:15 扫描已通过审核的合同
 * 1. 临期 —— 到期日距今 ≤ contract.remind.days 天（默认 30），提醒负责人安排续约/收尾款
 * 2. 逾期 —— 已过到期日且回款未收完，持续提醒（收完即停）
 */
@Component
public class ContractExpireRemindJob {

    private static final Logger log = LoggerFactory.getLogger(ContractExpireRemindJob.class);

    private final CrmContractMapper contractMapper;
    private final ConfigService configService;
    private final NoticeService noticeService;

    public ContractExpireRemindJob(CrmContractMapper contractMapper, ConfigService configService,
                                   NoticeService noticeService) {
        this.contractMapper = contractMapper;
        this.configService = configService;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 15 8 * * ?")
    public void remind() {
        int remindDays = configService.getInt("contract.remind.days", 30);
        LocalDate today = LocalDate.now();

        List<CrmContract> list = contractMapper.selectList(new LambdaQueryWrapper<CrmContract>()
                .eq(CrmContract::getAuditStatus, 2)
                .isNotNull(CrmContract::getEndDate)
                .le(CrmContract::getEndDate, today.plusDays(remindDays)));

        int expiring = 0;
        int overdue = 0;
        for (CrmContract c : list) {
            if (c.getOwnerUserId() == null) {
                continue; // 无负责人的合同无从提醒
            }
            long daysLeft = ChronoUnit.DAYS.between(today, c.getEndDate());
            boolean notFullyPaid = c.getReceivedAmount() == null
                    || c.getTotalAmount() == null
                    || c.getReceivedAmount().compareTo(c.getTotalAmount()) < 0;
            try {
                if (daysLeft < 0) {
                    if (notFullyPaid) {
                        if (noticeService.sentRecently(c.getOwnerUserId(), "合同已逾期", 1, c.getId(), 7)) {
                            continue; // 7 天内已提醒过，避免每天重复打扰
                        }
                        noticeService.send(c.getOwnerUserId(), "合同已逾期",
                                "合同「" + c.getName() + "」（" + c.getContractNo() + "）已逾期 "
                                        + Math.abs(daysLeft) + " 天且回款未收完（已收 "
                                        + c.getReceivedAmount() + "/" + c.getTotalAmount() + "），请跟进催收或续约。",
                                3, 1, c.getId());
                        overdue++;
                    }
                } else if (notFullyPaid || daysLeft <= 7) {
                    // 未收完款或最后一周，任何情形都值得提醒；收完款且还早则不打扰
                    if (noticeService.sentRecently(c.getOwnerUserId(), "合同即将到期", 1, c.getId(), 7)) {
                        continue; // 7 天内已提醒过
                    }
                    noticeService.send(c.getOwnerUserId(), "合同即将到期",
                            "合同「" + c.getName() + "」（" + c.getContractNo() + "）将于 "
                                    + c.getEndDate() + " 到期（剩 " + daysLeft + " 天），已收 "
                                    + c.getReceivedAmount() + "/" + c.getTotalAmount() + "，请安排续约或收尾款。",
                            3, 1, c.getId());
                    expiring++;
                }
            } catch (Exception ignore) { }
        }
        if (expiring + overdue > 0) {
            log.info("合同到期提醒已发送：临期 {} 条，逾期 {} 条", expiring, overdue);
        }
    }
}
