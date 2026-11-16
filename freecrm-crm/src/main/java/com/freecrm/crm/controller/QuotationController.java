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
import com.freecrm.crm.entity.CrmContractItem;
import com.freecrm.crm.entity.CrmQuotation;
import com.freecrm.crm.entity.CrmQuotationItem;
import com.freecrm.crm.mapper.CrmQuotationItemMapper;
import com.freecrm.crm.mapper.CrmQuotationMapper;
import com.freecrm.crm.service.impl.ContractServiceImpl;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import com.freecrm.crm.entity.CrmContract;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 报价单：商机→报价→合同链路 */
@RestController
@RequestMapping("/api/crm/quotation")
public class QuotationController {

    private static final DateTimeFormatter D8 = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CrmQuotationMapper quotationMapper;
    private final CrmQuotationItemMapper itemMapper;
    private final com.freecrm.crm.mapper.CrmCustomerMapper customerMapper;
    private final ContractServiceImpl contractService;
    private final TeamService teamService;

    public QuotationController(CrmQuotationMapper quotationMapper, CrmQuotationItemMapper itemMapper,
                               com.freecrm.crm.mapper.CrmCustomerMapper customerMapper,
                               ContractServiceImpl contractService, TeamService teamService) {
        this.quotationMapper = quotationMapper;
        this.itemMapper = itemMapper;
        this.customerMapper = customerMapper;
        this.contractService = contractService;
        this.teamService = teamService;
    }

    /** 保存（新建/编辑），明细全量重写，total = 明细合计 - 折让 */
    @PostMapping("/save")
    @SaCheckPermission("crm:quote:save")
    @Transactional(rollbackFor = Exception.class)
    public R<Long> save(@RequestBody SaveReq req) {
        if (req.getQuotation() == null || req.getQuotation().getCustomerId() == null) {
            throw new BizException("客户不能为空");
        }
        CrmQuotation q = req.getQuotation();
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, q.getCustomerId(), TeamServiceImpl.WRITE);

        BigDecimal sum = BigDecimal.ZERO;
        for (CrmQuotationItem it : req.getItems() == null ? new ArrayList<CrmQuotationItem>() : req.getItems()) {
            if (it.getQuantity() == null) {
                it.setQuantity(BigDecimal.ONE);
            }
            if (it.getPrice() == null) {
                it.setPrice(BigDecimal.ZERO);
            }
            it.setAmount(it.getPrice().multiply(it.getQuantity()));
            sum = sum.add(it.getAmount());
        }
        BigDecimal discount = q.getDiscount() == null ? BigDecimal.ZERO : q.getDiscount();
        if (discount.compareTo(sum) > 0) {
            throw new BizException("折让不能超过明细合计");
        }
        q.setTotalAmount(sum.subtract(discount));

