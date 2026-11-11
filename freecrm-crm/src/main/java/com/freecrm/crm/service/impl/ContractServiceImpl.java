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
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.entity.CrmPayment;
import com.freecrm.crm.entity.CrmPaymentPlan;
import com.freecrm.crm.mapper.CrmContractMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.entity.CrmContractItem;
import com.freecrm.crm.mapper.CrmContractItemMapper;
import com.freecrm.crm.mapper.CrmPaymentMapper;
import com.freecrm.crm.mapper.CrmPaymentPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 合同 + 回款计划 + 回款
 * 审核状态：0草稿 1审批中 2通过 3驳回（M4 接工作流引擎，当前由负责人直接审核）
 */
@Service
public class ContractServiceImpl {

    public static final int DRAFT = 0;
    public static final int AUDITING = 1;
    public static final int PASSED = 2;
    public static final int REJECTED = 3;

    private static final DateTimeFormatter NO_DF = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CrmContractMapper contractMapper;
    private final CrmPaymentPlanMapper planMapper;
    private final CrmPaymentMapper paymentMapper;
    private final CrmContractItemMapper itemMapper;
    private final TeamService teamService;
    private final com.freecrm.oa.service.FlowEngine flowEngine;

    public ContractServiceImpl(CrmContractMapper contractMapper, CrmPaymentPlanMapper planMapper,
                               CrmPaymentMapper paymentMapper, CrmContractItemMapper itemMapper, TeamService teamService,
                               com.freecrm.oa.service.FlowEngine flowEngine) {
        this.contractMapper = contractMapper;
        this.planMapper = planMapper;
        this.paymentMapper = paymentMapper;
        this.itemMapper = itemMapper;
        this.teamService = teamService;
        this.flowEngine = flowEngine;
    }

    // ---------- 合同 ----------

    public Long create(CrmContract c) {
        if (c.getOwnerUserId() == null) {
            c.setOwnerUserId(StpUtil.getLoginIdAsLong());
        }
        c.setAuditStatus(DRAFT);
        c.setReceivedAmount(BigDecimal.ZERO);
        c.setContractNo(genNo());
        contractMapper.insert(c);
        return c.getId();
    }

