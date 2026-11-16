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
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.entity.CrmSalesTarget;
import com.freecrm.crm.mapper.CrmContractMapper;
import com.freecrm.crm.mapper.CrmSalesTargetMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 业绩目标：月度目标设置 + 完成率（实际=当月已审合同签约额，order_date 口径） */
@RestController
@RequestMapping("/api/crm/target")
public class TargetController {

    private static final Pattern YM = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final CrmSalesTargetMapper targetMapper;
    private final CrmContractMapper contractMapper;

    public TargetController(CrmSalesTargetMapper targetMapper, CrmContractMapper contractMapper) {
        this.targetMapper = targetMapper;
        this.contractMapper = contractMapper;
    }

    /** 目标管理页：该月有目标的用户 + 实际完成 + 完成率（全员列表由前端用 simple-list 合并） */
    @GetMapping("/list")
    @SaCheckPermission("crm:target:list")
    public R<List<Map<String, Object>>> list(@RequestParam String month) {
        checkMonth(month);
        Map<Long, BigDecimal> actual = actualByUser(month);
        return R.ok(rows(month, actual));
    }

    /** 批量保存该月目标（整月物理删后重建） */
    @PostMapping("/save")
    @SaCheckPermission("crm:target:save")
    public R<Void> save(@RequestBody SaveReq req) {
        checkMonth(req.getMonth());
        targetMapper.physicalDeleteByMonth(req.getMonth());
        if (req.getItems() != null) {
            for (Item it : req.getItems()) {
                if (it.getUserId() == null || it.getAmount() == null
                        || it.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                CrmSalesTarget t = new CrmSalesTarget();
                t.setUserId(it.getUserId());
                t.setMonth(req.getMonth());
                t.setAmount(it.getAmount());
                t.setRemark(it.getRemark());
                targetMapper.insert(t);
            }
        }
        return R.ok(null);
    }

    /** 看板：我的目标进度 + 目标达成榜（与 sales-rank 同口径，登录即见） */
    @GetMapping("/progress")
    public R<Map<String, Object>> progress(@RequestParam(required = false) String month) {
        String m = (month == null || month.isEmpty())
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : month;
        checkMonth(m);
        Map<Long, BigDecimal> actual = actualByUser(m);
        List<Map<String, Object>> rows = rows(m, actual);
        long me = StpUtil.getLoginIdAsLong();
        Map<String, Object> mine = rows.stream().filter(r -> ((Number) r.get("userId")).longValue() == me)
                .findFirst().orElse(null);
        Map<String, Object> r = new HashMap<>();
        r.put("mine", mine);
        r.put("rank", rows);
        return R.ok(r);
    }

    private List<Map<String, Object>> rows(String month, Map<Long, BigDecimal> actual) {
        List<CrmSalesTarget> ts = targetMapper.selectList(
                new LambdaQueryWrapper<CrmSalesTarget>().eq(CrmSalesTarget::getMonth, month));
        return ts.stream().map(t -> {
            Map<String, Object> p = new HashMap<>();
            p.put("userId", t.getUserId());
            p.put("target", t.getAmount());
            p.put("remark", t.getRemark());
            BigDecimal act = actual.getOrDefault(t.getUserId(), BigDecimal.ZERO);
            p.put("actual", act);
            p.put("rate", t.getAmount().compareTo(BigDecimal.ZERO) > 0
                    ? act.multiply(new BigDecimal(100)).divide(t.getAmount(), 1, RoundingMode.HALF_UP)
                    : null);
            return p;
        }).sorted(Comparator.comparing((Map<String, Object> p) -> (BigDecimal) p.get("rate"),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /** 当月实际签约：已审合同按下单日所在月聚合到负责人 */
    private Map<Long, BigDecimal> actualByUser(String month) {
        LocalDate first = LocalDate.parse(month + "-01");
        List<CrmContract> cts = contractMapper.selectList(new LambdaQueryWrapper<CrmContract>()
                .eq(CrmContract::getAuditStatus, 2)
                .isNotNull(CrmContract::getOrderDate)
                .between(CrmContract::getOrderDate, first, first.withDayOfMonth(first.lengthOfMonth())));
        Map<Long, BigDecimal> m = new HashMap<>();
        for (CrmContract c : cts) {
            m.merge(c.getOwnerUserId(),
                    c.getTotalAmount() == null ? BigDecimal.ZERO : c.getTotalAmount(), BigDecimal::add);
        }
        return m;
    }

    private void checkMonth(String month) {
        if (month == null || !YM.matcher(month).matches()) {
            throw new IllegalArgumentException("月份格式应为 yyyy-MM");
        }
    }

    @lombok.Data
    public static class SaveReq {
        private String month;
        private List<Item> items;
    }

    @lombok.Data
    public static class Item {
        private Long userId;
        private BigDecimal amount;
        private String remark;
    }
}
