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

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.mapper.CrmContactMapper;
import com.freecrm.crm.mapper.CrmContractMapper;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.mapper.CrmFollowUpMapper;
import com.freecrm.crm.mapper.CrmOpportunityMapper;
import com.freecrm.crm.service.CustomerService;
import com.freecrm.crm.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CrmCustomerMapper customerMapper;
    private final TeamService teamService;
    private final CrmContactMapper contactMapper;
    private final CrmFollowUpMapper followUpMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmContractMapper contractMapper;

    public CustomerServiceImpl(CrmCustomerMapper customerMapper, TeamService teamService,
                               CrmContactMapper contactMapper, CrmFollowUpMapper followUpMapper,
                               CrmOpportunityMapper opportunityMapper, CrmContractMapper contractMapper) {
        this.customerMapper = customerMapper;
        this.teamService = teamService;
        this.contactMapper = contactMapper;
        this.followUpMapper = followUpMapper;
        this.opportunityMapper = opportunityMapper;
        this.contractMapper = contractMapper;
    }

    @Override
    public Long create(CrmCustomer customer) {
        long userId = StpUtil.getLoginIdAsLong();
        if (customer.getOwnerUserId() == null) {
            customer.setOwnerUserId(userId);
        }
        customer.setLockStatus(0);
        customer.setDealStatus(0);
        customerMapper.insert(customer);
        teamService.addOwner(LeadServiceImpl.BIZ_CUSTOMER, customer.getId(), customer.getOwnerUserId());
        return customer.getId();
    }

    @Override
    public void update(CrmCustomer customer) {
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, customer.getId(), TeamServiceImpl.WRITE);
        customer.setOwnerUserId(null);
        customerMapper.updateById(customer);
    }

    @Override
    public void delete(Long id) {
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, id, TeamServiceImpl.OWNER);
        customerMapper.deleteById(id);
        teamService.deleteByBiz(LeadServiceImpl.BIZ_CUSTOMER, id);
    }

    @Override
    public CrmCustomer get(Long id) {
        CrmCustomer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException("客户不存在");
        }
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, id, TeamServiceImpl.READ);
        return c;
    }

    @Override
    public Page<CrmCustomer> page(long pageNo, long pageSize, String keyword, Boolean pool) {
        LambdaQueryWrapper<CrmCustomer> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(CrmCustomer::getName, keyword);
        }
        if (Boolean.TRUE.equals(pool)) {
            qw.isNull(CrmCustomer::getOwnerUserId);
        } else {
            List<Long> bizIds = teamService.listMemberBizIds(LeadServiceImpl.BIZ_CUSTOMER,
                    StpUtil.getLoginIdAsLong());
            if (bizIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in(CrmCustomer::getId, bizIds);
        }
        return customerMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(CrmCustomer::getId));
    }

    @Override
    public void transfer(Long id, Long newOwnerUserId) {
        checkOwner(id);
        CrmCustomer upd = new CrmCustomer();
        upd.setId(id);
        upd.setOwnerUserId(newOwnerUserId);
        customerMapper.updateById(upd);
        teamService.addOwner(LeadServiceImpl.BIZ_CUSTOMER, id, newOwnerUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void putPool(Long id) {
        checkOwner(id);
        CrmCustomer c = customerMapper.selectById(id);
        if (c.getDealStatus() != null && c.getDealStatus() == 1) {
            throw new BizException("已成交客户不能放入公海");
        }
        if (c.getLockStatus() != null && c.getLockStatus() == 1) {
            throw new BizException("客户已锁定，先解锁");
        }
        // 显式置空负责人（updateById 会忽略 null 字段）
        customerMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmCustomer>()
                .eq(CrmCustomer::getId, id)
                .set(CrmCustomer::getOwnerUserId, null));
        // 负责人清空，团队解散
        teamService.deleteByBiz(LeadServiceImpl.BIZ_CUSTOMER, id);
    }

    @Override
    public void receive(Long id) {
        CrmCustomer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException("客户不存在");
        }
        if (c.getOwnerUserId() != null) {
            throw new BizException("客户已有负责人：" + c.getName());
        }
        long userId = StpUtil.getLoginIdAsLong();
        CrmCustomer upd = new CrmCustomer();
        upd.setId(id);
        upd.setOwnerUserId(userId);
        customerMapper.updateById(upd);
        teamService.addOwner(LeadServiceImpl.BIZ_CUSTOMER, id, userId);
    }

    private void checkOwner(Long id) {
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, id, TeamServiceImpl.OWNER);
    }

    // ---------- M11: 查重 / 合并 ----------

    @Override
    public List<CrmCustomer> checkDuplicate(String name, String mobile, Long excludeId) {
        LambdaQueryWrapper<CrmCustomer> qw = new LambdaQueryWrapper<>();
        qw.and(w -> {
            if (name != null && !name.isEmpty()) {
                w.eq(CrmCustomer::getName, name);
            }
            if (mobile != null && !mobile.isEmpty()) {
                w.or().eq(CrmCustomer::getMobile, mobile);
            }
        });
        if (excludeId != null) {
            qw.ne(CrmCustomer::getId, excludeId);
        }
        qw.last("LIMIT 10");
        return customerMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, Object> merge(Long mainId, Long dupId) {
        if (mainId.equals(dupId)) {
            throw new BizException("不能与自身合并");
        }
        // 对合并目标需可写
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, mainId, TeamServiceImpl.WRITE);
        CrmCustomer dup = customerMapper.selectById(dupId);
        if (dup == null) {
            throw new BizException("被合并客户不存在");
        }
        java.util.Map<String, Object> stat = new java.util.HashMap<>();
        stat.put("contacts", contactMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.freecrm.crm.entity.CrmContact>()
                .eq(com.freecrm.crm.entity.CrmContact::getCustomerId, dupId)
                .set(com.freecrm.crm.entity.CrmContact::getCustomerId, mainId)));
        stat.put("followUps", followUpMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.freecrm.crm.entity.CrmFollowUp>()
                .eq(com.freecrm.crm.entity.CrmFollowUp::getBizType, LeadServiceImpl.BIZ_CUSTOMER)
                .eq(com.freecrm.crm.entity.CrmFollowUp::getBizId, dupId)
                .set(com.freecrm.crm.entity.CrmFollowUp::getBizId, mainId)));
        stat.put("opportunities", opportunityMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.freecrm.crm.entity.CrmOpportunity>()
                .eq(com.freecrm.crm.entity.CrmOpportunity::getCustomerId, dupId)
                .set(com.freecrm.crm.entity.CrmOpportunity::getCustomerId, mainId)));
        stat.put("contracts", contractMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.freecrm.crm.entity.CrmContract>()
                .eq(com.freecrm.crm.entity.CrmContract::getCustomerId, dupId)
                .set(com.freecrm.crm.entity.CrmContract::getCustomerId, mainId)));
        // 团队成员解散（目标客户团队保持不变），被合并客户逻辑删除
        teamService.deleteByBiz(LeadServiceImpl.BIZ_CUSTOMER, dupId);
        customerMapper.deleteById(dupId);
        stat.put("dupId", dupId);
        stat.put("mainId", mainId);
        return stat;
    }
}
