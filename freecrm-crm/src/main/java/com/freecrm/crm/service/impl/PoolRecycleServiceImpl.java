/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.service.PoolRecycleService;
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

@Service
public class PoolRecycleServiceImpl implements PoolRecycleService {

    private static final Logger log = LoggerFactory.getLogger(PoolRecycleServiceImpl.class);

    /** 回收执行互斥：定时 Job 与手动按钮并发触发时后来者直接跳过，防止双发通知 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final CrmCustomerMapper customerMapper;
    private final TeamService teamService;
    private final ConfigService configService;
    private final NoticeService noticeService;

    public PoolRecycleServiceImpl(CrmCustomerMapper customerMapper, TeamService teamService,
                                  ConfigService configService, NoticeService noticeService) {
        this.customerMapper = customerMapper;
        this.teamService = teamService;
        this.configService = configService;
        this.noticeService = noticeService;
    }

    @Override
    public List<Map<String, Object>> preview() {
        int recycleDays = configService.getInt("pool.recycle.days", 30);
        int expireDays = configService.getInt("pool.expire.days", 90);
        int protectDays = configService.getInt("pool.protect.days", 7);

        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (CrmCustomer c : candidates(protectDays)) {
            LocalDateTime last = c.getLastContactTime() != null ? c.getLastContactTime() : c.getCreateTime();
            long noContact = ChronoUnit.DAYS.between(last, LocalDateTime.now());
            long age = ChronoUnit.DAYS.between(c.getCreateTime(), LocalDateTime.now());
            if (noContact >= recycleDays) {
                addPreview(byId, c, "掉保回收", noContact, recycleDays);
            }
            if (age >= expireDays) {
                addPreview(byId, c, "超期回收", age, expireDays);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private void addPreview(Map<String, Map<String, Object>> byId, CrmCustomer c,
                            String reason, long days, int threshold) {
        Map<String, Object> p = byId.get(String.valueOf(c.getId()));
        if (p == null) {
            p = new LinkedHashMap<>();
            p.put("customerId", c.getId());
            p.put("customerName", c.getName());
            p.put("ownerUserId", c.getOwnerUserId());
            p.put("createTime", c.getCreateTime() == null ? null : c.getCreateTime().toLocalDate().toString());
            p.put("lastContactTime", c.getLastContactTime() == null ? null : c.getLastContactTime().toLocalDate().toString());
            p.put("reasons", new ArrayList<String>());
            byId.put(String.valueOf(c.getId()), p);
        }
        ((List<String>) p.get("reasons")).add(reason + "（" + days + " 天 ≥ " + threshold + " 天）");
    }

    @Override
    public Map<String, Integer> recycleNow() {
        return recycleNow(false);
    }

    /** manual=true 为管理页手动触发（仅影响日志文案） */
    public Map<String, Integer> recycleNow(boolean manual) {
        Map<String, Integer> skipped = new HashMap<>();
        skipped.put("noContact", 0);
        skipped.put("expire", 0);
        skipped.put("skipped", 1);
        if (!enabled()) {
            return skipped;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("公海回收已在执行中，本次触发跳过");
            return skipped;
        }
        try {
            int recycleDays = configService.getInt("pool.recycle.days", 30);
            int expireDays = configService.getInt("pool.expire.days", 90);
            int protectDays = configService.getInt("pool.protect.days", 7);

            List<CrmCustomer> candidates = candidates(protectDays); // 只查一次，两规则共用
            int n1 = doRecycle(candidates.stream()
                    .filter(c -> daysSince(c.getLastContactTime() != null ? c.getLastContactTime() : c.getCreateTime()) >= recycleDays)
                    .collect(java.util.stream.Collectors.toList()),
                    "掉保回收（超过 " + recycleDays + " 天未跟进）");
            int n2 = doRecycle(candidates.stream()
                    .filter(c -> daysSince(c.getCreateTime()) >= expireDays)
                    .collect(java.util.stream.Collectors.toList()),
                    "超期回收（创建超过 " + expireDays + " 天未成交）");

            Map<String, Integer> r = new HashMap<>();
            r.put("noContact", n1);
            r.put("expire", n2);
            log.info("公海回收完成：掉保 {} 个，超期 {} 个（{}）", n1, n2, manual ? "手动触发" : "定时任务");
            return r;
        } finally {
            running.set(false);
        }
    }

    public boolean enabled() {
        return "1".equals(configService.get("pool.recycle.enabled", "1"));
    }

    /** 可回收范围：有负责人 + 未成交 + 未锁定 + 领取/转移超保护期 */
    private List<CrmCustomer> candidates(int protectDays) {
        LocalDateTime protectDeadline = LocalDateTime.now().minusDays(protectDays);
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                .isNotNull(CrmCustomer::getOwnerUserId)
                .eq(CrmCustomer::getDealStatus, 0)
                .eq(CrmCustomer::getLockStatus, 0)
                .and(w -> w.isNull(CrmCustomer::getOwnerTime)
                        .or(v -> v.le(CrmCustomer::getOwnerTime, protectDeadline))));
    }

    private long daysSince(LocalDateTime t) {
        return t == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(t, LocalDateTime.now());
    }

    private int doRecycle(List<CrmCustomer> list, String reason) {
        int count = 0;
        for (CrmCustomer c : list) {
            try {
                Long owner = c.getOwnerUserId();
                customerMapper.update(null, new LambdaUpdateWrapper<CrmCustomer>()
                        .eq(CrmCustomer::getId, c.getId())
                        .set(CrmCustomer::getOwnerUserId, null)
                        .set(CrmCustomer::getOwnerTime, null));
                teamService.deleteByBiz(LeadServiceImpl.BIZ_CUSTOMER, c.getId());
                if (owner != null) {
                    noticeService.send(owner, "客户已回收至公海",
                            "客户「" + c.getName() + "」已被系统回收：" + reason + "。可在公海重新领取。",
                            3, null, c.getId());
                }
                count++;
            } catch (Exception e) {
                log.warn("回收客户失败 id={} : {}", c.getId(), e.getMessage());
            }
        }
        return count;
    }
}
