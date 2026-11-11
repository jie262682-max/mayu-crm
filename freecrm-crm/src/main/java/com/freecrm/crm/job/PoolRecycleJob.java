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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import com.freecrm.system.service.ConfigService;
import com.freecrm.system.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公海自动回收：
 * 1. 掉保回收 —— 超 N 天未跟进（lastContactTime 为空则看创建时间）自动入池
 * 2. 超期回收 —— 创建超 N 天未成交自动入池
 * 已成交 / 已锁定客户不回收；每小时整点执行
 */
@Component
public class PoolRecycleJob {

    private static final Logger log = LoggerFactory.getLogger(PoolRecycleJob.class);

    private final CrmCustomerMapper customerMapper;
    private final TeamService teamService;
    private final ConfigService configService;
    private final NoticeService noticeService;

    public PoolRecycleJob(CrmCustomerMapper customerMapper, TeamService teamService,
                          ConfigService configService, NoticeService noticeService) {
        this.customerMapper = customerMapper;
        this.teamService = teamService;
        this.configService = configService;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void recycle() {
        int recycleDays = configService.getInt("pool.recycle.days", 30);
        int expireDays = configService.getInt("pool.expire.days", 90);

        int n1 = recycleByNoContact(recycleDays);
        int n2 = recycleByExpire(expireDays);
        if (n1 + n2 > 0) {
            log.info("公海回收完成：掉保 {} 个，超期 {} 个", n1, n2);
        }
    }

    /** 掉保：N 天未跟进入池 */
    private int recycleByNoContact(int days) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<CrmCustomer> list = customerMapper.selectList(baseCondition()
                .and(w -> w.lt(CrmCustomer::getLastContactTime, deadline)
                        .or(v -> v.isNull(CrmCustomer::getLastContactTime)
                                .lt(CrmCustomer::getCreateTime, deadline))));
        return doRecycle(list, "掉保回收（超过 " + days + " 天未跟进）");
    }

    /** 超期：创建 N 天未成交入池 */
    private int recycleByExpire(int days) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<CrmCustomer> list = customerMapper.selectList(baseCondition()
                .lt(CrmCustomer::getCreateTime, deadline));
        return doRecycle(list, "超期回收（创建超过 " + days + " 天未成交）");
    }

    /** 可回收范围：有负责人 + 未成交 + 未锁定 */
    private LambdaQueryWrapper<CrmCustomer> baseCondition() {
        return new LambdaQueryWrapper<CrmCustomer>()
                .isNotNull(CrmCustomer::getOwnerUserId)
                .eq(CrmCustomer::getDealStatus, 0)
                .eq(CrmCustomer::getLockStatus, 0);
    }

    private int doRecycle(List<CrmCustomer> list, String reason) {
        int count = 0;
        for (CrmCustomer c : list) {
            try {
                Long owner = c.getOwnerUserId();
                customerMapper.update(null, new LambdaUpdateWrapper<CrmCustomer>()
                        .eq(CrmCustomer::getId, c.getId())
                        .set(CrmCustomer::getOwnerUserId, null));
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