    public void update(CrmContract c) {
        CrmContract db = require(c.getId());
        if (db.getAuditStatus() != DRAFT) {
            throw new BizException("合同已进入审批流程，不能修改");
        }
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.WRITE);
        c.setContractNo(null);
        c.setReceivedAmount(null);
        c.setAuditStatus(null);
        contractMapper.updateById(c);
    }

    public void delete(Long id) {
        CrmContract db = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.OWNER);
        Long payed = paymentMapper.selectCount(new LambdaQueryWrapper<CrmPayment>().eq(CrmPayment::getContractId, id));
        if (payed != null && payed > 0) {
            throw new BizException("已有回款记录，不能删除合同");
        }
        contractMapper.deleteById(id);
    }

    public CrmContract get(Long id) {
        CrmContract c = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, c.getCustomerId(), TeamServiceImpl.READ);
        return c;
    }

    public Page<CrmContract> page(long pageNo, long pageSize, String keyword, Long customerId, Integer auditStatus) {
        List<Long> custIds = teamService.listMemberBizIds(LeadServiceImpl.BIZ_CUSTOMER,
                StpUtil.getLoginIdAsLong());
        if (custIds.isEmpty()) {
            return new Page<>(pageNo, pageSize);
        }
        LambdaQueryWrapper<CrmContract> qw = new LambdaQueryWrapper<CrmContract>()
                .in(CrmContract::getCustomerId, custIds);
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(CrmContract::getName, keyword).or().like(CrmContract::getContractNo, keyword);
        }
        if (customerId != null) {
            qw.eq(CrmContract::getCustomerId, customerId);
        }
        if (auditStatus != null) {
            qw.eq(CrmContract::getAuditStatus, auditStatus);
        }
        return contractMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(CrmContract::getId));
    }

    /** 提交审批：配置了流程走引擎，否则保留负责人直审 */
    public void submit(Long id) {
        CrmContract db = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.WRITE);
        if (db.getAuditStatus() != DRAFT) {
            throw new BizException("仅草稿可提交审批");
        }
        if (flowEngine.hasActiveFlow(1)) {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("amount", db.getTotalAmount() == null ? 0 : db.getTotalAmount());
            flowEngine.start(1, id, "合同审批：" + db.getContractNo(), StpUtil.getLoginIdAsLong(), vars);
        }
        updStatus(id, AUDITING);
    }

    public void audit(Long id, boolean pass) {
        CrmContract db = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.OWNER);
        if (db.getAuditStatus() != AUDITING) {
            throw new BizException("仅审批中状态可审核");
        }
        updStatus(id, pass ? PASSED : REJECTED);
    }

    // ---------- 回款计划 ----------

    public Long createPlan(CrmPaymentPlan plan) {
        CrmContract db = require(plan.getContractId());
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.WRITE);
        planMapper.insert(plan);
        return plan.getId();
    }

    public List<CrmPaymentPlan> listPlans(Long contractId) {
        return planMapper.selectList(new LambdaQueryWrapper<CrmPaymentPlan>()
                .eq(CrmPaymentPlan::getContractId, contractId)
                .orderByAsc(CrmPaymentPlan::getPeriodNo));
    }

    public void deletePlan(Long id) {
        planMapper.deleteById(id);
    }

    // ---------- 回款 ----------

    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(CrmPayment p) {
        CrmContract db = require(p.getContractId());
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.WRITE);
        if (db.getAuditStatus() != PASSED) {
            throw new BizException("合同未审核通过，不能录入回款");
        }
        // 回款不得超合同额
        BigDecimal received = db.getReceivedAmount() == null ? BigDecimal.ZERO : db.getReceivedAmount();
        if (received.add(p.getAmount()).compareTo(db.getTotalAmount()) > 0) {
            throw new BizException("回款累计超过合同总额");
        }
        p.setAuditStatus(0);
        paymentMapper.insert(p);
        if (flowEngine.hasActiveFlow(2)) {
            // 走审批流：通过后由 PaymentFlowCallback 累计合同已回款
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("amount", p.getAmount() == null ? 0 : p.getAmount());
            vars.put("contractAmount", db.getTotalAmount() == null ? 0 : db.getTotalAmount());
            flowEngine.start(2, p.getId(), "回款审批：合同 " + db.getContractNo()
                    + " ￥" + p.getAmount(), StpUtil.getLoginIdAsLong(), vars);
            return p.getId();
        }
        // 未配置回款审批流：直接通过并累计
        CrmContract upd = new CrmContract();
        upd.setId(db.getId());
        upd.setReceivedAmount(received.add(p.getAmount()));
        contractMapper.updateById(upd);
        CrmPayment pUpd = new CrmPayment();
        pUpd.setId(p.getId());
        pUpd.setAuditStatus(1);
        paymentMapper.updateById(pUpd);
        return p.getId();
    }

    public Page<CrmPayment> pagePayments(long pageNo, long pageSize, Long contractId) {
        LambdaQueryWrapper<CrmPayment> qw = new LambdaQueryWrapper<CrmPayment>()
                .eq(contractId != null, CrmPayment::getContractId, contractId)
                .orderByDesc(CrmPayment::getId);
        return paymentMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    public CrmPayment getPayment(Long id) {
        return paymentMapper.selectById(id);
    }

    // ---------- 合同产品明细 ----------

    /** 全量重写合同明细（保存前校验团队可写） */
    @Transactional(rollbackFor = Exception.class)
    public void saveItems(Long contractId, java.util.List<CrmContractItem> items) {
        CrmContract db = require(contractId);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.WRITE);
        itemMapper.physicalDeleteByContract(contractId);
        if (items != null) {
            for (CrmContractItem it : items) {
                it.setId(null);
                it.setContractId(contractId);
                itemMapper.insert(it);
            }
        }
    }

    public java.util.List<CrmContractItem> listItems(Long contractId) {
        return itemMapper.selectList(new LambdaQueryWrapper<CrmContractItem>()
                .eq(CrmContractItem::getContractId, contractId)
                .orderByAsc(CrmContractItem::getId));
    }

    // ---------- 私有 ----------

    private String genNo() {
        String date = java.time.LocalDate.now().format(NO_DF);
        return "HT" + date + "-" + System.nanoTime() % 100000;
    }

    private void updStatus(Long id, int status) {
        CrmContract upd = new CrmContract();
        upd.setId(id);
        upd.setAuditStatus(status);
        contractMapper.updateById(upd);
    }

    private CrmContract require(Long id) {
        CrmContract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BizException("合同不存在");
        }
        return c;
    }
}
