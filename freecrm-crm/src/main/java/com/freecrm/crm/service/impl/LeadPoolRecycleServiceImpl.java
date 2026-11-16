/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.freecrm.crm.entity.CrmLead;
import com.freecrm.crm.mapper.CrmLeadMapper;
import com.freecrm.crm.service.LeadPoolRecycleService;
import com.freecrm.crm.service.TeamService;
import com.freecrm.system.service.ConfigService;
import com.freecrm.system.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 线索池回收：有负责人 + 未转化 + 领取超保护期 + 超过 N 天未跟进 */
@Service
public class LeadPoolRecycleServiceImpl implements LeadPoolRecycleService {

    private static final Logger log = LoggerFactory.getLogger(LeadPoolRecycleServiceImpl.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final CrmLeadMapper leadMapper;
    private final TeamService teamService;
    private final ConfigService configService;
    private final NoticeService noticeService;

    public LeadPoolRecycleServiceImpl(CrmLeadMapper leadMapper, TeamService teamService,
                                      ConfigService configService, NoticeService noticeService) {
        this.leadMapper = leadMapper;
        this.teamService = teamService;
        this.configService = configService;
        this.noticeService = noticeService;
    }

    @Override
    public List<Map<String, Object>> preview() {
        int recycleDays = configService.getInt("pool.lead.recycle.days", 30);
        int protectDays = configService.getInt("pool.protect.days", 7);
        List<Map<String, Object>> r = new ArrayList<>();
        for (CrmLead l : candidates(protectDays)) {
            LocalDateTime last = l.getLastContactTime() != null ? l.getLastContactTime() : l.getOwnerTime();
            long noContact = daysSince(last);
            if (noContact >= recycleDays) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("leadId", l.getId());
                p.put("leadName", l.getName());
                p.put("ownerUserId", l.getOwnerUserId());
                p.put("createTime", l.getCreateTime() == null ? null : l.getCreateTime().toLocalDate().toString());
                p.put("lastContactTime", last == null ? null : last.toLocalDate().toString());
                p.put("reason", "超过 " + noContact + " 天未跟进（阈值 " + recycleDays + " 天）");
                r.add(p);
            }
        }
        return r;
    }

    @Override
    public Map<String, Integer> recycleNow() {
        return recycleNow(false);
    }

    public Map<String, Integer> recycleNow(boolean manual) {
        Map<String, Integer> r = new HashMap<>();
        r.put("recycled", 0);
        r.put("skipped", 1);
        if (!"1".equals(configService.get("pool.lead.enabled", "1"))) {
            return r;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("线索池回收已在执行中，本次触发跳过");
            return r;
        }
        try {
            int recycleDays = configService.getInt("pool.lead.recycle.days", 30);
            int protectDays = configService.getInt("pool.protect.days", 7);
            int count = 0;
            for (CrmLead l : candidates(protectDays)) {
                LocalDateTime last = l.getLastContactTime() != null ? l.getLastContactTime() : l.getOwnerTime();
                if (daysSince(last) < recycleDays) {
                    continue;
                }
                try {
                    Long owner = l.getOwnerUserId();
                    leadMapper.update(null, new LambdaUpdateWrapper<CrmLead>()
                            .eq(CrmLead::getId, l.getId())
                            .set(CrmLead::getOwnerUserId, null)
                            .set(CrmLead::getOwnerTime, null));
                    teamService.deleteByBiz(LeadServiceImpl.BIZ_LEAD, l.getId());
                    if (owner != null) {
                        noticeService.send(owner, "线索已回收至线索池",
                                "线索「" + l.getName() + "」超过 " + recycleDays + " 天未跟进，已被系统回收。",
                                3, null, l.getId());
                    }
                    count++;
                } catch (Exception e) {
                    log.warn("回收线索失败 id={} : {}", l.getId(), e.getMessage());
                }
            }
            r.put("recycled", count);
            r.put("skipped", 0);
            log.info("线索池回收完成：{} 个（{}）", count, manual ? "手动触发" : "定时任务");
            return r;
        } finally {
            running.set(false);
        }
    }

    private List<CrmLead> candidates(int protectDays) {
        LocalDateTime protectDeadline = LocalDateTime.now().minusDays(protectDays);
        return leadMapper.selectList(new LambdaQueryWrapper<CrmLead>()
                .isNotNull(CrmLead::getOwnerUserId)
                .and(w -> w.isNull(CrmLead::getTransformStatus).or().eq(CrmLead::getTransformStatus, 0))
                .and(w -> w.isNull(CrmLead::getOwnerTime)
                        .or(v -> v.le(CrmLead::getOwnerTime, protectDeadline))));
    }

    private long daysSince(LocalDateTime t) {
        return t == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(t, LocalDateTime.now());
    }
}
