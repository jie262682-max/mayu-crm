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
import com.freecrm.crm.entity.CrmContact;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.entity.CrmFollowUp;
import com.freecrm.crm.entity.CrmLead;
import com.freecrm.crm.mapper.CrmContactMapper;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.mapper.CrmFollowUpMapper;
import com.freecrm.crm.mapper.CrmLeadMapper;
import com.freecrm.crm.service.LeadService;
import com.freecrm.crm.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeadServiceImpl implements LeadService {

    /** biz_type：线索 */
    public static final int BIZ_LEAD = 1;
    public static final int BIZ_CUSTOMER = 2;

    private final CrmLeadMapper leadMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;
    private final CrmFollowUpMapper followUpMapper;
    private final TeamService teamService;

    public LeadServiceImpl(CrmLeadMapper leadMapper, CrmCustomerMapper customerMapper,
                           CrmContactMapper contactMapper, CrmFollowUpMapper followUpMapper,
                           TeamService teamService) {
        this.leadMapper = leadMapper;
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
        this.followUpMapper = followUpMapper;
        this.teamService = teamService;
    }

    @Override
    public Long create(CrmLead lead) {
        long userId = StpUtil.getLoginIdAsLong();
        if (lead.getOwnerUserId() == null) {
            lead.setOwnerUserId(userId);
        }
        lead.setTransformStatus(0);
        leadMapper.insert(lead);
        teamService.addOwner(BIZ_LEAD, lead.getId(), lead.getOwnerUserId());
        return lead.getId();
    }

    @Override
    public void update(CrmLead lead) {
        teamService.checkPermission(BIZ_LEAD, lead.getId(), TeamServiceImpl.WRITE);
        lead.setTransformStatus(null);
        lead.setCustomerId(null);
        leadMapper.updateById(lead);
    }

    @Override
    public void delete(Long id) {
        teamService.checkPermission(BIZ_LEAD, id, TeamServiceImpl.OWNER);
        leadMapper.deleteById(id);
        teamService.deleteByBiz(BIZ_LEAD, id);
    }

    @Override
    public CrmLead get(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BizException("线索不存在");
        }
        teamService.checkPermission(BIZ_LEAD, id, TeamServiceImpl.READ);
        return lead;
    }

    @Override
    public Page<CrmLead> page(long pageNo, long pageSize, String keyword, Boolean pool) {
        LambdaQueryWrapper<CrmLead> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(CrmLead::getName, keyword).or().like(CrmLead::getContactName, keyword);
        }
        if (Boolean.TRUE.equals(pool)) {
            qw.isNull(CrmLead::getOwnerUserId);
        } else {
            // 数据权限：只看自己团队参与的线索
            List<Long> bizIds = visibleBizIds();
            if (bizIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in(CrmLead::getId, bizIds);
        }
        return leadMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(CrmLead::getId));
    }

    private List<Long> visibleBizIds() {
        long userId = StpUtil.getLoginIdAsLong();
        return teamService.listMemberBizIds(BIZ_LEAD, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long transform(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BizException("线索不存在");
        }
        teamService.checkPermission(BIZ_LEAD, id, TeamServiceImpl.WRITE);
        if (lead.getTransformStatus() != null && lead.getTransformStatus() == 1) {
            throw new BizException("线索已转化，不能重复转化");
        }
        // 1. 建客户
        CrmCustomer customer = new CrmCustomer();
        customer.setName(lead.getName());
        customer.setMobile(lead.getMobile());
        customer.setEmail(lead.getEmail());
        customer.setSource(lead.getSource());
        customer.setIndustry(lead.getIndustry());
        customer.setRemark(lead.getRemark());
        customer.setOwnerUserId(lead.getOwnerUserId());
        customer.setLockStatus(0);
        customer.setDealStatus(0);
        customerMapper.insert(customer);
        teamService.addOwner(BIZ_CUSTOMER, customer.getId(), lead.getOwnerUserId());
        // 2. 有联系人则建联系人
        if (lead.getContactName() != null && !lead.getContactName().isEmpty()) {
            CrmContact contact = new CrmContact();
            contact.setName(lead.getContactName());
            contact.setCustomerId(customer.getId());
            contact.setMobile(lead.getMobile());
            contact.setEmail(lead.getEmail());
            contact.setOwnerUserId(lead.getOwnerUserId());
            contactMapper.insert(contact);
        }
        // 3. 迁移跟进记录
        List<CrmFollowUp> follows = followUpMapper.selectList(new LambdaQueryWrapper<CrmFollowUp>()
                .eq(CrmFollowUp::getBizType, BIZ_LEAD).eq(CrmFollowUp::getBizId, id));
        for (CrmFollowUp f : follows) {
            CrmFollowUp nf = new CrmFollowUp();
            nf.setBizType(BIZ_CUSTOMER);
            nf.setBizId(customer.getId());
            nf.setType(f.getType());
            nf.setContent(f.getContent());
            nf.setNextTime(f.getNextTime());
            followUpMapper.insert(nf);
        }
        // 4. 回写转化状态
        CrmLead upd = new CrmLead();
        upd.setId(id);
        upd.setTransformStatus(1);
        upd.setCustomerId(customer.getId());
        leadMapper.updateById(upd);
        return customer.getId();
    }
}
