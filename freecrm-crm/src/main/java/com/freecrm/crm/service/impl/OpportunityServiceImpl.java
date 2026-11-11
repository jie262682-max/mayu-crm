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
import com.freecrm.crm.entity.*;
import com.freecrm.crm.mapper.*;
import com.freecrm.crm.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商机：阶段推进 / 赢单输单。数据权限跟客户团队（bizType=2）走。
 */
@Service
public class OpportunityServiceImpl {

    private final CrmOpportunityMapper oppMapper;
    private final CrmStageGroupMapper groupMapper;
    private final CrmStageMapper stageMapper;
    private final CrmCustomerMapper customerMapper;
    private final TeamService teamService;

    public OpportunityServiceImpl(CrmOpportunityMapper oppMapper, CrmStageGroupMapper groupMapper,
                                  CrmStageMapper stageMapper, CrmCustomerMapper customerMapper,
                                  TeamService teamService) {
        this.oppMapper = oppMapper;
        this.groupMapper = groupMapper;
        this.stageMapper = stageMapper;
        this.customerMapper = customerMapper;
        this.teamService = teamService;
    }

    public Long create(CrmOpportunity opp) {
        if (opp.getOwnerUserId() == null) {
            opp.setOwnerUserId(StpUtil.getLoginIdAsLong());
        }
        // 默认取状态组第一阶段
        if (opp.getStageId() == null && opp.getStageGroupId() != null) {
            CrmStage first = stageMapper.selectList(new LambdaQueryWrapper<CrmStage>()
                            .eq(CrmStage::getGroupId, opp.getStageGroupId())
                            .orderByAsc(CrmStage::getSort).last("LIMIT 1"))
                    .stream().findFirst().orElse(null);
            if (first != null) {
                opp.setStageId(first.getId());
            }
        }
        oppMapper.insert(opp);
        return opp.getId();
    }

    public void update(CrmOpportunity opp) {
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, opp.getCustomerId(), TeamServiceImpl.WRITE);
        oppMapper.updateById(opp);
    }

    public void delete(Long id) {
        CrmOpportunity opp = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, opp.getCustomerId(), TeamServiceImpl.OWNER);
        oppMapper.deleteById(id);
    }

    public CrmOpportunity get(Long id) {
        CrmOpportunity opp = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, opp.getCustomerId(), TeamServiceImpl.READ);
        return opp;
    }

    public Page<CrmOpportunity> page(long pageNo, long pageSize, String keyword, Long customerId, Integer endStatus) {
        // 可见客户范围
        List<Long> custIds = teamService.listMemberBizIds(LeadServiceImpl.BIZ_CUSTOMER,
                StpUtil.getLoginIdAsLong());
        if (custIds.isEmpty()) {
            return new Page<>(pageNo, pageSize);
        }
        LambdaQueryWrapper<CrmOpportunity> qw = new LambdaQueryWrapper<CrmOpportunity>()
                .in(CrmOpportunity::getCustomerId, custIds);
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(CrmOpportunity::getName, keyword);
        }
        if (customerId != null) {
            qw.eq(CrmOpportunity::getCustomerId, customerId);
        }
        if (endStatus != null) {
            qw.eq(CrmOpportunity::getEndStatus, endStatus);
        }
        return oppMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(CrmOpportunity::getId));
    }

    /** 推进到指定阶段 */
    public void moveStage(Long id, Long stageId) {
        CrmOpportunity opp = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, opp.getCustomerId(), TeamServiceImpl.WRITE);
        CrmStage stage = stageMapper.selectById(stageId);
        if (stage == null || !stage.getGroupId().equals(opp.getStageGroupId())) {
            throw new BizException("阶段不属于该商机的状态组");
        }
        CrmOpportunity upd = new CrmOpportunity();
        upd.setId(id);
        upd.setStageId(stageId);
        oppMapper.updateById(upd);
    }

    /** 赢单/输单 */
    public void end(Long id, int endStatus) {
        if (endStatus != 1 && endStatus != 2) {
            throw new BizException("endStatus 仅支持 1赢单 2输单");
        }
        CrmOpportunity opp = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, opp.getCustomerId(), TeamServiceImpl.WRITE);
        if (opp.getEndStatus() != null) {
            throw new BizException("商机已结束，不能重复操作");
        }
        CrmOpportunity upd = new CrmOpportunity();
        upd.setId(id);
        upd.setEndStatus(endStatus);
        oppMapper.updateById(upd);
        // 赢单联动：客户标记成交
        if (endStatus == 1) {
            CrmCustomer custUpd = new CrmCustomer();
            custUpd.setId(opp.getCustomerId());
            custUpd.setDealStatus(1);
            customerMapper.updateById(custUpd);
        }
    }

    private CrmOpportunity require(Long id) {
        CrmOpportunity opp = oppMapper.selectById(id);
        if (opp == null) {
            throw new BizException("商机不存在");
        }
        return opp;
    }

    // ---- 状态组管理 ----

    public Long createGroup(String name, List<CrmStage> stages) {
        CrmStageGroup g = new CrmStageGroup();
        g.setName(name);
        groupMapper.insert(g);
        int sort = 0;
        for (CrmStage s : stages) {
            s.setGroupId(g.getId());
            s.setSort(sort++);
            stageMapper.insert(s);
        }
        return g.getId();
    }

    public List<CrmStageGroup> listGroups() {
        return groupMapper.selectList(null);
    }

    public List<CrmStage> listStages(Long groupId) {
        return stageMapper.selectList(new LambdaQueryWrapper<CrmStage>()
                .eq(CrmStage::getGroupId, groupId).orderByAsc(CrmStage::getSort));
    }

    public void deleteGroup(Long id) {
        Long used = oppMapper.selectCount(new LambdaQueryWrapper<CrmOpportunity>()
                .eq(CrmOpportunity::getStageGroupId, id));
        if (used != null && used > 0) {
            throw new BizException("状态组已被商机使用，不能删除");
        }
        stageMapper.delete(new LambdaQueryWrapper<CrmStage>().eq(CrmStage::getGroupId, id));
        groupMapper.deleteById(id);
    }
}