        if (q.getId() == null) {
            q.setQuoteNo("BJ" + LocalDate.now().format(D8) + "-" + System.nanoTime() % 100000);
            q.setStatus(0);
            q.setOwnerUserId(StpUtil.getLoginIdAsLong());
            quotationMapper.insert(q);
        } else {
            CrmQuotation db = require(q.getId());
            if (db.getStatus() != 0) {
                throw new BizException("仅草稿可编辑");
            }
            q.setQuoteNo(null);
            q.setStatus(null);
            quotationMapper.updateById(q);
        }
        itemMapper.deleteByQuotation(q.getId());
        if (req.getItems() != null) {
            for (CrmQuotationItem it : req.getItems()) {
                it.setId(null);
                it.setQuotationId(q.getId());
                itemMapper.insert(it);
            }
        }
        return R.ok(q.getId());
    }

    @GetMapping("/page")
    @SaCheckPermission("crm:quote:list")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNo,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) Long customerId,
                                             @RequestParam(required = false) Long opportunityId) {
        LambdaQueryWrapper<CrmQuotation> w = new LambdaQueryWrapper<CrmQuotation>()
                .eq(customerId != null, CrmQuotation::getCustomerId, customerId)
                .eq(opportunityId != null, CrmQuotation::getOpportunityId, opportunityId)
                .orderByDesc(CrmQuotation::getId);
        if (customerId == null && !StpUtil.getPermissionList().contains("*")) {
            List<Long> custIds = teamService.listMemberBizIds(LeadServiceImpl.BIZ_CUSTOMER,
                    StpUtil.getLoginIdAsLong());
            if (custIds == null || custIds.isEmpty()) {
                return R.ok(new Page<>(pageNo, pageSize));
            }
            w.in(CrmQuotation::getCustomerId, custIds);
        }
        Page<CrmQuotation> pg = quotationMapper.selectPage(new Page<>(pageNo, pageSize), w);
        Page<Map<String, Object>> r = new Page<>(pageNo, pageSize, pg.getTotal());
        r.setRecords(pg.getRecords().stream().map(this::withCustomer).collect(Collectors.toList()));
        return R.ok(r);
    }

    private Map<String, Object> withCustomer(CrmQuotation q) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("quoteNo", q.getQuoteNo());
        m.put("customerId", q.getCustomerId());
        m.put("customerName", custName(q.getCustomerId()));
        m.put("opportunityId", q.getOpportunityId());
        m.put("ownerUserId", q.getOwnerUserId());
        m.put("totalAmount", q.getTotalAmount());
        m.put("discount", q.getDiscount());
        m.put("status", q.getStatus());
        m.put("contractId", q.getContractId());
        m.put("remark", q.getRemark());
        m.put("createTime", q.getCreateTime());
        return m;
    }

    @GetMapping("/get")
    @SaCheckPermission("crm:quote:list")
    public R<Map<String, Object>> get(@RequestParam Long id) {
        CrmQuotation q = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, q.getCustomerId(), TeamServiceImpl.READ);
        Map<String, Object> m = withCustomer(q);
        m.put("customerName", custName(q.getCustomerId()));
        m.put("items", itemMapper.selectList(new LambdaQueryWrapper<CrmQuotationItem>()
                .eq(CrmQuotationItem::getQuotationId, id).orderByAsc(CrmQuotationItem::getId)));
        return R.ok(m);
    }

    private String custName(Long id) {
        com.freecrm.crm.entity.CrmCustomer c = customerMapper.selectById(id);
        return c == null ? ("客户" + id) : c.getName();
    }

    /** 确认（草稿→已确认，客户可读范围内） */
    @PutMapping("/confirm")
    @SaCheckPermission("crm:quote:save")
    public R<Void> confirm(@RequestParam Long id) {
        CrmQuotation q = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, q.getCustomerId(), TeamServiceImpl.WRITE);
        if (q.getStatus() != 0) {
            throw new BizException("仅草稿可确认");
        }
        CrmQuotation upd = new CrmQuotation();
        upd.setId(id);
        upd.setStatus(1);
        quotationMapper.updateById(upd);
        return R.ok();
    }

    /** 作废（草稿/已确认 → 已作废） */
    @PutMapping("/void")
    @SaCheckPermission("crm:quote:save")
    public R<Void> voidQuote(@RequestParam Long id) {
        CrmQuotation q = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, q.getCustomerId(), TeamServiceImpl.WRITE);
        if (q.getStatus() != 0 && q.getStatus() != 1) {
            throw new BizException("该状态不可作废");
        }
        CrmQuotation upd = new CrmQuotation();
        upd.setId(id);
        upd.setStatus(3);
        quotationMapper.updateById(upd);
        return R.ok();
    }

    /** 转合同：已确认报价 → 生成合同草稿（带金额与明细），报价置已转合同 */
    @PutMapping("/to-contract")
    @SaCheckPermission("crm:quote:save")
    @Transactional(rollbackFor = Exception.class)
    public R<Long> toContract(@RequestParam Long id) {
        CrmQuotation q = require(id);
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, q.getCustomerId(), TeamServiceImpl.WRITE);
        if (q.getStatus() != 1) {
            throw new BizException("仅已确认报价可转合同");
        }
        CrmContract c = new CrmContract();
        c.setCustomerId(q.getCustomerId());
        c.setOpportunityId(q.getOpportunityId());
        c.setName("报价 " + q.getQuoteNo() + " 转合同");
        c.setTotalAmount(q.getTotalAmount());
        c.setOrderDate(LocalDate.now());
        Long cid = contractService.create(c);
        List<CrmQuotationItem> qItems = itemMapper.selectList(
                new LambdaQueryWrapper<CrmQuotationItem>().eq(CrmQuotationItem::getQuotationId, id));
        List<CrmContractItem> cItems = qItems.stream().map(i -> {
            CrmContractItem it = new CrmContractItem();
            it.setProductId(i.getProductId());
            it.setQuantity(i.getQuantity());
            it.setPrice(i.getPrice());
            it.setAmount(i.getAmount());
            return it;
        }).collect(Collectors.toList());
        if (!cItems.isEmpty()) {
            contractService.saveItems(cid, cItems);
        }
        CrmQuotation upd = new CrmQuotation();
        upd.setId(id);
        upd.setStatus(2);
        upd.setContractId(cid);
        quotationMapper.updateById(upd);
        return R.ok(cid);
    }

    private CrmQuotation require(Long id) {
        CrmQuotation q = quotationMapper.selectById(id);
        if (q == null) {
            throw new BizException("报价单不存在");
        }
        return q;
    }

    @lombok.Data
    public static class SaveReq {
        private CrmQuotation quotation;
        private List<CrmQuotationItem> items;
    }
}
