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

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.*;
import com.freecrm.crm.mapper.*;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CRM 统计：全部限定在用户可见（团队参与）客户范围内
 */
@RestController
@RequestMapping("/api/crm/stats")
public class StatsController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CrmCustomerMapper customerMapper;
    private final CrmOpportunityMapper oppMapper;
    private final CrmContractMapper contractMapper;
    private final CrmPaymentMapper paymentMapper;
    private final CrmLeadMapper leadMapper;
    private final TeamService teamService;

    public StatsController(CrmCustomerMapper customerMapper, CrmOpportunityMapper oppMapper,
                           CrmContractMapper contractMapper, CrmPaymentMapper paymentMapper,
                           CrmLeadMapper leadMapper, TeamService teamService) {
        this.customerMapper = customerMapper;
        this.oppMapper = oppMapper;
        this.contractMapper = contractMapper;
        this.paymentMapper = paymentMapper;
        this.leadMapper = leadMapper;
        this.teamService = teamService;
    }

    /** 顶部卡片：客户数/商机金额/合同额/回款额 + 本月三指标 + 临期/逾期合同数 */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        List<Long> custIds = visibleCustomerIds();
        Map<String, Object> m = new HashMap<>();
        if (custIds.isEmpty()) {
            m.put("customers", 0); m.put("oppAmount", 0); m.put("contractAmount", 0); m.put("received", 0);
            m.put("monthNewCustomers", 0); m.put("monthContractAmount", 0); m.put("monthReceived", 0);
            m.put("expiringContracts", 0); m.put("overdueContracts", 0);
            return R.ok(m);
        }
        m.put("customers", customerMapper.selectCount(
                new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getId, custIds)));
        List<CrmOpportunity> opps = oppMapper.selectList(
                new LambdaQueryWrapper<CrmOpportunity>().in(CrmOpportunity::getCustomerId, custIds)
                        .isNull(CrmOpportunity::getEndStatus));
        m.put("oppAmount", opps.stream().map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        List<CrmContract> cts = contractMapper.selectList(
                new LambdaQueryWrapper<CrmContract>().in(CrmContract::getCustomerId, custIds));
        m.put("contractAmount", cts.stream().map(c -> c.getTotalAmount() == null ? BigDecimal.ZERO : c.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("received", cts.stream().map(c -> c.getReceivedAmount() == null ? BigDecimal.ZERO : c.getReceivedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 本月指标
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        m.put("monthNewCustomers", customerMapper.selectCount(
                new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getId, custIds)
                        .ge(CrmCustomer::getCreateTime, monthStart)));
        m.put("monthContractAmount", cts.stream()
                .filter(c -> c.getCreateTime() != null && c.getCreateTime().isAfter(monthStart)
                        && c.getAuditStatus() != null && c.getAuditStatus() == 2)
                .map(c -> c.getTotalAmount() == null ? BigDecimal.ZERO : c.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("monthReceived", cts.isEmpty() ? BigDecimal.ZERO : paymentMapper.selectList(
                        new LambdaQueryWrapper<CrmPayment>().in(CrmPayment::getContractId,
                                        cts.stream().map(CrmContract::getId).collect(Collectors.toList()))
                                .eq(CrmPayment::getAuditStatus, 1)
                                .ge(CrmPayment::getReturnTime, monthStart.toLocalDate()))
                .stream().map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 临期/逾期合同（已通过审核，按到期日）
        LocalDate today = LocalDate.now();
        List<CrmContract> audited = cts.stream()
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2 && c.getEndDate() != null)
                .collect(Collectors.toList());
        m.put("expiringContracts", audited.stream()
                .filter(c -> !c.getEndDate().isBefore(today) && !c.getEndDate().isAfter(today.plusDays(30)))
                .count());
        m.put("overdueContracts", audited.stream()
                .filter(c -> c.getEndDate().isBefore(today)
                        && (c.getReceivedAmount() == null || c.getTotalAmount() == null
                            || c.getReceivedAmount().compareTo(c.getTotalAmount()) < 0))
                .count());
        return R.ok(m);
    }

    /** 近 N 月合同额（下单月）vs 回款额（回款月，已审核） */
    @GetMapping("/payment-trend")
    public R<List<Map<String, Object>>> paymentTrend(@RequestParam(defaultValue = "6") int months) {
        List<Long> custIds = visibleCustomerIds();
        if (custIds.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        List<CrmContract> cts = contractMapper.selectList(
                new LambdaQueryWrapper<CrmContract>().in(CrmContract::getCustomerId, custIds));
        LocalDate first = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1);
        Map<String, BigDecimal> contractByMonth = new HashMap<>();
        for (CrmContract c : cts) {
            if (c.getOrderDate() != null && !c.getOrderDate().isBefore(first)
                    && c.getAuditStatus() != null && c.getAuditStatus() == 2) {
                contractByMonth.merge(c.getOrderDate().withDayOfMonth(1).format(YM),
                        c.getTotalAmount() == null ? BigDecimal.ZERO : c.getTotalAmount(), BigDecimal::add);
            }
        }
        Map<String, BigDecimal> payByMonth = new HashMap<>();
        if (!cts.isEmpty()) {
            for (CrmPayment p : paymentMapper.selectList(new LambdaQueryWrapper<CrmPayment>()
                    .in(CrmPayment::getContractId, cts.stream().map(CrmContract::getId).collect(Collectors.toList()))
                    .eq(CrmPayment::getAuditStatus, 1)
                    .isNotNull(CrmPayment::getReturnTime)
                    .ge(CrmPayment::getReturnTime, first))) {
                payByMonth.merge(p.getReturnTime().withDayOfMonth(1).format(YM),
                        p.getAmount() == null ? BigDecimal.ZERO : p.getAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> r = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            String ym = LocalDate.now().withDayOfMonth(1).minusMonths(i).format(YM);
            Map<String, Object> p = new HashMap<>();
            p.put("month", ym);
            p.put("contractAmount", contractByMonth.getOrDefault(ym, BigDecimal.ZERO));
            p.put("receivedAmount", payByMonth.getOrDefault(ym, BigDecimal.ZERO));
            r.add(p);
        }
        return R.ok(r);
    }

    /** 客户来源 / 行业分布（饼图） */
    @GetMapping("/customer-dist")
    public R<List<Map<String, Object>>> customerDist(@RequestParam(defaultValue = "source") String dim) {
        List<Long> custIds = visibleCustomerIds();
        if (custIds.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        List<CrmCustomer> list = customerMapper.selectList(
                new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getId, custIds));
        Map<Integer, Long> by = list.stream().filter(c -> dim.equals("industry")
                        ? c.getIndustry() != null : c.getSource() != null)
                .collect(Collectors.groupingBy(dim.equals("industry") ? CrmCustomer::getIndustry : CrmCustomer::getSource,
                        Collectors.counting()));
        return R.ok(by.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> p = new HashMap<>();
                    p.put("key", e.getKey());
                    p.put("count", e.getValue());
                    return p;
                })
                .collect(Collectors.toList()));
    }

    /** 近 N 天新增客户趋势 */
    @GetMapping("/customer-trend")
    public R<List<Map<String, Object>>> customerTrend(@RequestParam(defaultValue = "30") int days) {
        List<Long> custIds = visibleCustomerIds();
        if (custIds.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();
        List<CrmCustomer> list = customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                .in(CrmCustomer::getId, custIds).ge(CrmCustomer::getCreateTime, start));
        Map<String, Long> byDay = list.stream().collect(Collectors.groupingBy(
                c -> c.getCreateTime().toLocalDate().format(DF), Collectors.counting()));
        List<Map<String, Object>> r = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).format(DF);
            Map<String, Object> p = new HashMap<>();
            p.put("date", day);
            p.put("count", byDay.getOrDefault(day, 0L));
            r.add(p);
        }
        return R.ok(r);
    }

    /** 商机漏斗：各阶段商机数与金额 */
    @GetMapping("/opp-funnel")
    public R<List<Map<String, Object>>> oppFunnel() {
        List<Long> custIds = visibleCustomerIds();
        if (custIds.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        List<CrmOpportunity> opps = oppMapper.selectList(
                new LambdaQueryWrapper<CrmOpportunity>().in(CrmOpportunity::getCustomerId, custIds)
                        .isNull(CrmOpportunity::getEndStatus));
        Map<Long, List<CrmOpportunity>> byStage = opps.stream()
                .filter(o -> o.getStageId() != null)
                .collect(Collectors.groupingBy(CrmOpportunity::getStageId));
        List<Map<String, Object>> r = new ArrayList<>();
        for (Map.Entry<Long, List<CrmOpportunity>> e : byStage.entrySet()) {
            Map<String, Object> p = new HashMap<>();
            p.put("stageId", e.getKey());
            p.put("count", e.getValue().size());
            p.put("amount", e.getValue().stream()
                    .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            r.add(p);
        }
        r.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
        return R.ok(r);
    }

    /** 销售排行：按负责人统计合同额（已审）与回款额 */
    @GetMapping("/sales-rank")
    public R<List<Map<String, Object>>> salesRank() {
        List<CrmContract> cts = contractMapper.selectList(
                new LambdaQueryWrapper<CrmContract>().isNotNull(CrmContract::getTotalAmount)
                        .eq(CrmContract::getAuditStatus, 2));
        Map<Long, BigDecimal> contractByUser = new HashMap<>();
        Map<Long, BigDecimal> receivedByUser = new HashMap<>();
        for (CrmContract c : cts) {
            contractByUser.merge(c.getOwnerUserId(), c.getTotalAmount(), BigDecimal::add);
            receivedByUser.merge(c.getOwnerUserId(),
                    c.getReceivedAmount() == null ? BigDecimal.ZERO : c.getReceivedAmount(), BigDecimal::add);
        }
        return R.ok(contractByUser.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> p = new HashMap<>();
                    p.put("userId", e.getKey());
                    p.put("amount", e.getValue());
                    p.put("received", receivedByUser.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return p;
                })
                .collect(Collectors.toList()));
    }

    private List<Long> visibleCustomerIds() {
        // 超管（* 通配权限）看全局，普通用户看团队可见范围
        if (StpUtil.getPermissionList().contains("*")) {
            return customerMapper.selectList(null).stream()
                    .map(CrmCustomer::getId).collect(Collectors.toList());
        }
        return teamService.listMemberBizIds(LeadServiceImpl.BIZ_CUSTOMER,
                StpUtil.getLoginIdAsLong());
    }
}
