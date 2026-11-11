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

    /** 顶部卡片：客户数/商机金额/合同额/回款额 */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        List<Long> custIds = visibleCustomerIds();
        Map<String, Object> m = new HashMap<>();
        if (custIds.isEmpty()) {
            m.put("customers", 0); m.put("oppAmount", 0); m.put("contractAmount", 0); m.put("received", 0);
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
        return R.ok(m);
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

    /** 销售排行：按负责人统计合同额 */
    @GetMapping("/sales-rank")
    public R<List<Map<String, Object>>> salesRank() {
        List<CrmContract> cts = contractMapper.selectList(
                new LambdaQueryWrapper<CrmContract>().isNotNull(CrmContract::getTotalAmount));
        Map<Long, BigDecimal> byUser = new HashMap<>();
        for (CrmContract c : cts) {
            byUser.merge(c.getOwnerUserId(), c.getTotalAmount(), BigDecimal::add);
        }
        return R.ok(byUser.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> p = new HashMap<>();
                    p.put("userId", e.getKey());
                    p.put("amount", e.getValue());
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
