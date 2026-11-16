/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.entity.CrmInvoice;
import com.freecrm.crm.mapper.CrmContractMapper;
import com.freecrm.crm.mapper.CrmInvoiceMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import com.freecrm.oa.service.FlowEngine;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 发票管理：开票申请（审批流）+ 台账 + 合同已开票对账 */
@RestController
@RequestMapping("/api/crm/invoice")
public class InvoiceController {

    private final CrmInvoiceMapper invoiceMapper;
    private final CrmContractMapper contractMapper;
    private final TeamService teamService;
    private final FlowEngine flowEngine;

    public InvoiceController(CrmInvoiceMapper invoiceMapper, CrmContractMapper contractMapper,
                             TeamService teamService, FlowEngine flowEngine) {
        this.invoiceMapper = invoiceMapper;
        this.contractMapper = contractMapper;
        this.teamService = teamService;
        this.flowEngine = flowEngine;
    }

    /** 开票申请：合同须已审；已开票+本次不得超过合同额（对账拦截） */
    @PostMapping("/apply")
    @SaCheckPermission("crm:invoice:save")
    public R<Long> apply(@RequestBody CrmInvoice req) {
        if (req.getContractId() == null || req.getAmount() == null
                || req.getAmount().compareTo(BigDecimal.ZERO) <= 0 || req.getTitle() == null || req.getTitle().isEmpty()) {
            throw new BizException("合同、金额、票头不能为空");
        }
        if (req.getType() != null && req.getType() == 2
                && (req.getTaxNo() == null || req.getTaxNo().isEmpty())) {
            throw new BizException("专票必须填写纳税人识别号");
        }
        CrmContract db = contractMapper.selectById(req.getContractId());
        if (db == null) {
            throw new BizException("合同不存在");
        }
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, db.getCustomerId(), TeamServiceImpl.WRITE);
        if (db.getAuditStatus() == null || db.getAuditStatus() != 2) {
            throw new BizException("合同未审核通过，不能申请开票");
        }
        BigDecimal invoiced = invoicedSum(db.getId());
        if (invoiced.add(req.getAmount()).compareTo(db.getTotalAmount()) > 0) {
            throw new BizException("开票累计将超过合同总额（已开 " + invoiced + "）");
        }
        req.setStatus(0);
        if (req.getType() == null) {
            req.setType(1);
        }
        invoiceMapper.insert(req);
        if (flowEngine.hasActiveFlow(5)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("amount", req.getAmount());
            vars.put("contractAmount", db.getTotalAmount() == null ? 0 : db.getTotalAmount());
            flowEngine.start(5, req.getId(), "开票审批：合同 " + db.getContractNo()
                    + " ￥" + req.getAmount(), StpUtil.getLoginIdAsLong(), vars);
        } else {
            CrmInvoice upd = new CrmInvoice();
            upd.setId(req.getId());
            upd.setStatus(1);
            invoiceMapper.updateById(upd);
        }
        return R.ok(req.getId());
    }

    /** 台账：指定合同 = 该合同发票；否则按可见客户范围（超管不限） */
    @GetMapping("/page")
    @SaCheckPermission("crm:invoice:list")
    public R<Page<CrmInvoice>> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) Long contractId) {
        LambdaQueryWrapper<CrmInvoice> w = new LambdaQueryWrapper<CrmInvoice>()
                .eq(contractId != null, CrmInvoice::getContractId, contractId)
                .orderByDesc(CrmInvoice::getId);
        if (contractId == null && !StpUtil.getPermissionList().contains("*")) {
            List<Long> custIds = teamService.listMemberBizIds(LeadServiceImpl.BIZ_CUSTOMER,
                    StpUtil.getLoginIdAsLong());
            if (custIds == null || custIds.isEmpty()) {
                return R.ok(new Page<>(pageNo, pageSize));
            }
            List<Long> cts = contractMapper.selectList(new LambdaQueryWrapper<CrmContract>()
                            .in(CrmContract::getCustomerId, custIds)).stream()
                    .map(CrmContract::getId).collect(Collectors.toList());
            if (cts.isEmpty()) {
                return R.ok(new Page<>(pageNo, pageSize));
            }
            w.in(CrmInvoice::getContractId, cts);
        }
        return R.ok(invoiceMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    /** 合同已开票合计（status=1 已开；待审金额前端单独看） */
    @GetMapping("/sum")
    @SaCheckPermission("crm:invoice:list")
    public R<Map<String, Object>> sum(@RequestParam Long contractId) {
        Map<String, Object> m = new HashMap<>();
        m.put("invoiced", invoicedSum(contractId));
        m.put("pending", invoiceMapper.selectList(new LambdaQueryWrapper<CrmInvoice>()
                        .eq(CrmInvoice::getContractId, contractId).eq(CrmInvoice::getStatus, 0))
                .stream().map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return R.ok(m);
    }

    private BigDecimal invoicedSum(long contractId) {
        return invoiceMapper.selectList(new LambdaQueryWrapper<CrmInvoice>()
                        .eq(CrmInvoice::getContractId, contractId).eq(CrmInvoice::getStatus, 1))
                .stream().map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
