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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.crm.entity.CrmContact;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.mapper.CrmContactMapper;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.system.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 联系人生日关怀：每日 08:20 提醒客户负责人（当天/明天生日；sentRecently 1 天去重防重发）
 */
@Component
public class BirthdayRemindJob {

    private static final Logger log = LoggerFactory.getLogger(BirthdayRemindJob.class);
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");

    private final CrmContactMapper contactMapper;
    private final CrmCustomerMapper customerMapper;
    private final NoticeService noticeService;

    public BirthdayRemindJob(CrmContactMapper contactMapper, CrmCustomerMapper customerMapper,
                            NoticeService noticeService) {
        this.contactMapper = contactMapper;
        this.customerMapper = customerMapper;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 20 8 * * ?")
    public void run() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        List<CrmContact> cs = contactMapper.selectList(new LambdaQueryWrapper<CrmContact>()
                .isNotNull(CrmContact::getBirthday));
        int sent = 0;
        for (CrmContact c : cs) {
            if (c.getCustomerId() == null || c.getBirthday() == null) {
                continue;
            }
            String when = md(c.getBirthday(), today, tomorrow);
            if (when == null) {
                continue;
            }
            CrmCustomer cust = customerMapper.selectById(c.getCustomerId());
            if (cust == null || cust.getOwnerUserId() == null) {
                continue; // 公海客户无人负责
            }
            String title = "客户生日关怀：" + when;
            if (noticeService.sentRecently(cust.getOwnerUserId(), title, null, c.getId(), 1)) {
                continue;
            }
            noticeService.send(cust.getOwnerUserId(), title,
                    "客户「" + cust.getName() + "」的联系人 " + c.getName()
                            + " 生日是 " + when + "（" + c.getBirthday().format(MD) + "），别忘了送上祝福。",
                    2, null, c.getId());
            sent++;
        }
        if (sent > 0) {
            log.info("生日关怀提醒已发送 {} 条", sent);
        }
    }

    /** 今天/明天生日返回「今天/明天」，否则 null */
    private String md(LocalDate birthday, LocalDate today, LocalDate tomorrow) {
        int bm = birthday.getMonthValue();
        int bd = birthday.getDayOfMonth();
        if (bm == today.getMonthValue() && bd == today.getDayOfMonth()) {
            return "今天";
        }
        if (bm == tomorrow.getMonthValue() && bd == tomorrow.getDayOfMonth()) {
            return "明天";
        }
        return null;
    }
}
